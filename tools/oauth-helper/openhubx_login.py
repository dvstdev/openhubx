#!/usr/bin/env python3
"""OpenHubX laptop login helper (Phase 5.1).

Signs in to GitHub with the OAuth device flow of YOUR GitHub App, then renders the
OpenHubX login QR (and the equivalent openhubx://login?d=... link) in a local HTML
file. Python 3 stdlib only. The only network calls go to https://github.com.

The token is never printed unless --show is given. The HTML file holds the token
(that is what the QR encodes); it is written 0600 to a private temp dir and deleted
when you press Enter, unless --keep is given.

Usage:
    OPENHUBX_CLIENT_ID=Iv23li... python3 openhubx_login.py
    python3 openhubx_login.py --client-id Iv23li... [--no-refresh] [--show] [--keep]
"""

import argparse
import base64
import json
import os
import re
import secrets
import sys
import tempfile
import time
import urllib.error
import urllib.parse
import urllib.request
import webbrowser

DEVICE_CODE_URL = "https://github.com/login/device/code"
TOKEN_URL = "https://github.com/login/oauth/access_token"
DEVICE_GRANT = "urn:ietf:params:oauth:grant-type:device_code"
LINK_PREFIX = "openhubx://login?d="

HERE = os.path.dirname(os.path.abspath(__file__))
QR_JS_SOURCE = os.path.join(HERE, "..", "qr-generator", "index.html")


class HelperError(Exception):
    """User-facing failure (never carries token material)."""


# ---------------------------------------------------------------- envelope

def build_envelope(token, now, expires_in=None, refresh_token=None,
                   refresh_expires_in=None, client_id=None, include_refresh=True,
                   nonce=None):
    """Build the locked v1 envelope dict for a GitHub App user token (tt="ghapp").

    exp/rexp are absolute unix seconds. rt/rexp/cid are only included when a refresh
    token exists and include_refresh is set, so the phone can refresh without a secret.
    """
    if not token:
        raise HelperError("no access token")
    env = {"v": 1, "typ": "ghtok", "tt": "ghapp", "tok": token,
           "exp": (now + int(expires_in)) if expires_in else None}
    env["nonce"] = nonce or secrets.token_hex(8)
    if include_refresh and refresh_token:
        env["rt"] = refresh_token
        if refresh_expires_in:
            env["rexp"] = now + int(refresh_expires_in)
        env["cid"] = client_id
    return env


def encode_envelope(env):
    """base64url (unpadded) of compact JSON, matching the phone's parser."""
    raw = json.dumps(env, separators=(",", ":")).encode("utf-8")
    return base64.urlsafe_b64encode(raw).rstrip(b"=").decode("ascii")


def decode_envelope(encoded):
    """Inverse of encode_envelope (accepts a raw envelope or an openhubx:// link)."""
    if encoded.startswith(LINK_PREFIX):
        encoded = urllib.parse.unquote(encoded[len(LINK_PREFIX):])
    pad = "=" * (-len(encoded) % 4)
    return json.loads(base64.urlsafe_b64decode(encoded + pad).decode("utf-8"))


# ---------------------------------------------------------------- device flow

def _post_form(url, fields, opener=None):
    """POST form fields to a github.com URL and return the parsed JSON body."""
    if not url.startswith("https://github.com/"):
        raise HelperError("refusing non-github.com URL")
    data = urllib.parse.urlencode(fields).encode("ascii")
    req = urllib.request.Request(url, data=data, method="POST",
                                 headers={"Accept": "application/json",
                                          "User-Agent": "openhubx-login-helper"})
    try:
        with (opener or urllib.request.urlopen)(req, timeout=30) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        raise HelperError("GitHub returned HTTP %d for %s" % (e.code, url))
    except urllib.error.URLError as e:
        raise HelperError("network error: %s" % e.reason)


def request_device_code(client_id, post=_post_form, offline_access=False):
    """Step 1: obtain device_code/user_code. Only the public client_id is sent."""
    fields = {"client_id": client_id}
    if offline_access:
        fields["scope"] = "offline_access"
    body = post(DEVICE_CODE_URL, fields)
    if "error" in body:
        raise HelperError(_describe_error(body))
    for key in ("device_code", "user_code", "verification_uri"):
        if key not in body:
            raise HelperError("unexpected device-code response (missing %s)" % key)
    return body


def poll_for_token(client_id, device, post=_post_form, sleep=time.sleep,
                   clock=time.time):
    """Step 3: poll the token endpoint at the server's interval until authorized.

    Honors authorization_pending, slow_down (new interval, else +5s) and the
    device code's expires_in deadline. Returns the token response dict.
    """
    interval = int(device.get("interval", 5))
    deadline = clock() + int(device.get("expires_in", 900))
    fields = {"client_id": client_id, "device_code": device["device_code"],
              "grant_type": DEVICE_GRANT}
    while True:
        sleep(interval)
        if clock() > deadline:
            raise HelperError("device code expired; run the helper again")
        body = post(TOKEN_URL, fields)
        err = body.get("error")
        if err is None:
            if not body.get("access_token"):
                raise HelperError("token response had no access_token")
            return body
        if err == "authorization_pending":
            continue
        if err == "slow_down":
            interval = int(body.get("interval", interval + 5))
            continue
        raise HelperError(_describe_error(body))


def _describe_error(body):
    """Map GitHub OAuth error codes to actionable text."""
    err = body.get("error", "unknown_error")
    hints = {
        "device_flow_disabled": "enable 'Device Flow' in the GitHub App settings",
        "incorrect_client_credentials": "check the client id (not the app id)",
        "expired_token": "device code expired; run the helper again",
        "access_denied": "authorization was cancelled on GitHub",
        "unverified_user_email": "verify your primary email on GitHub",
    }
    return "%s: %s" % (err, hints.get(err, body.get("error_description", "")))


# ---------------------------------------------------------------- rendering

def load_qr_js(path=QR_JS_SOURCE):
    """Extract the vendored qrcode-generator (MIT, Kazuhiko Arase) <script> block."""
    with open(path, encoding="utf-8") as f:
        html = f.read()
    m = re.search(r"<script>\s*(// ---- Vendored QR encoder.*?)</script>", html, re.S)
    if not m:
        raise HelperError("could not find vendored QR encoder in %s" % path)
    return m.group(1)


def render_html(encoded, env, qr_js):
    """Self-contained page (CSP: no network) that draws the QR client-side."""
    exp = env.get("exp")
    exp_txt = time.strftime("%Y-%m-%d %H:%M %Z", time.localtime(exp)) if exp else "never"
    refresh_txt = "yes (phone refreshes on its own)" if "rt" in env else "no (re-scan after expiry)"
    data = json.dumps(encoded)  # base64url + JSON string: no HTML/JS breakout possible
    return """<!DOCTYPE html>
<html lang="en"><head><meta charset="utf-8">
<meta http-equiv="Content-Security-Policy"
      content="default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; img-src data:; connect-src 'none'; base-uri 'none'; form-action 'none'">
<title>OpenHubX login QR</title>
<style>
 body{font-family:system-ui,sans-serif;background:#0f1117;color:#e6e6e6;text-align:center;padding:24px}
 #qr{background:#fff;padding:12px;border-radius:10px;display:inline-block}
 .meta{color:#9aa4b2;font-size:.9rem} .warn{color:#ffb86b;font-size:.85rem}
</style></head><body>
<h1>Scan with OpenHubX</h1>
<p class="meta">Sign in with QR &rarr; scan. Expires: %(exp)s &middot; Refresh: %(refresh)s</p>
<div id="qr"></div>
<p class="warn">This code is a live GitHub credential. Close this tab after scanning.</p>
<p class="meta">QR encoder: qrcode-generator by Kazuhiko Arase (MIT license).</p>
<script>
%(qrjs)s
</script>
<script>
(function(){
  var payload = %(data)s;
  var qr = qrcode(0, "M"); qr.addData(payload, "Byte"); qr.make();
  var n = qr.getModuleCount(), cell = 6, m = 4 * cell, size = n * cell + 2 * m;
  var c = document.createElement("canvas"); c.width = c.height = size;
  var x = c.getContext("2d"); x.fillStyle = "#fff"; x.fillRect(0, 0, size, size);
  x.fillStyle = "#000";
  for (var r = 0; r < n; r++) for (var k = 0; k < n; k++)
    if (qr.isDark(r, k)) x.fillRect(m + k * cell, m + r * cell, cell, cell);
  document.getElementById("qr").appendChild(c);
})();
</script></body></html>
""" % {"exp": exp_txt, "refresh": refresh_txt, "qrjs": qr_js, "data": data}


def write_private_html(html, keep_dir=None):
    """Write the page 0600 in a fresh 0700 temp dir (or keep_dir); return its path."""
    d = keep_dir or tempfile.mkdtemp(prefix="openhubx-login-")
    path = os.path.join(d, "openhubx-login.html")
    fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
    with os.fdopen(fd, "w", encoding="utf-8") as f:
        f.write(html)
    return path


# ---------------------------------------------------------------- main

def main(argv=None):
    p = argparse.ArgumentParser(description="Sign in to GitHub and render an OpenHubX login QR.")
    p.add_argument("--client-id", default=os.environ.get("OPENHUBX_CLIENT_ID"),
                   help="GitHub App client id (or env OPENHUBX_CLIENT_ID)")
    p.add_argument("--no-refresh", action="store_true",
                   help="leave the refresh token out of the QR (phone must re-scan after ~8h)")
    p.add_argument("--offline-access", action="store_true",
                   help="request scope=offline_access (forces an expiring token + refresh token)")
    p.add_argument("--show", action="store_true",
                   help="also print the openhubx:// link (contains the token)")
    p.add_argument("--keep", action="store_true", help="do not delete the HTML file")
    p.add_argument("--no-browser", action="store_true", help="do not open a browser")
    a = p.parse_args(argv)
    if not a.client_id:
        p.error("a client id is required (--client-id or OPENHUBX_CLIENT_ID)")

    try:
        qr_js = load_qr_js()
        device = request_device_code(a.client_id, offline_access=a.offline_access)
        print("Open %s and enter code:  %s" % (device["verification_uri"], device["user_code"]))
        if not a.no_browser:
            webbrowser.open(device["verification_uri"])
        print("Waiting for authorization (Ctrl-C to abort)...")
        tok = poll_for_token(a.client_id, device)
        env = build_envelope(tok["access_token"], int(time.time()),
                             tok.get("expires_in"), tok.get("refresh_token"),
                             tok.get("refresh_token_expires_in"), a.client_id,
                             include_refresh=not a.no_refresh)
    except HelperError as e:
        print("error: %s" % e, file=sys.stderr)
        return 1
    except KeyboardInterrupt:
        print("aborted", file=sys.stderr)
        return 130

    if env["exp"] is None:
        print("warning: GitHub issued a NON-expiring token; enable 'User-to-server token "
              "expiration' on the app or pass --offline-access", file=sys.stderr)
    encoded = encode_envelope(env)
    path = write_private_html(render_html(encoded, env, qr_js))
    print("QR written to %s" % path)
    if a.show:
        print(LINK_PREFIX + encoded)
    if not a.no_browser:
        webbrowser.open("file://" + path)
    if a.keep:
        print("--keep: delete the file yourself when done (it contains the token).")
        return 0
    try:
        input("Scan the QR, then press Enter to delete the file... ")
    except (EOFError, KeyboardInterrupt):
        pass
    finally:
        os.remove(path)
        os.rmdir(os.path.dirname(path))
        print("deleted")
    return 0


if __name__ == "__main__":
    sys.exit(main())
