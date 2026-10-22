# LastBabe provider audit

**As of 2026-10-18.** The public LastBabe endpoint supplied for this audit returned **HTTP 404** when requested directly (metadata request only). The non-tokenized `/manifest.json` URL shared publicly was also not readable through the web extractor. This establishes endpoint unavailability from this audit environment, not that the service is universally down. The Reddit post calls it “LastBebe Add-on,” shares the provider URL and gives app setup hints (update BitChord, prioritize source, test before saving, optionally upgrade quality); it does not publish source code, a manifest body, authentication details, or a license.[1]

BitChord documents native addons as a JSON-over-HTTP service with `/manifest.json`, `/search?q=`, and `/stream/{id}` endpoints; native addon code is not downloaded or executed. Its expected manifest includes fields such as `id`, `name`, `version`, `resources`, `settings`, and download/lossless flags. A separate QuickJS module source instead requires a module-index JSON and JavaScript exports such as `searchTracks()` and `getTrackStreamUrl()`; scripts run in a sandbox with a wired `fetch()`. Merely linking a manifest URL does not demonstrate either protocol, and LastBabe cannot be matched to either contract without a readable manifest/source or successful endpoint tests.[2][3][4]

No authenticated requests, protected media requests, remote-code execution, DRM circumvention, or playback tests were performed. Therefore stream authentication, source resolution, audio playback, true lossless/Hi-Res delivery, and authorized downloads are **not verified**. No direct evidence supports any credential requirement or download authorization policy. The add-on’s runtime, supported Android/platform range, and license are likewise undisclosed. BitChord’s own release notes list Android 8.0/API 26+ as the app minimum; this is not evidence of LastBabe compatibility.[5]

## References

[1]: https://www.reddit.com/r/bitchord/comments/1x5nkhc/lastbebe_addon/ "Public LastBebe addon announcement"
[2]: https://raw.githubusercontent.com/kushagrasinghx/BitChord/main/app/src/main/java/com/music/bitchord/data/sources/addon/AddonModels.kt "BitChord native addon JSON models"
[3]: https://raw.githubusercontent.com/kushagrasinghx/BitChord/main/app/src/main/java/com/music/bitchord/data/sources/SourceKind.kt "BitChord native addon and module source kinds"
[4]: https://raw.githubusercontent.com/kushagrasinghx/BitChord/main/app/src/main/java/com/music/bitchord/data/sources/module/ModuleManager.kt "BitChord QuickJS module loader and exports"
[5]: https://github.com/kushagrasinghx/BitChord/releases "BitChord release notes and platform requirements"
