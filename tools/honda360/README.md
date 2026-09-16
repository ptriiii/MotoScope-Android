# Honda 360 development utility

`honda360.py` is a bounded, development-only utility for the owner-attested MotoScope prototype authorization. It discovers entries from the live Honda Philippines motorcycle catalog, resolves product pages, reads the public `productVariants[].threeSixtyImages[]` data embedded in those pages, and archives the referenced frames under `prototype-assets/honda/<slug>/`.

Usage from the repository root:

```text
python tools/honda360/honda360.py --model click125-standard
python tools/honda360/honda360.py --batch
python tools/honda360/process_sequence.py --source prototype-assets/honda/click125-standard/source --output shared/src/commonMain/composeResources/drawable
python tools/honda360/test_honda360.py
```

The utility makes low-rate sequential requests, has three finite attempts per request, and stops at the catalog/product/asset surfaces. It creates `source/` (original files), `processed/`, `manifest.json`, and `provenance.json`. Existing files are reused only when their recorded source URL, byte count, and SHA-256 still match. Partial sequences are marked `incomplete` and are not reported as successful. HTTP 401/403/407/429 responses raise a distinct blocking signal and terminate the command nonzero without continuing the batch.

`process_sequence.py` is a separate development-time Pillow tool. For the Click125 prototype it converts the transparent palette PNGs to RGBA WebP, retaining only alpha values at or above the audited cutoff of 192 to remove the low-opacity presentation shadow. Originals remain untouched in `source/`; this processing is never performed by the mobile app.

Downloaded Honda prototype assets are ignored by Git by default. Do not publish this folder or the generated catalog.
