# OpenHubX QR userscripts (Tampermonkey)

Two offline userscripts that turn a GitHub token into an OpenHubX login QR.

| Script | Where it runs | What you do |
|---|---|---|
| `openhubx-github-tokens.user.js` | `github.com/settings/*` | Click the green **QR** button next to a token |
| `openhubx-qr-selection.user.js` | any page | Select text, right-click > **Tampermonkey** > *OpenHubX: QR from selected text* |

The QR dialog has two modes:

- **App login**: a GitHub token is wrapped in the OpenHubX envelope
  `{v:1, typ:"ghtok", tt, tok, nonce}`. Scan it with **Scan QR** in the app.
- **Plain text**: a QR of the text itself. Any other selection uses this mode.

Close it with **Close**, **Esc**, or a click outside. **Save PNG** downloads the image.

## Install

1. Install the [Tampermonkey](https://www.tampermonkey.net/) extension.
2. Open the Tampermonkey dashboard, then **Utilities > Import from file**. Pick a
   `.user.js` from this folder. You can also drag the file into the dashboard.
3. Make a token on [github.com/settings/personal-access-tokens](https://github.com/settings/personal-access-tokens).
   Right after GitHub shows it, a **QR** button appears next to it.

The right-click menu needs a Chromium browser (Chrome, Edge, Brave). Tampermonkey
on Firefox has no `context-menu` run mode. On Firefox, use the GitHub script's own
menu instead: Tampermonkey icon > **QR code for selected text**. That menu only works
on GitHub settings pages.

## Safety

- No network calls and no `@require`. The QR encoder is copied into the file, so no
  remote code runs on your token page.
- The token is never stored or logged. Closing the dialog removes the QR from the page.
- The QR holds your token. Don't screenshot it or share it. A saved PNG holds the token
  too, so delete it after use.
- GitHub shows a new token only once. Make a short-expiry token for testing.

## Edit and rebuild

The `.user.js` files are generated. Edit `qr-core.js` or `*.entry.js`, then run:

```sh
python3 tools/userscript/build.py
```

The build copies the QR encoder (qrcode-generator 1.4.4, MIT, Kazuhiko Arase) from
`tools/qr-generator/index.html`, so there is one vendored copy. Bump `VERSION` in
`build.py` when you change behavior, so Tampermonkey picks up the update.
