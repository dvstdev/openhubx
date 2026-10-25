package com.darktile.openhubx.auth.qr;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Envelope carried inside a login QR code or an {@code openhubx://login?d=} link.
 *
 * <p>Wire form is base64url (no padding) of the compact JSON:
 * {@code {"v":1,"typ":"ghtok","tt":"pat|oauth|ghapp","tok":"<token>","exp":<unix|null>,
 * "scope":"<csv|optional>","nonce":"<rand>"}}
 *
 * <p>{@code tt:"ghapp"} (GitHub App user token) may also carry the optional fields
 * {@code rt} (refresh token), {@code rexp} (refresh-token expiry, unix seconds) and
 * {@code cid} (the app's public client id, needed to refresh). Readers that do not
 * know them ignore them, so {@code v} stays 1.
 *
 * <p>Plain Java only (no Android APIs) so it is directly unit-testable.
 */
public class QrLoginPayload {

    /** Only supported envelope version. */
    public static final int VERSION = 1;
    /** Only supported payload type. */
    public static final String TYPE = "ghtok";

    /** Token type: personal access token. */
    public static final String TT_PAT = "pat";
    /** Token type: OAuth app token. */
    public static final String TT_OAUTH = "oauth";
    /** Token type: GitHub App user-to-server token (short-lived, optionally refreshable). */
    public static final String TT_GHAPP = "ghapp";

    private static final Gson GSON = new Gson();

    @SerializedName("v") private int v;
    @SerializedName("typ") private String typ;
    @SerializedName("tt") private String tt;
    @SerializedName("tok") private String tok;
    @SerializedName("exp") private Long exp;
    @SerializedName("scope") private String scope;
    @SerializedName("nonce") private String nonce;
    @SerializedName("rt") private String rt;
    @SerializedName("rexp") private Long rexp;
    @SerializedName("cid") private String cid;

    /** No-arg constructor for Gson. */
    public QrLoginPayload() {
    }

    public QrLoginPayload(String tt, String tok, Long exp, String scope, String nonce) {
        this(tt, tok, exp, scope, nonce, null, null, null);
    }

    public QrLoginPayload(String tt, String tok, Long exp, String scope, String nonce,
                          String rt, Long rexp, String cid) {
        this.v = VERSION;
        this.typ = TYPE;
        this.tt = tt;
        this.tok = tok;
        this.exp = exp;
        this.scope = scope;
        this.nonce = nonce;
        this.rt = rt;
        this.rexp = rexp;
        this.cid = cid;
    }

    /** Serialize to the base64url (unpadded) wire form. */
    public String serialize() {
        byte[] json = GSON.toJson(this).getBytes(StandardCharsets.UTF_8);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
    }

    public int getV() {
        return v;
    }

    public String getTyp() {
        return typ;
    }

    public String getTt() {
        return tt;
    }

    public String getTok() {
        return tok;
    }

    /** Expiry as unix epoch seconds, or {@code null} when the token never expires. */
    public Long getExp() {
        return exp;
    }

    public String getScope() {
        return scope;
    }

    public String getNonce() {
        return nonce;
    }

    /** Refresh token ({@code ghapp} only), or {@code null}. */
    public String getRt() {
        return rt;
    }

    /** Refresh-token expiry as unix epoch seconds, or {@code null} when unknown. */
    public Long getRexp() {
        return rexp;
    }

    /** GitHub App client id (public, not a secret), or {@code null}. */
    public String getCid() {
        return cid;
    }

    /**
     * True when the phone can refresh this token itself: a {@code ghapp} token with a
     * refresh token, a client id, and a refresh token not yet expired at {@code nowEpochSeconds}.
     */
    public boolean isRefreshable(long nowEpochSeconds) {
        return TT_GHAPP.equals(tt)
                && rt != null && !rt.isEmpty()
                && cid != null && !cid.isEmpty()
                && (rexp == null || rexp > nowEpochSeconds);
    }
}
