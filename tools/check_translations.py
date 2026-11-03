#!/usr/bin/env python3
"""Validate registered Android translations without third-party dependencies."""
import argparse
from collections import Counter
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ANDROID = "{http://schemas.android.com/apk/res/android}"
FORMAT = re.compile(r"%(?:(\d+)\$)?[-#+ 0,(<]*\d*(?:\.\d+)?([tT][a-zA-Z]|[a-zA-Z%])")


def placeholders(text):
    return Counter((index, kind) for index, kind in FORMAT.findall(text) if kind not in ("%", "n"))


def resources(folder, errors):
    result = {}
    for path in sorted(folder.glob("*.xml")):
        for element in ET.parse(path).getroot():
            if element.tag not in ("string", "plurals", "string-array"):
                continue
            key = element.tag, element.get("name")
            if key in result:
                errors.append(f"{folder.name}: duplicate resource {key[1]}")
            result[key] = element
    return result


def validate(res_dir):
    errors = []
    defaults = resources(res_dir / "values", errors)
    translated = {k: v for k, v in defaults.items() if v.get("translatable") != "false"}
    config = ET.parse(res_dir / "xml" / "locales_config.xml").getroot()
    tags = [e.get(ANDROID + "name") for e in config.findall("locale")]
    if not tags or tags[0] != "en":
        errors.append("locales_config.xml: list the default English locale (en) first")
    if len(tags) != len(set(tags)):
        errors.append("locales_config.xml: duplicate language tags")
    for tag in tags:
        if tag == "en":
            continue
        parts = tag.split("-")
        legacy = "values-" + parts[0] + ("-r" + parts[1] if len(parts) == 2 else "")
        bcp47 = "values-b+" + tag.replace("-", "+")
        folder = res_dir / bcp47
        if not folder.is_dir() and len(parts) <= 2:
            folder = res_dir / legacy
        if not folder.is_dir():
            errors.append(f"{tag}: missing translation directory ({bcp47})")
            continue
        actual = resources(folder, errors)
        for key in translated.keys() - actual.keys():
            errors.append(f"{tag}: missing {key[1]}")
        for key in actual.keys() - translated.keys():
            errors.append(f"{tag}: unknown or non-translatable resource {key[1]}")
        for key in actual.keys() & translated.keys():
            source, target = translated[key], actual[key]
            if key[0] == "string":
                pairs = [(source, target)]
            elif key[0] == "plurals":
                source_items = {e.get("quantity"): e for e in source}
                if not any(e.get("quantity") == "other" for e in target):
                    errors.append(f"{tag}: {key[1]} requires an other plural")
                pairs = [(source_items.get(e.get("quantity"), source_items["other"]), e) for e in target]
            else:
                if len(source) != len(target):
                    errors.append(f"{tag}: {key[1]} array length differs")
                pairs = zip(source, target)
            for a, b in pairs:
                if placeholders("".join(a.itertext())) != placeholders("".join(b.itertext())):
                    errors.append(f"{tag}: {key[1]} format placeholders differ")
    return errors, len(translated), tags


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--res-dir", type=Path, default=Path(__file__).resolve().parents[1] / "app/src/main/res")
    args = parser.parse_args()
    try:
        errors, count, tags = validate(args.res_dir)
    except (ET.ParseError, OSError, KeyError, AttributeError) as error:
        print(f"Invalid translation resources: {error}", file=sys.stderr)
        return 1
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    print(f"Translation check passed: {count} resources; languages: {', '.join(tags)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
