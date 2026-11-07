# OpenHubX — test plan

Focus: **end-to-end tests** that drive the real app on a phone or emulator. Unit tests stay
small (existing ones in `app/src/test` cover QR parsing, search query building, history and
debounce) and are not the main safety net.

## How the e2e tests work

- Code: `app/src/androidTest/java/com/darktile/openhubx/e2e/`.
- They use UiAutomator, so they can follow the app into the browser and back.
  A crash anywhere kills the app process and fails the test.
- They run on a separate app, `com.darktile.openhubx.e2e` (build type `e2e`). Your real
  app and its accounts and bookmarks are never touched. Gradle uninstalls the e2e app after.
- Tests that need a signed-in account read a token from the `ghToken` argument and are
  skipped without it.

## Run

```sh
# Phone with USB debugging on, or an emulator with a browser installed.
tools/e2e/run-e2e.sh                         # no-token tests only
GH_TOKEN=github_pat_... tools/e2e/run-e2e.sh # all tests
tools/e2e/run-e2e.sh GuestModeE2eTest        # one class
```

Report: `app/build/reports/androidTests/connected/e2e/flavors/normal/index.html`.

CI (`.github/workflows/debug-apk.yml`, job `e2e`) runs the same tests on an API 34 emulator
after the debug build. CI gets no token, so the signed-in tests are skipped there.

Use a short-expiry fine-grained token made for testing. The token is passed on the Gradle
command line, so it can show up in the process list while the run is going.

## Regression map (bugs found → tests)

| Bug | Root cause | Test | Needs |
|---|---|---|---|
| "Just browse" crashed | Account menu read a null user | `GuestModeE2eTest.justBrowse_opensHomeAndStays` | — |
| Guest app could not start (loop) | `BaseActivity` bounced guests to Splash | `GuestModeE2eTest.guestRelaunch_landsOnHomeWithoutLoop` | — |
| Guest tapped an account page | Pages assumed a user | `GuestModeE2eTest.guest_accountPage_asksToSignIn` | — |
| About force-closed | `MaterialCardView` on an AppCompat theme | `GuestModeE2eTest.about_opens` | — |
| Keyboard covered search | Box was in the toolbar | `GuestModeE2eTest.search_boxStaysVisibleWithKeyboard` | — |
| "No browser clients installed" | No `<queries>` on Android 11+ | `BrowserLoginE2eTest.appCanSeeABrowser` | a browser |
| Browser login crashed | `PendingIntent` with no mutability flag (Android 12+) | `BrowserLoginE2eTest.browserRow_opensBrowserWithoutCrash` | a browser |
| Add / switch account crashed | Same `PendingIntent` bug via Add account | `AccountE2eTest.addAccount_browserLogin_doesNotCrash` | token |
| Switch with one account | — (guard) | `AccountE2eTest.switchAccount_withOneAccount_offersAdd` | token |
| Notifications: "java.lang.Error · Tap to retry" | Empty HTTP/2 reason + 403 shown as retryable | `AccountE2eTest.notifications_fineGrainedToken_showsHintNoRetry` | fine-grained token |
| Bookmarks lost on switch | `OWNER_LOGIN_ID` never written or read | `BookmarkPerAccountE2eTest` (2 tests, device database) | — |
| Import took another account's bookmarks | Import kept the file's bookmark ids, so it overwrote rows on the same phone | `BookmarkPerAccountE2eTest.importOtherAccountsExport_copiesWithoutTakingTheirs` | — |
| Import failed on files from other phones | Dates are saved as locale text; one bad row failed the whole file | `BookmarkBackupParseTest` (unit) | — |

## Still manual

| Check | Why not automated |
|---|---|
| Full browser OAuth round trip | Needs a real GitHub sign-in in the browser |
| Scan QR with the camera | Needs a printed or on-screen QR in front of the camera |
| QR refresh of `ghapp` tokens after 8 h | Needs a long wait |
| Tampermonkey scripts on github.com | Run in a desktop browser; logic was checked in jsdom |
| Every screen after the RxJava 3 move | Broad smoke pass; add e2e tests as bugs show up |

## Adding a test for a new bug

1. Reproduce it by hand and note the screen ids involved (`android:id`).
2. Add a method to the matching `*E2eTest` (or a new class) using the helpers in `E2e.java`:
   `launchLogin`, `launchFromIcon`, `tap`, `waitFor`, `shows`, `signInWithToken`.
3. Name it `<screen>_<action>_<expected>` and add a row to the table above.
