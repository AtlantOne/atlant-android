# Atlant for Android

Android graphical client for [Atlant Core](https://github.com/AtlantOne/atlant-core).
This repository contains application sources, not server credentials or signing keys.

## Build

1. Build `artifacts/android/atlant.aar` in the matching core checkout using its
   `scripts/build-aar.ps1`. Keep application and core wire formats compatible.
2. Copy that AAR to `artifacts/android/atlant.aar` in this checkout.
3. Configure your Android SDK locally through `ANDROID_HOME` or `local.properties`.
4. Run `./gradlew :app:testDebugUnitTest :app:assembleDebug` (`gradlew.bat` on Windows).

Use the SDK/JDK versions required by the Gradle build. AAR API compatibility is
checked before Kotlin compilation. Release signing keys must stay outside source
control; use your own stable key for application updates.

Original application sources are MIT licensed. Libraries retain their own terms.
See `LICENSE` and `THIRD_PARTY_NOTICES.md`. Experimental protocol limitations are
described in `THREAT_MODEL.md`.
