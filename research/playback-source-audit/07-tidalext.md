# tidalext provider audit

**Candidate:** [nicopowa/tidalext](https://github.com/nicopowa/tidalext) · audited 2026-10-18. Public repository was reachable and cloned read-only; source was inspected, not executed. No sign-in, credential request, protected media fetch, or playback test was performed.

## Findings

TidalExt is a browser WebExtension written in vanilla JavaScript, not a BitChord HTTP add-on or QuickJS module. Its README describes Chromium and Firefox desktop installation, with Android explicitly “Not stable, testing only” via Edge Canary CRX sideload; iOS is unsupported. The checked-in Chromium manifest targets `https://tidal.com/*`, runs browser extension scripts, and declares browser APIs such as storage, downloads, webRequest, and offscreen. No Android native app/runtime support is declared. [1] [2]

Its code works by injecting into the logged-in Tidal web player, observing fetches and localStorage, and then making Tidal API requests (including `/v1/tracks/{id}/playbackinfopostpaywall`) using a stored Authorization value. The project says credentials are not requested and an active trial or paid subscription is required. In practice its dependency is an existing active browser session; whether authentication succeeds for any account was not tested. [1] [3]

The app's claimed function is downloading tracks from Tidal, not providing BitChord's `GET /manifest.json`, `/search?q=`, `/stream/{id}` JSON service. The inspected repository contains extension manifests and browser scripts only; no implementation of that HTTP contract or QuickJS exported `searchTracks()` / `getTrackStreamUrl()` module. It is therefore not directly compatible with either BitChord integration. [1] [4] [5]

The source includes track playback-info request logic and an audio processor that parses a DASH manifest, fetches initialization/media segments, and emits FLAC. It also offers quality labels including HIRES and LOSSLESS. This is code-declared behavior only: stream authentication, successful source resolution, actual playback, true lossless/Hi-Res output, and authorized downloads were **not verified**. No protected stream media was fetched. [3] [6]

The repository declares GPL-3.0. Latest release page shows v1.8 (6 May; year not exposed in extracted release page) and a v1.8.1 pre-release dated 25 June; this audit does not infer runtime success from release packaging. [1] [7]

## References

[1]: https://github.com/nicopowa/tidalext "TidalExt repository and README"
[2]: https://github.com/nicopowa/tidalext/blob/main/manifest.chromium.json "Chromium extension manifest"
[3]: https://github.com/nicopowa/tidalext/blob/main/background.js "Tidal API request and playback manifest logic"
[4]: https://github.com/nicopowa/tidalext/blob/main/common/cnt.js "Injected fetch and storage observation"
[5]: https://github.com/kushagrasinghx/BitChord/blob/main/docs/web-app-build-prompt.md "BitChord add-on and module protocols"
[6]: https://github.com/nicopowa/tidalext/blob/main/proc.audio.js "DASH segment processing and FLAC output code"
[7]: https://github.com/nicopowa/tidalext/releases "TidalExt releases"
