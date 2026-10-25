# OpenHubX Modernization — P2c → P4 (Room · Butterknife · RxJava 3 · JDK 25 · Java 21)

Autonomous, checkpointed. Repo: `~/playground/openhubx`, branch `openhubx-modernization`.

## Already DONE and committed (do NOT redo)
- P1 namespace → com.darktile.openhubx (`06c91d1`)
- P2a blocker libs replaced with bespoke code (`29a96e7`)
- P2b AndroidX + AGP 8.7.3 + Gradle 8.9 + compileSdk/targetSdk 34 + 161-file support→androidx rewrite (`3553b7c`)
- P2c Room scaffolding: entities/DAOs/`OpenHubRoomDatabase`/`Migrations`/`DateConverter` under `app/src/main/java/com/darktile/openhubx/dao/room/` + Room 2.6.1 deps (`44c3674`, WIP — call sites NOT yet migrated, NOT build-verified). Start P2c from here.

## Ground rules
- **This entire plan is PRE-APPROVED by the user for unattended autonomous execution. Do NOT set `requires_approval` or `force_approval` on ANY task — every step is authorized. No approval gates.**
- Build command (JDK 17 through P3b; JDK 25 at the P4 end):
  `JAVA_HOME=<jdk> ANDROID_HOME=~/android-sdk ./gradlew assembleNormalDebug testNormalDebugUnitTest --no-daemon --no-watch-fs -Dorg.gradle.vfs.watch=false`
- Commit at every GREEN checkpoint with a `Pxx:` message prefix. Never commit red.
- `applicationId` = `com.darktile.openhubx` (changed from `com.thirtydegreesray.openhub`).
- Do NOT push (mainline protected; SSH auth is user-side). Leave commits local.
- If a step needs a genuinely new human decision not covered here, stop and report rather than guess — but do NOT gate routine modernization steps.

## Decided scope
- greenDAO → **Room** (migrate schema/DAOs; drop the archived greendao gradle plugin).
- RxJava 1 → **RxJava 3** (`io.reactivex.rxjava3`) app-wide.
- Butterknife → **removed** (ViewBinding/findViewById).

## Steps

### P2c — greenDAO → Room (finish the migration)
1. Verify/complete the Room layer scaffolded in `44c3674`: 4 entities (AuthUser, Bookmark, BookMarkRepo, BookMarkUser) as `@Entity` + `@Dao` + `@Database`; confirm `DBOpenHelper` migrations (incl. the v5→6 bookmark ownerLoginId column) are ported to Room `Migration`s.
2. Replace all greenDAO `DaoSession`/`*Dao` call sites (BookmarkPresenter, RepositoryPresenter, ProfilePresenter, BookmarkBackup, and any others) with the Room DAOs.
3. Remove greenDAO deps + the greendao gradle plugin.
4. Build (JDK 17) + tests → fix → GREEN → commit `P2c: greenDAO → Room`.

### P3 — remove Butterknife → ViewBinding
5. Enable `viewBinding`. Convert the 3 base classes (BaseActivity, BaseFragment, BaseViewHolder) off `ButterKnife.bind`, then the 45 subclasses: `@BindView`→binding refs, `@OnClick`→listeners.
6. Remove butterknife deps + plugin. Build + tests → GREEN → commit `P3: remove Butterknife`.

### P3b — RxJava 1 → 3
7. Swap deps: rxjava 3.1.8, rxandroid 3.0.2, retrofit 2.11.0 + adapter-rxjava3 + converter-gson; drop converter-simplexml (remove/replace the XML path). rxpermissions → `com.github.tbruyelle:rxpermissions:0.12`.
8. Rewrite `rx.*` → `io.reactivex.rxjava3.*`; Observable/Flowable/Single split; schedulers; null-safety. Dagger 2.11 → 2.51.1.
9. Build + tests → GREEN → commit `P3b: RxJava 1 → 3`.

### P4 — JDK 25 toolchain + Java 21 + desugaring
10. AGP → 9.2.x, Gradle wrapper → 9.7, `compileSdk` → 37 (build-tools 36.0.0+).
11. `compileOptions { sourceCompatibility/targetCompatibility = VERSION_21; coreLibraryDesugaringEnabled true }`; add `coreLibraryDesugaring "com.android.tools:desugar_jdk_libs:2.1.5"`.
12. Build with the Gradle daemon on **JDK 25** → fix → GREEN. Run unit tests. Assemble release APK.
13. Commit `P4: JDK 25 toolchain + Java 21 language level + desugaring`. Report final status + any live-smoke TODO.
