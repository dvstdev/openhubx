# OpenHub Modernization & Feature Plan

_Plan only — no implementation yet. Drafted 2026-09-02. Repo: `~/playground/openhubx` (OpenHub, a pure-Java Android GitHub client; MVP + Dagger2 + greenDAO + Retrofit)._

## Baseline reality (verified today)

The project **does not build as-is**:

| Fact | Value | Consequence |
|---|---|---|
| Gradle wrapper | 6.5 (2020) | Runs on JDK 8/11 only — not 17/21 |
| AGP | 4.1.0 | Caps Java language level at 8; refuses AndroidX-less builds only in v8 |
| Buildscript repos | `jcenter()` + `google()` | jcenter is **shut down** → config fails resolving `com.novoda:gradle-build-properties-plugin:0.3` |
| Android SDK | **not installed** (`ANDROID_HOME` missing, no `local.properties`) | Nothing compiles |
| Language | pure Java (no Kotlin), `sourceCompat 1.8` | — |
| Support libs | `com.android.support:*:25.4.0` (2017) | Pre-AndroidX; blocks AGP 8 |
| Reactive | RxJava **1.1.0** + `adapter-rxjava` (v1) | 1→2/3 is a breaking cascade |
| ORM | greenDAO 3.2.2 (EOL, no v4) | Stays on 3.x, or full Room rewrite later |
| SDK levels | compileSdk/target 29, minSdk 21 | Must reach compileSdk 34 for AGP 8 |

**Task 0 (prerequisite for everything and for any testing): get a green build.**
Install Android SDK + write `local.properties`; swap `jcenter()`→`mavenCentral()`; remove/replace the dead novoda `gradle-build-properties-plugin`. This establishes the baseline we test every later change against.

---

## Recommended sequencing

Two clusters with very different risk:

- **Low-risk, high-value, app-code-only** (items 3 & 4): can ship on the *current* toolchain the moment it builds, independent of the risky migration.
- **High-risk modernization** (items 1 & 2): AndroidX migration + multi-step Gradle/AGP/Java jump — touches nearly every file.

**Recommendation: do the bookmark fix first** (real user-facing data-loss *feel*, tiny, zero migration), then decide whether to front-load the modernization or the search/recommendation feature.

```
Phase 0  Green baseline build ............... unblocks all + test baseline
Phase 1  Bookmark fix + export (item 3) ..... app-code only, zero migration  ← ship early
Phase 2  Search + Global Recommendation (4) .. app-code only, additive
Phase 3  Toolchain stepping stone ........... Gradle 6.5→7.x, AGP 4.1→7.x, Java 8→11 (Jetifier still available)
Phase 4  AndroidX migration ................. the dominant risk; hard gate for AGP 8
Phase 5  Java 17 (item 1) .................. Gradle 8.7+, AGP 8.5+, compileSdk 34, sourceCompat 17
Phase 6  Dependency catch-up (item 2) ....... RxJava 1→3 cascade, Retrofit/OkHttp/Dagger/Glide bumps, ButterKnife exit
```
(Phases 1–2 may equally be done *after* 3–5 if you prefer to build features on a modern base — but they don't require it. Java 21 is optional; land on 17 unless a dep demands 21.)

---

## Item 1 — Java 21 "if applicable"

**Finding:** "Java 21 on Android" = build with a JDK 21 toolchain **+** `sourceCompatibility/targetCompatibility = VERSION_21` **on a modern AGP**. The language level is capped by AGP, not the JDK. minSdk stays 21 (bytecode runs on old devices via core-library desugaring).

**Version chain (order matters):** Java 21 language level ⇒ AGP ≥ 8.2.1 ⇒ Gradle ≥ 8.2 ⇒ build JDK 17+ ⇒ **compileSdk ≥ 34 + AndroidX (Jetifier gone in AGP 8)**.

**Recommendation: target Java 17, not 21.** 17 is the LTS every current AGP fully supports (records/sealed/pattern-switch), avoids bleeding-edge churn. Push to 21 only if a dependency requires it.
- Gradle 8.7+, AGP 8.5+, compileSdk/target 34, minSdk 21 (unchanged), drop explicit `buildToolsVersion`.
- `sourceCompatibility/targetCompatibility = VERSION_17` in both `:app` and `:daogenerator`; build with JDK 21 toolchain.
- Enable core-library desugaring if any `java.time`/stream APIs are used on minSdk 21.
- Do it as **6.5 → 7.x/AGP 7 (Java 11) stepping stone → 8.x/AGP 8 + AndroidX → Java 17**. Skipping straight to AGP 8 fails on both dead repos and support libs.

## Item 2 — Dependency catch-up

**Mechanical (low risk):** Retrofit 2.1→2.11 (already v2), Dagger 2.11→2.5x (within v2), OkHttp 3.6→3.14 (or 4.x, pulls Kotlin stdlib), Glide 4.0→4.16, jsoup/logger/eventbus/jUnit point bumps. 7 orphan version props are dead config — remove.

**Breaking (high effort), in order:**
1. **support 25.4.0 → AndroidX** — dominant migration; rewrites imports across the whole tree. Coupled UI libs need their own AndroidX-compatible bumps: ButterKnife 8→10 (deprecated — consider replacing with view binding), material-dialogs 0.9.x→3.x (API redesign), circleimageview, material-about-library, richtext, sticky-headers. Jetifier is gone in AGP 8 → must be a real port.
2. **RxJava 1.1 → 2/3** — package `rx.*`→`io.reactivex.*`, nullability, operator semantics; cascades to Retrofit `adapter-rxjava2/3` and rxpermissions 0.10+. Touches every `Observable`/`Subscriber`.
3. **greenDAO** — stay on 3.x (safe/mechanical). Room migration is a separate, later, full persistence-layer rewrite (also rewrites the `:daogenerator` module).

## Item 3 — Bookmark loss + export

**Confirmed root cause (NOT account partitioning):** the DB (`OpenHub.db`) is a single shared file with no account column; `MainPresenter.logout()` deletes only the `AuthUser` row — bookmarks are provably untouched. The real bug: `BookmarkPresenter.loadBookmarks()` (`mvp/presenter/BookmarkPresenter.java:46-76`) rehydrates each row via `User.generateFromLocalUser` / `Repository.generateFromLocalRepo` **with no null check**, but the bookmark-toggle path (`RepositoryPresenter.bookmark()` / `ProfilePresenter.bookmark()`) inserts **only** the `Bookmark` row and **never** the companion `LOCAL_USER`/`LOCAL_REPO`. Any bookmark whose companion is absent throws NPE inside `rxTx().run(...)`, and the `.subscribe(...)` has **no `onError`** → the whole list renders empty. A cold restart after login re-fires `loadBookmarks(1)`, which is why it *feels* tied to logout/login.

**Fix (pure app-code, zero schema migration, SCHEMA_VERSION stays 5):**
1. Null-guard the join in `loadBookmarks` — skip/placeholder rows with missing `LOCAL_*`.
2. Add an `onError` handler so one bad row can't blank the entire list.
3. **Durable fix:** make the toggle self-sufficient — upsert `LocalRepo`/`LocalUser` when the `Bookmark` is inserted (mirror `saveTrace()`).
4. Cleanup: delete dead `createForBookmark()` paths + `BookmarkRepo`/`BookmarkUser` DAOs that read tables the v3→v4 migration DROPS.

**Export feature:** `FileProvider` already registered (authority `${applicationId}.fileProvider`). Add an "Export bookmarks" action to `BookmarksFragment`; new `BookmarkPresenter.exportBookmarks()` serializes all rows to JSON (`{version, exportedAt, bookmarks:[{type, repoId/userId, fullName/login, markTime}]}`) with enough identity to survive re-import.
- Write path A (simplest): app-scoped `getExternalFilesDir()` + `ACTION_SEND` share via FileProvider (add `<external-files-path>` to `provider_paths.xml`). No runtime permission.
- Write path B (user picks location): SAF `ACTION_CREATE_DOCUMENT` (clean on target 29 scoped storage, no `WRITE_EXTERNAL_STORAGE`).
- Natural follow-up: "Import bookmarks" (`ACTION_OPEN_DOCUMENT` → parse → upsert), pairs with fix step 3.

## Item 4 — Search + home recommendation

**Search today:** only Repository + User tabs (`SearchService` has `searchIssues` but it's unwired; no code search). Submit-only (no debounce), sort hidden in overflow menu, **no language/qualifier filters** (users hand-type `language:`), history stored as a `$$`-joined prefs string (drops any query containing `$`).

**Search improvements (no new backend):**
- A. **Language filter** (highest value / lowest effort) — append `language:` qualifier from the existing `TrendingLanguage` list.
- B. **Advanced-filter sheet** — emit `stars:>N`, `pushed:>date`, `fork:`, `user:/org:`, `topic:`, `license:` into `q`.
- C. **Code + Issue/PR tabs** — wire existing `searchIssues`, add `searchCode` → `GET search/code`.
- D. **Debounce** live suggestions (~300–400ms, **flat** delay per your convention — not exponential).
- E. Default repo sort to `stars` desc; optional recency tie-break.
- F. Move history to greenDAO, per-type, add clear-history, stop dropping `$` queries.

**Home recommendation today:** the default feed is GitHub `received_events` — literally the activity of people you already follow. Cold-start (following nobody) = empty feed. Confirms your complaint.

**New "Global Recommendation" mode (independent of follow graph):**
- Powered by the **official** `search/repositories` API: `stars:>N pushed:>{last N days}` sorted `sort=stars` — "popular repos with recent activity." Reuses `SearchService.searchRepos` + the whole repo-list UI/adapter/pagination.
- Optional personalization: bias by the user's own top languages/topics (derived from their starred/owned repos) — personalized global *discovery* without any GitHub rec API.
- Distinct from the existing **Trending** page (which uses a self-hosted third-party scraper, not GitHub's API): Global Recommendation is API-native, dependency-free, tunable, and personalizable. Could later *replace* Trending's scraper data source.
- Make it the fallback feed when `received_events` is empty (fixes cold-start).
- Note: `search/repositories` is rate-limited (30 req/min auth, 1000-result cap) — cache page 1, don't fire on every scroll.

---

## Test strategy

**Reality:** repo has essentially no meaningful tests today (jUnit 4.12 present, Espresso 2.2.2 wired but no coverage). Test infra must be (re)built as part of Phase 0.

- **Phase 0 (baseline):** `./gradlew assembleDebug` green + `./gradlew testDebugUnitTest` green (even if near-empty) + `./gradlew lint`. This is the reference build every later phase must keep green. Capture APK builds + app launches on an emulator (`minSdk 21` + a modern API image).
- **Phase 1 (bookmark):** unit tests for `loadBookmarks` with (a) a bookmark whose `LOCAL_*` companion is missing → list still renders the good rows, no crash; (b) `onError` path exercised. Instrumented/manual: bookmark repo+user → force-quit → relaunch → list intact; logout→login→list intact; export → valid JSON on disk / share sheet opens. Regression guard: the exact repro (bookmark an item without visiting it, so no trace row) must no longer blank the list.
- **Phase 2 (search/rec):** unit tests on the query-builder (language + qualifier composition produces correct `q`), debounce timing, history round-trip through greenDAO (incl. a `$`-containing query). Instrumented: language filter narrows results; Global Recommendation returns non-empty for a no-follow account; rate-limit backoff on 403.
- **Phases 3–5 (toolchain/AndroidX/Java):** each step must `assembleDebug` + `testDebugUnitTest` + `lint` green before the next. AndroidX migration: run Android Studio's migrate-to-AndroidX as a first pass, then compile-fix; smoke-test every screen (login/OAuth, repo view, user profile, search, trending, bookmarks, issues, settings themes). Verify `sourceCompatibility` actually took (compile a Java-17 language feature). Confirm APK still installs/runs on an API-21 emulator (desugaring check).
- **Phase 6 (deps):** RxJava 1→3 is the riskiest — full manual pass over every reactive screen; watch for threading/`observeOn` regressions and swallowed errors. Pin exact versions; no open ranges.
- **Cross-cutting:** wire a CI-style local script that runs assemble+unit+lint; add smoke instrumentation for the top 8 screens; keep a per-phase "green build" checkpoint so any regression bisects to one phase.

## Open questions for you
1. **Sequencing:** ship the bookmark fix + search/rec on the current toolchain first (fast user value), or front-load the Java/AndroidX modernization so features are built on a modern base?
2. **Java target:** 17 (recommended) or hard-require 21?
3. **Export:** share-intent (path A) or user-picks-location SAF (path B)? Include the Import counterpart?
4. **Recommendation:** add Global Recommendation as a *new* tab alongside Trending, or eventually *replace* Trending's scraper with the API-native query?
5. **greenDAO:** stay on 3.x (recommended), or scope a Room migration separately?

## Phase 5 (backlog) — QR-code login / device pairing

Requested feature: generate a QR code on a laptop and scan it from the phone (in-app
scanner or the system camera) to log the phone in.

- **Design (no-backend):** OpenHubX has no server, so the QR encodes a credential the
  laptop already holds — a short-lived GitHub OAuth token (preferred) or a PAT. The phone
  scans it and imports the token into its auth store. Fully offline.
  - Security: the QR carries a bearer token. Acceptable for a personal single-user client
    only. Constrain to short-lived tokens, never log the value, and consider a wrap/nonce.
  - The "correct" GitHub pattern (OAuth **Device Flow**) is phone-initiated (code shown on
    the phone, entered on the laptop) — the reverse of the requested UX and it needs a
    pairing backend, so it's out of scope for a no-backend client.
- **Two scan entry points:**
  1. **In-app scanner** — CameraX + ML Kit barcode (preferred). Requires AndroidX, so this
     is gated on Phase 2. (Legacy ZXing would work on the old stack but we'd rip it out.)
  2. **System camera → app** — needs an app-link / custom deep-link scheme so scanning
     outside the app opens OpenHubX with the token payload.
- **Sequencing:** build AFTER Phase 2 (AndroidX) so we use CameraX/ML Kit, not a
  throwaway legacy dependency. Depends on: AndroidX migration complete.
- **Status:** BACKLOG (tracked). Not started. AndroidX dependency now SATISFIED (Phase 2 complete), so the CameraX/ML Kit path is unblocked.

### Phase 5 — detailed spec

#### Flow diagram (laptop -> phone)

```
  LAPTOP (generator)                              PHONE (OpenHubX)
  ------------------                              ----------------
  1. obtain GitHub token
     (fine-grained PAT, or
      OAuth via helper)
            |
  2. build envelope JSON  ---> encode base64url
            |
  3. render QR (client-side only,
     token never logged/sent)
            |
            |    (visual scan)
            +--------------------------------> 4. scan QR
                                                  - in-app: CameraX + ML Kit / ZXing
                                                  - or system camera -> deep link
                                                       |
                                               5. decode -> parse envelope
                                                  reject if v mismatch / no tok / exp past
                                                       |
                                               6. validate: GET /user with token
                                                       |
                                               7. store token in auth store -> logged in
```

No GitHub server-side cooperation at any step: the phone just makes normal
authenticated API calls with the transported bearer token.

#### Payload contract (both sides must agree)

Compact JSON, base64url-encoded, carried raw in the QR or as
`openhubx://login?d=<base64url>` for the deep-link variant:

```
{ "v":1, "typ":"ghtok", "tt":"pat|oauth|ghapp", "tok":"<token>",
  "exp":<unix|null>, "scope":"repo,user,notifications", "nonce":"<rand>" }
```

Phone rejects on: `v` mismatch, missing `tok`, or `exp` in the past. Always
validates with `GET /user` before storing. QR capacity is not a concern (even a
~120-char fine-grained PAT + envelope fits easily).

#### Generator (laptop) options

- **Option A - static offline HTML (MVP, paste-a-PAT):** one self-contained
  `.html` with bundled `qrcode.js`. User creates a fine-grained PAT at
  `github.com/settings/tokens` (scopes + expiry), pastes it, QR renders
  client-side. Offline, no backend, ~an afternoon. Con: manual + technical.
- **Option B - local OAuth helper (polished):** small Tauri/Electron or
  `localhost` page registered as a GitHub OAuth/GitHub App client, using Device
  Flow or auth-code + PKCE (loopback). User clicks "Sign in with GitHub" ->
  normal consent screen -> helper gets a short-lived GitHub App user token
  (~8h, refreshable) -> auto-renders QR with real `exp`. No PAT, no copy-paste.
  Closes the long-lived-bearer security caveat. Days of work + a registered app.
- **Common requirements:** emit the exact payload envelope; never log/transmit
  the token (client-side render only); surface scopes + expiry to the user.
- **Constraint:** OpenHubX is Android-only — there is no official desktop app, so
  the generator is net-new surface to build and distribute. This is the main
  reason Phase 5 stays backlog rather than a quick win.

#### Phone side (scanner) + size impact

Current release APK is ~4.5 MB. Approximate APK deltas (post-R8; estimates,
verify with apkanalyzer):

- CameraX (core/camera2/lifecycle/view): ~1-1.5 MB.
- ML Kit barcode **bundled** (`com.google.mlkit:barcode-scanning`): ~+3-4 MB,
  fully offline. Total with CameraX ~+4-5 MB -> APK roughly doubles (~9 MB).
- ML Kit barcode **unbundled** (`com.google.android.gms:play-services-mlkit-barcode-scanning`):
  ~+0.3-0.6 MB APK; model downloaded on first use via Play Services (requires GPS).
- **ZXing** (`zxing-android-embedded`): ~0.3-0.6 MB, pure-Java, no Play Services,
  decodes plain QR fine. Smallest option; ML Kit only pays off for robust
  multi-format / damaged-code / live scanning. For a single bearer-token QR,
  ZXing is the size-conscious choice.

Two scan entry points: (1) in-app scanner (camera permission -> live decode);
(2) system camera -> app-link/deep-link opens OpenHubX with the payload.

Failure UX to cover: bad QR, expired token, insufficient scopes, camera denied
(fall back to deep-link or manual paste).

#### Sequencing

- **Phase 5.0 (MVP):** Generator Option A (static paste-PAT) + phone-side scanner
  (ZXing for min size, or ML Kit unbundled) — validates the round-trip cheaply.
- **Phase 5.1 (polish):** upgrade generator to Option B (OAuth helper ->
  short-lived GitHub App user token), removing manual PAT and the long-lived
  bearer risk.

#### Phase 5 — decisions (locked 2026-09-08)

1. **Token type:** 5.0 = **fine-grained PAT** (user-set short expiry, no OAuth-app
   registration); 5.1 = **GitHub App user-to-server token** (~8h + refresh).
2. **Laptop generator:** 5.0 = **Option A** static offline paste-PAT HTML;
   5.1 = **Option B** local OAuth helper (PKCE / device flow).
3. **Phone scanner lib:** **ZXing** (`zxing-android-embedded`, ~0.3-0.6 MB, no
   Play Services). Reconsider ML Kit only if multi-format / damaged-code scanning
   is ever needed.
4. **Scan entry point:** 5.0 = **in-app scanner only**; system-camera deep-link
   deferred to 5.1+.
5. **Payload envelope (locked):** `{ v:1, typ:"ghtok", tt, tok, exp, nonce }`,
   `scope` optional, base64url. Phone rejects on v-mismatch / missing `tok` /
   past `exp`, then validates via `GET /user`. Deep-link scheme
   (`openhubx://login?d=...`) deferred with #4.

**MVP (5.0) = fine-grained PAT -> static HTML generator -> ZXing in-app scanner
-> locked envelope. Polish (5.1) = OAuth helper + GitHub App token (+ optional
deep-link).**

## Verified modernization plan (2026-09-06 recon) — Phases 2–4

Authoritative, supersedes the earlier Java-17 sketch above. Based on 3 recon passes.

### Target toolchain (Phase 4 end-state)
- AGP **9.2.x**, Gradle **9.7.x**, Gradle daemon on **JDK 25** (Gradle ≥9.1 required).
- App language level **Java 21** (D8/R8 hard cap — Java 25 bytecode is NOT a legal Android target).
- `compileSdk 37` (AGP 9.2 forces it; build-tools ≥36.0.0), minSdk 21 kept.
- Core-library desugaring: `com.android.tools:desugar_jdk_libs:2.1.5` + `isCoreLibraryDesugaringEnabled`.

### Hard blockers (need bespoke work, not a version bump)
1. `com.unstoppable:submitbutton 1.1.2` — no AndroidX → custom progress button (or loading-button-android).
2. `com.timehop.stickyheadersrecyclerview 0.4.3` — no AndroidX → custom RecyclerView ItemDecoration.
3. `rxpermissions 0.9.4` → `0.12` — forces RxJava 1→3 migration (or replace with AndroidX Activity Result API).
4. `material-dialogs 0.9.4.5` — archived → rewrite call sites to MDC MaterialAlertDialogBuilder.
5. greenDAO 3.2.2 codegen gradle plugin — not AGP-9 compatible → DECISION: pin/patch plugin vs migrate to Room.
6. `richtext 2.5.4` + `glideimagegetter 1.0.5` → `richtext 3.0.4` (absorbs image getter).

### Version bumps / namespace rewrites (low–med)
- support 25.4.0 → androidx: appcompat 1.6.1, recyclerview 1.3.2, material 1.11.0 (design), cardview 1.0.0,
  preference 1.2.1, browser 1.7.0 (customtabs), legacy-support-v4 1.0.0; constraintlayout 2.1.4.
- Butterknife 8.7.0 → remove → ViewBinding/findViewById (48 files, 246 usages, 3 base classes bind). [alt: bump to 10.2.3 AndroidX to defer, stays on archived lib.]
- RxJava 1.1→3.1.8 + RxAndroid 3.0.2 (app-wide API rewrite). Retrofit 2.1→2.11 + adapter-rxjava3, drop converter-simplexml.
- Dagger 2.11→2.51.1, Glide 4.0→4.16.0, EventBus 3.0→3.3.1, jsoup 1.11.2→1.17.2, circleimageview 3.1.0,
  Toasty 1.5.2, logger 2.2.0, material-about-library 3.2.0, junit 4.13.2, espresso 3.5.1 + androidx.test runner.
- DataAutoAccess 1.2.8 (author's own, pure-Java) — keep via mirror.

### Ordered execution (each a red→green push; commit at every green)
- **P2a** Replace the 2 no-successor blockers (submitbutton, stickyheaders) with bespoke AndroidX code.
- **P2b** Flip `android.useAndroidX=true`, bump AGP→8.x/Gradle interim, rewrite all android.support.*→androidx.*, swap all support/blocker deps to AndroidX targets (incl. material-dialogs→MDC, richtext 3.x).
- **P3** Remove Butterknife → ViewBinding across 48 files.
- **P3b** RxJava 1→3 migration (Observable/Flowable split, package rename) + rxpermissions 0.12 + retrofit adapter-rxjava3.
- **P4** AGP→9.2 / Gradle→9.7, compileSdk 37, sourceCompatibility 21, desugaring, daemon on JDK 25; re-verify build + tests + live smoke.

### Effort / risk
Rewrite-scale, multi-day. Stays un-buildable through most of P2. greenDAO plugin + RxJava migration are the
two biggest unknowns. Recommend executing as a checkpointed autonomous task (meshclaw run) or a dedicated
long session, NOT turn-by-turn in Slack.
