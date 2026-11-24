#!/usr/bin/env python3
"""Enable the repository's versioned Git hooks for this local checkout."""
from __future__ import annotations

import os
import subprocess
import sys
from pathlib import Path


def main() -> int:
    root_result = subprocess.run(
        ["git", "rev-parse", "--show-toplevel"],
        check=False, capture_output=True, text=True,
    )
    if root_result.returncode:
        print("Run this installer from inside a KuroToon Git checkout.", file=sys.stderr)
        return 2

    root = Path(root_result.stdout.strip())
    hook = root / ".githooks" / "commit-msg"
    if not hook.is_file():
        print(f"Expected hook was not found: {hook}", file=sys.stderr)
        return 2

    configured = subprocess.run(
        ["git", "config", "--local", "core.hooksPath", ".githooks"],
        cwd=root, check=False,
    )
    if configured.returncode:
        print("Could not configure core.hooksPath.", file=sys.stderr)
        return configured.returncode

    if os.name != "nt":
        hook.chmod(hook.stat().st_mode | 0o111)

    print("Installed KuroToon Git hooks for this checkout.")
    print("New local commits will have AI-tool credit trailers removed.")
    print("Existing commits and commits created outside this checkout are unchanged.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
