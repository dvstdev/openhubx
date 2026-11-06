# OpenHubX — Hand-off

Fork of the OpenHub Android GitHub client, rebranded to **OpenHubX** and modernized.
Path: `~/playground/openhubx`. Modernization is complete; remaining ideas live in the Backlog below.

## Stack
- Java only. MVP + Dagger 2.51.1 + Room 2.6.1 + Retrofit 2.11 + RxJava 3 + ViewBinding.
- AndroidX, AGP 9.2.0, Gradle 9.7.1, compileSdk 37, targetSdk 34, minSdk 21.
- Java 21 language level + core library desugaring (`desugar_jdk_libs:2.1.5`).
- applicationId and namespace: `com.darktile.openhubx`.
- Version: `2026.09.006` (versionCode 37) in `gradle.properties`.
- Specs (product, architecture, test plan): `vibes/`.
  Scheme `2026.MM.NNN`; each publish bumps NNN by a random 1–10 and versionCode by 1.

## Build
```bash
export ANDROID_HOME=~/android-sdk
export JAVA_HOME=~/jdks/amazon-corretto-25.0.4.8.1-linux-x64   # JDK 17+ works; 25 is the tested daemon
cd ~/playground/openhubx
./gradlew assembleNormalDebug testNormalDebugUnitTest --no-daemon --no-watch-fs -Dorg.gradle.vfs.watch=false --offline
./gradlew assembleNormalRelease assembleFullnameRelease --no-daemon --no-watch-fs -Dorg.gradle.vfs.watch=false --offline
```
- `--no-watch-fs` is required on this desktop (kernel ENOSYS on file watch).
- `--offline` avoids hangs on unreachable mirrors here.
- Dead jcenter-only artifacts are vendored in `third_party/maven`.
- Flavors: `normal`, `fullName`. Release APKs go to `dist/` (git-ignored).
- CI: `.github/workflows/debug-apk.yml` builds the debug APK + unit tests on every push (JDK 21).

## Signing
- Keystore `~/.android-keystores/openhubx-release.jks` (alias `openhubx`). Creds in `local.properties`
  (git-ignored). **Back it up — losing it blocks future updates.**

## Git
- `dev` holds modernize + rebrand + QR login + CI, search A–F and Phase 5.1. Local only; not merged to `mainline`.
- Room DB is v8: v7 adds `SEARCH_HISTORY`, v8 adds GitHub App token columns on `AUTH_USER`.

## What is built
- **Modernization (P1–P4):** namespace, AndroidX, greenDAO → Room (`dao/`, `Migrations.java`),
  Butterknife → ViewBinding, RxJava 1 → 3, AGP 9 / Java 21.
- **Navigation:** floating bottom bar (`content_main.xml`, `MainActivity`) — Search / Profile / Discover / More,
  each pops a selector card. Default landing = Trending.
- **Fixes:** OAuth null token, news feed NPEs, stale bookmarks tab, empty Trending/Collections/Topics,
  repo header overlap.
- **Features:** app-wide font size; bookmark export / import / share (`util/BookmarkBackup.java`).
- **QR login 5.0:** fine-grained PAT → offline generator `tools/qr-generator/index.html` → ZXing in-app scanner.
  Envelope base64url `{v:1, typ:"ghtok", tt, tok, exp, scope?, nonce}`, validated with `GET /user`
  (`auth/qr/QrLoginPayload*.java`).
- **Search:** language + advanced filters, Code and Issues/PR tabs, 350 ms debounce, stars sort,
  per-type history in Room (`util/SearchQueryBuilder.java`, `ui/widget/SearchFilterSheet.java`).
- **QR login 5.1:** GitHub App device-flow helper `tools/oauth-helper/openhubx_login.py` (see its README for
  one-time app setup), `tt:"ghapp"` tokens with on-phone refresh, `openhubx://login?d=` deep link.
  Notifications do not work with GitHub App tokens (GitHub limit).
- Bugly removed.

## Open TODOs
- Run the e2e tests on a real phone: `tools/e2e/run-e2e.sh` (see `vibes/test-plan.md`). Not run on a device yet.
- Remove the legacy side drawer (`BaseDrawerActivity`, `activity_main.xml`).
- About screen ignores font size (`AboutActivity` extends `MaterialAboutActivity`).
- Confirm GitHub links in `strings.xml`: `author_login_id`, `source_code_url`.
- Add an e2e test for each new bug (regression map in `vibes/test-plan.md`).
- Backlog: Global Recommendation feed, Gist bookmark sync, Daily/Weekly/Monthly Trending.
