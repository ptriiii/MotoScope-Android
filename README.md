# MotoScope showcase-first demo

This repository contains the bounded Kotlin Multiplatform + Compose Multiplatform MotoScope showcase-first demo Home. It presents one Honda Click125 Standard with the offline 36-frame WebP viewer, circular fractional horizontal rotation, and a fixed provisional bottom-navigation shell. Home is the only available destination; Stats, Garage, and More are visible but disabled placeholders. The richer Portrait V1 Home is preserved in history at commit `593f738e0bf5b148b6f9176514e932649c1c0c1a`. Landscape and iOS remain deferred. There is no runtime networking, backend, database, DI framework, or 3D renderer.

## Build and test

Run from this directory:

```text
gradlew.bat :shared:allTests --no-daemon
gradlew.bat :androidApp:assembleDebug :androidApp:lint --no-daemon
```

The project baseline was taken from JetBrains' official KMP App Template and reduced to the Android/iOS mobile targets. Versions are recorded in `gradle/libs.versions.toml`; Android compile/target SDK 36 matches the installed local SDK. The iOS target/project is prepared but cannot be compiled or run on this Windows host.

## Honda development utility

`tools/honda360/honda360.py` discovers current catalog slugs from Honda Philippines' motorcycle list, resolves public product-page payloads, archives embedded 360 assets by slug, and writes manifests/provenance. Use `--model click125-standard` for the bounded proof or `--batch` for the current catalog. Bulk downloads under `prototype-assets/` are ignored by Git and must remain private/local. The authorization note in generated provenance is explicitly owner-attested and not independently verifiable; the missing approval email is not represented as evidence.
