# Personal Android build

The `personal` product flavor supplies a non-provider equalizer mark for the launcher/notification and defaults the display name to **BitChord Personal**. It deliberately retains the production application ID, `com.music.bitchord`, so Android treats it as the same app identity. It cannot be installed alongside the `prod` flavor; the separate `dev` flavor remains `com.dev.bitchord`.

## Build

From the repository root, with the Android SDK components required by `app/build.gradle.kts` installed:

```bash
./gradlew :app:testPersonalDebugUnitTest :sharedUi:jvmTest
./gradlew :app:assemblePersonalDebug
```

To choose another launcher display name at build time:

```bash
./gradlew -PbitchordAppName="My BitChord" :app:assemblePersonalDebug
```

`personal` reuses the production package identity. A debug build is signed with the debug key and is not assumed to update a release-signed installation. For an in-place release update, configure the existing project signing properties with the owner's original signing key; do not uninstall the installed app to work around a signature mismatch. The release build may be unsigned when that key is absent.

## Brand resources

Flavor-specific resources override same-named `main` resources. The checked-in personal flavor includes:

- `app/src/personal/res/drawable/ic_launcher_foreground.xml` — adaptive launcher foreground.
- `app/src/personal/res/drawable/ic_notification_logo.xml` — monochrome notification icon.

The adaptive icon XML and background continue to be provided by the existing `main` resources. Replace the personal foreground/background resources with original artwork under `app/src/personal/res/` if desired; keep the artwork inside the adaptive icon safe zone. The personal intro displays the launcher foreground and build-time app name. No Spotify, Tidal, or Qobuz identity is implied.

Android does not reliably support replacing an installed app-drawer icon with an arbitrary gallery-picked image at runtime. A selected image can be used as in-app artwork or, on launchers that support it, a user-created shortcut; the installed app icon remains a build-time adaptive-icon resource.
