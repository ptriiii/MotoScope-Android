#!/usr/bin/env python3
"""Create deterministic, alpha-preserving WebP frames for the mobile prototype.

The Honda Click125 source PNGs are palette images with a transparency channel.
The source also includes a low-opacity presentation shadow. This development-
only processor keeps the original files untouched, removes only alpha values
below the audited cutoff, and writes RGBA WebP resources for Compose.
"""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image


DEFAULT_ALPHA_CUTOFF = 192


def process_frame(source: Path, destination: Path, alpha_cutoff: int) -> None:
    with Image.open(source) as image:
        image.load()
        rgba = image.convert("RGBA")
        alpha = rgba.getchannel("A")
        rgba.putalpha(alpha.point(lambda value: value if value >= alpha_cutoff else 0))
        destination.parent.mkdir(parents=True, exist_ok=True)
        # The faster encoder method keeps development regeneration bounded;
        # quality and alpha semantics are independent of this setting.
        rgba.save(destination, "WEBP", quality=90, method=0)


def process_directory(source_dir: Path, output_dir: Path, alpha_cutoff: int) -> int:
    sources = sorted(source_dir.glob("*.png"))
    if not sources:
        raise ValueError(f"no PNG source frames found in {source_dir}")
    dimensions: set[tuple[int, int]] = set()
    for index, source in enumerate(sources, start=1):
        with Image.open(source) as image:
            image.load()
            dimensions.add(image.size)
        destination = output_dir / f"click125_standard_{index:02d}.webp"
        process_frame(source, destination, alpha_cutoff)
    if len(dimensions) != 1:
        raise ValueError(f"inconsistent source dimensions: {sorted(dimensions)}")
    return len(sources)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--alpha-cutoff", type=int, default=DEFAULT_ALPHA_CUTOFF)
    args = parser.parse_args()
    if not 0 <= args.alpha_cutoff <= 255:
        parser.error("--alpha-cutoff must be between 0 and 255")
    count = process_directory(args.source, args.output, args.alpha_cutoff)
    print(f"processed={count} alphaCutoff={args.alpha_cutoff} output={args.output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
