# QobuzExt provider audit

**Candidate:** [nicopowa/qobuzext](https://github.com/nicopowa/qobuzext), repository description “qobuz downloader web extension”; inspected public source reports v1.8 and latest source commit `d387af50ea05e1b037c160d74e716794f74a9cd9` (2026-05-07). Source is publicly reachable. No sign-in, credential request, code execution, or protected media fetch was performed. [1] [2]

## Findings

QobuzExt is a browser extension, not a BitChord-compatible source. Its own description calls it a WebExtension implemented in vanilla JavaScript using browser APIs. Chromium manifest v3 declares a module service worker, an extension action, a content script for `https://play.qobuz.com/*`, Qobuz host permission, and storage/download/webRequest/offscreen permissions. Its intended workflow is install in a supported browser, open Qobuz web player, then click Download. It does not provide BitChord’s HTTP JSON `manifest.json`, `/search`, and `/stream` endpoints, nor export a QuickJS module with `search`/`stream` exports. Direct installation as either BitChord source type is incompatible. [2] [3] [4] [6]

The README says an active Qobuz subscription (trial or paid) is required and says it reads the active web session’s cookies/localStorage and intercepts fetch requests; it claims it does not need credentials. Source confirms content-script transfer of page `localStorage`. Background source builds a signed `track/getFileUrl` request from stored secret, timestamp, track ID, format ID, and `intent: stream`. Authentication therefore depends on the active Qobuz web-player session; no standalone login or credential flow is documented. Account/session/API use was not exercised. [1] [5]

The README marks Android “Not stable, testing only” and specifically describes Edge Canary with experimental CRX installation. This does not establish compatibility with BitChord. No Android installation or runtime test was performed. iOS is described as unsupported/work in progress. [1]

The repository is GPL-3.0 licensed. The source includes FLAC header/audio processing and browser download code, but that does not prove a stream resolves or plays in BitChord, that the audio is true lossless/Hi-Res, or that a download is authorized. Stream authentication, source resolution, playback, quality, and download authorization are **not verified**: none was attempted. [1] [5]

## Evidence boundary

Public repository files and relevant BitChord source were inspected. No app was launched and no runtime behavior was tested. This is a code/protocol compatibility audit, not evidence of successful playback or entitlement. [3] [4] [6]

## References

[1]: https://github.com/nicopowa/qobuzext "QobuzExt README and repository overview"
[2]: https://github.com/nicopowa/qobuzext/tree/main "QobuzExt public source tree"
[3]: https://github.com/nicopowa/qobuzext/blob/main/manifest.chromium.json "QobuzExt Chromium manifest"
[4]: https://github.com/kushagrasinghx/BitChord/blob/main/app/src/main/java/com/music/bitchord/data/sources/addon/AddonClient.kt "BitChord native addon client protocol"
[5]: https://github.com/nicopowa/qobuzext/blob/main/background.js "QobuzExt background source"
[6]: https://github.com/kushagrasinghx/BitChord/blob/main/app/src/main/java/com/music/bitchord/data/sources/ModuleSource.kt "BitChord QuickJS module source contract"
