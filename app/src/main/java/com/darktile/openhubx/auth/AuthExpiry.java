package com.darktile.openhubx.auth;

/**
 * Expiry math for stored accounts ({@code AuthUser.authTime + expireIn}).
 *
 * <p>Plain Java only so it is directly unit-testable. Uses long arithmetic: the
 * previous {@code expireIn * 1000} int multiply overflowed for the 360-day default.
 */
public final class AuthExpiry {

    /** Lifetime given to tokens that carry no expiry (OAuth app / non-expiring PAT). */
    public static final int DEFAULT_EXPIRE_IN_SECONDS = 360 * 24 * 60 * 60;

    private AuthExpiry() {
    }

    /** Absolute expiry in epoch millis. */
    public static long expiresAtMillis(long authTimeMillis, int expireInSeconds) {
        return authTimeMillis + expireInSeconds * 1000L;
    }

    /** True once {@code nowMillis} has passed the stored expiry. */
    public static boolean isExpired(long authTimeMillis, int expireInSeconds, long nowMillis) {
        return expiresAtMillis(authTimeMillis, expireInSeconds) < nowMillis;
    }

    /** True when the token expires within {@code skewMillis} of {@code nowMillis} (or already has). */
    public static boolean expiresWithin(long authTimeMillis, int expireInSeconds,
                                        long nowMillis, long skewMillis) {
        return expiresAtMillis(authTimeMillis, expireInSeconds) - skewMillis <= nowMillis;
    }

    /**
     * {@code expireIn} (seconds from {@code nowMillis}) for a token with absolute expiry
     * {@code expEpochSeconds}; the default lifetime when {@code null}. Clamped to [1, Integer.MAX_VALUE].
     */
    public static int expireInFor(Long expEpochSeconds, long nowMillis) {
        if (expEpochSeconds == null) {
            return DEFAULT_EXPIRE_IN_SECONDS;
        }
        long seconds = expEpochSeconds - nowMillis / 1000L;
        if (seconds < 1) {
            return 1;
        }
        return seconds > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) seconds;
    }
}
