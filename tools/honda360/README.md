# Honda 360 development utility

`honda360.py` is a bounded, development-only utility for the owner-attested MotoScope prototype authorization. It discovers entries from the live Honda Philippines motorcycle catalog, resolves product pages, reads the public `productVariants[].threeSixtyImages[]` data embedded in those pages, and archives the referenced frames under `prototype-assets/honda/<slug>/`.

Usage from the repository root:

```text
python tools/honda360/honda360.py --model click125-standard
python tools/honda360/honda360.py --batch
```

The utility makes low-rate sequential requests, has three finite attempts per request, and stops at the catalog/product/asset surfaces. It creates `source/` (original files), `processed/` (reserved for a future explicit conversion), `manifest.json`, and `provenance.json`. Existing files are reused only when their recorded source URL, byte count, and SHA-256 still match. Partial sequences are marked `incomplete` and are not reported as successful.

Downloaded Honda prototype assets are ignored by Git by default. Do not publish this folder or the generated catalog.
