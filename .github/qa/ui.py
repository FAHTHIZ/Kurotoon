"""Tiny adb driver that walks through a journey file and records what a user would see."""
import os, re, subprocess, sys, time, shlex, xml.etree.ElementTree as ET

OUT = os.environ.get("OUT", "qa-out")
os.makedirs(OUT, exist_ok=True)
log = open(f"{OUT}/journey.log", "w")
step = 0


def adb(*a, timeout=120):
    return subprocess.run(["adb", *a], capture_output=True, text=True, timeout=timeout).stdout


def say(msg):
    print(msg); log.write(msg + "\n"); log.flush()


def raw_dump():
    # uiautomator itself crashes now and then on a tree that changes mid-dump; try a few times
    for attempt in range(3):
        adb("shell", "rm", "-f", "/sdcard/ui.xml")
        adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
        xml = adb("shell", "cat", "/sdcard/ui.xml")
        try:
            return ET.fromstring(xml[xml.index("<"):])
        except Exception:
            time.sleep(1 + attempt)
    return None


def dump():
    """UI tree, after clearing any system "isn't responding" dialog the slow emulator throws up."""
    for _ in range(4):
        root = raw_dump()
        hit = None
        for t, d, x, y, n in nodes(root):
            if "isn't responding" in t:
                hit = True
            if hit and t == "Wait":
                adb("shell", "input", "tap", str(x), str(y)); time.sleep(2); break
        else:
            return root
    return raw_dump()


def app_bounds():
    """Screen rect apps may use (between the status and navigation bars); follows rotation."""
    m = re.search(r"mAppBounds=Rect\((\d+), (\d+) - (\d+), (\d+)\)", adb("shell", "dumpsys", "window", "displays"))
    return tuple(map(int, m.groups())) if m else None


def nodes(root):
    """(text, desc, x, y, node) for every node.

    uiautomator clips bounds to the display's *usable* height, which starts at 0 rather than below
    the status bar, so on an edge-to-edge app the strip just above the navigation bar (bottom bars,
    the tab dock, the onboarding Next button) comes back as [0,0][0,0]. Those nodes are mapped back
    into that strip, under the nearest ancestor that still has real bounds.
    """
    if root is None:
        return []
    parent = {c: p for p in root.iter() for c in p}
    clip = None
    out = []
    for n in root.iter("node"):
        b = re.findall(r"\d+", n.get("bounds", ""))
        if len(b) != 4:
            continue
        x1, y1, x2, y2 = map(int, b)
        if x2 <= x1 or y2 <= y1:
            a = parent.get(n)
            while a is not None:
                ab = list(map(int, re.findall(r"\d+", a.get("bounds", "")) or [0, 0, 0, 0]))
                if len(ab) == 4 and ab[2] > ab[0] and ab[3] > ab[1]:
                    break
                a = parent.get(a)
            if a is None:
                continue
            if clip is None:
                top = root.find("node")
                clip = int(re.findall(r"\d+", top.get("bounds"))[3]) if top is not None else 0
                bounds = app_bounds()
                clip = (clip, bounds[3] if bounds else clip)
            if ab[3] >= clip[0] - 2 and clip[1] > clip[0]:
                out.append((n.get("text", ""), n.get("content-desc", ""), (ab[0] + ab[2]) // 2, (clip[0] + clip[1]) // 2, n))
            continue
        out.append((n.get("text", ""), n.get("content-desc", ""), (x1 + x2) // 2, (y1 + y2) // 2, n))
    return out


def find(label, exact=False, root=None):
    root = root if root is not None else dump()
    want = label.lower()
    for t, d, x, y, n in nodes(root):
        for v in (t, d):
            v = v.lower()
            if (v == want) if exact else (want in v and v):
                return x, y
    return None


def shot(name=None):
    global step
    step += 1
    name = f"{step:03d}_{name or 'screen'}"
    png = b""
    for _ in range(3):
        try:
            png = subprocess.run(["adb", "exec-out", "screencap", "-p"], capture_output=True, timeout=60).stdout
            break
        except subprocess.TimeoutExpired:
            say("  .. screencap timed out, waiting for the emulator")
            subprocess.run(["adb", "wait-for-device"], timeout=180)
    open(f"{OUT}/{name}.png", "wb").write(png)
    root = dump()
    texts = [t or d for t, d, *_ in nodes(root) if (t or d)]
    say(f"  [shot {name}] visible: {texts[:60]}")


def run(path):
    for raw in open(path):
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        a = shlex.split(line)
        cmd, args = a[0], a[1:]
        say(f"> {line}")
        try:
            if cmd == "shot":
                shot(args[0] if args else None)
            elif cmd in ("tap", "tapx"):
                pos = None
                end = time.time() + float(args[1] if len(args) > 1 else 8)
                while pos is None and time.time() < end:
                    pos = find(args[0], exact=cmd == "tapx")
                    if pos is None:
                        time.sleep(1)
                if pos:
                    adb("shell", "input", "tap", str(pos[0]), str(pos[1]))
                    time.sleep(1.5)
                else:
                    say(f"  !! not found: {args[0]}")
                    shot("notfound")
            elif cmd == "tapuntil":
                # tapuntil LABEL EXPECT [tries]: tap LABEL until EXPECT shows up (slow emulators drop taps)
                for _ in range(int(args[2]) if len(args) > 2 else 5):
                    if find(args[1]):
                        break
                    pos = find(args[0], exact=True) or find(args[0])
                    if pos:
                        adb("shell", "input", "tap", str(pos[0]), str(pos[1]))
                    time.sleep(4)
                say("  found" if find(args[1]) else f"  !! never reached {args[1]}")
            elif cmd == "tapnear":
                # tapnear LABEL TARGET: tap the TARGET (text or description) on the same row as LABEL
                root = dump()
                anchor = find(args[0], exact=True, root=root) or find(args[0], root=root)
                hits = [(abs(y - anchor[1]), x, y) for t, d, x, y, n in nodes(root)
                        if anchor and args[1].lower() in (t.lower(), d.lower())]
                if hits:
                    _, x, y = min(hits)
                    adb("shell", "input", "tap", str(x), str(y)); time.sleep(1.5)
                else:
                    say(f"  !! not found: {args[1]} near {args[0]}")
            elif cmd == "dumpxml":
                # dumpxml NAME: keep the raw accessibility tree next to the screenshots
                root = raw_dump()
                if root is not None:
                    ET.ElementTree(root).write(f"{OUT}/{args[0]}.xml")
            elif cmd == "coords":
                for t, d, x, y, n in nodes(dump()):
                    if t or d:
                        say(f"  {x},{y} {(t or d)[:50]}")
            elif cmd == "tapif":
                pos = find(args[0], exact=True) or find(args[0])
                if pos:
                    adb("shell", "input", "tap", str(pos[0]), str(pos[1])); time.sleep(2)
            elif cmd == "tapxy":
                adb("shell", "input", "tap", args[0], args[1]); time.sleep(1.5)
            elif cmd == "long":
                # long LABEL, or long X Y for a spot with no label (a reader page)
                pos = (int(args[0]), int(args[1])) if len(args) > 1 and args[0].isdigit() else find(args[0])
                if pos:
                    adb("shell", "input", "swipe", str(pos[0]), str(pos[1]), str(pos[0]), str(pos[1]), "900"); time.sleep(1.5)
                else:
                    say(f"  !! not found: {args[0]}")
            elif cmd == "wait":
                end = time.time() + float(args[1] if len(args) > 1 else 20)
                while time.time() < end and not find(args[0]):
                    time.sleep(1)
                say("  found" if find(args[0]) else f"  !! timeout waiting for {args[0]}")
            elif cmd == "type":
                adb("shell", "input", "text", args[0].replace(" ", "%s")); time.sleep(1)
            elif cmd == "key":
                adb("shell", "input", "keyevent", args[0]); time.sleep(1)
            elif cmd == "back":
                adb("shell", "input", "keyevent", "4"); time.sleep(1.2)
            elif cmd == "swipe":
                adb("shell", "input", "swipe", *args); time.sleep(1.2)
            elif cmd == "scrollup":
                adb("shell", "input", "swipe", "540", "1800", "540", "700", "400"); time.sleep(1.2)
            elif cmd == "scrolldown":
                adb("shell", "input", "swipe", "540", "700", "540", "1800", "400"); time.sleep(1.2)
            elif cmd == "sleep":
                time.sleep(float(args[0]))
            elif cmd == "start":
                adb("shell", "monkey", "-p", args[0], "-c", "android.intent.category.LAUNCHER", "1"); time.sleep(4)
            elif cmd == "stop":
                adb("shell", "am", "force-stop", args[0])
            elif cmd == "clear":
                adb("shell", "pm", "clear", args[0])
            elif cmd == "view":
                adb("shell", "am", "start", "-a", "android.intent.action.VIEW", "-d", args[0], *args[1:]); time.sleep(3)
            elif cmd == "sh":
                say(adb("shell", *args))
            elif cmd == "install":
                say(adb("install", "-r", "-g", args[0], timeout=300))
            elif cmd == "rotate":
                adb("shell", "settings", "put", "system", "accelerometer_rotation", "0")
                adb("shell", "settings", "put", "system", "user_rotation", args[0]); time.sleep(2)
            else:
                say(f"  !! unknown command {cmd}")
        except Exception as e:
            say(f"  !! error {e}")


if __name__ == "__main__":
    for p in sys.argv[1:]:
        run(p)
