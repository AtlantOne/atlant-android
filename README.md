# Atlant for Android

[Download APK](https://github.com/AtlantOne/atlant-android/releases) · [Main project](https://github.com/AtlantOne/atlant-core) · [Help and feedback](https://github.com/AtlantOne/atlant-core/discussions/categories/q-a)

Atlant's Android app supports both TCP and QUIC, with direct REALITY and XHTTP
on the TCP path and direct QUIC REALITY over UDP. Import a subscription or scan
its QR code, choose a server and connect.
App and domain routing rules let you decide which traffic uses the VPN.

The native [Atlant Core](https://github.com/AtlantOne/atlant-core) handles the
tunnel. Its QUIC REALITY handshake starts directly over UDP, without first
opening a TCP connection. The VPN server does not need its own domain or a
public CA certificate: the client checks its pinned key instead.

The interface supports light and dark themes and English, Russian and Chinese.

## Start using Atlant

1. Download the universal APK from Releases, or select your device architecture.
2. Import an Atlant subscription URL, scan its QR code, or add a server profile.
3. Select the server, choose your transport settings and connect. Accept Android's
   VPN permission prompt when asked.

You need a profile from your own server or its operator. For the protocol,
server downloads and community, visit [Atlant Core](https://github.com/AtlantOne/atlant-core).
Do not include your subscription URL or credentials in public bug reports.

## Build

Installable APKs are available in [Releases](https://github.com/AtlantOne/atlant-android/releases).
Choose the universal APK if you do not know your device architecture.

1. Build `artifacts/android/atlant.aar` in the matching core checkout using its
   `scripts/build-aar.ps1`. Keep application and core wire formats compatible.
2. Copy that AAR to `artifacts/android/atlant.aar` in this checkout.
3. Configure your Android SDK locally through `ANDROID_HOME` or `local.properties`.
4. Run `./gradlew :app:assembleDebug` (`gradlew.bat` on Windows).

For all architectures, use `-PatlantAbis=arm64-v8a,armeabi-v7a,x86,x86_64`.
Add `-PatlantSplitApks=true` to produce architecture-specific and universal APKs.
Public APKs use the release build type and a private release signing key.

Use the SDK/JDK versions required by the Gradle build. AAR API compatibility is
checked before Kotlin compilation. Release signing keys must stay outside source
control; use your own stable key for application updates.

Original application sources are MIT licensed. Libraries retain their own terms.
See `LICENSE` and `THIRD_PARTY_NOTICES.md`. The protocol's security boundaries
are described in `THREAT_MODEL.md`.
