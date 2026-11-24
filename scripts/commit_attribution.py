#!/usr/bin/env python3
"""Remove AI-tool attribution trailers from a pending Git commit message.

Human co-authors, sign-offs, issue references, and the commit body are kept.
The script is used by the repository's optional Git commit-msg hook.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

AI_NAMES = (
    "perplexity", "claude", "anthropic", "chatgpt", "openai", "codex",
    "github copilot", "copilot", "gemini", "cursor", "devin", "aider",
    "cline", "roo code", "kilo code", "continue", "windsurf", "codeium",
    "amazon q", "codewhisperer", "kiro", "qodo", "sourcegraph cody",
    "amp", "factory", "droid", "opencode", "goose", "hermes", "qwen",
    "deepseek", "mistral", "grok", "llama", "ollama", "sweep",
    "coderabbit", "replit", "lovable", "bolt", "manus", "openhands",
    "warp", "zed", "augment", "trae", "junie", "jetbrains ai", "gitlab duo",
)
NAME_RE = re.compile(
    r"(?:^|[^a-z0-9])(?:"
    + "|".join(re.escape(name).replace(r"\ ", r"[\s_-]*")
              for name in sorted(AI_NAMES, key=len, reverse=True))
    + r")(?:$|[^a-z0-9])",
    re.IGNORECASE,
)
BOT_RE = re.compile(r"\[bot\]|(?:^|[.+_-])(?:no-?reply|agent|assistant|copilot|claude|codex|gemini|devin)(?:[.+_-]|@)", re.I)
AI_DOMAIN_RE = re.compile(
    r"@(?:[a-z0-9-]+\.)*(?:perplexity\.ai|anthropic\.com|openai\.com|"
    r"cursor\.(?:com|sh)|cognition(?:-labs)?\.(?:ai|com)|devin\.ai|"
    r"aider\.chat|codeium\.com|windsurf\.com|tabnine\.com|deepseek\.com|"
    r"mistral\.ai|moonshot\.(?:ai|cn)|qwen\.ai|x\.ai|replit\.com|"
    r"factory\.ai|ampcode\.com|coderabbit\.ai|lovable\.dev)$",
    re.I,
)
TRAILER_RE = re.compile(
    r"^(?:co-authored-by|co-author|coauthored-by|co-developed-by|"
    r"assisted-by|generated-by|created-by|made-with|written-by|"
    r"authored-by|ai-assisted|ai-generated|with-help-from|helped-by|"
    r"signed-off-by|reviewed-by|suggested-by)\s*:\s*(.+)$",
    re.I,
)
GENERATED_RE = re.compile(
    r"^\s*(?:<sub>\s*)?(?:[\U0001F300-\U0001FAFF]\s*)?"
    r"(?:generated|created|written|made|built|authored|drafted|produced|"
    r"co-authored|assisted)\s+(?:with|by|using|via|in)\b.*$",
    re.I,
)


def is_ai_credit(line: str) -> bool:
    """Return True only when a trailer/footer identifies an AI tool or bot."""
    trailer = TRAILER_RE.match(line.strip())
    if trailer:
        value = trailer.group(1).strip()
        address_match = re.search(r"<([^<>]+)>", value)
        address = address_match.group(1).strip() if address_match else ""
        name = value[:address_match.start()].strip() if address_match else value
        if "[bot]" in value.lower() or AI_DOMAIN_RE.search(address):
            return True
        # Keep a person's own address, even when their name resembles a tool.
        if address and not BOT_RE.search(address):
            return False
        return bool(NAME_RE.search(name) or BOT_RE.search(address or value))

    if GENERATED_RE.match(line):
        return bool(NAME_RE.search(re.sub(r"https?://\S+", " ", line)))
    return False


def strip_ai_attribution(message: str) -> str:
    """Remove AI credit lines while preserving all human-authored message text."""
    lines = message.splitlines(keepends=True)
    kept = [line for line in lines if not is_ai_credit(line.rstrip("\r\n"))]
    if len(kept) == len(lines):
        return message

    while kept and not kept[-1].strip():
        kept.pop()
    while kept and re.fullmatch(r"\s*(?:-{3,}|\*{3,}|_{3,})\s*", kept[-1]):
        kept.pop()
        while kept and not kept[-1].strip():
            kept.pop()

    result = "".join(kept)
    if message.endswith(("\n", "\r")) and result and not result.endswith(("\n", "\r")):
        result += "\n"
    return result


def main() -> int:
    if len(sys.argv) != 2:
        print("usage: commit_attribution.py COMMIT_MESSAGE_FILE", file=sys.stderr)
        return 2

    path = Path(sys.argv[1])
    try:
        original = path.read_text(encoding="utf-8")
        cleaned = strip_ai_attribution(original)
        if cleaned != original:
            path.write_text(cleaned, encoding="utf-8", newline="")
    except OSError as exc:
        print(f"commit attribution filter: {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
