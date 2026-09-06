# OpenHubX — Hand-off

Fork of the OpenHub Android GitHub client, rebranded to **OpenHubX** and modernized.
Path: `/home/jingaoh/playground/openhubx`

## Stack
- Java only (no Kotlin). MVP + Dagger2 2.11 + greenDAO 3.2.2 + Retrofit 2.1 + RxJava 1.1 + Butterknife 8.7.
- compileSdk 29 / minSdk 21, support-lib 25.4.0 (pre-AndroidX).
- applicationId unchanged: `com.thirtydegreesray.openhub`. Version: **2026.09.1** (versionCode 35).

## Build (IMPORTANT — desktop specifics)
```bash
export ANDROID_HOME=/home/jingaoh/android-sdk
export JAVA_HOME=/usr/lib/jvm/java-11        # MUST be JDK 11; system default is 17 which breaks Butterknife
cd /home/jingaoh/playground/openhubx
./gradlew assembleNormalDebug --no-daemon --no-watch-fs -Dorg.gradle.vfs.watch=false     # debug
./gradlew assembleNormalRelease assembleFullnameRelease --no-daemon --no-watch-fs -Dorg.gradle.vfs.watch=false  # signed release
```
- `--no-daemon --no-watch-fs` required — kernel ENOSYS on Gradle file-watch on this box.
- JDK 17 fails with `module jdk.compiler does not export com.sun.tools.javac.tree` (old Butterknife processor).
- Dead jcenter-only artifacts resurrected via Aliyun mirror (`repo.huaweicloud.com/repository/maven/`); `javax.annotation-api:1.3.2` added for JDK9+ Dagger/Glide codegen.
- Flavors: `normal` and `fullName`. Signed release APKs copied to `dist/` (git-ignored, NOT committed).

## Signing
- Private keystore: `/home/jingaoh/.android-keystores/openhubx-release.jks` (PKCS12, RSA-2048, alias `openhubx`, CN=dvstdev). Creds in `local.properties` (git-ignored). **Back this up — losing it blocks future updates.**

## Git state
Branch `mainline`, 4 local commits ahead of `origin/mainline` (NOT pushed; mainline is protected):
- `330dbb2` Bookmark backup (export/import/share) + fix duplicate Profile cell
- `9ef3afb` Bottom-nav redesign, discovery fixes, and app polish
- `eeae773` Rebrand to OpenHubX by dvstdev
- `d28fc2a` Make project buildable + fix bookmark-list crash

**Uncommitted working-tree changes** (staged for a follow-up commit):
- `README.md` — added a **Building & Debugging** section (JDK 11, SDK, `local.properties`, signing).
- Pruned now-dead bottom-nav resources: deleted `res/drawable/ic_bn_discover.xml` + `bn_cell_border.xml`; removed the `BnBar.Emoji` style (`styles_bottom_nav.xml`) and `bn_glyph_*` strings (`strings_bottom_nav.xml`). `ic_bn_more.xml` retained (still referenced by `content_main.xml`).
- `dist/` build artifacts (intentionally untracked).

## What was built

### Navigation — floating bottom bar (Telegram-style, centered pill)
`res/layout/content_main.xml` + `MainActivity.java`. Dark (#37474F) floating `CardView` with 4 white vector icons; active tab tinted amber (#FFCA28). Integrated into MainActivity's existing tag-based fragment-swap system.
- **🔍 Search** → `SearchActivity` (no in-place fragment).
- **👤 Profile** → pops a 2×3 selector card + defaults to My Repos. Cells: Trace · Notifications · Bookmarks · My Repos · Issues · Starred.
- **🌐 Discover** → pops a 2×2 card + defaults to Trending. Cells: Now · Trending · Topics · Following.
- **⋯ More** → card: Settings · About.
- Tapping a bar button pops its card AND navigates to the tab default. Tap-outside / scroll / Back dismisses the card. Default landing after login = **Trending**. The old drawer still exists (removal is a TODO).

Card cell → content mapping:
- **Trace** → `TraceActivity` 2-tab pager: **History** (`TraceFragment`, browsing history) + **Activity** (`ActivityFragment(User, me)` = your commit/star events).
- **Following** → `ActivityFragment(News)` (received-events feed of accounts you follow).
- **Now** → `ActivityFragment(PublicNews)` (global/public event feed).
- Notifications/Bookmarks/My Repos/Starred/Trending/Topics → existing fragments. Issues → `IssuesActivity.showForUser`.

### Bug / empty-tab fixes
- OAuth null-token crash (`LoginPresenter`/`BasicToken`/`OauthToken`).
- News PushEvent NPE + pagination action-enum crash (`ActivitiesAdapter`).
- Bookmark tab stale on tab switch (`BookmarksFragment.onHiddenChanged`).
- Trending empty → replaced dead HTML scraper with GitHub `search/repositories` API (`RepositoriesPresenter`).
- Collections + Topics empty → resilient selectors for current GitHub HTML (`CollectionsPresenter`, `TopicsPresenter` — key on stable `<article>` / `a[href^=/collections/|/topics/]`).
- Repo detail: bookmark action added beside star (`menu_repository.xml` + `RepositoryActivity`); fixed summary/name overlap on entry (`activity_repository.xml`, marginTop 86→112dp).
- Bottom bar no longer shifts on scroll (`setToolbarScrollAble(false)`).

### Features
- **Font size** setting (Small/Normal/Large/XL) applied app-wide via `configuration.fontScale` in `AppUtils.update*Resources()` (`SettingsFragment` + `PrefUtils.getFontScale`).
- **Bookmark backup** (`util/BookmarkBackup.java`): Export / Import / Share JSON from the **Bookmarks screen overflow menu** (`menu_bookmarks.xml`, `BookmarksFragment`). Export/Import via SAF; Share via FileProvider (`com.thirtydegreesray.openhub.fileProvider`, cache-path). Serializes bookmarks + companion LocalRepo/LocalUser; import dedupes by repoId/userId.

## Behavior notes / decisions
- Bookmarks are install-global (no per-account column). **Logout preserves them** (only deletes AuthUser). **Uninstall wipes** the local DB — hence export/import/share for durability. Cross-device Gist sync was proposed but NOT built.
- `allowBackup="true"` gives best-effort Auto Backup on reinstall.
- Rebrand: author dvstdev; About "source code" → `https://github.com/dvstdev/OpenHubX`, "follow on GitHub" → `https://github.com/dvstdev` (both open in browser). **Confirm/adjust the real handle/repo URL** in `strings.xml` (`source_code_url`, `author_login_id`) if different.

## Open TODOs / follow-ups
- Remove the legacy side drawer (agreed TODO).
- About screen doesn't pick up the font-scale (it extends `MaterialAboutActivity`, not `BaseActivity`).
- Confirm the GitHub source/follow URLs are correct.
- Optional: Gist-based cross-device bookmark sync.
- Optional: nested Daily/Weekly/Monthly Trending; a real in-place SearchFragment.
- ~~Prune unused bottom-nav resources~~ — DONE (uncommitted): `ic_bn_discover.xml`, `bn_cell_border.xml`, `BnBar.Emoji` style and `bn_glyph_*` strings removed. `ic_bn_more.xml` is still in use.

## Session log — 2026-09-03 (what was discussed / fixed / decided)

### Bugs fixed
- Login OAuth null-token NPE.
- News feed: PushEvent missing `commits` NPE + pagination `valueOf()` throw on newer GitHub action strings.
- Bookmarks tab stale on tab re-entry.
- Trending / Collections / Topics empty (dead scraper / stale selectors).
- Repo detail: summary overlapped repo name on entry.
- Bottom bar shifted on scroll.
- Duplicate "Bookmarks" cell in the Profile card.

### Features added
- Repo bookmark action beside the star.
- Font size setting (app-wide).
- Bookmark backup: Export / Import / Share.
- Version scheme → 2026.09.1.

### UI redesign iterations (bottom nav)
- Drawer-only → floating Telegram-style pill; settled on 4 tabs (Search / Profile / Discover / More) + selector cards.
- Icons: emoji → white vector icons + amber active-highlight; globe for Discover; dark bar with shadow.
- Behavior: tap-pops-card-and-navigates-to-default; tap-outside/scroll/Back dismisses card; default landing = Trending.
- Trace = 2-tab (History + my Activity); Now = PublicNews (global); Following = News (accounts you follow).

### Problems discussed (context, not standalone features)
- Bookmark durability: logout keeps bookmarks; only uninstall wipes them. Chose export/import/share over Gist sync.
- Build env: JDK 11 required (17 breaks Butterknife); `--no-watch-fs` needed (kernel ENOSYS).

### Pending
- Uncommitted (working tree): `README.md` Building & Debugging section (added), bottom-nav resource prune, and `HANDOFF.md`.
- Remove the legacy side drawer.
- Confirm/fix the GitHub source + follow URLs (`source_code_url`, `author_login_id`).
- About screen font-scale (extends `MaterialAboutActivity`, not `BaseActivity`).
- Optional: Gist cross-device bookmark sync; nested Daily/Weekly/Monthly Trending; in-place SearchFragment.
- Verify on device (no UI run here): card layouts, active highlight, dismiss-on-scroll, repo header spacing, font scaling, bookmark export/import/share round-trip.
- Not pushed: commits `9ef3afb` + `330dbb2` are local only (mainline protected).

## Key files
- Nav: `app/src/main/res/layout/content_main.xml`, `app/src/main/java/.../ui/activity/MainActivity.java`
- Cards/labels/styles: `res/values/strings_bottom_nav.xml`, `res/values/styles_bottom_nav.xml`, `res/values/ids.xml`
- Bookmark backup: `app/src/main/java/.../util/BookmarkBackup.java`, `.../ui/fragment/BookmarksFragment.java`, `res/menu/menu_bookmarks.xml`
- Trending/Collections/Topics: `.../mvp/presenter/RepositoriesPresenter.java`, `CollectionsPresenter.java`, `TopicsPresenter.java`
- Font size: `.../util/AppUtils.java`, `PrefUtils.java`, `res/xml/settings.xml`, `res/values/arrays_font.xml`
