# Upstream and source intake log

Checked 2026-10-18. This is a read-only intake record for public upstream/source metadata. No external provider authentication, protected-media fetch, remote-code execution, auto-enable, or merge occurred. The tokenized LastWave endpoint is intentionally not reproduced here.

## Upstream review

| Item | Verified evidence | Decision |
|---|---|---|
| Personal fork | [OPZENITSUUU/BitChord](https://github.com/OPZENITSUUU/BitChord), latest visible commit [`edc1e6f36febf71a118e649411d2fc21b70ead45`](https://github.com/OPZENITSUUU/BitChord/commit/edc1e6f36febf71a118e649411d2fc21b70ead45), dated 2026-10-17; fork had no GitHub releases at check time. | No upstream code ported in this change. Existing fork revision is the repository baseline; all changes below are local, reviewed work. |
| Parent release | [kushagrasinghx/BitChord v1.8](https://github.com/kushagrasinghx/BitChord/releases/tag/v1.8), published 2026-10-05 per public release metadata. | Reviewed as a discovery point, not as an install source for this personal fork. Do not point personal updates at the upstream APK feed. |
| Discord | [Public BitChord server listing](https://discord.com/servers/bitchord-1540054967573020772) is visible; `#what-new`/announcement message history was not accessible to this review. | Announcement contents are **unknown/inaccessible**, not treated as instructions or code. No Discord-derived changes were made. |

The repository advertises a separate indexed JavaScript module system loaded through QuickJS and a declarative JSON addon protocol. These are distinct interfaces. A repository or Discord server is neither a native JSON addon endpoint nor a verified module index. Remote JavaScript remains a higher-trust boundary and was not auto-enabled.

## Supplied source intake

| Source | Public evidence / reachability | Decision and unverified areas |
|---|---|---|
| LastWave | Supplied HTTPS manifest responded with JSON; published setup material describes user registration and a unique URL. | Conditional shape fit to BitChord’s JSON addon adapter only. Do not persist the tokenized URL in source or logs. Authentication, endpoint responses, actual stream, playback stability, and codec/output quality were not tested; keep unverified and disabled. |
| Qobuz hosted addon | `https://qobuz-addon.vercel.app/a/manifest.json` responded with JSON describing search/stream and quality settings; manifest says downloads disabled. | Shape fit only; endpoint authentication, provider authorization, actual search/stream/playback, license and codec remain unverified. Do not enable or claim lossless. |
| `vitiko98/qobuz-dl` | [Repository](https://github.com/vitiko98/qobuz-dl), Python CLI/package; repository declares GPL-3.0 and uses credential/token setup. | Not a BitChord addon or QuickJS module. No on-device integration, credential extraction, or download path added. GPL does not grant provider-media rights. |
| `nicopowa/qobuzext` | [Repository](https://github.com/nicopowa/qobuzext), GPL-3.0 WebExtension; README describes reading browser session cookies/local storage and intercepting requests. | Not compatible with either BitChord protocol and conflicts with the no-cookie/token-import rule. Not integrated. |
| `nicopowa/tidalext` | [Repository](https://github.com/nicopowa/tidalext), GPL-3.0 WebExtension using browser-specific APIs and session interception; Android support is described as unstable/testing only. | Not a native BitChord source. No browser-session extraction/download workflow ported. Live provider behavior was not tested. |
| Lastbabe | Supplied endpoint and public root manifest URL returned 404 HTML (“App not found”). | **Unverified/unavailable at those addresses**, not proven incompatible. No integration. |
| GGLos | Public home/status page was reachable; supplied manifest and `/s1`/`/s2` manifest paths returned 403. | **Unverified**, not proven unsupported. Status-page capability claims are not playback evidence. No integration. |

## Local changes, not upstream ports

- Prevent source quality labels alone from being interpreted as a FLAC codec.
- Tie renderer-format telemetry to the current media ID to avoid carrying the previous track’s codec across a queue transition.
- Separate source claims from Media3 runtime format values in Stats for Nerds; qualify bitrate provenance and explain unavailable metrics.
- Harden the generic JSON addon transport with HTTPS-only remote URLs, a debug-only loopback exception, same-origin redirects, JSON content-type validation, and a 1 MiB response limit.
- Add a persisted Now Playing speed selector with the requested preset values.

No individual provider was authenticated or tested for real stream resolution, codec/container signature, Media3 playback start/stability, lossless/Hi-Res, Atmos, or authorized download. Accordingly none is marked working or lossless verified.

## Local implementation and validation

- Lossless claims now require codec/container evidence rather than an addon's quality label; Stats for Nerds separates source-reported metadata from Media3 renderer data, identifies unknown values, and ties renderer format to the current media ID so a previous track cannot leave a stale badge.
- The native JSON addon transport has HTTPS/loopback, same-origin redirect, JSON MIME, and response-size checks. `SourcesScreen` already has a user-initiated Test/Save/Remove flow and preflight health display; it does not expose manifest version, last-check time, or a dedicated Refresh action yet. A successful preflight is not a playback verification.
- Now Playing has a persisted speed preset selector. The personal flavor adds a build-time app name and a non-provider equalizer launcher/notification mark; its intro is tap-to-skip, reduced-motion-aware, and timed to 3.2 seconds without delaying the underlying app composition. Liquid Glass remains default-on for unset preferences while honoring a saved opt-out.
- Update handling now targets this fork, validates the release/asset host and SHA-256 digest, shows byte/total progress (indeterminate without a total), preserves partial files for Range resume, checks package/signing identity and lineage, and remembers a dismissed release. These paths passed unit tests but were not exercised against an actual GitHub release or Android package installer.
- The supplied specification names `com.music.bitchord.personal`, but the checked-out repository's `defaultConfig`, existing production flavor and built APK all use `com.music.bitchord`. The implementation preserves that actual existing identity; no application-ID migration was made.
- Final local checks (2026-10-18): production and personal Android JVM suites each ran 1,094 tests, 0 failures/errors and 1 skipped live Genius-search test; shared UI JVM tests ran 44/44. `:app:assemblePersonalDebug` succeeded. The APK is debug-signed; no `keystore.properties`, ADB binary, or reachable Android device was available, so release signing and comparison against an installed APK certificate remain unavailable.
- A local release manifest and release-notes draft record the built artifact hash, but no public download URL or publication date exists because the personal fork had no release. The app's live feed remains the GitHub Releases API; the draft JSON is not a published or consumed feed.

## Follow-up requirements

Before any source is enabled, perform an owner-authorized preflight and a lawful real-playback test: validate schema/version and endpoint responses; document auth/permissions/license; record bounded health; inspect the response/container signature and current Media3 input format; observe stable playback and fallback; and confirm the negotiated device/output path separately. Download permission must be provider-declared and user-authorized; DRM/access-control bypass is out of scope. For upstream intake, inspect a specific release/commit diff manually, preserve GPL notices, record conflicts and test evidence, and port only reviewed changes.

## Playback-path correction — 2026-10-18

- The lossless-output gate previously accepted LDAC, LHDC and Samsung UHQ based on codec names or sample-rate labels. That is not evidence of bit-exact transport: Sony's own LDAC material describes high-resolution Bluetooth transfer at up to 990 kbps, while Qualcomm explicitly defines aptX Lossless as mathematically bit-for-bit at 16-bit/44.1 kHz and notes that link conditions can scale the mode down. The gate now treats those high-resolution labels as insufficient and accepts Bluetooth only when Android explicitly names aptX Lossless and reports 16-bit/44.1-kHz configuration. Wired/USB/etc. remain potential route eligibility, not a bit-perfect guarantee. References: [Sony LDAC](https://www.sony.co.jp/en/Products/LDAC/), [Qualcomm aptX Lossless](https://www.qualcomm.com/news/releases/2021/09/qualcomm-adds-bluetooth-lossless-audio-technology-snapdragon-sound), [LHDC](https://lhdc.co/).
- Added regressions for lossy/high-resolution Bluetooth labels, missing/wrong aptX metadata, and a source-claimed FLAC/24-bit stream whose actual Media3 input is Opus. Targeted Android tests passed: `AddonPolicyTest`, `AudioPipelineTest`, `AddonSourceTest`, and `PlaybackFormatOwnershipTest` (Gradle `BUILD SUCCESSFUL`).
- No Bluetooth hardware or authorized provider stream was available here; this code correction and test suite do not verify a live aptX route or real music playback.
