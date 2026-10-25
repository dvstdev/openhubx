# Testing login on OpenHubX (developer guide)

OpenHubX has three ways to sign in. This guide shows how to test each one on a
dev build, and how to make a login QR **safely** from your laptop.

- **Browser** — GitHub OAuth in the browser. No setup.
- **Token** — paste a GitHub token into the app.
- **QR** — scan a login QR made on your laptop.

All three end the same way: the app calls `GET /user` with the token and only
signs you in if GitHub accepts it.

---

## 0. Build and install the dev APK

```bash
export ANDROID_HOME=~/android-sdk
export JAVA_HOME=~/jdks/amazon-corretto-25.0.4.8.1-linux-x64
cd ~/playground/openhubx
./gradlew assembleNormalDebug --no-daemon --no-watch-fs -Dorg.gradle.vfs.watch=false --offline
~/android-sdk/platform-tools/adb install -r app/build/outputs/apk/normal/debug/app-normal-debug.apk
```

---

## 1. Make a GitHub token (needed for Token and QR login)

Open the fine-grained token page:
[github.com/settings/personal-access-tokens](https://github.com/settings/personal-access-tokens)

1. **Generate new token**.
2. **Token name**: anything, e.g. `openhubx-dev`.
3. **Expiration**: pick a short one for testing, e.g. 7 days.
4. **Resource owner**: your own account.
5. **Repository access**: *All repositories*, or select a few test repos.
6. **Permissions** — grant what OpenHubX uses:
   - Repository: **Contents** read & write, **Issues** read & write,
     **Pull requests** read & write, **Metadata** read (required),
     **Commit statuses** read.
   - Account: **Starring**, **Followers**, **Gists**, **Watching**
     read & write; **Email addresses** read.
7. **Generate token** and copy it. A fine-grained token starts with
   `github_pat_`. You see it only once.

Classic tokens from [github.com/settings/tokens](https://github.com/settings/tokens)
also work. Both tokens are a password — do not paste them into chat, commits,
or logs.

> Note: GitHub's Notifications API does not accept fine-grained or GitHub App
> tokens, so the **Notifications tab stays empty** with Token or QR login. Use
> Browser login if you need notifications. This is a GitHub limit, not a bug.

---

## 2. Test Token login

1. Open OpenHubX. On the login screen tap **SIGN IN WITH TOKEN** (purple).
2. Paste the token from step 1 into the field (tap the eye to reveal/hide).
3. Tap the button. The app validates it and opens the home screen.
4. A bad or empty token shows "Token rejected by GitHub" and nothing is saved.

Expected: your repos, profile and avatar load.

---

## 3. Test QR login

You make the QR on your laptop, then scan it with the phone. Two generators:

### Option A — offline PAT QR (simplest, safest)

Uses the token from step 1. Fully offline: the page makes **no network calls**
(CSP `connect-src 'none'`), so the token never leaves your laptop.

```bash
cd ~/playground/openhubx
xdg-open tools/qr-generator/index.html    # or: open on macOS
```

1. Paste the fine-grained PAT. Optionally fill scopes/expiry (display only).
2. **Generate QR**.
3. In OpenHubX tap **SIGN IN WITH QR** (green), grant camera, scan it.
4. **Close the browser tab when done** so the QR is gone.

The encoded payload holds the raw token in base64 — treat the QR like a
password. Do not screenshot it or leave the tab open.

### Option B — GitHub App device flow (no manual token)

Signs you in with your own GitHub App and renders the QR for you. One-time app
setup is in [`tools/oauth-helper/README.md`](../tools/oauth-helper/README.md)
(enable **Device Flow**).

```bash
export OPENHUBX_CLIENT_ID=Iv23li...      # your app's public client id
python3 tools/oauth-helper/openhubx_login.py
```

1. The helper prints a device code and opens GitHub. Enter the code, approve.
2. A browser tab shows the QR with its expiry. Scan it in OpenHubX.
3. Press **Enter** in the terminal to delete the QR file.

Safety notes for Option B:
- The QR HTML file is written `0600` in a private temp dir and deleted when you
  press Enter (`--keep` keeps it).
- The token is never printed unless you pass `--show`.
- By default the QR also carries a 6-month **refresh token** so the phone renews
  itself. Anyone who copies the QR gets that too — pass `--no-refresh` if the QR
  could be photographed.
- Revoke anytime at GitHub → Settings → Applications → Authorized GitHub Apps.

### Option C — Tampermonkey button on the GitHub token page

Install `tools/userscript/openhubx-github-tokens.user.js` in Tampermonkey (see
[`tools/userscript/README.md`](../tools/userscript/README.md)). After you make a token,
a green **QR** button appears next to it on GitHub. Click it and scan the QR in the app.
To get a QR of any selected text, install `openhubx-qr-selection.user.js` and use
right-click > Tampermonkey (Chromium browsers only).

### Deep link (either option)

Instead of scanning in-app, the phone camera can open the
`openhubx://login?d=<payload>` link the QR encodes, which hands the login
straight to OpenHubX.

---

## 4. Test Browser login

1. Tap **LOGIN WITH BROWSER** (blue).
2. GitHub opens; sign in and approve OpenHubX.
3. The browser hands back to the app (`openhub://login` callback) and you are
   signed in.

This path has full access including the Notifications tab.

---

## 5. What to verify after any login

- Home screen loads your repos and profile.
- Avatar and name are correct.
- Sign out, then sign in again with a different method to confirm all three work.

## Safety checklist

- Use a **short expiry** token for testing; revoke it when done.
- Never commit or paste a token/QR into chat, issues, or logs.
- Prefer the **offline** PAT generator (Option A) on a shared or untrusted
  network.
- Close the generator tab and delete any saved QR image after testing.
