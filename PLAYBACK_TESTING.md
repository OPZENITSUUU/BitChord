# BitChord real-playback test procedure

## What the tests cover

`PlaybackPipelineFlacInstrumentedTest` drives the production `PlaybackService` MediaSession and player/data-source chain. The local-file test uses an 8-second stereo 24-bit/96-kHz FLAC fixture and checks readiness, playing state, duration, seeking, embedded title propagation, renderer telemetry, and missing-file error reporting.

The same suite now includes `onlineCommonsFlacReachesTheBitChordMedia3Renderer`. It performs a live Commons API search for “Gute Nacht,” resolves the returned source track through BitChord's `WikimediaCommonsSource`, queues it with the normal `Song.toMediaItem()` mapping, and attempts playback through the production MediaSession. If run successfully on a test device, it checks the actual renderer codec, 44.1-kHz sample rate, channel count, source identity, source-reported bitrate, and bit depth if Media3 exposes it. For this file the Commons listing omits sample-rate and bit-depth fields; the test requires those to remain unknown in the source claim and measures them from the stream/renderer instead.

This test can establish one authorized public-domain Commons stream through BitChord if it runs successfully. It does not establish TIDAL/Qobuz entitlement, audible speaker output, a bit-perfect DAC path, or authorized downloads. A compile, an APK, or an external `ffprobe` inspection is not a successful BitChord playback test.

## Run on a test device or emulator

Use a disposable emulator or secondary Android test device. `personalDebug` uses application ID `com.music.bitchord` and the Android debug signing certificate. Do not use the procedure on a primary phone with a release-signed BitChord installation; Android may reject the different signer. Do not uninstall the existing app or clear its data to make the test run.

From the repository root with JDK 21 and Android SDK configured:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_HOME=/opt/android-sdk
export ANDROID_SDK_ROOT=/opt/android-sdk
/opt/android-sdk/platform-tools/adb devices -l
./gradlew :app:connectedPersonalDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.music.bitchord.PlaybackPipelineFlacInstrumentedTest \
  --console=plain
```

The connected test task installs the matching `personalDebug` app and test packages on the selected device, then runs the three test methods in `PlaybackPipelineFlacInstrumentedTest`. Inspect the instrumentation result; successful compilation or APK assembly is not a playback result.

## Current environment result

The personal JVM suite passed with 1,101 tests, 0 failures, 0 errors, and 1 skipped; all 5 `WikimediaCommonsSourceTest` cases passed. The Android test Kotlin source compiled, and the instrumentation APK assembled successfully. A real Commons file was separately fetched from its Public Domain Wikimedia upload URL and inspected with `ffprobe`; this verified the file container, not playback in BitChord.

The connected instrumentation execution was attempted and failed with `DeviceException: No connected devices!`. The SDK ADB listed no devices, no emulator binary is installed, and `/dev/kvm` is absent. None of the three Android instrumentation methods ran. No app was installed or changed, and no user data or signing material was touched. See `PLAYBACK_IMPLEMENTATION_REPORT.md` for the APK checksum, provider statuses, and remaining blockers.
