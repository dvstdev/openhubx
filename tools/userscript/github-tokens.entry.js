// ---- Entry: GitHub token pages ----
// Finds every GitHub token shown on the page and puts a "QR" button next to it.
// Also adds a Tampermonkey menu item for a QR of the selected text.

var OHX_DONE = new WeakSet();

/** Makes the small QR button placed next to a token. */
function ohxButton(tok) {
  var b = document.createElement("button");
  b.type = "button";
  b.textContent = "QR";
  b.title = "Show an OpenHubX login QR for this token";
  b.setAttribute("data-ohx-qr", "1");
  b.style.cssText =
    "margin-left:8px;padding:2px 10px;font:600 12px -apple-system,Segoe UI,sans-serif;" +
    "border-radius:6px;border:1px solid #1f883d;background:#1f883d;color:#fff;cursor:pointer;" +
    "vertical-align:middle;line-height:20px";
  b.addEventListener("click", function (e) {
    e.preventDefault();
    e.stopPropagation();
    ohxShowQr(tok);
  });
  return b;
}

/** Adds one button after el for each distinct token in text, once per element. */
function ohxAttach(el, text) {
  if (!el || OHX_DONE.has(el) || !text) return;
  OHX_TOKEN_RE.lastIndex = 0;
  var seen = {}, m, anchor = el;
  while ((m = OHX_TOKEN_RE.exec(text)) !== null) {
    if (seen[m[0]]) continue;
    seen[m[0]] = true;
    var b = ohxButton(m[0]);
    anchor.insertAdjacentElement("afterend", b);
    anchor = b;
  }
  if (anchor !== el) OHX_DONE.add(el);
}

/** Scans text, inputs and copy buttons for tokens. Skips our own buttons and overlays. */
function ohxScan() {
  var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, {
    acceptNode: function (n) {
      var p = n.parentElement;
      if (!p || p.closest("script,style,textarea,[data-ohx-qr]")) return NodeFilter.FILTER_REJECT;
      return /gh[pousr]_|github_pat_/.test(n.nodeValue)
        ? NodeFilter.FILTER_ACCEPT : NodeFilter.FILTER_REJECT;
    }
  });
  var n;
  while ((n = walker.nextNode())) ohxAttach(n.parentElement, n.nodeValue);
  document.querySelectorAll("input, textarea").forEach(function (el) {
    ohxAttach(el, el.value);
  });
  document.querySelectorAll("[value]").forEach(function (el) {
    if (el.tagName !== "INPUT") ohxAttach(el, el.getAttribute("value"));
  });
}

var ohxTimer = null;
function ohxScheduleScan() {
  clearTimeout(ohxTimer);
  ohxTimer = setTimeout(ohxScan, 300);
}

ohxScan();
new MutationObserver(ohxScheduleScan).observe(document.body, {
  childList: true, subtree: true, characterData: true
});

if (typeof GM_registerMenuCommand === "function") {
  GM_registerMenuCommand("QR code for selected text", function () {
    ohxShowQr(ohxSelectedText());
  });
}
