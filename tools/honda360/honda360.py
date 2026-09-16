#!/usr/bin/env python3
"""Discover and locally archive Honda Philippines motorcycle 360 sequences.

This is a development-only utility. It stays on the Honda motorcycle catalog,
product pages, and the image URLs those public pages embed. It intentionally
does not publish or copy assets into the mobile application.
"""

from __future__ import annotations

import argparse
import hashlib
import html
import json
import re
import shutil
import subprocess
import sys
import time
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode, urljoin, urlparse
from urllib.request import Request, urlopen


CATALOG_URL = "https://www.hondaph.com/motorcycle/list"
USER_AGENT = "MotoScope prototype asset discovery; local development"
AUTHORIZATION_NOTE = (
    "OWNER-ATTESTED AUTHORIZATION — NOT INDEPENDENTLY VERIFIABLE. "
    "Prototype/development use for MotoScope only; the missing approval email "
    "is not preserved evidence."
)


@dataclass
class Frame:
    source_order: int
    title: str
    filename: str
    url: str
    source_bytes: int | None
    source_width: int | None
    source_height: int | None
    frame_number: int | None = None


def fetch(url: str, delay: float, retries: int = 3) -> bytes:
    """Fetch a public page/asset with bounded retry and conservative pacing."""
    last_error: Exception | None = None
    for attempt in range(retries):
        if delay:
            time.sleep(delay if attempt == 0 else delay * (attempt + 1))
        try:
            curl = shutil.which("curl.exe") or shutil.which("curl")
            if curl:
                result = subprocess.run(
                    [curl, "-sS", "-L", "--max-time", "45", "-A", USER_AGENT, "-H", "Accept: */*", "-w", "\\n__MOTOSCOPE_STATUS__%{http_code}", url],
                    check=True,
                    capture_output=True,
                )
                status_marker = b"\n__MOTOSCOPE_STATUS__"
                body, status_bytes = result.stdout.rsplit(status_marker, 1)
                status = int(status_bytes.decode("ascii"))
                if status in {401, 403, 407, 429}:
                    raise RuntimeError(f"access actively blocked for {url}: HTTP {status}")
                if status >= 400 and status != 500:
                    raise RuntimeError(f"request returned HTTP {status}: {url}")
                # Honda's current Next.js product response can be a complete
                # RSC document with an HTTP 500 status; retain the body so the
                # public embedded product data remains inspectable.
                return body
            request = Request(url, headers={"User-Agent": USER_AGENT, "Accept": "*/*"})
            with urlopen(request, timeout=45) as response:
                return response.read()
        except (HTTPError, URLError, TimeoutError) as error:
            last_error = error
            if isinstance(error, HTTPError) and error.code in {401, 403, 407, 429}:
                raise RuntimeError(f"access actively blocked for {url}: HTTP {error.code}") from error
        except subprocess.CalledProcessError as error:
            message = error.stderr.decode("utf-8", errors="replace").strip()
            last_error = RuntimeError(message or f"curl exit code {error.returncode}")
    raise RuntimeError(f"request failed after {retries} attempts: {url}: {last_error}")


def decode_rsc_payloads(page: str) -> str:
    """Decode Next.js RSC push strings without relying on a private API."""
    decoder = json.JSONDecoder()
    marker = "self.__next_f.push([1,"
    payloads: list[str] = []
    cursor = 0
    while True:
        marker_start = page.find(marker, cursor)
        if marker_start < 0:
            break
        value_start = marker_start + len(marker)
        try:
            value, _ = decoder.raw_decode(page[value_start:])
        except json.JSONDecodeError:
            cursor = value_start + 1
            continue
        if isinstance(value, str):
            payloads.append(value)
        cursor = value_start + 1
    return "\n".join(payloads)


def extract_json_array(payload: str, key: str, start: int = 0) -> tuple[list[Any], int] | None:
    marker = f'"{key}":['
    position = payload.find(marker, start)
    if position < 0:
        return None
    array_start = position + len(marker) - 1
    try:
        value, end = json.JSONDecoder().raw_decode(payload[array_start:])
    except json.JSONDecodeError:
        return None
    return (value, position + end) if isinstance(value, list) else None


def normalize_url(raw: str) -> str:
    decoded = html.unescape(raw).replace("\\u0026", "&").replace("\\u002f", "/")
    if decoded.startswith("//"):
        return "https:" + decoded
    return urljoin("https://www.hondaph.com", decoded)


def numeric_frame_number(title: str, filename: str) -> int | None:
    candidate = f"{title} {filename}"
    numbers = re.findall(r"(?<!\d)(\d{1,3})(?!\d)", candidate)
    return int(numbers[-1]) if numbers else None


def frame_quality(frames: list[Frame]) -> tuple[list[int], list[int], int]:
    numbers = [frame.frame_number for frame in frames if frame.frame_number is not None]
    duplicates = sorted({number for number in numbers if numbers.count(number) > 1})
    missing = sorted(set(range(1, max(numbers, default=0) + 1)) - set(numbers)) if numbers else []
    non_numeric = len(frames) - len(numbers)
    return duplicates, missing, non_numeric


def product_name(payload: str, slug: str) -> str:
    for pattern in (r'"productName":"([^"]+)"', r'"title":"([^"]+)"'):
        match = re.search(pattern, payload)
        if match:
            return html.unescape(match.group(1))
    return slug.replace("-", " ").title()


def discover_catalog(delay: float) -> list[dict[str, str]]:
    """Read catalog links from successive live catalog pages, not a local list."""
    discovered: dict[str, dict[str, str]] = {}
    for page_number in range(1, 21):
        url = CATALOG_URL if page_number == 1 else f"{CATALOG_URL}?{urlencode({'page': page_number})}"
        body = fetch(url, delay)
        text = body.decode("utf-8", errors="replace")
        links = re.findall(r'href=["\'](/motorcycle/([a-z0-9-]+))["\']', text, flags=re.I)
        new_count = 0
        for path, slug in links:
            if slug in {"list", "comparison"} or slug in discovered:
                continue
            discovered[slug] = {"slug": slug, "url": urljoin(CATALOG_URL, path)}
            new_count += 1
        if not links or (page_number > 1 and new_count == 0):
            break
    return list(discovered.values())


def discover_sequences(product_url: str, slug: str, delay: float) -> tuple[str, list[Frame], int]:
    body = fetch(product_url, delay)
    payload = decode_rsc_payloads(body.decode("utf-8", errors="replace"))
    sequences: list[tuple[int, list[Frame]]] = []
    cursor = 0
    sequence_index = 0
    while True:
        extracted = extract_json_array(payload, "threeSixtyImages", cursor)
        if not extracted:
            break
        raw_frames, cursor = extracted
        frames: list[Frame] = []
        for source_order, item in enumerate(raw_frames):
            fields = item.get("fields", {}) if isinstance(item, dict) else {}
            file_data = fields.get("file", {}) if isinstance(fields, dict) else {}
            details = file_data.get("details", {}) if isinstance(file_data, dict) else {}
            image = details.get("image", {}) if isinstance(details, dict) else {}
            filename = str(file_data.get("fileName") or fields.get("title") or f"frame-{source_order + 1:03d}.bin")
            title = str(fields.get("title") or filename)
            raw_url = file_data.get("url")
            if not raw_url:
                continue
            frames.append(
                Frame(
                    source_order=source_order + 1,
                    title=title,
                    filename=Path(urlparse(str(raw_url)).path).name or filename,
                    url=normalize_url(str(raw_url)),
                    source_bytes=int(file_data.get("details", {}).get("size")) if file_data.get("details", {}).get("size") else None,
                    source_width=int(image.get("width")) if image.get("width") else None,
                    source_height=int(image.get("height")) if image.get("height") else None,
                    frame_number=numeric_frame_number(title, filename),
                )
            )
        if len(frames) >= 2:
            sequences.append((sequence_index, frames))
        sequence_index += 1

    if not sequences:
        raise ValueError("no discoverable coherent 360 image sequence")

    # The first complete variant is the page's primary sequence. It is not a
    # guessed CDN pattern: every URL came from the public product page payload.
    _, frames = max(sequences, key=lambda entry: len(entry[1]))
    numeric = [frame.frame_number for frame in frames]
    duplicate_numbers, missing_numbers, _ = frame_quality(frames)
    complete_numeric = (
        all(number is not None for number in numeric)
        and not duplicate_numbers
        and not missing_numbers
        and set(numeric) == set(range(1, len(frames) + 1))
    )
    if complete_numeric:
        frames = sorted(frames, key=lambda frame: frame.frame_number or 0)
    return product_name(payload, slug), frames, len(sequences)


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def image_info(data: bytes) -> tuple[int | None, int | None, str]:
    if data.startswith(b"\x89PNG\r\n\x1a\n") and len(data) >= 24:
        import struct

        return struct.unpack(">II", data[16:24]) + ("image/png",)
    if data.startswith(b"\xff\xd8"):
        position = 2
        while position + 9 < len(data):
            if data[position] != 0xFF:
                position += 1
                continue
            marker = data[position + 1]
            position += 2
            if marker in {0xD8, 0xD9}:
                continue
            if position + 2 > len(data):
                break
            segment_length = int.from_bytes(data[position:position + 2], "big")
            if marker in set(range(0xC0, 0xC4)) | set(range(0xC5, 0xC8)) | set(range(0xC9, 0xCC)) | set(range(0xCD, 0xD0)):
                if position + 7 <= len(data):
                    return (
                        int.from_bytes(data[position + 3:position + 5], "big"),
                        int.from_bytes(data[position + 5:position + 7], "big"),
                        "image/jpeg",
                    )
            position += segment_length
    if data.startswith(b"RIFF") and data[8:12] == b"WEBP" and len(data) >= 30:
        if data[12:16] == b"VP8X":
            width = 1 + int.from_bytes(data[24:27], "little")
            height = 1 + int.from_bytes(data[27:30], "little")
            return width, height, "image/webp"
    return None, None, "application/octet-stream"


def safe_filename(value: str) -> str:
    cleaned = re.sub(r"[^A-Za-z0-9._-]+", "_", value).strip("._")
    return cleaned or "frame.bin"


def read_existing_manifest(path: Path) -> dict[str, Any]:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (FileNotFoundError, json.JSONDecodeError):
        return {}


def archive_sequence(
    entry: dict[str, str],
    output_root: Path,
    delay: float,
    authorization: bool,
) -> dict[str, Any]:
    slug = entry["slug"]
    model_name, frames, variant_count = discover_sequences(entry["url"], slug, delay)
    duplicate_numbers, missing_numbers, non_numeric_count = frame_quality(frames)
    model_root = output_root / slug
    source_root = model_root / "source"
    processed_root = model_root / "processed"
    source_root.mkdir(parents=True, exist_ok=True)
    processed_root.mkdir(parents=True, exist_ok=True)
    manifest_path = model_root / "manifest.json"
    existing = read_existing_manifest(manifest_path)
    prior_by_url = {frame.get("sourceUrl"): frame for frame in existing.get("frames", [])}
    manifest_frames: list[dict[str, Any]] = []
    failures: list[dict[str, str]] = []

    for index, frame in enumerate(frames, start=1):
        local_name = f"{index:03d}__{safe_filename(frame.filename)}"
        local_path = source_root / local_name
        prior = prior_by_url.get(frame.url, {})
        local_hash = None
        asset_data: bytes | None = None
        if local_path.exists() and prior.get("sha256") and local_path.stat().st_size == prior.get("bytes"):
            asset_data = local_path.read_bytes()
            local_hash = hashlib.sha256(asset_data).hexdigest()
        needs_download = not local_path.is_file() or not prior.get("sha256") or local_hash != prior.get("sha256")
        if needs_download:
            try:
                asset_data = fetch(frame.url, delay)
                local_path.write_bytes(asset_data)
                if not local_path.is_file():
                    raise OSError(f"downloaded file did not remain on disk: {local_path}")
                local_hash = sha256_bytes(asset_data)
            except Exception as error:  # retain partial status; never call it success
                failures.append({"url": frame.url, "error": str(error)})
                continue
        if asset_data is None:
            failures.append({"url": frame.url, "error": "no downloaded bytes available"})
            continue
        width, height, content_type = image_info(asset_data)
        manifest_frames.append(
            {
                "sequenceIndex": index,
                "sourceOrder": frame.source_order,
                "frameNumber": frame.frame_number,
                "title": frame.title,
                "sourceUrl": frame.url,
                "originalFilename": frame.filename,
                "localFile": f"source/{local_name}",
                "bytes": local_path.stat().st_size,
                "sha256": local_hash,
                "width": width or frame.source_width,
                "height": height or frame.source_height,
                "contentType": content_type,
                "sourceBytes": frame.source_bytes,
                "sourceWidth": frame.source_width,
                "sourceHeight": frame.source_height,
            }
        )

    status = "success" if len(manifest_frames) == len(frames) and not failures else "incomplete"
    generated_at = datetime.now(timezone.utc).isoformat()
    manifest = {
        "schemaVersion": 1,
        "status": status,
        "stableLocalId": f"honda-ph:{slug}",
        "manufacturer": "Honda",
        "model": model_name,
        "slug": slug,
        "variantSequenceCountOnPage": variant_count,
        "frameCount": len(manifest_frames),
        "expectedFrameCount": len(frames),
        "duplicateFrameNumbers": duplicate_numbers,
        "missingFrameNumbers": missing_numbers,
        "nonNumericFrameCount": non_numeric_count,
        "frames": manifest_frames,
        "sourceProductUrl": entry["url"],
        "generatedAt": generated_at,
    }
    provenance = {
        "schemaVersion": 1,
        "sourceProductUrl": entry["url"],
        "sourceAssetUrls": sorted({frame.url for frame in frames}),
        "acquiredAt": generated_at,
        "retrievalMethod": "Public Honda Philippines product-page RSC payload and the embedded Contentful asset URLs; bounded sequential HTTP requests.",
        "authorization": AUTHORIZATION_NOTE if authorization else "Authorization note omitted by command option.",
        "originalFileHashes": {frame["localFile"]: frame["sha256"] for frame in manifest_frames},
        "sourceOrderPreserved": True,
        "canonicalOrderRule": "Numeric frame titles 01..N when complete and unique; otherwise the order exposed by the product-page payload.",
    }
    manifest_path.write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    (model_root / "provenance.json").write_text(json.dumps(provenance, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    return {
        "slug": slug,
        "model": model_name,
        "status": status,
        "frameCount": len(manifest_frames),
        "expectedFrameCount": len(frames),
        "sourceProductUrl": entry["url"],
        "failures": failures,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--model", help="stable slug discovered from the live catalog, e.g. click125-standard")
    group.add_argument("--batch", action="store_true", help="process the currently discoverable catalog")
    parser.add_argument("--output", type=Path, default=Path("prototype-assets/honda"))
    parser.add_argument("--delay", type=float, default=0.35, help="seconds between requests (default: 0.35)")
    parser.add_argument("--omit-authorization-note", action="store_true", help="omit the owner-attested note from generated metadata")
    args = parser.parse_args()

    if args.delay < 0.1:
        parser.error("--delay must be at least 0.1 seconds")
    catalog = discover_catalog(args.delay)
    if not catalog:
        print("No motorcycle entries discovered from the live catalog.", file=sys.stderr)
        return 2
    entries = catalog if args.batch else [entry for entry in catalog if entry["slug"] == args.model]
    if not entries:
        print(f"Model {args.model!r} was not present in the live catalog.", file=sys.stderr)
        return 2

    args.output.mkdir(parents=True, exist_ok=True)
    results: list[dict[str, Any]] = []
    for entry in entries:
        try:
            result = archive_sequence(entry, args.output, args.delay, not args.omit_authorization_note)
        except Exception as error:
            result = {"slug": entry["slug"], "sourceProductUrl": entry["url"], "status": "unavailable", "error": str(error)}
        results.append(result)
        print(json.dumps(result, ensure_ascii=False))

    summary = {
        "schemaVersion": 1,
        "catalogUrl": CATALOG_URL,
        "catalogCount": len(catalog),
        "requestedCount": len(entries),
        "successful": [result["slug"] for result in results if result.get("status") == "success"],
        "incompleteOrUnavailable": [result for result in results if result.get("status") != "success"],
        "generatedAt": datetime.now(timezone.utc).isoformat(),
    }
    (args.output / "run-summary.json").write_text(json.dumps(summary, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    return 0 if all(result.get("status") == "success" for result in results) else 1


if __name__ == "__main__":
    raise SystemExit(main())
