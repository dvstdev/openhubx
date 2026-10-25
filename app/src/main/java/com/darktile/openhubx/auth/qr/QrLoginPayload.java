package com.darktile.openhubx.auth.qr;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Envelope carried inside a login QR code.
 *
 * <p>Wire form is base64url (no padding) of the compact JSON:
 * {@code {"v":1,"typ":"ghtok","tt":"pat","tok":"<token>","exp":<unix|null>,"scope":"<csv|optional>","nonce":"<rand>"}}
 *
 * <p>Plain Java only (no Android APIs) so it is directly unit-testable.
 */
public class QrLoginPayload {

    /** Only supported envelope version. */
    public static final int VERSION = 1;
    /** Only supported payload type. */
    public static final String TYPE = "ghtok";

    private static final Gson GSON = new Gson();

    @SerializedName("v") private int v;
    @SerializedName("typ") private String typ;
    @SerializedName("tt") private String tt;
    @SerializedName("tok") private String tok;
    @SerializedName("exp") private Long exp;
    @SerializedName("scope") private String scope;
    @SerializedName("nonce") private String nonce;

    /** No-arg constructor for Gson. */
    public QrLoginPayload() {
    }

    public QrLoginPayload(String tt, String tok, Long exp, String scope, String nonce) {
        this.v = VERSION;
        this.typ = TYPE;
        this.tt = tt;
        this.tok = tok;
        this.exp = exp;
        this.scope = scope;
        this.nonce = nonce;
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
}
