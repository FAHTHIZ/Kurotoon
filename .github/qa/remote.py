"""Drive the emulator from a branch, one batch of journey commands at a time.

Push a `cmd.txt` to the `qa-cmd` branch: the first line is a batch id, the rest are ui.py commands
(plus `rebuild` to pull main and reinstall, and `quit`). Each batch's screenshots and log are pushed
to the `qa-live` branch under `<id>/`, so a person (or an agent) can use the app step by step.
"""
import os, subprocess, sys, time

sys.path.insert(0, os.path.dirname(__file__))
import ui  # noqa: E402

REPO = os.environ["GITHUB_REPOSITORY"]
TOKEN = os.environ["GH_TOKEN"]
REMOTE = f"https://x-access-token:{TOKEN}@github.com/{REPO}"
LIVE = "qa-live"
DEADLINE = time.time() + float(os.environ.get("MINUTES", "300")) * 60


def sh(*a, **k):
    return subprocess.run(a, capture_output=True, text=True, **k)


def head(branch):
    out = sh("git", "ls-remote", REMOTE, f"refs/heads/{branch}").stdout.split()
    return out[0] if out else None


def setup_live():
    os.makedirs("live", exist_ok=True)
    sh("git", "init", "-q", "-b", LIVE, cwd="live")
    sh("git", "config", "user.name", "github-actions[bot]", cwd="live")
    sh("git", "config", "user.email", "41898237+github-actions[bot]@users.noreply.github.com", cwd="live")


def publish(msg):
    sh("git", "add", "-A", cwd="live")
    sh("git", "commit", "-qm", msg, cwd="live")
    for _ in range(3):
        if sh("git", "push", "-qf", REMOTE, f"{LIVE}:{LIVE}", cwd="live").returncode == 0:
            return
        time.sleep(3)


def rebuild(out):
    log = sh("bash", "-c", "git pull -q origin main && ./gradlew -q assembleDebug 2>&1 | tail -30 && "
             "adb install -r -g $(ls app/build/outputs/apk/debug/*x86_64*.apk | head -1)")
    open(f"{out}/rebuild.txt", "w").write(log.stdout + log.stderr)


def main():
    setup_live()
    open("live/READY", "w").write(time.ctime())
    publish("ready")
    last = head("qa-cmd")
    while time.time() < DEADLINE:
        time.sleep(4)
        sha = head("qa-cmd")
        if not sha or sha == last:
            continue
        last = sha
        sh("git", "fetch", "-q", REMOTE, f"{sha}")
        text = sh("git", "show", f"{sha}:cmd.txt").stdout
        lines = text.splitlines()
        if not lines:
            continue
        batch = lines[0].strip() or sha[:7]
        out = f"live/{batch}"
        os.makedirs(out, exist_ok=True)
        ui.OUT = out
        ui.log = open(f"{out}/journey.log", "w")
        ui.step = 0
        body = [l for l in lines[1:]]
        if any(l.strip() == "quit" for l in body):
            publish(f"bye {batch}")
            return
        if any(l.strip() == "rebuild" for l in body):
            rebuild(out)
            body = [l for l in body if l.strip() != "rebuild"]
        path = f"{out}/cmd.txt"
        open(path, "w").write("\n".join(body) + "\n")
        try:
            ui.run(path)
        except Exception as e:  # keep the session alive whatever happens
            ui.say(f"!! batch failed: {e}")
        ui.shot("end")
        ui.log.close()
        crash = sh("adb", "logcat", "-d", "-b", "crash").stdout
        open(f"{out}/crash.txt", "w").write(crash[-20000:])
        publish(f"batch {batch}")


if __name__ == "__main__":
    main()
