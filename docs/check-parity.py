#!/usr/bin/env python3
"""Check that every English documentation page has a Russian counterpart.

Compares the sets of ``*.en.md`` and ``*.ru.md`` files under the docs directory
and exits non-zero when a page is missing from either language. Pages that are
intentionally available in one language only can be listed in ALLOWLIST as the
docs-relative path of the missing file (e.g. ``media.ru.md``).

Usage: python docs/check-parity.py
"""

import sys
from pathlib import Path

DOCS_DIR = Path(__file__).resolve().parent

# Pages intentionally available in one language only: list the MISSING file,
# e.g. "media.ru.md" when media.en.md deliberately has no Russian version.
ALLOWLIST: set[str] = set()

EN_SUFFIX = ".en.md"
RU_SUFFIX = ".ru.md"


def collect(suffix: str) -> set[str]:
    return {
        str(p.relative_to(DOCS_DIR))
        for p in DOCS_DIR.rglob(f"*{suffix}")
    }


def counterpart(name: str, source: str, target: str) -> str:
    return name[: -len(source)] + target


def main() -> int:
    en = collect(EN_SUFFIX)
    ru = collect(RU_SUFFIX)
    missing_ru = {counterpart(p, EN_SUFFIX, RU_SUFFIX) for p in en} - ru
    missing_en = {counterpart(p, RU_SUFFIX, EN_SUFFIX) for p in ru} - en

    unpaired = sorted((missing_ru | missing_en) - ALLOWLIST)
    if unpaired:
        print("Language parity check FAILED: missing page counterparts:")
        for name in unpaired:
            page = name[: -len(RU_SUFFIX)] if name.endswith(RU_SUFFIX) else name[: -len(EN_SUFFIX)]
            print(f"  - {page}.md  (missing file: {name})")
        return 1

    print(f"Language parity OK: {len(en)} en / {len(ru)} ru pages.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
