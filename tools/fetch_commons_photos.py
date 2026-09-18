#!/usr/bin/env python3
"""Finds extra photos for TotPocket on Wikimedia Commons (free, no API key).

Searches Commons' curated "Quality images" for each subject below, downloads candidates into a
local cache and records them in tools/commons_photos.json with their author and licence. Every
candidate starts unapproved; a person reviews the contact sheet this writes and sets
"approved": true on the ones that suit a toddler. Only approved photos go into the pack
(tools/build_media_pack.py reads the same manifest).

Usage:
    python3 tools/fetch_commons_photos.py          fetch candidates for subjects not fetched yet
    python3 tools/fetch_commons_photos.py --sheet  only redraw the review sheet

Commons asks API clients to send a descriptive User-Agent; licences are mostly CC BY / CC BY-SA,
which require crediting the author — the app shows the credit under each photo.
"""

import html
import io
import json
import re
import sys
import time
import urllib.parse
import urllib.request
from pathlib import Path

from PIL import Image, ImageDraw, ImageOps

ROOT = Path(__file__).resolve().parent
MANIFEST = ROOT / "commons_photos.json"
CACHE = ROOT / ".commons_cache"
SHEET = CACHE / "review_sheet.jpg"
USER_AGENT = "TotPocket-media-builder/1.0 (https://github.com/kabirnayeem99/TotPocket)"
API = "https://commons.wikimedia.org/w/api.php"
PER_SUBJECT = 5

# (category, subject, photo title, Commons search words)
SUBJECTS = [
    ("animals", "sheep", "Sheep", "sheep"),
    ("animals", "horse", "Horse", "horse grazing"),
    ("animals", "lion", "Lion", "lion"),
    ("animals", "zebra", "Zebra", "zebra"),
    ("animals", "giraffe", "Giraffe", "giraffe"),
    ("animals", "rabbit", "Rabbit", "rabbit"),
    ("animals", "panda", "Panda", "giant panda"),
    ("birds", "owl", "Owl", "owl"),
    ("birds", "peacock", "Peacock", "peacock"),
    ("birds", "penguin", "Penguin", "penguin"),
    ("birds", "pigeon", "Pigeon", "pigeon"),
    ("critters", "fish", "Fish", "aquarium fish"),
    ("critters", "turtle", "Turtle", "turtle"),
    ("critters", "ladybug", "Ladybug", "ladybird coccinella"),
    ("food", "apple", "Apple", "red apple fruit"),
    ("food", "orange", "Orange", "orange fruit citrus"),
    ("food", "watermelon", "Watermelon", "watermelon"),
    ("food", "strawberry", "Strawberry", "strawberry fruit"),
    ("vehicles", "airplane", "Aeroplane", "airliner"),
    ("vehicles", "helicopter", "Helicopter", "helicopter"),
    ("vehicles", "firetruck", "Fire engine", "fire engine truck"),
    ("vehicles", "tractor", "Tractor", "tractor"),
    ("vehicles", "bicycle", "Bicycle", "bicycle"),
    ("nature", "rainbow", "Rainbow", "rainbow"),
    ("nature", "moon", "Moon", "full moon"),
    ("nature", "sunflower", "Sunflower", "sunflower"),
    ("nature", "rose", "Rose", "rose flower"),
    ("nature", "lotus", "Lotus", "lotus flower nelumbo"),
    ("nature", "waterlily", "Water lily", "water lily nymphaea"),
]


def api(params: dict) -> dict:
    query = urllib.parse.urlencode({"format": "json", **params})
    request = urllib.request.Request(f"{API}?{query}", headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.load(response)


def plain(text: str) -> str:
    """Commons returns the artist as HTML; keep just the words."""
    return re.sub(r"\s+", " ", html.unescape(re.sub(r"<[^>]+>", "", text or ""))).strip()


def search(words: str, limit: int) -> list[dict]:
    data = api({
        "action": "query",
        "generator": "search",
        "gsrnamespace": 6,
        "gsrlimit": limit,
        "gsrsearch": f"{words} filetype:bitmap incategory:Quality_images",
        "prop": "imageinfo",
        "iiprop": "url|size|mime|extmetadata",
        "iiurlwidth": 1280,
        "iiextmetadatafilter": "LicenseShortName|Artist|AttributionRequired",
    })
    pages = sorted(data.get("query", {}).get("pages", {}).values(), key=lambda p: p.get("index", 0))
    results = []
    for page in pages:
        info = (page.get("imageinfo") or [{}])[0]
        if info.get("mime") not in ("image/jpeg", "image/png"):
            continue
        meta = info.get("extmetadata", {})
        results.append({
            "file": page["title"],
            "url": info["thumburl"],
            "page": info["descriptionurl"],
            "license": plain(meta.get("LicenseShortName", {}).get("value", "")),
            "author": plain(meta.get("Artist", {}).get("value", "")) or "Unknown",
        })
    return results


def cache_path(entry: dict) -> Path:
    safe = re.sub(r"[^A-Za-z0-9._-]+", "_", entry["file"].removeprefix("File:"))
    return CACHE / safe


def download(entry: dict) -> Path:
    path = cache_path(entry)
    if not path.exists():
        request = urllib.request.Request(entry["url"], headers={"User-Agent": USER_AGENT})
        with urllib.request.urlopen(request, timeout=60) as response:
            path.write_bytes(response.read())
        time.sleep(0.5)  # be gentle with Wikimedia's servers
    return path


def load_manifest() -> list[dict]:
    return json.loads(MANIFEST.read_text()) if MANIFEST.exists() else []


def draw_sheet(entries: list[dict]) -> None:
    tile, cols = 180, 8
    rows = (len(entries) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * tile, rows * (tile + 14)), "white")
    pen = ImageDraw.Draw(sheet)
    for i, entry in enumerate(entries):
        try:
            image = ImageOps.exif_transpose(Image.open(cache_path(entry))).convert("RGB")
        except Exception:
            continue
        image.thumbnail((tile - 4, tile - 4))
        x, y = (i % cols) * tile, (i // cols) * (tile + 14)
        sheet.paste(image, (x + 2, y + 2))
        mark = {True: "OK", False: "NO"}.get(entry.get("approved"), "?")
        pen.text((x + 2, y + tile), f"{i} {entry['subject']} {mark}", fill="black")
    sheet.save(SHEET, quality=70)
    print(f"review sheet: {SHEET}")


def main() -> None:
    CACHE.mkdir(exist_ok=True)
    manifest = load_manifest()
    if "--sheet" not in sys.argv:
        done = {e["subject"] for e in manifest}
        for category, subject, title, words in SUBJECTS:
            if subject in done:
                continue
            found = search(words, PER_SUBJECT * 3)[:PER_SUBJECT]
            for result in found:
                download(result)
                manifest.append({"category": category, "subject": subject, "title": title, "approved": None, **result})
            print(f"{subject:12} {len(found)} candidates")
            MANIFEST.write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + "\n")
    draw_sheet(manifest)


if __name__ == "__main__":
    main()
