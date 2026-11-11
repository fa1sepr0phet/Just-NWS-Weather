# 12.10 (version code 110)

## Play Console release notes

Improved background status bar temperature updates, including refresh attempts while the phone is locked when Android permits. Fixed the blank launcher icon when themed app icons are enabled. Standard full-color app icons are unchanged.

## Release configuration

- Both Play and FOSS variants inherit version name `12.10` and version code `110` from `app/build.gradle.kts`.
- Build the signed `playRelease` Android App Bundle with the existing upload key for Play Console.
- Earlier release documents and device investigation version numbers describe historical builds and remain unchanged.
- Locked/Doze delivery and the themed-icon fix still require device validation. Android can defer background refresh; the 20-minute idle alarm is not an exact deadline.
