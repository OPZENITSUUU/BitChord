# vitiko98/qobuz-dl — source audit

**As of 2026-10-18.** Public GitHub repository and raw source were reachable without signing in. The project is Python package/CLI `qobuz-dl`, not a BitChord streaming add-on: its package declares Python >=3.6 and an OS-independent classifier, and installs `qobuz-dl`/`qdl` console commands. It has no declared Android runtime, QuickJS module interface, `/manifest.json`, `/search`, or `/stream` endpoint. Thus it is not directly compatible with either BitChord protocol; a separate adapter/integration would be needed, and is not evidenced here. [1] [2]

Repository documentation describes searching and downloading Qobuz music and explicitly requires an active subscription. Source initializes a client via `user/login` with email/password, an app ID and API secrets; it obtains a `track/getFileUrl` result for stream intent, then the downloader fetches the returned URL and writes/tag files. That is a download workflow, not a JSON playback service. [1] [3] [4]

A public issue opened April 2026 reports credentials rejected despite working in Qobuz clients; a commenter attributes it to removal of the direct login endpoint and OAuth/reCAPTCHA migration. These are issue-user statements, not an independently verified Qobuz announcement. They are material evidence of an authentication blocker, but I did not attempt sign-in or token workarounds. The repository API snapshot reports last push 2025-07-19, consistent with the observed source being stale relative to that reported change. [5] [6]

**Verification boundary:** no code was installed or executed and no credentials, protected stream media, or playback were accessed. Public source declares quality IDs for MP3 and FLAC tiers and handles returned bit-depth/sample-rate fields, but actual stream authentication, URL resolution against current Qobuz, audio playback, true lossless/Hi-Res payload, and authorized download entitlements are **not verified**. The README says active subscription is needed; that is a project declaration, not proof that a particular download is authorized under Qobuz terms. License is GPL-3.0. [1] [2] [3]

## References

[1]: https://github.com/vitiko98/qobuz-dl "Official repository README and project files"
[2]: https://raw.githubusercontent.com/vitiko98/qobuz-dl/master/setup.py "Official package setup metadata"
[3]: https://raw.githubusercontent.com/vitiko98/qobuz-dl/master/qobuz_dl/qopy.py "Official API client source"
[4]: https://raw.githubusercontent.com/vitiko98/qobuz-dl/master/qobuz_dl/downloader.py "Official downloader source"
[5]: https://github.com/vitiko98/qobuz-dl/issues/329 "Public authentication failure report and discussion"
[6]: https://api.github.com/repos/vitiko98/qobuz-dl "GitHub repository metadata"
