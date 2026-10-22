# Qobuz hosted addon audit

**Audit date:** 2026-10-18. **Candidate:** `https://qobuz-addon.vercel.app/a/manifest.json` (Qobuz-DL isolated source prefix).

## Findings

The exact public manifest URL is reachable over HTTPS and returned HTTP 200 with JSON declaring `id: com.bitchord.qobuz.qobuzdl`, `resources: ["search", "stream"]`, `allowDownloads: 0`, and a quality setting with `lossless`, `high`, and `low` values. The same endpoint's source code registers `/a` as the Qobuz-DL provider prefix. The deployed manifest differs in name/id from the repository root manifest because `/a` is an isolated provider path. [1] [2] [3]

A read-only `GET https://qobuz-addon.vercel.app/a/search?q=test` returned HTTP 200 and a `{ "tracks": [...] }` JSON array. The response included catalogue metadata and labels such as `format: flac` and `audioQuality: LOSSLESS`; those are provider claims, not evidence of the actual stream codec, bit depth, sample rate, or playback. No stream URL was requested or retrieved. No audio was fetched or played. [4]

The repository README describes a TypeScript/Node.js addon (Node.js 20+), with `/manifest.json`, `/search`, and `/stream/:id`, and says it delegates to public JSON backends. It claims credential-free handling, validates HTTPS rendition URLs and refuses encrypted streams. Source code confirms the `/a` route builds a Qobuz-DL provider and exposes manifest/search/stream paths. These are declarations and implementation observations, not independent proof that upstream stream resolution succeeds or that a given rendition is authorized. [2] [3]

**BitChord native add-on compatibility:** the candidate's manifest and observed search response fit the JSON addon model: BitChord fetches `${base}/manifest.json`, searches `/search?q=...`, and resolves `/stream/{id}` as JSON. Passing the supplied manifest URL is compatible with BitChord URL normalization: it strips the final `.json` filename, leaving the `/a` prefix as base. Manifest advertises both required resources, and disables downloads. This is an HTTP addon, not a QuickJS module: the QuickJS protocol loads and evaluates JavaScript exporting functions such as `searchTracks` or `getTrackStreamUrl`; the deployed HTTP manifest is not such a module and should not be treated as one. [5] [6]

**Authentication and blockers:** addon code says it does not accept Qobuz credentials; the backend is an intermediary public JSON provider. No sign-in or credential submission was attempted. The public manifest/search work, but this audit did not exercise a valid `/stream/{id}` resolution, inspect a returned media URL, or fetch protected media. Therefore stream authentication requirements, stream source resolution, real audio playback, true lossless/Hi-Res delivery, and lawful/authorized downloads are **not verified**. Downloads are explicitly disabled by `allowDownloads: 0`. The exact playback-evidence blocker is intentionally not making a stream request that could return a signed or protected media URL; no valid stream-response or player evidence exists here.

**Platforms and license:** this addon is an ordinary HTTPS JSON service and declares no target OS; BitChord's addon client is Android-side, but no Android installation/playback test was performed. Qobuz's official app page separately states its own Android app requires Android 6 or later; this is not a minimum for the addon itself. GitHub identifies the source repository as GPL-3.0. [7] [8]

## References

[1]: https://qobuz-addon.vercel.app/a/manifest.json "Live Qobuz-DL addon manifest"
[2]: https://github.com/KATx-dev/qobuz-addon "Qobuz BitChord Addon repository and README"
[3]: https://github.com/KATx-dev/qobuz-addon/blob/main/src/server.ts "Addon route and provider registration source"
[4]: https://qobuz-addon.vercel.app/a/search?q=test "Read-only live search response"
[5]: https://github.com/kushagrasinghx/BitChord/blob/main/app/src/main/java/com/music/bitchord/data/sources/addon/AddonClient.kt "BitChord native JSON addon client"
[6]: https://github.com/kushagrasinghx/BitChord/blob/main/app/src/main/java/com/music/bitchord/data/sources/module/QuickJsExecutor.kt "BitChord QuickJS module executor"
[7]: https://www.qobuz.com/us-en/discover/apps-partners "Official Qobuz platform support page"
[8]: https://api.github.com/repos/KATx-dev/qobuz-addon "Public repository metadata including license"
