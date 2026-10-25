# OpenHubX laptop login helper (Phase 5.1)

Signs you in to GitHub on your laptop with the OAuth **device flow** of your own
GitHub App, then shows an OpenHubX login QR. Scan it from the app (*Sign in with QR*)
or with the system camera (it opens `openhubx://login?d=...`).

- Python 3 stdlib only, no `pip install`.
- Network: only `https://github.com/login/device/code` and
  `https://github.com/login/oauth/access_token`. No client secret is used or needed.
- The QR is drawn in a local HTML page (CSP `connect-src 'none'`) with the vendored
  `qrcode-generator` (Kazuhiko Arase, MIT) read from `../qr-generator/index.html`.
- The token is never printed unless you pass `--show`. The HTML file contains it (that
  is what the QR encodes); it is written `0600` in a private temp dir and deleted when
  you press Enter (unless `--keep`).

## One-time GitHub App setup (you do this once)

1. Register an app: GitHub → Settings → Developer settings → GitHub Apps →
   **New GitHub App** ([docs](https://docs.github.com/en/apps/creating-github-apps/registering-a-github-app/registering-a-github-app)).
   - Name: anything, e.g. `OpenHubX (yourname)`. Homepage URL: any URL.
   - Callback URL: not needed for device flow (leave empty if allowed).
   - **Webhook: uncheck "Active".**
   - **Enable Device Flow: check it.** Without it the helper fails with
     `device_flow_disabled`.
   - Where can it be installed: *Only on this account*.
2. Permissions (a user token can only do what BOTH the app and you can do). For the
   features OpenHubX uses, grant:
   - Repository: **Contents** read & write (browse code, README, releases),
     **Issues** read & write, **Pull requests** read & write,
     **Metadata** read (mandatory), **Commit statuses** read (optional).
   - Account: **Starring** read & write, **Followers** read & write,
     **Gists** read & write, **Watching** read & write,
     **Email addresses** read, **Events** read (optional).
3. Optional features → **User-to-server token expiration: Opt-in** (default for new
   apps). This gives 8h tokens + 6-month refresh tokens. If it is off, pass
   `--offline-access` to still get an expiring token.
4. Install the app on your account (app page → *Install App*) and choose the
   repositories it may access (all, or a selection). Private repos outside the
   installation are not visible to the token.
5. Copy the **Client ID** (starts with `Iv`; it is not the App ID). It is public,
   not a secret. Do **not** generate or paste a client secret anywhere.

Known limitation: GitHub's notifications API does not accept GitHub App user tokens
([docs](https://docs.github.com/en/rest/activity/notifications)), so the
Notifications tab will not load with this login (same as with a fine-grained PAT).
Use the OAuth browser login if you need notifications.

## Use

```sh
export OPENHUBX_CLIENT_ID=Iv23li...        # or pass --client-id
python3 tools/oauth-helper/openhubx_login.py
```

1. The helper prints `https://github.com/login/device` and an 8-character code, and
   opens the page. Enter the code and approve.
2. A browser tab shows the QR with its expiry. Scan it in OpenHubX.
3. Press Enter in the terminal to delete the HTML file.

Options:

| flag | effect |
|---|---|
| `--no-refresh` | leave the refresh token out of the QR; the phone must re-scan after ~8h. Safer if the QR could be photographed. |
| `--offline-access` | request `scope=offline_access` (forces an expiring token + refresh token). |
| `--show` | also print the `openhubx://login?d=...` link (contains the token). |
| `--keep` | do not delete the HTML file. |
| `--no-browser` | do not open anything; open the printed file path yourself. |

By default the QR carries the refresh token (`rt`, valid 6 months) so the phone
renews its 8h token on its own using only the public client id. Anyone who copies the
QR gets that refresh token too: treat the QR as a password, or use `--no-refresh`.
Revoke at GitHub → Settings → Applications → Authorized GitHub Apps.

## Envelope

base64url (unpadded) of compact JSON, `v` stays 1:

```
{"v":1,"typ":"ghtok","tt":"ghapp","tok":"ghu_...","exp":<unix>,"nonce":"<hex>",
 "rt":"ghr_...","rexp":<unix>,"cid":"Iv..."}
```

`rt`/`rexp`/`cid` are omitted with `--no-refresh`. Older `tt:"pat"` envelopes from
`tools/qr-generator` still work.

## Tests (offline, fake tokens)

```sh
cd tools/oauth-helper && python3 -B -m unittest -v test_openhubx_login
```
