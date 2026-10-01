# MotoScope demo

## Current demo decision

The active demo Home shows the interactive, offline 36-frame Click125 motorcycle viewer on a white canvas, plus a fixed floating Home / Stats / Garage / More navigation. Home is selected; Stats, Garage, and More remain visible but disabled. The MotoScope wordmark, HONDA and oversized CLICK125 display labels, and affiliate links are deferred from the current Home.

This decision supersedes the active Home presentation introduced by `7dd3801` (showcase-first demo). The richer Portrait V1 Home remains in history at `593f738e0bf5b148b6f9176514e932649c1c0c1a`. Landscape and iOS remain deferred. There is no runtime networking, backend, database, DI framework, or 3D renderer.

## Build and test

Run from this directory:

```text
gradlew.bat :shared:allTests --no-daemon
gradlew.bat :androidApp:assembleDebug :androidApp:lint --no-daemon
```

The project baseline was taken from JetBrains' official KMP App Template and reduced to the Android/iOS mobile targets. Versions are recorded in `gradle/libs.versions.toml`; Android compile/target SDK 36 matches the installed local SDK. The iOS target/project is prepared but cannot be compiled or run on this Windows host.

## Honda development utility

`tools/honda360/honda360.py` discovers current catalog slugs from Honda Philippines' motorcycle list, resolves public product-page payloads, archives embedded 360 assets by slug, and writes manifests/provenance. Use `--model click125-standard` for the bounded proof or `--batch` for the current catalog. Bulk downloads under `prototype-assets/` are ignored by Git and must remain private/local. The authorization note in generated provenance is explicitly owner-attested and not independently verifiable; the missing approval email is not represented as evidence.
