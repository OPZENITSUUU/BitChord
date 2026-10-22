# BitChord Personal 1.8.1-beta1 — local draft

This is a draft for the locally built personal debug APK; it is not a published GitHub release. The artifact is signed with the Android debug key and is not asserted to be compatible with an already-installed release build.

## Changes

- Added a `personal` Android flavor with a configurable display name and an original equalizer mark for the launcher and notifications. The actual package name remains `com.music.bitchord`.
- Added a tap-to-skip animated startup intro, honoring the reduced-animation setting and preserving the remaining duration across a screen rotation.
- Added a compact Now Playing speed selector for 0.5×, 0.75×, 1.0×, 1.25×, 1.5×, 1.75× and 2.0×; the saved value continues across tracks.
- Improved Stats for Nerds provenance: source claims are separated from Media3 renderer data, unknown metrics are explained, and stale renderer metadata is not carried over to a different media item.
- Hardened the native JSON addon transport and personal update flow with HTTPS/host, redirect, payload/MIME, digest and signing-identity checks; update progress uses actual bytes and keeps partial downloads for HTTP Range resume.
- Added missing Simplified and Traditional Chinese resource strings.

## Validation and limitations

The production and personal Android unit suites each passed 1,094 tests with no failures/errors and one skipped live Genius-search test. Shared UI JVM tests passed 44/44. A personal debug APK assembled successfully.

No Tidal/Qobuz/LastWave/Lastbabe/GGLos provider was verified for authorized authentication, real stream resolution, stable playback, lossless/Hi-Res output, Atmos or permitted audio download. The existing generic addon protocol is not proof that any named provider works. The personal release has not been published, and the installed app's signing certificate could not be compared because no Android device/ADB was available. Do not uninstall the existing app or treat this debug APK as a safe in-place update without a compatible signing certificate.
