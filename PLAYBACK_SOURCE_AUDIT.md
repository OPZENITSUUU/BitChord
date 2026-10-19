# BitChord Playback Source Audit

## Scope and interpretation

This report synthesizes the completed source audits supplied for the nine candidates below. It uses only the structured facts and evidence URLs in those audits. No additional research, credential use, sign-in, protected-media retrieval, remote code execution, DRM bypass, download, installation, or playback test was performed.

**Important distinction:** an HTTP endpoint or manifest being reachable only proves reachability of that endpoint/manifest. It does **not** prove that a track can be resolved to a usable stream, that the stream is authorized, that audio can be fetched, or that BitChord can play it. Those are reported separately for every candidate.

## At-a-glance status

| Candidate | Reachability / manifest | BitChord compatibility | Actual playback | Primary blocker |
|---|---|---|---|---|
| Official TIDAL Android/API access | Public documentation and repositories reachable; actual manifest not retrieved [1][2][3][4][5][6] | Not a drop-in native addon or demonstrated QuickJS module | Not verified | No registered app/user OAuth authorization, no playback environment, and no adapter matching BitChord's contract [1][2][3][4] |
| Official Qobuz Android/API access | Public app/help/legal/API materials reachable; no authenticated API or manifest access [7][8][9][10] | Not a drop-in native addon or demonstrated QuickJS module | Not verified | Qobuz-issued Application ID/secret and an authorized integration are unavailable; no public official BitChord adapter [7] |
| LastWave | Supplied private endpoint and `/manifest.json` reachable; blank `/search` returned 200 empty result; track search/stream not tested [13][14][15] | Partially addon-shaped; field-level compatibility unverified | Not verified | Provider account/channel verification and private endpoint access were not performed; no track-level resolution/audio test [13][14][15] |
| Qobuz-DL hosted addon (`/a` prefix) | Public `/a/manifest.json` and `/a/search?q=test` returned 200 [16][17][18][19] | Native manifest/search shape supported; stream response untested; not QuickJS [16][17][18][19][20][21] | Not verified | No `/a/stream/{id}` request or protected-media fetch was made [16][17][18] |
| qobuz-dl | Public repository/source reachable; no execution or protected access [22][23][24][25][26][27] | Not compatible as native HTTP addon or QuickJS module | Not verified | Python CLI/library requires an untested current authenticated Qobuz login/stream-resolution flow [22][24][25] |
| QobuzExt | Public repository/source reachable; no installation or execution [28][29][30][31] | Incompatible as-is with native HTTP addon and QuickJS protocols | Not verified | Requires an authorized active Qobuz web-player browser session and is the wrong runtime/interface [28][30][31] |
| TidalExt | Public repository/source/release materials reachable; no installation or execution [32][33][34][35][36][37] | Incompatible as-is with native HTTP addon and QuickJS protocols | Not verified | Browser extension requires an authenticated TIDAL web session and does not implement BitChord's interfaces [32][34][35] |
| LastBabe / LastBebe addon | Supplied manifest endpoint returned HTTP 404; manifest/endpoints could not be retrieved [38][39][40][41][42] | Unverified | Not verified | No readable manifest, successful endpoint response, or official source identifying the protocol [38][39][40][41] |
| GGLos | Homepage/status reachable; direct manifest and per-source manifests returned HTTP 403 outside the designated client [43][44][45][46][47][48][49] | Not independently confirmed | Not verified | Direct manifest access is client-gated; required BitChord headers/auth and endpoint contracts are unknown [44][45][46] |

---

## Candidate audits

### 1. Official TIDAL Android/API access (`tidal-official`)

- **Reachability and manifest status:** Official API documentation, API reference, Android SDK repository, and player README were publicly reachable. The API reference documents `GET /trackManifests/{id}` as an endpoint that can provide streaming URIs and format details, but no manifest was retrieved. Therefore, documentation/endpoint reachability is established, while actual manifest reachability, source resolution, and stream usability are **not verified** [1][2][3][4][5][6].
- **Runtime and compatibility:** The official SDK is a Kotlin/native Android library; its Player module uses AndroidX Media3/ExoPlayer. The public API is a REST/JSON:API service. This is not a drop-in match for BitChord's native routes (`/manifest.json`, `/search`, `/stream`) and no matching official QuickJS module/protocol implementation was found in the reviewed evidence. An adapter would require implementation work and authorized API/SDK use [1][2][3].
- **Authentication:** OAuth 2.1 access is required. Documented flows include client credentials, authorization code with PKCE, and refresh tokens. The Android Player requires a `CredentialsProvider`; its README states client credentials support 30-second previews, while full-length playback requires Device Login or Authorization Code Flow. Developer app registration/client credentials and TIDAL login credentials are required for Developer Tools. No credentials or authenticated request were used [1][3][4].
- **Stream resolution:** The existence of the documented track-manifest endpoint is not evidence that a manifest was obtained. No source URI, entitlement result, required headers, or playable stream was inspected [6].
- **Actual playback:** **Not verified.** The official Player documentation describes playback functionality and previews, but the SDK was not built or run, no track was resolved, and no audio was played [1][2][3].
- **Quality:** **Lossless/Hi-Res not verified.** No authorized stream or audio output was accessed or measured [1][2][3].
- **Downloads:** **Not verified and not attempted.** The reviewed Developer Terms prohibit stream ripping/permanent copies and allow only narrowly necessary temporary local caching for metadata and cover art; they do not authorize third-party audio downloads [5].
- **License/runtime notes:** The Android SDK repository identifies Apache-2.0 for the SDK source, not for TIDAL content. Developer Terms separately govern Developer Tools and state the relevant non-commercial/account restrictions [2][5].
- **Exact blocker/status:** No registered TIDAL app credentials or user OAuth authorization, playback environment, or BitChord-compatible adapter was supplied or used. Consequently, stream authentication/resolution, playback, quality, and authorized downloads remain unverified [1][2][3][4][5][6].

### 2. Official Qobuz Android/API access (`qobuz-official`)

- **Reachability and manifest status:** The official Qobuz API Terms PDF, app listing, help pages, and legal terms were reachable without authentication. No authenticated API call, manifest retrieval, or protected stream request was performed; public consumer-app materials are not manifest/playback evidence for BitChord [7][8][9][10].
- **Runtime and compatibility:** Official evidence supports Qobuz's own Android smartphone/tablet application, with help materials also listing Android TV and vehicle options. It does not establish a public Android SDK or BitChord-ready runtime. Qobuz's REST API uses different routes, parameters, response shapes, and authentication from BitChord's `/manifest.json`, `/search?q=`, and `/stream/{id}` contract. No official QuickJS module or matching adapter was identified [7][8][9].
- **Authentication:** The official API Terms require prior agreement and valid Qobuz-issued Application ID and Application secret; they prohibit sharing those credentials. Consumer streaming additionally requires a Qobuz account/subscription. No sign-in, credential request, token exchange, or API authentication was performed [7][10].
- **Stream resolution:** **Not verified.** No authenticated source URL resolution was attempted. A third-party profile in the supplied evidence describes stream URL generation, but it is not direct reproducible evidence of a playable URL and is not treated as such [11][12].
- **Actual playback:** **Not verified in BitChord.** Qobuz first-party app/help materials describe playback in Qobuz applications, not playback through BitChord [8][9].
- **Quality:** Qobuz's first-party listing advertises CD lossless and Hi-Res up to 24-bit/192 kHz, but actual format/quality delivered to BitChord is **not verified** [8][9].
- **Downloads:** **Not verified through BitChord.** Qobuz terms distinguish the download store and subscription offline import. Offline imports play within Qobuz applications and are subscription-dependent; no Qobuz file was downloaded [10].
- **License/runtime notes:** The API Terms grant a limited, non-exclusive right subject to restrictions and Qobuz-issued credentials. No official open-source Android integration license or official BitChord adapter was identified [7].
- **Exact blocker/status:** Qobuz-issued app ID/secret and an authorized integration are unavailable in this audit, and no public official BitChord-compatible adapter was evidenced. Protected stream resolution, playback, quality, and downloads remain unverified [7][8][9][10].

### 3. LastWave (`com.lastwave.addon`)

- **Reachability and manifest status:** The supplied private endpoint was reachable. Its root descriptor and `/manifest.json` returned HTTP 200 JSON describing LastWave; a blank `/search` returned HTTP 200 with an empty result object. No track-specific search or `/stream` request was made. This establishes descriptor/endpoint reachability only, not track manifest or stream reachability [13][14][15].
- **Runtime and compatibility:** The provider is a declared HTTP/JSON addon service. Its descriptor declares search and stream resources, which aligns at a high level with BitChord's native JSON routes. Field-level schema compatibility and real-query `/search` and `/stream` behavior were not tested. No QuickJS-module compatibility was evidenced; LastWave's own client is described as native Kotlin/Jetpack Compose Android [13][16].
- **Authentication:** The official addon setup guide says users create an account and verify Telegram or Discord membership to obtain a private, quota-limited endpoint. This audit did not sign in, request credentials, or attempt account verification. Stream-level authentication behavior is unknown [13][14].
- **Stream resolution:** **Not verified.** No real track query or `/stream/{id}` request was made; source resolver behavior, resulting URL/format, and required headers are unknown [13][14].
- **Actual playback:** **Not verified.** No audio was fetched or played. A descriptor or reachable endpoint is not playback evidence [13][14][15].
- **Quality:** **Lossless/Hi-Res not verified.** Descriptor options claim Lossless, Dolby Atmos, High, and Low, and product/setup pages claim FLAC/lossless, but no stream metadata, bit depth, sample rate, or playback was independently reproduced [13][14][15].
- **Downloads:** **Not verified.** No download was attempted and the reviewed evidence does not establish permission to save or redistribute media [13][14][16].
- **License/runtime notes:** The LastWave Native repository identifies GPL-3.0 for the Android application. No separate license for the hosted addon service/backend was identified [16][17].
- **Exact blocker/status:** Account/channel verification required to obtain/access the private endpoint was not performed, and no authorized track-level resolution or audio test was attempted. Authentication at stream time, source resolution, playback, true quality, and authorized downloads remain unverified [13][14][15].

### 4. Qobuz-DL hosted addon (`com.bitchord.qobuz.qobuzdl`)

- **Reachability and manifest status:** The public hosted `/a/manifest.json` returned HTTP 200, and read-only `GET /a/search?q=test` returned HTTP 200 with `{tracks:[...]}`. This verifies manifest and search reachability only. No `/a/stream/{id}` request was made, so stream reachability and actual playback remain separate unresolved questions [18][19][20].
- **Runtime and compatibility:** The repository describes a TypeScript/Node.js 20+ HTTP service deployed at a Vercel-hosted URL, not a QuickJS module. The manifest/search shape and `/a` prefix align with BitChord's native protocol at manifest/search level; the source registers `/a/manifest.json`, `/a/search`, and `/a/stream/:id`. A valid stream response was not tested [18][19][20][21][22].
- **Authentication:** The README/source state that the addon does not handle Qobuz credentials and uses public JSON backends. Whether upstream stream resolution requires separate authentication is **not verified**; no credential or sign-in flow was attempted [20][21].
- **Stream resolution:** **Not verified.** No stream URL was requested, obtained, inspected, or used. Manifest/search reachability does not establish that a track ID resolves to a valid stream [18][19].
- **Actual playback:** **Not verified.** No audio was fetched or played and no player/device test occurred [18][19][20].
- **Quality:** **Not verified.** Search results may label tracks FLAC/LOSSLESS, but metadata labels are not direct evidence of delivered codec, bit depth, sample rate, or Hi-Res fidelity [18][20].
- **Downloads:** **Not verified as an authorization matter.** The manifest explicitly declares `allowDownloads: 0`, so BitChord should not offer addon downloads. No download was attempted [18][20].
- **License/runtime notes:** Public GitHub metadata identifies GPL-3.0. The hosted deployment's precise build/revision correspondence to current source was not established [20][22].
- **Exact blocker/status:** The candidate is accessible and partially protocol-compatible (manifest/search verified), but stream resolution, authentication, audio quality, and Android playback remain unverified because no stream request or protected-media fetch was made [18][19][20][21].

### 5. qobuz-dl (`vitiko98/qobuz-dl`)

- **Reachability and manifest status:** The public GitHub repository and raw source were reachable anonymously. No service manifest exists in the inspected evidence, and no installation, execution, login, or protected media access occurred [23][24][25][26][27].
- **Runtime and compatibility:** This is a Python package/CLI (`qobuz-dl`/`qdl` scripts), not a JSON HTTP service. The inspected repository exposes no `/manifest.json`, `/search`, or `/stream` endpoints and no QuickJS module declaration or exports. It cannot be loaded by BitChord without a separately developed adapter [23][24][25].
- **Authentication:** The README requires an active Qobuz subscription and the source performs Qobuz user/login using email/password plus app ID/secrets. Issue #329 reports login failures after an alleged Qobuz migration to OAuth/reCAPTCHA; that issue is user-reported and not treated as independently confirmed official documentation. No sign-in or workaround was attempted [23][24][26].
- **Stream resolution:** Source constructs a track/getFileUrl request with `intent=stream` and a format ID, then the downloader consumes the returned URL. No live authenticated track metadata or URL was resolved, so current source resolution is **not verified** [25].
- **Actual playback:** **Not verified.** The inspected source provides download-oriented Python code and no BitChord `/stream` route; no audio was fetched or played [23][25].
- **Quality:** The README advertises FLAC/MP3 downloading and source declares MP3/FLAC quality IDs and returned bit-depth/sampling-rate metadata. Delivered lossless/Hi-Res media and playback quality are **not verified** [23][25].
- **Downloads:** **Not verified.** An active subscription is stated as required, but no account, entitlement, or download was tested; a manifest or URL would not by itself prove authorization [23][25].
- **License/runtime notes:** GPL-3.0 is identified by repository metadata [23][27].
- **Exact blocker/status:** It is unsuitable for direct BitChord native-addon or QuickJS integration. Current authentication, stream resolution, playback, delivered quality, and authorized download status remain unverified [23][24][25][26].

### 6. QobuzExt (`nicopowa/qobuzext`)

- **Reachability and manifest status:** The public repository and source were reachable. No BitChord manifest or HTTP endpoint was identified, and the extension was not installed or run [28][29][30][31].
- **Runtime and compatibility:** QobuzExt is a vanilla JavaScript Chromium Manifest V3 browser extension with extension APIs, a module service worker, and a content script. It has none of BitChord's native `/manifest.json`, `/search`, or `/stream` endpoints and no QuickJS module exports. It requires a Qobuz web-player/browser context and is incompatible as-is [28][29][30][31].
- **Authentication:** The README requires an active Qobuz trial or paid subscription. The source observes browser localStorage and constructs signed track/getFileUrl requests from stored session state. No active authorized web-player session was supplied or used [28][30].
- **Stream resolution:** **Not verified.** Source declares signed request construction, but no live API request or track resolution was performed and no playback URL was obtained [30][31].
- **Actual playback:** **Not verified.** No source resolution, stream request, media fetch, or audio playback occurred [28][30].
- **Quality:** **Not verified.** The repository contains FLAC-processing code, but that does not establish the quality of a resolved stream, Hi-Res properties, or BitChord playback [28][30].
- **Downloads:** **Not verified.** The project describes a downloader and requires an active subscription, but no download or entitlement/terms assessment was performed [28][30].
- **License/runtime notes:** GPL-3.0 is shown by the repository [28].
- **Exact blocker/status:** The candidate is a browser extension rather than a BitChord native addon or QuickJS module, and it depends on an authorized active Qobuz web-player session. Adapter work and testing were not performed [28][30][31].

### 7. TidalExt (`nicopowa/tidalext`)

- **Reachability and manifest status:** The public repository, README, manifests, request/interception logic, and release page were reachable. No BitChord manifest or native HTTP endpoint was found, and the extension was not installed or run [32][33][34][35][36][37].
- **Runtime and compatibility:** TidalExt is a vanilla JavaScript browser WebExtension using Chromium/Firefox extension APIs. It does not implement BitChord's native `/manifest.json`, `/search?q=`, or `/stream/{id}` service and exposes no QuickJS `searchTracks()`/`getTrackStreamUrl()` module. Its Android method is unstable/testing-only Edge Canary CRX sideloading, not native BitChord support [32][33][34][37].
- **Authentication:** The README requires an active TIDAL trial or paid subscription. Source observes browser-player localStorage and requests and supplies a stored Authorization value to TIDAL API calls. Success was not tested [32][34].
- **Stream resolution:** Source declares calls to `/v1/tracks/{id}/playbackinfopostpaywall` and parsing of a returned DASH manifest, but no authenticated resolution was reproduced and no protected stream was obtained [34][35].
- **Actual playback:** **Not verified.** The stated workflow is browser-based downloading; the extension was not installed or run and no audio playback test occurred [32][34][36].
- **Quality:** **Not verified.** Source includes LOSSLESS/HIRES labels and DASH segment-to-FLAC processing, but code declarations do not establish the actual rendition or bit depth/sample rate delivered [34][36].
- **Downloads:** Code declares downloads and the README requires an active subscription, but authorization and successful download were not verified; no protected media was fetched [32][34].
- **License/runtime notes:** GPL-3.0 is declared by the repository [32].
- **Exact blocker/status:** Wrong software interface/runtime: it is a browser extension dependent on a TIDAL web session, not either BitChord HTTP JSON addon endpoints or a QuickJS module. No installation, execution, sign-in, stream fetch, or playback test was performed [32][34][35].

### 8. LastBabe / LastBebe addon (`lastbabe`)

- **Reachability and manifest status:** The user-supplied manifest endpoint returned HTTP 404 on the audit date. The publicly shared non-tokenized `/manifest.json` URL was not extractable by the web fetcher. No manifest or endpoint could therefore be retrieved, and this does not establish a universal service outage [38].
- **Runtime and compatibility:** The provider runtime is undisclosed. BitChord supports native JSON/HTTP addons and a separate sandboxed QuickJS JavaScript-module protocol, but no accessible LastBabe evidence identifies which one is used. The expected native routes could not be compared with a readable manifest [39][40][41].
- **Authentication:** **Not verified.** No sign-in, credential request, or authenticated request was attempted [38].
- **Stream resolution:** **Not verified.** No successful search or stream-resolution response was obtained and no protected stream media was requested [38][39][40].
- **Actual playback:** **Not verified.** No audio playback test was conducted; manifest availability alone would not establish playback [38].
- **Quality:** **Lossless/Hi-Res not verified.** No directly reproducible evidence establishes delivery or source characteristics [38].
- **Downloads:** **Not verified.** No evidence establishes download permission or authorization and no download was attempted [38].
- **License/runtime notes:** No LastBabe license was identified in the inspected public evidence [38]. BitChord's own Android minimum/API documentation is not provider support evidence [39][42].
- **Exact blocker/status:** Supplied endpoint returned 404; no official source repository, readable manifest, or successful protocol evidence was found. Testing was limited to public text/metadata, with no credentials, protected media, or playback [38][39][40][41][42].

### 9. GGLos (`gglos`)

- **Reachability and manifest status:** The public homepage and status APIs were reachable. Direct unauthenticated inspection of `/manifest.json` and the two per-source manifests returned HTTP 403 JSON stating `This addon only serves BitChord`. This confirms endpoint reachability but not successful app access or readable manifest contents [43][44][45][46].
- **Runtime and compatibility:** GGLos presents itself as a hosted music addon and its status API declares addon version 1.0.0 and minimum BitChord version 1.0.0. Because the actual manifest is gated, its JSON schema, `/search` and `/stream` contracts, required headers, and authentication could not be compared with BitChord's native protocol. QuickJS compatibility is likewise unverified [44][45][46][47].
- **Authentication:** No sign-in or credentials were attempted. The exact direct-request blocker is HTTP 403 with the response body above; required BitChord client headers or other authentication cannot be determined from public evidence [44][45].
- **Stream resolution:** **Not verified.** The dashboard reports streaming health checks, but returned source URLs and independent stream resolution were not inspected or tested [45][46].
- **Actual playback:** **Not verified.** No audio playback was tested, and a manifest or status response is not playback evidence [44][45].
- **Quality:** **Not verified.** Dashboard labels include Hi-Res Lossless (24-bit) and Lossless (16-bit), and status data reports those tiers active for source 1. These are provider declarations/health data, not independently verified output quality [45].
- **Downloads:** **Not verified as authorization.** The dashboard reported a successful download check for source 1 and a source 2 download failure reporting a TIDAL 403 (`Client cannot access this content`); these checks do not establish permission to download [45].
- **License/runtime notes:** BitChord's host application is GPLv3 according to its official repository, but no GGLos provider/source license was discoverable. The host license must not be applied to the addon [47][48][49].
- **Exact blocker/status:** Direct manifest retrieval is blocked outside the designated client, so protocol conformance, authentication behavior, stream resolution, playback, actual quality, and download authorization remain unverified [44][45][46].

---

## References

[1] [TIDAL API/SDK overview](https://developer.tidal.com/documentation/api-sdk/api-sdk-overview) — official overview of TIDAL API and SDK materials.

[2] [TIDAL Android SDK repository](https://github.com/tidal-music/tidal-sdk-android) — official Android SDK repository and license/runtime information.

[3] [TIDAL Android Player README](https://github.com/tidal-music/tidal-sdk-android/blob/main/player/README.md) — Player module, credentials provider, preview/full-length authentication notes.

[4] [TIDAL API authorization](https://developer.tidal.com/documentation/api-sdk/api-sdk-authorization) — official OAuth 2.1 authorization flows.

[5] [TIDAL Developer Terms](https://developer.tidal.com/documentation/guidelines/guidelines-developer-terms-1_0) — official developer terms and copying/caching restrictions.

[6] [TIDAL API reference](https://tidal-music.github.io/tidal-api-reference/) — official API reference including track-manifest documentation.

[7] [Qobuz API Terms of Use](https://static.qobuz.com/apps/api/QobuzAPI-TermsofUse.pdf) — official API credential, agreement, and usage terms.

[8] [Qobuz Android app listing](https://play.google.com/store/apps/details?id=com.qobuz.music&hl=en_US) — official consumer Android app listing and service quality claims.

[9] [Qobuz help: How to listen to Qobuz](https://help.qobuz.com/en/articles/602448-how-to-listen-to-qobuz) — official supported application/platform and listening guidance.

[10] [Qobuz legal terms](https://www.qobuz.com/us-en/legal/terms) — official subscription, offline-import, and download-store terms.

[11] [Qobuz API Evangelist profile](https://github.com/api-evangelist/qobuz) — third-party API profile, used only as corroborating context.

[12] [QobuzApiSharp](https://github.com/DJDoubleD/QobuzApiSharp) — third-party Qobuz API client profile, not treated as official access evidence.

[13] [LastWave addon site](https://lastwaveaddons.clashprojects.qd.je/) — official addon descriptor/setup endpoint and account/channel-verification information.

[14] [LastWave addon setup](https://lastwave.app/addon/) — official setup and service information.

[15] [LastWave product site](https://lastwave.app/) — official product claims and platform information.

[16] [LastWave Native repository](https://github.com/Clash-Projects/LastWave-Native) — Android client source, runtime, and GPL-3.0 information.

[17] [LastWave Native releases](https://github.com/Clash-Projects/LastWave-Native/releases) — public release materials.

[18] [Qobuz-DL hosted addon manifest](https://qobuz-addon.vercel.app/a/manifest.json) — public hosted `/a` manifest response.

[19] [Qobuz-DL hosted addon search test](https://qobuz-addon.vercel.app/a/search?q=test) — public hosted search response used for reachability verification.

[20] [KATx-dev qobuz-addon repository](https://github.com/KATx-dev/qobuz-addon) — hosted addon source, routes, runtime, and license metadata.

[21] [qobuz-addon server source](https://github.com/KATx-dev/qobuz-addon/blob/main/src/server.ts) — source route registration and server behavior.

[22] [BitChord AddonClient](https://github.com/kushagrasinghx/BitChord/blob/main/app/src/main/java/com/music/bitchord/data/sources/addon/AddonClient.kt) — native BitChord addon protocol behavior.

[23] [qobuz-dl repository](https://github.com/vitiko98/qobuz-dl) — Python CLI/library README, subscription requirement, and license.

[24] [qobuz-dl setup.py](https://raw.githubusercontent.com/vitiko98/qobuz-dl/master/setup.py) — package/runtime metadata.

[25] [qobuz-dl qopy.py](https://raw.githubusercontent.com/vitiko98/qobuz-dl/master/qobuz_dl/qopy.py) — login and track/getFileUrl request construction.

[26] [qobuz-dl issue #329](https://github.com/vitiko98/qobuz-dl/issues/329) — user-reported login failure discussion; not independent official confirmation.

[27] [qobuz-dl GitHub API metadata](https://api.github.com/repos/vitiko98/qobuz-dl) — repository/license metadata.

[28] [QobuzExt repository](https://github.com/nicopowa/qobuzext) — browser extension README, runtime, subscription, and license.

[29] [QobuzExt Chromium manifest](https://github.com/nicopowa/qobuzext/blob/main/manifest.chromium.json) — browser extension manifest/runtime declaration.

[30] [QobuzExt content script](https://github.com/nicopowa/qobuzext/blob/main/common/cnt.js) — session/localStorage and request-interception behavior.

[31] [QobuzExt background script](https://github.com/nicopowa/qobuzext/blob/main/background.js) — browser extension background and request workflow.

[32] [TidalExt repository](https://github.com/nicopowa/tidalext) — browser extension README, runtime, subscription, and license.

[33] [TidalExt Chromium manifest](https://github.com/nicopowa/tidalext/blob/main/manifest.chromium.json) — browser extension manifest/runtime declaration.

[34] [TidalExt background script](https://github.com/nicopowa/tidalext/blob/main/background.js) — browser-session and TIDAL API request behavior.

[35] [TidalExt content script](https://github.com/nicopowa/tidalext/blob/main/common/cnt.js) — browser-player interception/session behavior.

[36] [TidalExt audio processing](https://github.com/nicopowa/tidalext/blob/main/proc.audio.js) — DASH/FLAC and quality-label processing declarations.

[37] [TidalExt releases](https://github.com/nicopowa/tidalext/releases) — public release materials.

[38] [LastBebe addon discussion](https://www.reddit.com/r/bitchord/comments/1x5nkhc/lastbebe_addon/) — public discussion associated with the supplied addon endpoint.

[39] [BitChord AddonModels](https://raw.githubusercontent.com/kushagrasinghx/BitChord/main/app/src/main/java/com/music/bitchord/data/sources/addon/AddonModels.kt) — native addon data model/protocol evidence.

[40] [BitChord SourceKind](https://raw.githubusercontent.com/kushagrasinghx/BitChord/main/app/src/main/java/com/music/bitchord/data/sources/SourceKind.kt) — source/runtime classification evidence.

[41] [BitChord ModuleManager](https://raw.githubusercontent.com/kushagrasinghx/BitChord/main/app/src/main/java/com/music/bitchord/data/sources/module/ModuleManager.kt) — QuickJS/module loading evidence.

[42] [BitChord releases](https://github.com/kushagrasinghx/BitChord/releases) — public Android release/minimum-version context.

[43] [GGLos homepage](https://gglos.ggottes.deno.net/) — provider homepage and BitChord setup recommendation.

[44] [GGLos manifest endpoint](https://gglos.ggottes.deno.net/manifest.json) — direct manifest request and HTTP 403 response evidence.

[45] [GGLos status API](https://gglos.ggottes.deno.net/api/status) — provider health, source checks, and quality/download status declarations.

[46] [GGLos qualities API](https://gglos.ggottes.deno.net/api/qualities) — provider quality declarations.

[47] [BitChord repository](https://github.com/kushagrasinghx/BitChord) — host application/runtime and license context.

[48] [BitChord README](https://github.com/kushagrasinghx/BitChord/blob/main/README.md) — host application usage and support context.

[49] [BitChord LICENSE](https://github.com/kushagrasinghx/BitChord/blob/main/LICENSE) — host application GPLv3 license.
