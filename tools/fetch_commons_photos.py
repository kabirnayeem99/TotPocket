#!/usr/bin/env python3
"""Finds extra photos for TotPocket on Wikimedia Commons (free, no API key).

Searches Commons' curated "Quality images" for each subject below, downloads candidates into a
local cache and records them in tools/commons_photos.json with their author and licence. Every
candidate starts unapproved; a person reviews the contact sheet this writes and approves the ones
that suit a toddler. Only approved photos go into the pack (tools/build_media_pack.py reads the
same manifest).

Usage:
    python3 tools/fetch_commons_photos.py            fetch candidates for SUBJECTS not fetched yet
    python3 tools/fetch_commons_photos.py sheet      only redraw the review sheet
    python3 tools/fetch_commons_photos.py add CATEGORY SUBJECT TITLE "SEARCH WORDS" [--count N] [--any]
        fetch N new candidates for one subject (again for more; already-seen files are skipped)
        and draw a sheet of just those; --any searches all Commons photos, not only Quality images
    python3 tools/fetch_commons_photos.py approve INDEX...   mark candidates (sheet numbers) as OK
    python3 tools/fetch_commons_photos.py reject INDEX...    mark candidates as NO

After approving, rebuild the pack with tools/build_media_pack.py. A new subject also needs an entry
in that script's SUBJECTS (video title and sound).

Commons asks API clients to send a descriptive User-Agent; licences are mostly CC BY / CC BY-SA,
which require crediting the author — the app shows the credit under each photo.
"""

import argparse
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
NEW_SHEET = CACHE / "new_candidates.jpg"
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
    # Everyday things a toddler sees at home. Only photos with no people in them are approved.
    ("things", "cup", "Cup", "cup mug"),
    ("things", "spoon", "Spoon", "spoon"),
    ("things", "plate", "Plate", "dinner plate"),
    ("things", "bowl", "Bowl", "bowl"),
    ("things", "ball", "Ball", "ball toy"),
    ("things", "shoes", "Shoes", "pair of shoes"),
    ("things", "chair", "Chair", "chair"),
    ("things", "clock", "Clock", "wall clock"),
    ("things", "umbrella", "Umbrella", "umbrella"),
    ("things", "book", "Book", "books"),
    ("things", "teddy", "Teddy bear", "teddy bear"),
    ("things", "toycar", "Toy car", "toy car"),
    ("things", "bucket", "Bucket", "bucket"),
    ("things", "soap", "Soap", "soap bar"),
    ("things", "comb", "Comb", "comb"),
    ("things", "key", "Key", "key"),
    ("things", "lamp", "Lamp", "lamp"),
    ("things", "pencil", "Pencils", "colored pencils"),
    ("things", "balloon", "Balloons", "balloons"),
    ("things", "hat", "Hat", "hat"),
    ("things", "kettle", "Kettle", "kettle"),
    ("things", "door", "Door", "door"),
]


def api(params: dict) -> dict:
    query = urllib.parse.urlencode({"format": "json", **params})
    request = urllib.request.Request(f"{API}?{query}", headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.load(response)


def plain(text: str) -> str:
    """Commons returns the artist as HTML; keep just the words."""
    return re.sub(r"\s+", " ", html.unescape(re.sub(r"<[^>]+>", "", text or ""))).strip()


def search(words: str, limit: int, quality_only: bool = True) -> list[dict]:
    wanted = f"{words} filetype:bitmap" + (" incategory:Quality_images" if quality_only else "")
    data = api({
        "action": "query",
        "generator": "search",
        "gsrnamespace": 6,
        "gsrlimit": limit,
        "gsrsearch": wanted,
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


def save_manifest(manifest: list[dict]) -> None:
    MANIFEST.write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + "\n")


def draw_sheet(numbered: list[tuple[int, dict]], path: Path = SHEET, tile: int = 180, cols: int = 8) -> None:
    """Contact sheet; each tile is labelled with its manifest index, used by approve/reject."""
    cols = min(cols, max(len(numbered), 1))
    rows = (len(numbered) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * tile, max(rows, 1) * (tile + 14)), "white")
    pen = ImageDraw.Draw(sheet)
    for slot, (index, entry) in enumerate(numbered):
        try:
            image = ImageOps.exif_transpose(Image.open(cache_path(entry))).convert("RGB")
        except Exception:
            continue
        image.thumbnail((tile - 4, tile - 4))
        x, y = (slot % cols) * tile, (slot // cols) * (tile + 14)
        sheet.paste(image, (x + 2, y + 2))
        mark = {True: "OK", False: "NO"}.get(entry.get("approved"), "?")
        pen.text((x + 2, y + tile), f"{index} {entry['subject']} {mark}", fill="black")
    sheet.save(path, quality=80)
    print(f"review sheet: {path}")


def fetch_batch(manifest: list[dict]) -> None:
    done = {e["subject"] for e in manifest}
    for category, subject, title, words in SUBJECTS:
        if subject in done:
            continue
        found = search(words, PER_SUBJECT * 3)[:PER_SUBJECT]
        for result in found:
            download(result)
            manifest.append({"category": category, "subject": subject, "title": title, "approved": None, **result})
        print(f"{subject:12} {len(found)} candidates")
        save_manifest(manifest)


def add(manifest: list[dict], args: argparse.Namespace) -> None:
    import build_media_pack as pack

    if args.category not in pack.CATEGORIES:
        sys.exit(f"unknown category {args.category!r}; pick one of {', '.join(pack.CATEGORIES)}")
    seen = {e["file"] for e in manifest}
    fresh = [r for r in search(args.words, 50, quality_only=not args.any) if r["file"] not in seen][: args.count]
    if not fresh:
        sys.exit("no new candidates; try other search words or --any")
    start = len(manifest)
    for result in fresh:
        download(result)
        manifest.append({"category": args.category, "subject": args.subject, "title": args.title, "approved": None, **result})
    save_manifest(manifest)
    for index in range(start, len(manifest)):
        entry = manifest[index]
        print(f"{index:4}  {entry['license']:14}  {entry['author'][:30]:30}  {entry['file']}")
    draw_sheet(list(enumerate(manifest))[start:], NEW_SHEET, tile=360, cols=3)
    if args.subject not in pack.SUBJECTS:
        print(f"note: add {args.subject!r} to SUBJECTS in tools/build_media_pack.py before building the pack")


def mark(manifest: list[dict], indices: list[int], approved: bool) -> None:
    for index in indices:
        if not 0 <= index < len(manifest):
            sys.exit(f"no candidate {index}")
        manifest[index]["approved"] = approved
        print(f"{index:4} {'OK' if approved else 'NO'}  {manifest[index]['subject']}  {manifest[index]['file']}")
    save_manifest(manifest)


def main() -> None:
    parser = argparse.ArgumentParser(description="Find and review Wikimedia Commons photos for TotPocket.")
    commands = parser.add_subparsers(dest="command")
    commands.add_parser("sheet", help="only redraw the review sheet")
    new = commands.add_parser("add", help="fetch candidates for one subject")
    new.add_argument("category")
    new.add_argument("subject", help="short id, e.g. fan")
    new.add_argument("title", help="photo title shown in the app, e.g. 'Fan'")
    new.add_argument("words", help="Commons search words, e.g. 'electric fan'")
    new.add_argument("--count", type=int, default=PER_SUBJECT)
    new.add_argument("--any", action="store_true", help="search all Commons photos, not only Quality images")
    for name in ("approve", "reject"):
        commands.add_parser(name).add_argument("indices", type=int, nargs="+")
    args = parser.parse_args()

    CACHE.mkdir(exist_ok=True)
    manifest = load_manifest()
    if args.command == "add":
        add(manifest, args)
        return
    if args.command in ("approve", "reject"):
        mark(manifest, args.indices, args.command == "approve")
        return
    if args.command is None:
        fetch_batch(manifest)
    draw_sheet(list(enumerate(manifest)))


if __name__ == "__main__":
    main()
