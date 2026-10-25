package com.darktile.openhubx.auth;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;

/**
 * Pure parsing of GitHub's {@code POST /login/oauth/access_token} refresh response
 * ({@code grant_type=refresh_token}) for GitHub App user tokens.
 *
 * <p>Device-flow tokens refresh with {@code client_id} + {@code refresh_token} only; no
 * client secret is involved. Plain Java only so it is directly unit-testable.
 */
public final class GhAppRefreshResponse {

    private static final Gson GSON = new Gson();

    @SerializedName("access_token") private String accessToken;
    @SerializedName("expires_in") private Long expiresIn;
    @SerializedName("refresh_token") private String refreshToken;
    @SerializedName("refresh_token_expires_in") private Long refreshTokenExpiresIn;
    @SerializedName("error") private String error;

    /** Result of a successful refresh, with absolute (epoch-second) expiries. */
    public static final class Result {
        public final String accessToken;
        /** Access-token expiry, or {@code null} if GitHub returned a non-expiring token. */
        public final Long expEpochSeconds;
        /** New refresh token; the old one is invalid after use. May be {@code null}. */
        public final String refreshToken;
        public final Long refreshExpEpochSeconds;

        Result(String accessToken, Long expEpochSeconds,
               String refreshToken, Long refreshExpEpochSeconds) {
            this.accessToken = accessToken;
            this.expEpochSeconds = expEpochSeconds;
            this.refreshToken = refreshToken;
            this.refreshExpEpochSeconds = refreshExpEpochSeconds;
        }
    }

    /** Thrown when the response carries an OAuth error or no access token. */
    public static final class RefreshException extends Exception {
        public RefreshException(String message) {
            super(message);
        }
    }

    /**
     * Parse a JSON refresh response.
     *
     * @param json            response body
     * @param nowEpochSeconds reference time for converting relative expiries
     * @throws RefreshException on malformed JSON, an {@code error} field (e.g.
     *                          {@code bad_refresh_token}) or a missing access token
     */
    public static Result parse(String json, long nowEpochSeconds) throws RefreshException {
        GhAppRefreshResponse r;
        try {
            r = GSON.fromJson(json, GhAppRefreshResponse.class);
        } catch (JsonSyntaxException e) {
            throw new RefreshException("malformed refresh response");
        }
        if (r == null) {
            throw new RefreshException("empty refresh response");
        }
        if (r.error != null) {
            // Error codes are fixed strings; safe to surface (no token material).
            throw new RefreshException(r.error);
        }
        if (r.accessToken == null || r.accessToken.isEmpty()) {
            throw new RefreshException("missing access_token");
        }
        Long exp = r.expiresIn == null ? null : nowEpochSeconds + r.expiresIn;
        Long rexp = r.refreshTokenExpiresIn == null ? null
                : nowEpochSeconds + r.refreshTokenExpiresIn;
        String rt = r.refreshToken == null || r.refreshToken.isEmpty() ? null : r.refreshToken;
        return new Result(r.accessToken, exp, rt, rexp);
    }
}
