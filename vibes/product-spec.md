# OpenHubX — product spec

OpenHubX is a free Android client for GitHub. It is a fork of OpenHub, rebranded and
rebuilt on a current Android stack. There is no OpenHubX server: the phone talks to
GitHub directly, and every account and bookmark lives on the phone.

## Users and goals

| User | Goal |
|---|---|
| GitHub user on a phone | Read repos, issues, people, trending, and keep bookmarks |
| Visitor without an account | Look at Trending and Topics without signing in |
| Person with several accounts | Switch between them without losing anything |
| Developer of OpenHubX | Sign in fast on test phones (token / QR), run e2e tests |

## 1. Sign in

The login screen is a list. Each row has an icon, a title, one short line and an arrow.

| Row | What it does | Notes |
|---|---|---|
| **Browser** | GitHub OAuth in a Custom Tab or the default browser; returns via `openhub://login` | Only way that also reads Notifications |
| **Scan QR** | In-app ZXing scanner; camera permission asked on first use | Also accepts `openhubx://login?d=...` from the system camera |
| **Paste token** | Opens a panel: steps, a masked token field, a sign-in button | Fine-grained or classic token |
| **Just browse** | Guest mode, lands on Trending | No account is created |

Rules:
- Every token (pasted or scanned) is checked with `GET /user` before it is saved. A token
  GitHub rejects shows "Token rejected by GitHub" and nothing is saved.
- A QR envelope is rejected when the version is wrong, the token is missing, or `exp` has passed.
- GitHub App tokens (`ghu_`) with a refresh token renew themselves on the phone.
  An expired token that can't renew shows "Login expired, scan a new code".
- Tokens are never logged. The token field is masked by default.

### QR envelope (v1)

Base64url (no padding) of compact JSON:

```
{"v":1,"typ":"ghtok","tt":"pat|oauth|ghapp","tok":"<token>","exp":<unix|absent>,
 "scope":"<csv, optional>","nonce":"<hex>", "rt":"<refresh, ghapp only>",
 "rexp":<unix>, "cid":"<public client id>"}
```

### Laptop-side QR makers

| Tool | Use |
|---|---|
| `tools/qr-generator/index.html` | Offline page: paste a PAT, get a QR. No network (CSP `connect-src 'none'`) |
| `tools/oauth-helper/openhubx_login.py` | GitHub App device flow; shows a QR with a short-lived token + refresh |
| `tools/userscript/openhubx-github-tokens.user.js` | Tampermonkey: a **QR** button next to each token on `github.com/settings/*` |
| `tools/userscript/openhubx-qr-selection.user.js` | Tampermonkey: right-click > QR for any selected text (Chromium only) |

## 2. Guest mode ("Just browse")

- Works with no account and no token. Trending and Topics load as a visitor.
- Pages that need an account (My repos, Starred, Bookmarks, Following, Now, Notifications,
  Issues, Profile, Switch account, Logout) show "Sign in to use this." with a Login button.
- The guest choice is remembered across app restarts. A real sign-in or logout clears it.
- The side drawer header shows "Guest / Tap to sign in".

## 3. Accounts

Found in the bottom bar **More** card, top row.

| Action | Behavior |
|---|---|
| Switch account | Lists the other signed-in accounts; picking one restarts the app as that account. With no other account, it offers "Add account" |
| Add account | Opens the login screen; the new account is added and becomes current |
| Logout | Confirms, removes only the current account, returns to the login screen |

## 4. Bookmarks

- Repos and people can be bookmarked. Each bookmark belongs to the account that made it.
- Switching accounts shows that account's own bookmarks. No bookmark is lost on switch.
- Export / import / share as JSON (`util/BookmarkBackup.java`), scoped to the current account.
  Import skips duplicates within that account.

## 5. Home and navigation

- A floating bottom bar: **Search**, **Profile**, **Discover**, **More**. Each of the last
  three opens a card of choices above the bar. Default landing page: Trending.
- Profile card: Trace, Notifications, Bookmarks, My repos, Issues, Starred.
- Discover card: Random (public news), Trending, Topics, Following.
- More card: Switch account, Add account, Logout / Settings, About.

## 6. Search

- The type box and the search button sit at the **bottom**. The keyboard never covers them
  (the screen resizes).
- History shows above the box, newest nearest the box. Tap a row to search it again.
- Tabs for Repositories, Users, Code and Issues/PRs. Language and advanced filters, sort,
  350 ms debounce, per-type history in Room, "Clear history" in the menu.

## 7. Errors and limits

| Case | What the user sees |
|---|---|
| Fine-grained / GitHub App token opens Notifications (GitHub 403) | "Notifications aren't available for token or QR sign-in…" and no retry button |
| Token can't read issues (403) | "This sign-in can't read these issues…" and no retry button |
| Other HTTP error | "Request failed: HTTP <code>" (never a bare "java.lang.Error") |
| 401 on an imported token | "Login expired, scan a new code", then the login screen |
| No network and no cache | The existing "no cache and network" tip with Tap to retry |

## 8. Settings and About

- Settings: theme, accent color, start page, cache first, system downloader, Custom Tabs,
  no images, language, font size (app-wide).
- About: version, source link, author, share, feedback. Must open on every theme.

## Non-goals

- No OpenHubX backend, sync service or analytics (Bugly was removed).
- No push notifications.
- No GitHub Enterprise.

## Backlog

Global Recommendation feed, Gist bookmark sync, Daily/Weekly/Monthly Trending,
remove the legacy side drawer, About screen following font size.
