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
