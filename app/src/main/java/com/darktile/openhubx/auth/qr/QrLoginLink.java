package com.darktile.openhubx.auth.qr;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Locale;

/**
 * Normalizes scanned or deep-linked login input to the bare base64url envelope.
 *
 * <p>Accepts either the raw envelope or an {@code openhubx://login?d=<envelope>} link,
 * so the in-app scanner and the system-camera deep link share one import path.
 *
 * <p>Plain Java only (no Android APIs) so it is directly unit-testable.
 */
public final class QrLoginLink {

    /** Deep-link scheme. */
    public static final String SCHEME = "openhubx";
    /** Deep-link host. */
    public static final String HOST = "login";
    /** Query parameter holding the envelope. */
    public static final String PARAM = "d";

    private static final String PREFIX = SCHEME + "://" + HOST;

    private QrLoginLink() {
    }

    /** True if {@code input} looks like an {@code openhubx://login} link (case-insensitive scheme/host). */
    public static boolean isLoginLink(String input) {
        return input != null
                && input.trim().toLowerCase(Locale.ROOT).startsWith(PREFIX);
    }

    /**
     * Returns the envelope carried by {@code input}: the {@code d} parameter for a login
     * link, otherwise {@code input} unchanged.
     *
     * @throws QrLoginParseException for a login link with no/empty {@code d} parameter,
     *                               or a link to a different path
     */
    public static String extractEnvelope(String input) throws QrLoginParseException {
        if (!isLoginLink(input)) {
            return input;
        }
        String s = input.trim();
        String rest = s.substring(PREFIX.length());
        // Allow "openhubx://login?d=..." and "openhubx://login/?d=...".
        if (rest.startsWith("/")) {
            rest = rest.substring(1);
        }
        if (!rest.startsWith("?")) {
            throw new QrLoginParseException("missing login link data");
        }
        String query = rest.substring(1);
        int hash = query.indexOf('#');
        if (hash >= 0) {
            query = query.substring(0, hash);
        }
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            String key = eq >= 0 ? pair.substring(0, eq) : pair;
            if (PARAM.equals(key)) {
                String value = eq >= 0 ? pair.substring(eq + 1) : "";
                // base64url never contains '+', so decoding '+' to space is harmless.
                try {
                    value = URLDecoder.decode(value, "UTF-8");
                } catch (UnsupportedEncodingException | IllegalArgumentException e) {
                    throw new QrLoginParseException("malformed login link data", e);
                }
                if (value.isEmpty()) {
                    throw new QrLoginParseException("missing login link data");
                }
                return value;
            }
        }
        throw new QrLoginParseException("missing login link data");
    }
}
