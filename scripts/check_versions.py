"""Fail fast when Big Orbit drifts from the shared 1.3 release train."""

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def _expect(actual: str, expected: str, source: str) -> None:
    if actual != expected:
        raise SystemExit(f"{source} is {actual!r}; expected {expected!r}")


def _value(path: Path, pattern: str, source: str) -> str:
    match = re.search(pattern, path.read_text(encoding="utf-8"), re.MULTILINE)
    if match is None:
        raise SystemExit(f"{source} version was not found")
    return match.group(1)


def main() -> None:
    version = (ROOT / "VERSION").read_text(encoding="utf-8").strip()
    train = json.loads((ROOT / "release-train.json").read_text(encoding="utf-8"))
    for key in ("release_train", "little_orbit_version", "big_orbit_version"):
        _expect(str(train[key]), version, key)
    _expect(
        _value(ROOT / "build.gradle.kts", r'^version = "([^"]+)"', "root Gradle"),
        version,
        "root Gradle",
    )
    _expect(
        _value(ROOT / "app/build.gradle.kts", r'versionName = "([^"]+)"', "app"),
        version,
        "app",
    )
    print(f"Big Orbit release train {version} is internally consistent.")


if __name__ == "__main__":
    main()
