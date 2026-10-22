# BitChord launcher icon cleanup report

- **Branch:** `manus/upgrade-audit-2026-10`
- **Commit:** `06dfce1899d1a0b64b593f3f28bc59a1ecad029d`
- **App ID:** `com.music.bitchord` (unchanged)
- **APK:** `app/build/outputs/apk/personal/debug/app-personal-universal-debug.apk`
- **APK size:** 199,026,764 bytes
- **APK SHA-256:** `4957a6387a90228ee8985101c88210594016a615301f2f2fd31a0b07790ff8e9`

## Implemented

The launcher uses the exact green icon image previously supplied (`spotifylogo.jpeg`). No AI-generated artwork or substitute design was used. The square source image is 736×736; its green artwork bounds were measured at x/y 78–658. The source was cropped to (68,68)–(668,668), retaining 10 source pixels around the artwork and avoiding artwork clipping. The adaptive foreground was centered on a 1024×1024 transparent canvas and scaled so the colored disc occupies about 65.7% of the canvas, close to Android’s approximately 66.7% adaptive safe-area limit. This provides an appropriately large icon without launcher-mask clipping. Legacy density icons are generated at 48, 72, 96, 144 and 192 pixels, with the artwork filling about 97% of each icon tile.

Adaptive background uses the sampled source corner color `#140C0A`, maintaining the original contrast on light and dark launcher surroundings. The manifest has one launcher alias, and the app icon and round icon both point to the same artwork. The Settings multi-icon chooser and its obsolete labels were removed. The personal flavor’s old equalizer foreground override was removed.

## Build and checks

- Clean command: `./gradlew --no-daemon :app:clean :app:testPersonalDebugUnitTest :app:assemblePersonalDebug --console=plain`
- Result: `BUILD SUCCESSFUL`.
- Unit tests: 1,103 tests; 0 failures; 0 errors; 1 skipped.
- APK inspection: package and label confirmed; one `LauncherDefault` alias; adaptive XML and density fallbacks present; old launcher alternatives and old vector foreground absent from the clean APK.
- Source asset check: adaptive artwork alpha bounds are `(164, 164)–(860, 860)` on the 1024-pixel canvas; green artwork bounds are centered. Legacy icon PNGs are 48/72/96/144/192 px.
- Device/emulator check: **not performed**; ADB reported no connected devices. Therefore app-drawer/home-screen/recents/installer visual appearance has not been verified on a physical device or emulator.

## Exact files added/modified

### Added (13)

- `app/src/main/res/drawable-nodpi/ic_launcher_foreground.png`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- `app/src/main/res/mipmap-hdpi/ic_launcher.png`
- `app/src/main/res/mipmap-hdpi/ic_launcher_round.png`
- `app/src/main/res/mipmap-mdpi/ic_launcher.png`
- `app/src/main/res/mipmap-mdpi/ic_launcher_round.png`
- `app/src/main/res/mipmap-xhdpi/ic_launcher.png`
- `app/src/main/res/mipmap-xhdpi/ic_launcher_round.png`
- `app/src/main/res/mipmap-xxhdpi/ic_launcher.png`
- `app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png`
- `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png`
- `app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png`

### Modified (20)

- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/music/bitchord/ui/screens/SettingsSheet.kt`
- `app/src/main/res/drawable/ic_launcher_background.xml`
- `app/src/main/res/values-de/strings.xml`
- `app/src/main/res/values-es/strings.xml`
- `app/src/main/res/values-fr/strings.xml`
- `app/src/main/res/values-he/strings.xml`
- `app/src/main/res/values-hi/strings.xml`
- `app/src/main/res/values-id/strings.xml`
- `app/src/main/res/values-it/strings.xml`
- `app/src/main/res/values-ja/strings.xml`
- `app/src/main/res/values-pl/strings.xml`
- `app/src/main/res/values-pt/strings.xml`
- `app/src/main/res/values-ru/strings.xml`
- `app/src/main/res/values-th/strings.xml`
- `app/src/main/res/values-tr/strings.xml`
- `app/src/main/res/values-vi/strings.xml`
- `app/src/main/res/values-zh-rTW/strings.xml`
- `app/src/main/res/values-zh/strings.xml`
- `app/src/main/res/values/strings.xml`

## Exact files removed (120)

All deleted paths from this icon-only cleanup:

- `app/src/main/java/com/music/bitchord/AppIcon.kt`
- `app/src/main/res/drawable-nodpi/ic_custom_green_drip.png`
- `app/src/main/res/drawable-nodpi/ic_custom_spotify_chrome.png`
- `app/src/main/res/drawable-nodpi/ic_custom_spotify_green.png`
- `app/src/main/res/drawable-nodpi/ic_custom_spotify_purple.png`
- `app/src/main/res/drawable/ic_launcher_foreground.xml`
- `app/src/main/res/mipmap-anydpi/ic_launcher.xml`
- `app/src/main/res/mipmap-anydpi/ic_launcher_round.xml`
- `app/src/main/res/mipmap-anydpi/ic_liquid_glass.xml`
- `app/src/main/res/mipmap-anydpi/ic_liquid_glass_clear.xml`
- `app/src/main/res/mipmap-anydpi/ic_liquid_glass_clear_dark.xml`
- `app/src/main/res/mipmap-anydpi/ic_liquid_glass_dark.xml`
- `app/src/main/res/mipmap-anydpi/ic_pixel_glass.xml`
- `app/src/main/res/mipmap-anydpi/ic_pixel_glass_clear.xml`
- `app/src/main/res/mipmap-anydpi/ic_pixel_glass_clear_dark.xml`
- `app/src/main/res/mipmap-anydpi/ic_pixel_glass_dark.xml`
- `app/src/main/res/mipmap-anydpi/ic_samsung_glass.xml`
- `app/src/main/res/mipmap-anydpi/ic_samsung_glass_clear.xml`
- `app/src/main/res/mipmap-anydpi/ic_samsung_glass_clear_dark.xml`
- `app/src/main/res/mipmap-anydpi/ic_samsung_glass_dark.xml`
- `app/src/main/res/mipmap-hdpi/ic_glass_background.png`
- `app/src/main/res/mipmap-hdpi/ic_glass_clear_background.png`
- `app/src/main/res/mipmap-hdpi/ic_glass_clear_dark_background.png`
- `app/src/main/res/mipmap-hdpi/ic_glass_dark_background.png`
- `app/src/main/res/mipmap-hdpi/ic_glass_monochrome.png`
- `app/src/main/res/mipmap-hdpi/ic_launcher.webp`
- `app/src/main/res/mipmap-hdpi/ic_launcher_round.webp`
- `app/src/main/res/mipmap-hdpi/ic_liquid_glass_clear_dark_foreground.png`
- `app/src/main/res/mipmap-hdpi/ic_liquid_glass_clear_foreground.png`
- `app/src/main/res/mipmap-hdpi/ic_liquid_glass_dark_foreground.png`
- `app/src/main/res/mipmap-hdpi/ic_liquid_glass_foreground.png`
- `app/src/main/res/mipmap-hdpi/ic_pixel_glass_clear_dark_foreground.png`
- `app/src/main/res/mipmap-hdpi/ic_pixel_glass_clear_foreground.png`
- `app/src/main/res/mipmap-hdpi/ic_pixel_glass_dark_foreground.png`
- `app/src/main/res/mipmap-hdpi/ic_pixel_glass_foreground.png`
- `app/src/main/res/mipmap-hdpi/ic_samsung_glass_clear_dark_foreground.png`
- `app/src/main/res/mipmap-hdpi/ic_samsung_glass_clear_foreground.png`
- `app/src/main/res/mipmap-hdpi/ic_samsung_glass_dark_foreground.png`
- `app/src/main/res/mipmap-hdpi/ic_samsung_glass_foreground.png`
- `app/src/main/res/mipmap-mdpi/ic_glass_background.png`
- `app/src/main/res/mipmap-mdpi/ic_glass_clear_background.png`
- `app/src/main/res/mipmap-mdpi/ic_glass_clear_dark_background.png`
- `app/src/main/res/mipmap-mdpi/ic_glass_dark_background.png`
- `app/src/main/res/mipmap-mdpi/ic_glass_monochrome.png`
- `app/src/main/res/mipmap-mdpi/ic_launcher.webp`
- `app/src/main/res/mipmap-mdpi/ic_launcher_round.webp`
- `app/src/main/res/mipmap-mdpi/ic_liquid_glass_clear_dark_foreground.png`
- `app/src/main/res/mipmap-mdpi/ic_liquid_glass_clear_foreground.png`
- `app/src/main/res/mipmap-mdpi/ic_liquid_glass_dark_foreground.png`
- `app/src/main/res/mipmap-mdpi/ic_liquid_glass_foreground.png`
- `app/src/main/res/mipmap-mdpi/ic_pixel_glass_clear_dark_foreground.png`
- `app/src/main/res/mipmap-mdpi/ic_pixel_glass_clear_foreground.png`
- `app/src/main/res/mipmap-mdpi/ic_pixel_glass_dark_foreground.png`
- `app/src/main/res/mipmap-mdpi/ic_pixel_glass_foreground.png`
- `app/src/main/res/mipmap-mdpi/ic_samsung_glass_clear_dark_foreground.png`
- `app/src/main/res/mipmap-mdpi/ic_samsung_glass_clear_foreground.png`
- `app/src/main/res/mipmap-mdpi/ic_samsung_glass_dark_foreground.png`
- `app/src/main/res/mipmap-mdpi/ic_samsung_glass_foreground.png`
- `app/src/main/res/mipmap-nodpi/ic_custom_green_drip.png`
- `app/src/main/res/mipmap-nodpi/ic_custom_spotify_chrome.png`
- `app/src/main/res/mipmap-nodpi/ic_custom_spotify_green.png`
- `app/src/main/res/mipmap-nodpi/ic_custom_spotify_purple.png`
- `app/src/main/res/mipmap-xhdpi/ic_glass_background.png`
- `app/src/main/res/mipmap-xhdpi/ic_glass_clear_background.png`
- `app/src/main/res/mipmap-xhdpi/ic_glass_clear_dark_background.png`
- `app/src/main/res/mipmap-xhdpi/ic_glass_dark_background.png`
- `app/src/main/res/mipmap-xhdpi/ic_glass_monochrome.png`
- `app/src/main/res/mipmap-xhdpi/ic_launcher.webp`
- `app/src/main/res/mipmap-xhdpi/ic_launcher_round.webp`
- `app/src/main/res/mipmap-xhdpi/ic_liquid_glass_clear_dark_foreground.png`
- `app/src/main/res/mipmap-xhdpi/ic_liquid_glass_clear_foreground.png`
- `app/src/main/res/mipmap-xhdpi/ic_liquid_glass_dark_foreground.png`
- `app/src/main/res/mipmap-xhdpi/ic_liquid_glass_foreground.png`
- `app/src/main/res/mipmap-xhdpi/ic_pixel_glass_clear_dark_foreground.png`
- `app/src/main/res/mipmap-xhdpi/ic_pixel_glass_clear_foreground.png`
- `app/src/main/res/mipmap-xhdpi/ic_pixel_glass_dark_foreground.png`
- `app/src/main/res/mipmap-xhdpi/ic_pixel_glass_foreground.png`
- `app/src/main/res/mipmap-xhdpi/ic_samsung_glass_clear_dark_foreground.png`
- `app/src/main/res/mipmap-xhdpi/ic_samsung_glass_clear_foreground.png`
- `app/src/main/res/mipmap-xhdpi/ic_samsung_glass_dark_foreground.png`
- `app/src/main/res/mipmap-xhdpi/ic_samsung_glass_foreground.png`
- `app/src/main/res/mipmap-xxhdpi/ic_glass_background.png`
- `app/src/main/res/mipmap-xxhdpi/ic_glass_clear_background.png`
- `app/src/main/res/mipmap-xxhdpi/ic_glass_clear_dark_background.png`
- `app/src/main/res/mipmap-xxhdpi/ic_glass_dark_background.png`
- `app/src/main/res/mipmap-xxhdpi/ic_glass_monochrome.png`
- `app/src/main/res/mipmap-xxhdpi/ic_launcher.webp`
- `app/src/main/res/mipmap-xxhdpi/ic_launcher_round.webp`
- `app/src/main/res/mipmap-xxhdpi/ic_liquid_glass_clear_dark_foreground.png`
- `app/src/main/res/mipmap-xxhdpi/ic_liquid_glass_clear_foreground.png`
- `app/src/main/res/mipmap-xxhdpi/ic_liquid_glass_dark_foreground.png`
- `app/src/main/res/mipmap-xxhdpi/ic_liquid_glass_foreground.png`
- `app/src/main/res/mipmap-xxhdpi/ic_pixel_glass_clear_dark_foreground.png`
- `app/src/main/res/mipmap-xxhdpi/ic_pixel_glass_clear_foreground.png`
- `app/src/main/res/mipmap-xxhdpi/ic_pixel_glass_dark_foreground.png`
- `app/src/main/res/mipmap-xxhdpi/ic_pixel_glass_foreground.png`
- `app/src/main/res/mipmap-xxhdpi/ic_samsung_glass_clear_dark_foreground.png`
- `app/src/main/res/mipmap-xxhdpi/ic_samsung_glass_clear_foreground.png`
- `app/src/main/res/mipmap-xxhdpi/ic_samsung_glass_dark_foreground.png`
- `app/src/main/res/mipmap-xxhdpi/ic_samsung_glass_foreground.png`
- `app/src/main/res/mipmap-xxxhdpi/ic_glass_background.png`
- `app/src/main/res/mipmap-xxxhdpi/ic_glass_clear_background.png`
- `app/src/main/res/mipmap-xxxhdpi/ic_glass_clear_dark_background.png`
- `app/src/main/res/mipmap-xxxhdpi/ic_glass_dark_background.png`
- `app/src/main/res/mipmap-xxxhdpi/ic_glass_monochrome.png`
- `app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp`
- `app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.webp`
- `app/src/main/res/mipmap-xxxhdpi/ic_liquid_glass_clear_dark_foreground.png`
- `app/src/main/res/mipmap-xxxhdpi/ic_liquid_glass_clear_foreground.png`
- `app/src/main/res/mipmap-xxxhdpi/ic_liquid_glass_dark_foreground.png`
- `app/src/main/res/mipmap-xxxhdpi/ic_liquid_glass_foreground.png`
- `app/src/main/res/mipmap-xxxhdpi/ic_pixel_glass_clear_dark_foreground.png`
- `app/src/main/res/mipmap-xxxhdpi/ic_pixel_glass_clear_foreground.png`
- `app/src/main/res/mipmap-xxxhdpi/ic_pixel_glass_dark_foreground.png`
- `app/src/main/res/mipmap-xxxhdpi/ic_pixel_glass_foreground.png`
- `app/src/main/res/mipmap-xxxhdpi/ic_samsung_glass_clear_dark_foreground.png`
- `app/src/main/res/mipmap-xxxhdpi/ic_samsung_glass_clear_foreground.png`
- `app/src/main/res/mipmap-xxxhdpi/ic_samsung_glass_dark_foreground.png`
- `app/src/main/res/mipmap-xxxhdpi/ic_samsung_glass_foreground.png`
- `app/src/personal/res/drawable/ic_launcher_foreground.xml`
