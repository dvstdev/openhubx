package com.darktile.openhubx.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.darktile.openhubx.dao.AuthUser;

import org.junit.Test;

import java.util.Date;

/** Unit tests for {@link AuthExpiry}, {@link GhAppRefreshResponse} and refresh application. */
public class GhAppTokenTest {

    private static final long NOW_S = 1_700_000_000L;
    private static final long NOW_MS = NOW_S * 1000L;

    @Test
    public void defaultLifetimeDoesNotOverflow() {
        // 360 days * 1000 overflowed int arithmetic and expired logins after ~12 days.
        long authTime = NOW_MS;
        assertFalse(AuthExpiry.isExpired(authTime, AuthExpiry.DEFAULT_EXPIRE_IN_SECONDS,
                NOW_MS + 100L * 24 * 3600 * 1000));
        assertTrue(AuthExpiry.isExpired(authTime, AuthExpiry.DEFAULT_EXPIRE_IN_SECONDS,
                NOW_MS + 361L * 24 * 3600 * 1000));
    }

    @Test
    public void expireInForAbsoluteExpiry() {
        assertEquals(28800, AuthExpiry.expireInFor(NOW_S + 28800, NOW_MS));
        assertEquals(AuthExpiry.DEFAULT_EXPIRE_IN_SECONDS, AuthExpiry.expireInFor(null, NOW_MS));
        assertEquals(1, AuthExpiry.expireInFor(NOW_S - 50, NOW_MS));
        assertEquals(Integer.MAX_VALUE, AuthExpiry.expireInFor(Long.MAX_VALUE / 2, NOW_MS));
    }

    @Test
    public void expiresWithinSkew() {
        assertTrue(AuthExpiry.expiresWithin(NOW_MS, 60, NOW_MS, 5 * 60 * 1000L));
        assertFalse(AuthExpiry.expiresWithin(NOW_MS, 28800, NOW_MS, 5 * 60 * 1000L));
        assertTrue(AuthExpiry.expiresWithin(NOW_MS, 28800, NOW_MS + 28800_000L, 0));
    }

    @Test
    public void parsesRefreshResponse() throws Exception {
        String json = "{\"access_token\":\"ghu_new\",\"expires_in\":28800,"
                + "\"refresh_token\":\"ghr_new\",\"refresh_token_expires_in\":15897600,"
                + "\"scope\":\"\",\"token_type\":\"bearer\"}";

        GhAppRefreshResponse.Result r = GhAppRefreshResponse.parse(json, NOW_S);

        assertEquals("ghu_new", r.accessToken);
        assertEquals(Long.valueOf(NOW_S + 28800), r.expEpochSeconds);
        assertEquals("ghr_new", r.refreshToken);
        assertEquals(Long.valueOf(NOW_S + 15897600), r.refreshExpEpochSeconds);
    }

    @Test
    public void nonExpiringRefreshResponse() throws Exception {
        GhAppRefreshResponse.Result r =
                GhAppRefreshResponse.parse("{\"access_token\":\"ghu_x\"}", NOW_S);
        assertNull(r.expEpochSeconds);
        assertNull(r.refreshToken);
    }

    @Test
    public void refreshErrorsRejected() {
        assertRefreshRejected("{\"error\":\"bad_refresh_token\",\"error_description\":\"x\"}",
                "bad_refresh_token");
        assertRefreshRejected("{\"token_type\":\"bearer\"}", "missing access_token");
        assertRefreshRejected("{not json", "malformed refresh response");
        assertRefreshRejected("null", "empty refresh response");
    }

    @Test
    public void applyRefreshRotatesTokensAndKeepsIdentity() {
        AuthUser old = new AuthUser();
        old.setAccessToken("ghu_old");
        old.setAuthTime(new Date(NOW_MS - 28000_000L));
        old.setExpireIn(28800);
        old.setScope("");
        old.setSelected(true);
        old.setLoginId("octocat");
        old.setName("Octo");
        old.setAvatar("a");
        old.setTokenType("ghapp");
        old.setRefreshToken("ghr_old");
        old.setRefreshExpiresAt(NOW_S + 1000);
        old.setClientId("Iv1.c");
        assertTrue(old.isRefreshable(NOW_MS));

        GhAppRefreshResponse.Result r = new GhAppRefreshResponse.Result(
                "ghu_new", NOW_S + 28800, "ghr_new", NOW_S + 15897600);
        AuthUser u = GhAppTokenRefresher.applyRefresh(old, r, NOW_MS);

        assertEquals("ghu_new", u.getAccessToken());
        assertEquals("ghr_new", u.getRefreshToken());
        assertEquals(Long.valueOf(NOW_S + 15897600), u.getRefreshExpiresAt());
        assertEquals(28800, u.getExpireIn());
        assertEquals(NOW_MS, u.getAuthTime().getTime());
        assertEquals("octocat", u.getLoginId());
        assertEquals("ghapp", u.getTokenType());
        assertEquals("Iv1.c", u.getClientId());
        assertTrue(u.getSelected());
    }

    @Test
    public void authUserRefreshableRules() {
        AuthUser u = new AuthUser();
        u.setTokenType("ghapp");
        u.setRefreshToken("ghr");
        u.setClientId("Iv1.c");
        assertTrue(u.isRefreshable(NOW_MS));
        u.setRefreshExpiresAt(NOW_S);
        assertFalse(u.isRefreshable(NOW_MS));
        u.setRefreshExpiresAt(null);
        u.setTokenType("pat");
        assertFalse(u.isRefreshable(NOW_MS));
        u.setTokenType(null);
        assertFalse(u.isRefreshable(NOW_MS));
    }

    private static void assertRefreshRejected(String json, String expected) {
        try {
            GhAppRefreshResponse.parse(json, NOW_S);
            fail("expected RefreshException for " + json);
        } catch (GhAppRefreshResponse.RefreshException e) {
            assertEquals(expected, e.getMessage());
        }
    }
}
