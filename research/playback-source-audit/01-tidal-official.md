# Official TIDAL Android/API access — evidence note

**Audit date:** 2026-10-18. **Scope:** public official TIDAL materials, read-only; no login, token request, SDK execution, or playback test was performed.

## Finding

TIDAL offers an official Android Kotlin SDK with `auth` and `player` modules, but that is a native Android integration, not a BitChord addon implementation. The published BitChord native addon contract expects JSON at `/manifest.json`, `/search`, and `/stream`; TIDAL's official API is a JSON:API REST API with OAuth bearer authorization, while its Player module is an Android SDK requiring Android application code and TIDAL auth/event dependencies. No evidence reviewed declares these BitChord endpoints or a QuickJS-compatible module, so direct drop-in compatibility is **not established**. [1] [2] [3]

The official Android Player README describes catalog streaming and ExoPlayer-based playback. It says client-credentials authentication permits 30-second previews, while full-length playback requires Device Login or Authorization Code Flow; developer app registration and client credentials are prerequisites. The official platform overview calls the Player module the only allowed way for third-party apps to incorporate TIDAL playback and says its stated third-party playback is previews using an official, unmodified Player module. This is a policy qualification, not proof that a particular integration or account is entitled to full-length playback. [1] [2] [4]

OAuth is required for API requests. TIDAL documents client credentials, authorization code with PKCE, and refresh-token flows; user-context authorization needs end-user approval. Developer Terms require TIDAL platform login credentials to access Developer Tools, limit use to non-commercial offerings, prohibit stream ripping/permanent copies, and limit local caching to temporary metadata and cover art. [3] [5] The Android SDK repository identifies an Apache-2.0 license; that license covers repository code and should not be read as a license to TIDAL music content. [2]

## Verification boundary

Public official docs/repository are reachable. No access token/client registration was used and no authenticated API request or playback was tested. The API reference describes `/trackManifests/{id}` as a manifest with streaming URIs and format details, but a manifest reference is not a playable source and cannot establish access, decryption, entitlement, or output. Stream authentication, source resolution, audio playback, true lossless/Hi-Res output, and authorized downloads are **not verified**. The terms' prohibition on making permanent copies and allowance for only limited temporary metadata/art caching does not authorize third-party audio downloads. [5] [6]

## References

[1]: https://developer.tidal.com/documentation/api-sdk/api-sdk-overview "TIDAL API and SDK overview"
[2]: https://github.com/tidal-music/tidal-sdk-android "Official TIDAL Android SDK repository"
[3]: https://developer.tidal.com/documentation/api-sdk/api-sdk-authorization "TIDAL authorization documentation"
[4]: https://github.com/tidal-music/tidal-sdk-android/blob/main/player/README.md "Official Android Player module README"
[5]: https://developer.tidal.com/documentation/guidelines/guidelines-developer-terms-1_0 "TIDAL Developer Terms"
[6]: https://tidal-music.github.io/tidal-api-reference/ "Official TIDAL Web API reference"
