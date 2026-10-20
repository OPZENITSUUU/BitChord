# BitChord Playback Implementation Report

**Date:** 2026-10-18
**Repository:** `OPZENITSUUU/BitChord`
**Branch:** `manus/upgrade-audit-2026-10`
**Base commit:** `edc1e6f3` (`Merge pull request #873 from kushagrasinghx/web-player`). Changes are in the working tree and have not been committed.

## Outcome

I made and tested a real playback-path correction: Bluetooth output eligibility for an add-on that requires a lossless-capable route no longer treats LDAC, LHDC, or Samsung UHQ/high sample-rate labels as proof of bit-exact transport. Bluetooth now qualifies only when Android explicitly reports aptX Lossless configured at 16-bit/44.1 kHz. This is a route-eligibility check, not a claim of bit-perfect output. I added regression tests for misleading codec/quality labels and for a source claiming FLAC/24-bit while Media3 actually selects Opus.

The existing stream path is still `SourceRegistry` → `SourceResolver` → `SourceStream` → the playback service's Media3 data-source/player path. Media3 input-format telemetry is tied to the current media ID; Stats for Nerds uses that selected input format for codec/lossless classification and keeps source-reported metadata separate. These checks are covered by unit tests, but they do not replace a live device playback test.

**No supplied provider was verified for end-to-end authorized playback, so no new provider was enabled or represented as working.** The Qobuz hosted addon is the closest protocol fit: its public manifest and search endpoint worked, and BitChord's generic JSON-addon adapter is already implemented. Its stream endpoint, authorization, actual audio, and delivered quality were not tested; its manifest explicitly disallows downloads. The full candidate-by-candidate evidence is in [PLAYBACK_SOURCE_AUDIT.md](PLAYBACK_SOURCE_AUDIT.md), with individual notes under `research/playback-source-audit/`.

## Provider status

| Candidate | Status | Evidence / blocker |
|---|---|---|
| Official TIDAL Android/API | **BLOCKED** | Official native SDK/API materials are public, but this is not a drop-in BitChord addon. Requires registered app credentials and user OAuth authorization; no credentials, authenticated stream resolution, or playback were available. Terms and SDK documentation impose additional playback/copying restrictions. |
| Official Qobuz Android/API | **BLOCKED** | Official API terms require a Qobuz-issued Application ID and secret, plus an authorized integration; no compatible official BitChord adapter or credentials were available. A Qobuz account/subscription alone is not sufficient proof of third-party API authorization. |
| LastWave | **BLOCKED** | The private endpoint/descriptor and empty-query search responded, but the provider requires account/channel verification for a private URL. No real track search, stream resolution, audio playback, format, or download authorization was tested. |
| Qobuz-DL hosted addon (`/a`) | **IMPLEMENTED BUT REAL STREAM NOT VERIFIED** | Only protocol-level support through BitChord's existing generic JSON-addon adapter: manifest and search returned JSON matching the native-addon shape. No stream request or playback was made; authorization and output quality remain unknown. `allowDownloads: 0`. This is not a provider-specific integration or a verified working source. |
| `vitiko98/qobuz-dl` | **INCOMPATIBLE** | Python CLI/library, not BitChord's HTTP-addon or QuickJS protocol. It requires Qobuz credentials/subscription; the inspected login flow is not verified against current service behavior. |
| `nicopowa/qobuzext` | **INCOMPATIBLE** | Browser WebExtension that relies on an active Qobuz web-player session and browser storage/network APIs; not a BitChord HTTP addon or QuickJS module. No cookies/session tokens were imported. |
| `nicopowa/tidalext` | **INCOMPATIBLE** | Browser WebExtension dependent on an authenticated TIDAL browser session, not either BitChord source interface. No session interception, execution, or protected-media fetch was performed. |
| LastBabe / LastBebe | **BLOCKED** | The supplied endpoint returned HTTP 404; no readable manifest or successful endpoint response was available to establish its protocol. |
| GGLos | **BLOCKED** | Homepage/status endpoints were reachable, but the supplied manifest and per-source manifests returned HTTP 403 outside the designated BitChord client. Required headers/auth and protocol contract remain unknown. Dashboard quality/download labels are not playback evidence. |

**Verified working provider count: 0.** No candidate received an authenticated track resolution, stream-media inspection, stable BitChord playback test, measured lossless/Hi-Res result, or authorized download test. No DRM or subscription restrictions were bypassed.

## Tests and build

The targeted source/audio regression command passed:

```bash
./gradlew :app:testProdDebugUnitTest \
  --tests 'com.music.bitchord.AddonPolicyTest' \
  --tests 'com.music.bitchord.AudioPipelineTest' \
  --tests 'com.music.bitchord.AddonSourceTest' \
  --tests 'com.music.bitchord.PlaybackFormatOwnershipTest' \
  --console=plain
```

The complete verification/build command passed with `BUILD SUCCESSFUL` in 25 seconds:

```bash
./gradlew :app:testProdDebugUnitTest \
  :app:testPersonalDebugUnitTest \
  :sharedUi:jvmTest \
  :app:assemblePersonalDebug \
  --console=plain
```

| Test/build target | Result |
|---|---:|
| `:app:testProdDebugUnitTest` | 1,095 tests; 0 failures; 0 errors; 1 skipped |
| `:app:testPersonalDebugUnitTest` | 1,095 tests; 0 failures; 0 errors; 1 skipped |
| `:sharedUi:jvmTest` | 44 tests; 0 failures; 0 errors; 0 skipped |
| `:app:assemblePersonalDebug` | Successful |
| `git diff --check` | Passed |

The single skipped case in each Android variant is `GeniusTest.live genius search and scraping test with noisy titles` (live-network test); it is not counted as a pass.

**Actual stream playback was not tested.** The sandbox had no attached ADB device, and no authorized provider stream/account was available. Thus the tests/build verify code behavior and compilation, not sound output or service entitlement.

## APK artifact and identity

- **APK:** `app/build/outputs/apk/personal/debug/app-personal-universal-debug.apk`
- **Size:** 203,117,009 bytes
- **SHA-256:** `2deb8c935fc0ce202662e482adb6e7b51e77a131aee102a629fbc6e90bc8af60`
- **Package/application ID:** `com.music.bitchord`
- **Version:** `1.8.1-beta1` (`versionCode` 28)
- **Signing:** Android debug certificate; signer SHA-256 `ac90f65637b18f5a290f9ac4862d7c2361515930757447b17178def668334653`

The checked-out baseline uses `com.music.bitchord`; the supplied spec's `com.music.bitchord.personal` did not match the existing source. I preserved the actual baseline application ID and did not migrate it. The APK is debug-signed. There was no connected device or existing installed APK to compare, so **in-place update/signing compatibility is not verified**. Do not assume this debug APK can update a release-signed installation.

## Safe backup and update procedure

1. Keep the current app installed. In BitChord open **Settings → Export data** and save the JSON file somewhere separate. That export contains preferences and listening history, but not login credentials/source secrets, downloaded audio, cache, or artwork. Back up any downloaded audio separately if it matters to you; in-place updates normally retain app data when Android accepts the update.
2. Before installing, compare the currently installed APK's signing certificate with the debug signer SHA-256 above. No device was connected here, so I could not make that comparison. If the signer differs, **do not uninstall** and do not attempt to replace the existing app with this APK; build with the original signing key or test separately.
3. Only after confirming both package ID and signer match, use Android's package installer or `adb install -r` for an in-place update. Do not use `adb uninstall`, clear app data, or reset the device.

## Changed files

The cumulative branch changes include:

**Playback, sources, and telemetry:**
- `app/src/main/java/com/music/bitchord/data/sources/AddonSource.kt`
- `app/src/main/java/com/music/bitchord/data/sources/addon/AddonClient.kt`
- `app/src/main/java/com/music/bitchord/playback/PlaybackService.kt`
- `app/src/main/java/com/music/bitchord/playback/PlaybackFormatOwnership.kt`
- `app/src/main/java/com/music/bitchord/playback/audio/LosslessOutput.kt`
- `app/src/main/java/com/music/bitchord/ui/components/AudioPipelineDialog.kt`
- `shared/src/jvmSharedMain/kotlin/com/music/bitchord/data/NerdStats.kt`
- `app/src/test/java/com/music/bitchord/AddonPolicyTest.kt`
- `app/src/test/java/com/music/bitchord/AddonSourceTest.kt`
- `app/src/test/java/com/music/bitchord/AudioPipelineTest.kt`
- `app/src/test/java/com/music/bitchord/PlaybackFormatOwnershipTest.kt`

**Other spec work already present on the same feature branch:**
- `app/build.gradle.kts`, `wearos/build.gradle.kts`
- `app/src/main/java/com/music/bitchord/MainActivity.kt`
- `app/src/main/java/com/music/bitchord/data/AppUpdateChecker.kt`
- `app/src/main/java/com/music/bitchord/data/settings/AppSettings.kt`
- `app/src/main/java/com/music/bitchord/ui/MainViewModel.kt`
- `app/src/main/java/com/music/bitchord/ui/components/ColdStartIntro.kt`
- `app/src/main/java/com/music/bitchord/ui/components/UpdateAvailableDialog.kt`
- `app/src/main/res/values/strings.xml`, `app/src/main/res/values-zh/strings.xml`, `app/src/main/res/values-zh-rTW/strings.xml`
- `app/src/personal/res/drawable/ic_launcher_foreground.xml`, `app/src/personal/res/drawable/ic_notification_logo.xml`
- `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/NowPlayingScreen.kt`
- `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/PlayerControls.kt`
- `sharedUi/src/jvmTest/kotlin/com/music/bitchord/ui/player/PlaybackSpeedPresetsTest.kt`
- `app/src/test/java/com/music/bitchord/AppUpdateCheckerTest.kt`
- `app/src/test/java/com/music/bitchord/ui/components/StartupIntroTimingTest.kt`
- `PERSONAL_BUILD.md`, `UPSTREAM_SYNC.md`, `RELEASE_NOTES_DRAFT.md`, `release-manifest.draft.json`
- `PLAYBACK_SOURCE_AUDIT.md` and `research/playback-source-audit/01-tidal-official.md` through `09-gglos.md`

## Reproducible build environment

With JDK 21 and Android SDK installed, from the repository root:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_HOME=/opt/android-sdk
export ANDROID_SDK_ROOT=/opt/android-sdk
./gradlew :app:testProdDebugUnitTest :app:testPersonalDebugUnitTest :sharedUi:jvmTest :app:assemblePersonalDebug --console=plain
```


## Follow-on: real local FLAC playback test harness

To make a real device playback check reproducible, this pass adds `PlaybackPipelineFlacInstrumentedTest` plus a generated, tagged 8-second stereo FLAC fixture (`app/src/androidTest/assets/flac/fixture-24bit-96k.flac`; ffprobe reports FLAC, 96,000 Hz, 2 channels, 24-bit raw sample depth). The test submits the local file through the production `PlaybackService` MediaSession and checks player readiness/playing state, duration, seeking, embedded title propagation, the renderer's FLAC/rate/channel telemetry, and error reporting for a missing local file. It explicitly does not claim acoustic audibility or bit-perfect hardware output.

### Results from this follow-on pass

- `:app:compilePersonalDebugAndroidTestKotlin`: **passed**.
- `:app:assemblePersonalDebugAndroidTest`: **passed**; test APK is `app/build/outputs/apk/androidTest/personal/debug/app-personal-debug-androidTest.apk`, 1,424,235 bytes, SHA-256 `a8ddea5d7e37e57ec07278ea47441a16244c6273e5d8b53a06c92f11c09e7977`.
- Fresh forced JVM run (`:app:testProdDebugUnitTest :app:testPersonalDebugUnitTest :sharedUi:jvmTest --rerun-tasks`): **BUILD SUCCESSFUL** in 1m54s. Production Android: 1,095 tests, 0 failures, 0 errors, 1 skipped. Personal Android: 1,095 tests, 0 failures, 0 errors, 1 skipped. Shared UI JVM: 44 tests, 0 failures, 0 errors, 0 skipped. The Chinese locale cases `strings_containsAllBaseKeys[zh-CN]` and `[zh-TW]` both passed in both Android variants. The one skipped test remains the live-network Genius search/scraping case.
- `:app:connectedPersonalDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.music.bitchord.PlaybackPipelineFlacInstrumentedTest`: **not executed**. Gradle failed with `DeviceException: No connected devices!`; SDK ADB listed no attached devices. `/dev/kvm` is also absent. The two new instrumentation methods therefore have compile/package evidence only, not runtime playback results.
- No installation, uninstall, data clear, or signing change was performed. The personal universal debug APK currently on disk is 203,117,009 bytes, package `com.music.bitchord`, version `1.8.1-beta1`/code 28, SHA-256 `2deb8c935fc0ce202662e482adb6e7b51e77a131aee102a629fbc6e90bc8af60`, signed by the Android debug certificate (`ac90f65637b18f5a290f9ac4862d7c2361515930757447b17178def668334653`). The test setup could not compare this signer with an installed APK because no device was connected; update compatibility remains unverified.
- `git diff --check`: passed.

This pass adds a test harness and fixture, not a new provider integration. The named commercial/provider endpoints remain in the statuses above; no authenticated third-party stream or download was established. Run the on-device procedure in [PLAYBACK_TESTING.md](PLAYBACK_TESTING.md) on a disposable emulator/secondary device. Current-pass additions are `PLAYBACK_TESTING.md`, `app/src/androidTest/java/com/music/bitchord/PlaybackPipelineFlacInstrumentedTest.kt`, and `app/src/androidTest/assets/flac/fixture-24bit-96k.flac`.


## Current follow-on — authorized Commons source and playback verification (2026-10-18)

This section updates the earlier report: BitChord now contains an opt-in Wikimedia Commons FLAC source, but **real online playback through BitChord has still not been verified**.

### What changed

`WikimediaCommonsSource` uses the existing `MusicSource`/`SourceRegistry`/`SourceResolver` path. It searches the official Commons API, accepts only FLAC MIME types with per-file Public Domain or CC0 license metadata, rechecks that license when resolving a track, and only follows HTTPS media URLs on `upload.wikimedia.org`. It reports source fields only when Commons provides them and rejects a capped request when the listed bitrate is unknown or exceeds the cap. The source is opt-in by default and requires no service account or subscription.

Regression tests cover live-source search response parsing, Public Domain/CC0 filtering, rechecking the license at stream resolution, refusing a non-Wikimedia media host, and refusing lossless content above a capped request. The Android test suite now includes an online Commons search → source resolution → BitChord MediaSession/Media3 playback case, in addition to the local FLAC and missing-file cases. The Android test compiles and packages, but the online case has not run on a device.

### Real online file evidence versus BitChord playback

A live Commons API query returned HTTP 200 for `File:01 - Gute Nacht (CK 2946-2, ES 383).flac`, with its file page identifying the license as Public Domain. The HTTPS media host was `upload.wikimedia.org`. The file was temporarily fetched for inspection and then removed from the sandbox. `ffprobe` reported FLAC, 44,100 Hz, 2 channels, 16-bit raw sample depth, 795,027 bit/s, and 248.999 seconds; the fetched file size was 24,745,153 bytes.[1]

Commons' listing metadata for this file included bitrate and duration but did **not** include sample rate or bit depth. The app must not infer those values from the file label. The instrumented test therefore expects these source-claimed fields to remain null and expects the actual sample rate to come from Media3's stream/renderer telemetry. If Media3 exposes decoded bit depth, the test checks that it is 16-bit. The external `ffprobe` result verifies the downloaded public-domain FLAC file itself; it does not prove that BitChord resolved or played it.

**Answer to the primary verification question:** no real online track was successfully played in BitChord in this environment. A real online Commons FLAC file was fetched and its file-level format was verified, but the actual app playback step remains unverified.

### Provider status

| Provider/candidate | Current status | Evidence or blocker |
|---|---|---|
| Wikimedia Commons | **IMPLEMENTED — source file verified; BitChord playback NOT VERIFIED** | Public Domain FLAC file and its bytes were verified with `ffprobe`; the online Media3 instrumentation test compiled but could not run without an Android device. This is the only currently viable no-credential source path identified. |
| TIDAL | **BLOCKED** | `ERROR_CODE_IO_BAD_HTTP_STATUS` is Media3's generic error. No authenticated TIDAL stream attempt or current sanitized client log was available, so the actual HTTP status and root cause—expired signature, missing headers, IP gating, or entitlement—remain unknown. The code now refreshes source stream URLs per playback attempt and retains numeric HTTP status diagnostics, but that mitigation has not been proven against TIDAL. An authorized registered app/OAuth flow and eligible account are still required. |
| Official Qobuz | **BLOCKED** | Requires a Qobuz-issued Application ID/secret, an authorized integration, and an eligible account/subscription. Those credentials and a Qobuz-authorized adapter were not available. |
| Hosted Qobuz addon | **BLOCKED — real stream not verified** | Earlier evidence established a reachable manifest and search response, and its manifest says `allowDownloads: 0`. No stream request was made because the candidate delegates to a third-party public backend without demonstrated Qobuz entitlement; the returned URL's authorization/protection behavior is unknown. No protected URL or media was fetched. |
| `qobuzext`, `tidalext`, `qobuz-dl` | **INCOMPATIBLE as supplied** | They are browser extensions requiring an active provider session, or a Python CLI/library, rather than BitChord's native HTTP addon or QuickJS module protocols. Details remain in `PLAYBACK_SOURCE_AUDIT.md`. |
| LastWave | **BLOCKED** | Private endpoint/account-channel verification is required before a real track search and stream can be tested. |
| LastBabe/LastBebe | **BLOCKED** | The supplied manifest endpoint previously returned HTTP 404. |
| GGLos | **BLOCKED** | The supplied manifest routes previously returned HTTP 403 outside the designated client; required authorization/protocol headers remain unknown. |

No DRM, subscription, or protected-session restriction was bypassed. No downloads were enabled for the Qobuz addon.

### Tests and build from this pass

The following completed successfully after the online-test assertions were corrected to distinguish source claims from renderer measurements:

```bash
./gradlew --no-daemon :app:testPersonalDebugUnitTest :app:compilePersonalDebugAndroidTestKotlin :app:assemblePersonalDebugAndroidTest --console=plain
```

Result: **BUILD SUCCESSFUL**. The personal JVM suite had 1,101 tests, 0 failures, 0 errors, and 1 skipped; all 5 `WikimediaCommonsSourceTest` cases passed. Android instrumentation source compilation and test APK assembly passed. `git diff --check` passed.

The actual device test was attempted separately:

```bash
./gradlew --no-daemon :app:connectedPersonalDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.music.bitchord.PlaybackPipelineFlacInstrumentedTest
```

It failed with `DeviceException: No connected devices!`. SDK ADB listed no attached devices; the emulator binary and `/dev/kvm` were absent. Therefore none of the three instrumentation tests ran. There is no claim of verified Media3 playback or audio-sink output.

### APK identity and signing

A personal **debug** APK was built at `app/build/outputs/apk/personal/debug/app-personal-universal-debug.apk` (203,117,934 bytes; SHA-256 `b8b2ac514554cbb5d663a9e3d3f1599beae1ca549c298befc88bac3f7b9fb8f0`). It reports package `com.music.bitchord`, version `1.8.1-beta1`/code 28, and is signed with the Android debug certificate whose SHA-256 is `ac90f65637b18f5a290f9ac4862d7c2361515930757447b17178def668334653`. The instrumentation APK is `app/build/outputs/apk/androidTest/personal/debug/app-personal-debug-androidTest.apk` (1,448,072 bytes; SHA-256 `974d33989d1b7811770e3559d7904da4f8d2cfb2facaafa091bf2869a784cdd3`).

The application ID remains the existing `com.music.bitchord`. No release-signed APK was produced. No device was connected, so the signer could not be compared with an installed BitChord APK; update compatibility is **not verified**. Nothing was installed or uninstalled, user data was not cleared, and signing configuration was not changed. Do not use this debug APK to replace a release-signed installation unless the certificate is independently confirmed to match.

### Changed files in this follow-on

- `app/src/main/java/com/music/bitchord/data/sources/WikimediaCommonsSource.kt`
- `app/src/main/java/com/music/bitchord/data/sources/SourceKind.kt`
- `app/src/main/java/com/music/bitchord/data/sources/SourceRegistry.kt`
- `app/src/main/java/com/music/bitchord/data/sources/SourceResolver.kt`
- `app/src/main/java/com/music/bitchord/data/sources/addon/AddonClient.kt`
- `app/src/main/java/com/music/bitchord/playback/PlaybackService.kt`
- `app/src/test/java/com/music/bitchord/WikimediaCommonsSourceTest.kt`
- `app/src/androidTest/java/com/music/bitchord/PlaybackPipelineFlacInstrumentedTest.kt`
- `PLAYBACK_TESTING.md`

For the precise on-device procedure, see [PLAYBACK_TESTING.md](PLAYBACK_TESTING.md). The APK build and signing guidance remains in [PERSONAL_BUILD.md](PERSONAL_BUILD.md).

## References

[1]: https://commons.wikimedia.org/wiki/File:01_-_Gute_Nacht_%28CK_2946-2%2C_ES_383%29.flac "Public-domain Schubert FLAC file page"
[2]: https://www.mediawiki.org/wiki/API:Imageinfo "MediaWiki image metadata API"


## Follow-on UI, source preference, playlist, and CSV recovery pass (2026-10-20)

### Implemented in source

- Settings now has a search field, category landing cards and category filtering styled to the supplied dark Settings reference, plus the requested `Made by OPZENITSU69` footer.
- The song action menu now persists Hide from Home / Show on Home by exact video ID; it filters only recommendation Home shelves and leaves library, queue, downloads, and search untouched.
- A Home-only Listen Together control is placed immediately left of the profile control.
- Bottom navigation now displays Home → Search → Explore → Library while mapping to the existing internal route IDs.
- Adding a song to selected playlists performs a video-ID duplicate preflight. If any duplicates are found, the dialog names affected playlists and offers Cancel, Skip duplicate, or Keep both. A failed playlist fetch aborts rather than silently adding to an unverifiable playlist.
- Settings has an opt-in Automatic best source preference. It waits for enabled matching sources and compares their resolved `StreamFormat` metadata (codec/lossless classification/bitrate), not a catalogue label. This is source-reported/resolver metadata and does not establish the actual Android output format; no real provider stream or device playback was verified in this pass.
- Full JSON backup/restore remains available. Added separate CSV export/import for monthly listening history. CSV rows are spreadsheet-readable and contain a Base64-encoded full bucket payload for lossless round-trip of internal day/hour/chart details. CSV is history-only; it does not contain preferences, credentials, or settings. Import validates all rows before replacing history and requires the user's confirmation.
- Neutral equalizer branding and OPZENITSU69 app display name/logo edits from the same working branch remain in the build. Application ID remains `com.music.bitchord`.

### Verification

- `:app:compilePersonalDebugKotlin`: **passed**.
- `:app:testPersonalDebugUnitTest`: **passed** — 1,103 tests, 0 failures, 0 errors, 1 skipped. The skipped case is `GeniusTest.live genius search and scraping test with noisy titles`.
- `:app:assemblePersonalDebug`: **passed**.
- `:app:assemblePersonalDebugAndroidTest`: **passed** (instrumentation package compiles; no device execution).
- `git diff --check`: **passed**.
- No ADB device is attached. Settings interactions, duplicate decision behavior against a live account, CSV picker round-trip, and online playback were not device-tested.

### Current APK

- `app/build/outputs/apk/personal/debug/app-personal-universal-debug.apk`
- 205,409,679 bytes; SHA-256 `627d862c0850ecd907946eb3305aa4a8ee31d2d79cd79f378cc93a46ae51bd76`.
- Package `com.music.bitchord`, version `1.8.1-beta1`, version code 28.
- Android debug certificate SHA-256 `ac90f65637b18f5a290f9ac4862d7c2361515930757447b17178def668334653`.
- No installed app/device was available for certificate comparison. This debug APK is not asserted to be update-compatible with any release-signed installation. No app was installed, uninstalled, or had its data cleared.
