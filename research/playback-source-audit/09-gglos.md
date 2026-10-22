# GGLos provider audit

**Audit date:** 2026-10-18. **Target:** `https://gglos.ggottes.deno.net/manifest.json`.

## Findings

The public homepage describes “gglos” as a BitChord music addon and tells users to paste its manifest URL into BitChord Settings → Sources → Add addon. It also advertises merged and per-source manifests. The supplied `/manifest.json` was directly requested without an app client and returned HTTP 403 JSON: `{"error":"This addon only serves BitChord"}`. `/s1/manifest.json` and `/s2/manifest.json` likewise returned 403. Thus the route is reachable, but its manifest document is not publicly readable by this audit. This response does not establish whether a BitChord client with its expected request headers can retrieve it.

The public status dashboard API reports addon version `1.0.0`, minimum BitChord version `1.0`, timestamped status data, search/streaming/download checks for two sources, and tier labels including Hi-Res Lossless (24-bit), Lossless (16-bit), High, Low and Dolby Atmos. As of the dashboard check observed at 2026-10-18 08:07:33Z, source 1 was reported online for all three checks; source 2 reported search and streaming online, download failed with a TIDAL 403 (“Client cannot access this content”). The dashboard declares tier/health labels; these are not independent verification of media format, authentication, source resolution, working audio playback, or download authorization. No protected stream media was requested, and playback was not tested.

BitChord’s public repository describes source management and health checking, supports Android and desktop (Windows/Linux), and licenses BitChord itself under GPLv3. The GGLos manifest/source’s own license is not discoverable from the manifest because access is blocked; do not infer GPLv3 for this provider. The observed 403 explicitly says “This addon only serves BitChord”; this is the exact blocker to independent manifest inspection. No sign-in or credentials were attempted.

Native compatibility is not confirmed. The actual manifest could not be inspected to compare JSON `/manifest.json`, `/search`, and `/stream` contracts or verify request/response shapes, headers, authentication, or stream resolution. Nor was a QuickJS module/source artifact identified or tested. The server’s generic native-request rejection is suggestive of client gating, not proof of protocol conformance. Android support is claimed indirectly by the BitChord host app, not independently asserted for GGLos beyond its dashboard’s “minimum BitChord version 1.0” declaration.

**Verification limits:** stream authentication, source resolution, actual audio playback, true lossless/Hi-Res output, and authorized downloads are **not verified**. The public dashboard is secondary operational evidence and its successful checks are not equivalent to an independently reproducible client playback test.

## References

[1]: https://gglos.ggottes.deno.net/ "GGLos public status homepage"
[2]: https://gglos.ggottes.deno.net/manifest.json "GGLos supplied manifest endpoint"
[3]: https://gglos.ggottes.deno.net/api/status "GGLos public status API"
[4]: https://gglos.ggottes.deno.net/api/qualities "GGLos declared audio tiers"
[5]: https://github.com/kushagrasinghx/BitChord "BitChord official repository"
[6]: https://github.com/kushagrasinghx/BitChord/blob/main/README.md "BitChord README and platform/license statements"
[7]: https://github.com/kushagrasinghx/BitChord/blob/main/LICENSE "BitChord GPLv3 license"
