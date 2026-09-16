# MotoScope V0.5

This repository contains the bounded Kotlin Multiplatform + Compose Multiplatform V0.5 vertical slice: one shared `MotoScope` screen with one offline Honda Click125 Standard image-sequence viewer. The viewer uses 36 local WebP resources and a circular, fractional horizontal drag state machine; it has no runtime networking, backend, database, DI framework, or 3D renderer.

## Build and test

Run from this directory:

```text
gradlew.bat :shared:allTests --no-daemon
gradlew.bat :androidApp:assembleDebug :androidApp:lint --no-daemon
```

The project baseline was taken from JetBrains' official KMP App Template and reduced to the Android/iOS mobile targets. Versions are recorded in `gradle/libs.versions.toml`; Android compile/target SDK 36 matches the installed local SDK. The iOS target/project is prepared but cannot be compiled or run on this Windows host.

## Honda development utility

`tools/honda360/honda360.py` discovers current catalog slugs from Honda Philippines' motorcycle list, resolves public product-page payloads, archives embedded 360 assets by slug, and writes manifests/provenance. Use `--model click125-standard` for the bounded proof or `--batch` for the current catalog. Bulk downloads under `prototype-assets/` are ignored by Git and must remain private/local. The authorization note in generated provenance is explicitly owner-attested and not independently verifiable; the missing approval email is not represented as evidence.
