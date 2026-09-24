"""Validate Big Orbit's complete Markdown inventory and local links."""

from __future__ import annotations

import re
from pathlib import Path
from urllib.parse import unquote

ROOT = Path(__file__).resolve().parents[1]
MAP = ROOT / "docs" / "DOCUMENTATION-MAP.md"
EXCLUDED = {".git", ".gradle", "build", "verification-output"}
INVENTORY = re.compile(r"^- `([^`]+\.md)`", re.MULTILINE)
LINK = re.compile(r"(?<!!)\[[^\]]+\]\(([^)]+)\)")


def _markdown_files() -> set[str]:
    return {
        path.relative_to(ROOT).as_posix()
        for path in ROOT.rglob("*.md")
        if not EXCLUDED.intersection(path.relative_to(ROOT).parts)
    }


def _broken_links(files: set[str]) -> list[str]:
    failures: list[str] = []
    for relative in sorted(files):
        source = ROOT / relative
        for raw in LINK.findall(source.read_text(encoding="utf-8")):
            target = raw.strip().strip("<>").split("#", 1)[0]
            if not target or "://" in target or target.startswith(("mailto:", "/")):
                continue
            if not (source.parent / unquote(target)).resolve().exists():
                failures.append(f"{relative}: missing link target {target}")
    return failures


def main() -> None:
    """Fail when a document is unowned, stale in the map, or locally broken."""

    actual = _markdown_files()
    listed = set(INVENTORY.findall(MAP.read_text(encoding="utf-8")))
    failures = [f"unlisted Markdown: {path}" for path in sorted(actual - listed)]
    failures.extend(f"missing listed Markdown: {path}" for path in sorted(listed - actual))
    failures.extend(_broken_links(actual))
    if failures:
        raise SystemExit("\n".join(failures))
    print(f"Big Orbit documentation inventory and links are valid ({len(actual)} files).")


if __name__ == "__main__":
    main()

