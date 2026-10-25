package com.darktile.openhubx.auth.qr;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Decodes and strictly validates a scanned {@link QrLoginPayload}.
 *
 * <p>Rejects (via {@link QrLoginParseException}) on malformed base64url/JSON,
 * {@code v != 1}, {@code typ != "ghtok"}, missing/empty {@code tok}, or an
 * {@code exp} that is already in the past. A caller MUST still validate the
 * token against {@code GET /user} before storing it.
 *
 * <p>Plain Java only (no Android APIs) so it is directly unit-testable.
 */
public class QrLoginPayloadParser {

    private static final Gson GSON = new Gson();

    /** Parse using the current wall-clock for expiry checks. */
    public QrLoginPayload parse(String encoded) throws QrLoginParseException {
        return parse(encoded, System.currentTimeMillis() / 1000L);
    }

    /**
     * Parse and validate an envelope.
     *
     * @param encoded          base64url (padded or unpadded) of the envelope JSON
     * @param nowEpochSeconds  reference time used for the expiry check
     */
    public QrLoginPayload parse(String encoded, long nowEpochSeconds) throws QrLoginParseException {
        if (encoded == null || encoded.trim().isEmpty()) {
            throw new QrLoginParseException("empty payload");
        }

        byte[] json;
        try {
            json = Base64.getUrlDecoder().decode(encoded.trim());
        } catch (IllegalArgumentException e) {
            throw new QrLoginParseException("malformed base64url", e);
        }

        QrLoginPayload payload;
        try {
            payload = GSON.fromJson(new String(json, StandardCharsets.UTF_8), QrLoginPayload.class);
        } catch (JsonSyntaxException e) {
            throw new QrLoginParseException("malformed JSON", e);
        }
        if (payload == null) {
            throw new QrLoginParseException("empty JSON");
        }

        if (payload.getV() != QrLoginPayload.VERSION) {
            throw new QrLoginParseException("unsupported version: " + payload.getV());
        }
        if (!QrLoginPayload.TYPE.equals(payload.getTyp())) {
            throw new QrLoginParseException("unsupported type: " + payload.getTyp());
        }
        if (payload.getTok() == null || payload.getTok().isEmpty()) {
            throw new QrLoginParseException("missing token");
        }
        if (payload.getExp() != null && payload.getExp() <= nowEpochSeconds) {
            throw new QrLoginParseException("token expired");
        }

        return payload;
    }
}
