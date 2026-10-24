# Third-party code

- Android camera QR scanning: ZXing Android Embedded 4.3.0 by JourneyApps,
  Apache-2.0, https://github.com/journeyapps/zxing-android-embedded.

- Panel and Windows GUI `web/qrcode.js`: QR Code Generator by Kazuhiko Arase,
  MIT. Full notice: the adjacent `QRCODE-LICENSE.txt`. Vendored upstream source
  SHA-256: `79ec86f82856005b1c887905cfccfcfbec3821ca61c7fd5a952faa5f778f791c`.
- Android QR encoding: ZXing core 3.5.3, Apache-2.0,
  https://github.com/zxing/zxing (resolved from Maven Central).

The root Apache-2.0 license applies to original Atlant core code. Client
applications retain MIT under `client/LICENSE` and `app/LICENSE`. Original
management-panel code has its own GPL-3.0-only license in `management/LICENSE`;
see `management/LICENSING.md` for its boundary. Third-party code retains
its original copyright notices and licenses; the root license does not relicense it.

- `third_party/utls`: local fork of refraction-networking/uTLS v1.8.2. See its
  `LICENSE`, source headers and `ATLANT_PATCHES.md` for the optional ClientHello hook.
- `third_party/uquic`: local fork of refraction-networking/uQUIC. See its
  `LICENSE`, source headers and `ATLANT_PATCHES.md` for Atlant modifications.
- `third_party/quic-go`: local fork of quic-go v0.63.0. See its `LICENSE`
  and `ATLANT_PATCHES.md` for the send-only GSO adapter. Its two identical
  `internal/testdata/priv.key` fixtures are public upstream test keys, not
  deployment credentials; never use them for a real server.
- Go dependencies are pinned by each module's `go.mod` and `go.sum`. Their
  licenses also apply when redistributing a binary that contains them.
- The Gradle wrapper is a required build tool, not a VPN release binary. Its
  source headers and the Gradle project's licensing terms remain applicable.
- Windows TUN builds require the official Wintun runtime. It is not included
  in the public source export; consult its distribution terms before packaging it.

The public uTLS example private key in `testdata` is an upstream test fixture,
never a production credential. Publication checking exempts only its exact
known path and SHA256; replacing its contents causes a finding.
