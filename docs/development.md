# Development

Notes for changing and rebuilding the launcher. To install it, see the [install guides](../README.md#install); for how each feature works, see [features.md](features.md).

The optional Net traffic monitor (Settings → Net traffic, new in 1.15) includes native code. Install **NDK 28.2.13676358** and **CMake 3.22.1** through the Android SDK manager (Gradle can offer to download them after accepting the SDK licences):

```bash
sdkmanager "ndk;28.2.13676358" "cmake;3.22.1"
```

Its forwarding library is pinned in `launcher/app/src/main/cpp/zdtun/`, built separately as `libzdtun.so` for ARM32/ARM64/x86/x86_64 with 16 KB page alignment. The engine (`libzdtun.so` and its bridge `libtraffic.so`) is **not packed into the APK**: the `netEngineRelease` Gradle task copies it to `launcher/app/build/outputs/netengine/release/netengine-<cpu>-<lib>.so` and writes each file's SHA-256 into the APK as `assets/netengine.txt`. The app loads only files matching those fingerprints, from where `scripts/build-and-install.sh` pushes a local build's engine (`/sdcard/Android/data/com.swadaya.tv/files/netengine/`), or else downloaded from the installed version's GitHub release: the build workflow attaches the same `netengine-<cpu>-<lib>.so` files to every release (with the library's licence texts), and `TrafficEngine` asks the GitHub API for the tag of the release that version came from (`v1.1.6-build33`) before downloading, so an APK installed from a release needs no ADB. Keep the corresponding source and LGPL/GPL notices with distributions; see that directory's `UPSTREAM.md`. No capture is enabled by building or installing the APK. See [Net traffic](features.md#net-traffic-internet-connection-monitor) for consent, limitations, and the live-network test checklist.

## Folder structure

```
SwadayaTV/
├── README.md
├── PROMPT.md                    paste into an AI coding agent to repeat the whole setup (debloat + launcher) on another TV
├── docs/                        install guides (install-*.md), features.md, xiaomi-debloat.md, development.md, screenshots
├── backup/<serial>/             each TV's state captured BEFORE any change (local only, not in the repo: it identifies the TV)
│   ├── disabled-by-us.txt       every package this project disabled (restore.sh reads it)
│   ├── packages-*.txt           full package lists (all, system, third-party, disabled)
│   ├── getprop.txt, settings-{global,secure,system}.txt
│   ├── stock-home.txt           the TV's own launcher, recorded by build-and-install.sh (restore.sh returns to it)
│   ├── home-default.txt, home-candidates.txt, accessibility.txt, accessibility-enabled-before.txt, private-dns-before.txt
│   └── dumpsys/                 activities/services of every manufacturer package that was checked
├── launcher/                    the launcher source (Java, AndroidX RecyclerView, minSdk 26, targetSdk 36), incl. the phone remote
│   ├── app/src/main/            the launcher, the phone remote page (assets/remote.html) and its resources
│   └── app/src/test/            unit tests for the pure logic (no TV or emulator needed)
├── index.html, id.html, …       the download site (also whats-new, help, tv/ and release/): published by CI, see below
├── .github/workflows/ci.yml     GitHub Actions: lint, unit tests and the release build on every push
├── .github/workflows/build.yml  build, publish the release, refresh release/ and deploy the site
├── .github/workflows/pages.yml  deploy the site from main for a docs-only push (and check it)
├── .github/workflows/release-app.yml  release Swadaya App, refresh release/swadaya-app.apk
├── .github/workflows/download-stats.yml  refresh release/downloads.json once a day
├── scripts/
│   ├── build-and-install.sh     check the computer and TV, build the launcher (R8 release, ~0.6 MB APK), install it, make it the home screen
│   ├── collect-site.sh          copy exactly what the site serves into one folder (every deploy uses it)
│   ├── check-site.sh            post-deploy smoke test: live pages, APKs and version files
│   ├── write-version-json.sh    release/version.json + release/releases.json from a released APK
│   ├── write-app-json.sh        release/app.json from the phone app's APK
│   ├── write-downloads-json.sh  release/downloads.json from GitHub's release download counts
│   ├── benchmark.sh             measure a launcher's memory, CPU, start-up and smoothness
│   ├── restore.sh               undo everything (back to the TV's own launcher)
│   ├── disable.sh               disable packages, or --recommended (and log them so restore.sh can undo)
│   ├── restrict.sh              stop apps running in the background, or --recommended (logged the same way)
│   ├── key.sh / shot.sh         send remote keys / pull a screenshot (for testing)
│   ├── setup-signing.sh         create the release key and store the four CI signing secrets, in one command
│   └── env.sh                   TV address: TV=<ip> once, then remembered in .tv (not in the repo)
└── shots/                       screenshots taken during the work
```

The scripts need ADB debugging switched on; the [install guides](../README.md#install) cover connecting.

## Rebuild and reinstall

```bash
scripts/build-and-install.sh                 # build, install, set as home
scripts/build-and-install.sh --disable-stock # same, and disable the TV's own launcher (needed on Google TV, and wherever the TV's own launcher outranks TV Home for Home)
```
Notes on the install:
- It uses `pm install` over the shell, not `adb install`.
- It grants location permission so the Wi-Fi pill can show the network name. Android 10 requires that.
- It uses `cmd package set-home-activity`, because several apps claim HOME on this TV: the factory test, tvusage and FallbackHome.
- While the stock launcher is enabled, it always wins (its priority is 2). That's why it has to be disabled.
- **New TV IP:** run `TV=<new-ip> scripts/build-and-install.sh` once; the new address is saved in `.tv` for every script.
- **Adding a permission:** adding a permission like INTERNET to an already-installed app doesn't reach the network firewall on Android 10, so the app gets `EPERM` on sockets. If you ever add one, reinstall cleanly: `adb shell pm uninstall com.swadaya.tv && scripts/build-and-install.sh`.
- **Signing:** every release build is signed with the project's release key (`launcher/tvhome-release.jks` and `launcher/keystore.properties`, both committed; see [Release signing and GitHub Releases](#release-signing-and-github-releases)), so any build upgrades any other in place. Replacing that key needs one uninstall (`adb shell pm uninstall com.swadaya.tv`), which loses the launcher's settings and the paired phones.

## Release signing and GitHub Releases

Every build, CI or on your own computer, is signed with one fixed **release key**, committed to the repository as `launcher/tvhome-release.jks` with its passwords in `launcher/keystore.properties` (alias `swadaya`, certificate `CN=SwadayaTV`). So an APK from GitHub installs over the previous one, the TV keeps the launcher's settings and the bell's update works. A version tag publishes the APK as a **GitHub Release**, which anyone can download without signing in.

**The key is public on purpose.** This project is open source, and the key only has to be *the same* in every build, so it is committed instead of living in the repository owner's secrets: nothing to set up, and a build on any computer signs the same way. The cost is that anyone who can read the repository can sign an APK that Android accepts as an update of this app. The app itself only ever downloads the APK attached to this repository's latest release, and anyone else's APK has to be installed by hand like any other app. If that trade ever stops being acceptable, rotate the key (below) and take the new one out of the repository.

### Rotating the key (optional)

`scripts/setup-signing.sh` creates a new key and stores it as the four repository secrets `SIGNING_KEYSTORE_BASE64`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS` and `SIGNING_KEY_PASSWORD` (or add them on GitHub: repository → Settings → Secrets and variables → Actions → New repository secret). CI uses a secret when one is set and the committed key otherwise, and `SIGNING_*` environment variables win over `launcher/keystore.properties` everywhere (`launcher/app/build.gradle`).

**Changing the key costs one manual install.** Android only accepts an update signed with the same key, so an app installed from a build signed with a different one (any release before 1.1.2, signed with the runner's throwaway debug key) has to be removed once: `adb shell pm uninstall com.swadaya.tv`, then install the new APK. The launcher's settings and the paired phones are lost that once; every update after that installs from the bell and the settings stay.

Check a build's signature with `apksigner verify --print-certs launcher/app/build/outputs/apk/release/app-release.apk`: it prints `CN=SwadayaTV`, never `CN=Android Debug`.

### Publishing a version

1. Bump `versionCode` (by 1) and `versionName` in `launcher/app/build.gradle`, add the changelog line as `release/notes/<versionName>.txt`, and commit.
2. Push it to `main`. The Build workflow (`.github/workflows/build.yml`) runs the tests, builds the release APK, publishes the release `v<versionName>-build<number>` with `tvhome-<versionName>.apk` and the eight `netengine-*.so` files, then refreshes `release/` and deploys the site, where a smoke job checks the live files.
3. CI (`.github/workflows/ci.yml`) runs lint and the unit tests on the push and on pull requests.

TVs running 1.1.2 or newer see the release in the bell within a day and can update from there; **Settings → About** checks on demand.

`release/notes/<versionName>.txt` is the one-paragraph changelog quoted in `version.json`, `releases.json` and the What's new history; it is named by `versionName`, so 1.2.1 adds `release/notes/1.2.1.txt`. Without it the changelog is left empty rather than copied from the previous release.

The release tag is `v<versionName>-build<number>`, and the APK asset is named `tvhome-<versionName>.apk` so the in-app updater finds it. The number is the count of releases already published plus one (26, 27, 28…), so it runs on without gaps whatever happens to the CI runs; a push that is still building holds the next one back (the workflow has a concurrency group). Every build signs with the committed release key (`launcher/tvhome-release.jks` and `launcher/keystore.properties`), so a downloaded release updates the previous one in place, settings kept. `SIGNING_*` secrets only override that key when it is rotated.


## The site, version files and download statistics

The site is the single place a TV reads its version from. Everything under `release/` is written by CI from the APK it just published, so the download button, the What's new history and the launcher's update check cannot describe different builds, and no build needs `api.github.com` to learn which version is newest (the API allows 60 requests an hour per IP, which a shared network uses up).

| File | Written by | What it holds |
| --- | --- | --- |
| `release/tvhome-latest.apk` | Build → *Refresh the site's APK, version file and history* | The APK of the build that just became a release, under a name with no version in it. |
| `release/tvhome-previous.apk` | The same step, **before** that copy | The APK that was `tvhome-latest.apk` until this build, so the previous version stays one tap away. It is not rotated when the new build is byte-identical, and the smoke test skips it in a repository that has no such file. |
| `release/version.json` | `scripts/write-version-json.sh` | `versionName`, `versionCode`, `build` (the release number), `tag`, `releaseDate`, `apkUrl`, `apkName`, `apkSize`, `apkSha256`, `changelog`, `notesUrl`. |
| `release/releases.json` | The same script | The last 40 builds, newest first, each with its APK's SHA-256 and size; one entry per tag, so a rerun cannot duplicate a build. |
| `release/app.json` | `scripts/write-app-json.sh` in *Release Swadaya App* | The same details for `release/swadaya-app.apk`. |
| `release/downloads.json` | `scripts/write-downloads-json.sh` in *Download stats* | Totals and per-release download counts. |

`apkUrl` points at this site, not at GitHub, so the updater's first request is a static file. `UpdateCheck` and the phone app's `LauncherApk` read the site first and fall back to `api.github.com` for a TV that cannot reach it or that still runs a build from before this change.

### Collecting and checking a deploy

Four workflows publish the site — Build (after the bot commit that updates `release/`), Release Swadaya App (after it replaces `release/swadaya-app.apk`), Download stats (nightly) and Pages (a docs-only push) — and all of them run the same two scripts:

- `scripts/collect-site.sh [out]` copies exactly what the pages load: every root `*.html`, `site.css`, `site.js`, `og.jpg`, `sitemap.xml`, `robots.txt`, `docs/`, `release/` and `tv/`. Nothing else goes into the artifact, and no workflow lists files by hand, so the four deploys cannot drift apart.
- `scripts/check-site.sh` is the smoke test after a deploy. It fetches every page and asset with a cache-busting query, downloads the three APKs, and verifies that each JSON describes the APK actually served — SHA-256, size, `apkUrl`, and the `versionName` in the matching `build.gradle`. It retries for about two minutes because a deploy reaches GitHub's edges at different times; `BASE_URL`, `SMOKE_ATTEMPTS` and `SMOKE_DELAY` override the defaults for a local run.

### Why the deploy order matters

Two of those workflows can be in flight at once, and their shared `concurrency: pages` group only queues deploys — it does not promise the newer content lands last. An older deploy landing last would overwrite the newer site, and the newer run's smoke check would be right to fail. So every deploy job asks `git ls-remote` whether `main` is still the commit it collected; if `main` has moved on, it skips the deploy and the later run publishes instead.

The smoke job checks out the exact commit its deploy published, not whatever `main` is by now. `check-site.sh` then treats a site that is *newer* than that checkout as a race it lost: it still requires the served JSON to describe the served APK, but does not demand the file be byte-identical to its own commit. A site *older* than the checkout still fails after the retries — that is the failure the check exists for.

### Download statistics

Pages keeps no access logs, so the site cannot count its own downloads. `scripts/write-downloads-json.sh` sums the `download_count` GitHub keeps for each release asset instead: `launcher` (every `tvhome-*.apk` across releases), `app` (`swadaya-app-*.apk`), and one entry per tag for the history list. Because the API is rate-limited per IP, the numbers are collected server-side by `download-stats.yml` (03:17 UTC daily, or *Run workflow*) using the runner's token, committed as `release/downloads.json` and published like any other site change. The pages say the counts come from GitHub Releases; copies downloaded from the site itself are not counted.

To refresh them locally: `gh auth login && scripts/write-downloads-json.sh`, or point `RELEASES_JSON` at a saved `gh api --paginate "repos/{owner}/{repo}/releases?per_page=100"` dump.

## Tests and CI

Live TV plays public HLS playlists in Android's media framework, so there are no injected player
scripts to test on a computer. Check a channel on the TV itself and use `adb logcat -v time
TvPlayer:I '*:S'` for its tune and playback timing.

To inspect real startup timing, `adb -s <TV IP>:5555 logcat -v time TvPlayer:I '*:S'` shows browser
preparation and each channel's elapsed time to page visibility, page load, player/video presence,
media metadata and playback. It logs only channel slugs and timings, not stream URLs or credentials.
The independent 45-second watchdog still ends a stalled attempt if navigation discards a JavaScript
callback. When comparing builds, measure several cold and warm runs on the same channels and network;
one faster run does not establish a speedup.

The launcher's pure logic is unit tested on the computer, so a change doesn't need the TV to catch a regression:

```bash
cd launcher
./gradlew testDebugUnitTest     # just the tests (70 of them, a few seconds)
./gradlew lint                  # Android lint; errors fail the build, warnings don't
./gradlew assembleRelease       # the APK scripts/build-and-install.sh installs
```

Gradle needs Java 17-21: `export JAVA_HOME="$(/usr/libexec/java_home -v 21)"` on macOS, or `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64` on Linux. The build script also picks a suitable JDK itself when `JAVA_HOME` is missing or too new. The tests read a stubbed `android.jar`, so they can't touch the TV's APIs — that's why the logic they cover is kept free of `Context`:

| Test | Covers |
|---|---|
| `FileApiTest` | Files tab: name cleaning, unique names, `Range` parsing, keeping paths inside a root |
| `HidKeysTest` | Phone typing → USB-HID reports (letters, digits, punctuation) |
| `DockConfigTest` | `dock.txt` parsing and cleaning (comments, blanks, duplicates, the 5-app cap) |
| `UsageScoresTest` | "Most used": the week window, the per-day merge, per-app totals, exclusions |
| `RemotePairingTest` | Pairing: PIN lifetime, tries, lockout and its exponential backoff |
| `RemoteDevicesTest` | Paired phones' names: the name typed on the phone wins, Chrome's "K" isn't a model, older browsers and iPhones |
| `PlayUpdatesTest` | The bell's Play Store updates: reading the Play Store's log lines, and apps leaving the list once updated, disabled or forgotten |
| `UpdateCheckTest` | TV Home's own updates: versions compared number by number |
| `StatusOverlayTest` | Info overlay: speed in decimal bits a second, uptime and elapsed time, the clock |
| `DreamViewTest` | Screen savers: splitting `logcat` lines (Live log), the Hijri date as on the home screen |
| `ZoneDetectTest` | Auto prayer city: every Indonesian city resolves to itself, no guess outside Indonesia |
| `GlobeTest` | Globe: where the sun is overhead, the prayer lines against Kemenag's times for Jakarta, the Earth's tilt and the sun in the ecliptic frame, great circles (qibla), every prayer city has a place, reading OpenStreetMap, USGS and aurora feeds, snow and ice map colours, picking the GFS run |
| `OrbitTest` | Globe satellites: SGP4 against the reference implementation (ISS and Tiangong within 10 m; GPS within 25 km without the Moon and Sun), reading CelesTrak's files |
| `GribTest` | Globe wind: reading NOAA's GRIB2 (complex packing with spatial differencing, against a real global field; simple packing) |

`.github/workflows/ci.yml` runs the same three tasks on every push and pull request, validates the Gradle wrapper and uploads the release APK as a build artifact.

## Benchmark

Measured on 2026-11-04 with `scripts/benchmark.sh <home component> <label>`, using the optimised R8 release build of TV Home 1.16.0. Conditions:
- Nothing was playing, and USB debugging was on.
- The stock launcher ran without its recommendation services, which this setup disables. It was enabled only for its own run (with it enabled, Home keeps going to it), then disabled again before this launcher's run.

| | TV Home 1.16.0 | Stock Android TV launcher |
|---|---|---|
| Memory, idle / after navigating (PSS) | 87 / **115 MB** | 90 / 178 MB |
| CPU, idle, 60 s | 0.18–0.27 s settled (0.71 s while the weather effects play) | 0.08 s |
| Cold start (process killed → first frame) | **0.76–0.81 s** | 1.47–2.34 s |
| Back to home from an app (wait time) | 18–20 ms | 15–17 ms |
| Janky frames while navigating | **4.8 % (59/1240)** | 29.4 % (109/371) |
| Frame time p50 / p90 / p99 | 9 / **12** / **38** ms | 7 / 28 / 150 ms |
| APK size | **0.53 MB** | 12.7 MB |

**Notes:**
- **Idle memory depends on the graphics buffers.** About 45 MB of this launcher's PSS is GPU memory (`GL mtrack`: the window's buffers, the wallpaper and the tile textures). Whether they're all allocated when the idle sample is taken changes the idle figure a lot: earlier runs measured 44–51 MB idle, this one 88 MB, settling at 82 MB a few minutes later. The after-navigating figure, when everything is drawn, is steady across runs (75–83 MB) and is the fairer comparison.
- **Extra processes only while used:** the launcher is a single process at idle. The Now Playing helper (~23 MB) runs only while something plays, and the virtual keypad (~21 MB) only for 5 minutes after a phone-remote key press inside an app.
- **Screensaver:** under the screensaver the launcher uses no CPU (0 ticks in 60 s). The System card refreshes every 5 s, and only while home is on screen and in focus.
- **Other apps in front:** after 30 s with another full-screen app in front, the app-list tile images are freed and redrawn on return. Hardware bitmaps live in graphics memory, so this saving doesn't show up in PSS.
- **Idle CPU and the weather effects:** the script measures idle 25 s after its last cold start, while the weather effects (45 s after the home screen starts, when it's raining or stormy; it was) are still playing: 0.71 s. Measured again once they had settled: 0.18–0.27 s per 60 s, with the System card's 5-second refresh the only regular work.
- **Since 1.8** (2026-10-25: 88 / 83 MB, 0.15 s idle CPU, 0.51–0.57 s cold start, 5.8 % janky frames, against the stock launcher's 87 / 122 MB, 1.49–1.59 s and 22.4 %), the weather card and effects, the Globe, Net traffic, quick actions and Button Mapper came in: cold start is about 0.25 s slower and memory after navigating about 30 MB higher. The stock launcher measured higher too this time (178 MB after navigating, 29.4 % janky), so part of the difference is the TV's state on the day.
- **Before these optimisations** the same benchmark measured 104 / 133 MB, 0.54 s idle CPU, 2.0–2.1 s cold start and 11.4 % janky frames.
