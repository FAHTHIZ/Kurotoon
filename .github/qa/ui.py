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


def dump():
    adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    xml = adb("shell", "cat", "/sdcard/ui.xml")
    try:
        return ET.fromstring(xml[xml.index("<"):])
    except Exception:
        return None


def nodes(root):
    if root is None:
        return []
    out = []
    for n in root.iter("node"):
        b = re.findall(r"\d+", n.get("bounds", ""))
        if len(b) == 4:
            x1, y1, x2, y2 = map(int, b)
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
    png = subprocess.run(["adb", "exec-out", "screencap", "-p"], capture_output=True, timeout=60).stdout
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
            elif cmd == "tapxy":
                adb("shell", "input", "tap", args[0], args[1]); time.sleep(1.5)
            elif cmd == "long":
                pos = find(args[0])
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
