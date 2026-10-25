// ---- OpenHubX QR core (shared by both userscripts) ----
// Builds the OpenHubX login envelope and shows a QR in a page overlay.
// Fully offline: no network calls, the token is never stored or logged.

var OHX_TOKEN_RE = /(github_pat_[A-Za-z0-9_]{22,}|gh[pousr]_[A-Za-z0-9]{36,})/g;

/** Maps a GitHub token prefix to the envelope token type the phone stores. */
function ohxTokenType(tok) {
  if (tok.indexOf("gho_") === 0) return "oauth";
  if (tok.indexOf("ghu_") === 0) return "ghapp";
  return "pat";
}

/** True when the whole text is one GitHub token, so a login QR makes sense. */
function ohxIsToken(text) {
  OHX_TOKEN_RE.lastIndex = 0;
  var m = OHX_TOKEN_RE.exec(text);
  return !!m && m[0] === text;
}

/** UTF-8 safe base64url, unpadded; matches the phone's Base64 URL decoder. */
function ohxBase64Url(str) {
  var b64 = btoa(unescape(encodeURIComponent(str)));
  return b64.replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

/** Random hex nonce so two QRs for the same token differ. */
function ohxNonce() {
  var a = new Uint8Array(8);
  crypto.getRandomValues(a);
  var s = "";
  for (var i = 0; i < a.length; i++) s += ("0" + a[i].toString(16)).slice(-2);
  return s;
}

/** The locked v1 envelope the app's QR parser accepts (exp omitted: unknown here). */
function ohxLoginPayload(tok) {
  var env = { v: 1, typ: "ghtok", tt: ohxTokenType(tok), tok: tok, nonce: ohxNonce() };
  return ohxBase64Url(JSON.stringify(env));
}

/** Draws text as a QR on a canvas. Throws when the text is too long for a QR. */
function ohxDrawQr(text) {
  var qr;
  try {
    qr = qrcode(0, "M");
    qr.addData(text, "Byte");
    qr.make();
  } catch (e) {
    qr = qrcode(0, "L");            // lower error correction fits more data
    qr.addData(text, "Byte");
    qr.make();
  }
  var count = qr.getModuleCount();
  var cell = Math.max(3, Math.floor(300 / count));
  var margin = 4 * cell;            // 4-module quiet zone
  var size = count * cell + margin * 2;
  var canvas = document.createElement("canvas");
  canvas.width = size;
  canvas.height = size;
  var ctx = canvas.getContext("2d");
  ctx.fillStyle = "#ffffff";
  ctx.fillRect(0, 0, size, size);
  ctx.fillStyle = "#000000";
  for (var r = 0; r < count; r++) {
    for (var c = 0; c < count; c++) {
      if (qr.isDark(r, c)) ctx.fillRect(margin + c * cell, margin + r * cell, cell, cell);
    }
  }
  return canvas;
}

var OHX_CSS =
  ".bg{position:fixed;inset:0;background:rgba(0,0,0,.55);display:flex;align-items:center;" +
  "justify-content:center;z-index:2147483647;font:14px -apple-system,Segoe UI,Roboto,sans-serif}" +
  ".pn{background:#fff;color:#1f2328;border-radius:12px;padding:18px 20px;max-width:380px;" +
  "box-shadow:0 10px 30px rgba(0,0,0,.35);text-align:center}" +
  ".tt{font-weight:700;font-size:16px;margin-bottom:4px}" +
  ".pv{font-family:ui-monospace,Menlo,monospace;font-size:12px;color:#59636e;margin-bottom:10px;word-break:break-all}" +
  ".md{display:flex;gap:6px;justify-content:center;margin-bottom:10px}" +
  ".qr canvas{display:block;margin:0 auto;max-width:320px;height:auto;image-rendering:pixelated}" +
  ".wn{font-size:12px;color:#9a6700;margin-top:10px}" +
  ".er{color:#d1242f;margin:16px 0}" +
  ".bt{display:flex;gap:8px;justify-content:center;margin-top:12px}" +
  "button{font:inherit;padding:6px 14px;border-radius:6px;border:1px solid #d1d9e0;background:#f6f8fa;" +
  "color:#1f2328;cursor:pointer}" +
  "button.on{background:#1f883d;border-color:#1f883d;color:#fff}";

/** Shows the QR overlay for text. A token gets an app-login QR, with a plain-text toggle. */
function ohxShowQr(text) {
  text = (text || "").trim();
  if (!text) { alert("Select some text first."); return; }
  var isTok = ohxIsToken(text);

  var host = document.createElement("div");
  var root = host.attachShadow({ mode: "closed" });   // page CSS and scripts can't reach in
  root.innerHTML =
    "<style>" + OHX_CSS + "</style>" +
    "<div class='bg'><div class='pn' role='dialog' aria-label='QR code'>" +
    "<div class='tt'></div><div class='pv'></div><div class='md'></div>" +
    "<div class='qr'></div><div class='wn'></div>" +
    "<div class='bt'><button class='sv'>Save PNG</button><button class='cl'>Close</button></div>" +
    "</div></div>";
  var $ = function (s) { return root.querySelector(s); };
  $(".pv").textContent = text.length > 16 ? text.slice(0, 12) + "…" + text.slice(-4) : text;

  var current = null;
  function render(asLogin) {
    $(".tt").textContent = asLogin ? "OpenHubX login QR" : "QR code";
    $(".wn").textContent = isTok
      ? "Anyone who sees this QR gets your token. Close it when done."
      : "";
    $(".qr").textContent = "";
    try {
      current = ohxDrawQr(asLogin ? ohxLoginPayload(text) : text);
      $(".qr").appendChild(current);
      $(".sv").disabled = false;
    } catch (e) {
      current = null;
      $(".qr").innerHTML = "<div class='er'>Too long for a QR code (about 2,300 bytes max).</div>";
      $(".sv").disabled = true;
    }
    if (isTok) {
      $(".md").innerHTML = "<button class='lg'>App login</button><button class='pt'>Plain text</button>";
      $(".lg").className = "lg" + (asLogin ? " on" : "");
      $(".pt").className = "pt" + (asLogin ? "" : " on");
      $(".lg").onclick = function () { render(true); };
      $(".pt").onclick = function () { render(false); };
    }
  }

  function close() {
    document.removeEventListener("keydown", onKey, true);
    host.remove();                     // drops the canvas with the token in it
  }
  function onKey(e) { if (e.key === "Escape") close(); }

  $(".cl").onclick = close;
  $(".bg").onclick = function (e) { if (e.target === $(".bg")) close(); };
  $(".sv").onclick = function () {
    if (!current) return;
    var a = document.createElement("a");
    a.href = current.toDataURL("image/png");
    a.download = isTok ? "openhubx-login-qr.png" : "qr.png";
    a.click();
  };
  document.addEventListener("keydown", onKey, true);
  document.documentElement.appendChild(host);
  render(isTok);
}

/** Returns the selected text, including a selection inside an input or textarea. */
function ohxSelectedText() {
  var el = document.activeElement;
  if (el && (el.tagName === "INPUT" || el.tagName === "TEXTAREA")
      && typeof el.selectionStart === "number" && el.selectionEnd > el.selectionStart) {
    return el.value.slice(el.selectionStart, el.selectionEnd);
  }
  return String(window.getSelection() || "");
}
