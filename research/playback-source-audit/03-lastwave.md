# LastWave supplied addon audit

**Assessment (2026-10-18):** The supplied personal endpoint is reachable and returns a LastWave descriptor declaring `com.lastwave.addon`, version `1.0.0`, resources `search` and `stream`, and a `quality` setting defaulting to `lossless`. A metadata-only request to its `/manifest.json` route returned the same descriptor (HTTP 200); a blank `/search` request returned an empty result object (HTTP 200). The personal endpoint path is intentionally omitted because it functions as a bearer-like private URL. No `/stream` request was made.

**BitChord fit:** BitChord's native addon path is plain HTTP/JSON and expects `/manifest.json`, `/search?q=...`, and `/stream/{id}`. The descriptor's resources and reachable metadata routes look addon-shaped, but the response fields alone do not establish BitChord's precise manifest/search/stream response compatibility. No actual track search or stream-resolution request was made. The returned `/search` was an empty-query probe, not proof that a real query works. QuickJS modules are a separate protocol: a module index and JavaScript exporting `searchTracks()` and `getTrackStreamUrl()` run in a sandbox. This provider presents a JSON HTTP service, not that module format; it is not established as QuickJS-compatible.

**Access, platform, and evidence boundary:** The public addon site says users create an account and verify Telegram or Discord membership to obtain a personal URL; the guide describes that URL as private and quota-bound. This is an authentication/access requirement, not evidence that this audit account performed sign-in. LastWave is declared as a native Kotlin Android application; project README lists Android 7.0+, while the official site indicates current builds may be Android 10–16+ with a legacy Android 7–9 build. That does not imply BitChord compatibility on any Android version. The repository advertises GPL-3.0 for the LastWave Android application; no independent license for the hosted addon service/backend was found in the reviewed evidence.

LastWave's marketing and setup materials claim lossless/FLAC and Dolby Atmos options, but this audit did not resolve a stream or test audio. Stream authentication behavior, source resolution, actual playback/audio output, true lossless or Hi-Res fidelity, and permission/authorization to download media are **not verified**. A manifest declaration is not proof of any of these. Exact blocker: required verification/account access to generate the private URL and make authorized track-level requests was not performed; in addition, no stream request or playback test was within the read-only verification performed.

## References

[1]: https://lastwaveaddons.clashprojects.qd.je/ "LastWave Addons public homepage"
[2]: https://lastwave.app/addon/ "LastWave addon setup guide"
[3]: https://lastwave.app/ "LastWave official product site"
[4]: https://github.com/Clash-Projects/LastWave-Native "LastWave Native source repository"
[5]: https://github.com/Clash-Projects/LastWave-Native/releases "LastWave Native releases"
