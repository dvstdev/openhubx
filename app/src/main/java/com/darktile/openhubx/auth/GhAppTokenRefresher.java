package com.darktile.openhubx.auth;

import android.util.Log;

import androidx.annotation.Nullable;

import com.darktile.openhubx.AppApplication;
import com.darktile.openhubx.AppConfig;
import com.darktile.openhubx.AppData;
import com.darktile.openhubx.dao.AuthUser;
import com.darktile.openhubx.dao.AuthUserDao;
import com.darktile.openhubx.dao.OpenHubRoomDatabase;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Refreshes the selected account's GitHub App user token before it expires.
 *
 * <p>Uses {@code client_id + refresh_token} only (device-flow tokens need no client
 * secret). Called synchronously from the API interceptor on an OkHttp worker thread;
 * {@code synchronized} so concurrent requests trigger at most one refresh. On any
 * failure the current token is kept and the request proceeds; a resulting 401 sends
 * the user to the "login expired" path. Token values are never logged.
 */
public enum GhAppTokenRefresher {
    INSTANCE;

    private static final String TAG = "GhAppTokenRefresher";
    /** Refresh this long before expiry to avoid racing the 8h deadline mid-request. */
    private static final long SKEW_MILLIS = 5 * 60 * 1000L;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(AppConfig.HTTP_TIME_OUT, TimeUnit.MILLISECONDS)
            .readTimeout(AppConfig.HTTP_TIME_OUT, TimeUnit.MILLISECONDS)
            .build();

    /** Last token swapped out by a refresh, and its replacement. */
    private String replacedToken;
    private String replacementToken;

    /**
     * Returns a usable access token for the selected account, refreshing first if it is
     * a refreshable GitHub App token close to expiry. Returns {@code currentToken} when
     * no refresh applies or the refresh fails.
     */
    public synchronized String ensureFresh(@Nullable String currentToken) {
        if (currentToken == null) {
            return null;
        }
        if (currentToken.equals(replacedToken)) {
            // Request was built just before a refresh; the old token is now invalid.
            return replacementToken;
        }
        AuthUser user = AppData.INSTANCE.getAuthUser();
        if (user == null || !currentToken.equals(user.getAccessToken())) {
            // Not the selected account's token (e.g. validating a newly scanned one).
            return currentToken;
        }
        long now = System.currentTimeMillis();
        if (!user.isRefreshable(now)
                || !AuthExpiry.expiresWithin(user.getAuthTime().getTime(),
                user.getExpireIn(), now, SKEW_MILLIS)) {
            return currentToken;
        }
        try {
            GhAppRefreshResponse.Result result = refresh(user.getClientId(), user.getRefreshToken());
            AuthUser updated = applyRefresh(user, result, now);
            AuthUserDao dao = OpenHubRoomDatabase.getInstance(AppApplication.get()).authUserDao();
            // ACCESS_TOKEN is the primary key, so replace the row.
            dao.delete(user);
            dao.insert(updated);
            AppData.INSTANCE.setAuthUser(updated);
            replacedToken = currentToken;
            replacementToken = updated.getAccessToken();
            Log.i(TAG, "GitHub App token refreshed");
            return updated.getAccessToken();
        } catch (IOException | GhAppRefreshResponse.RefreshException e) {
            Log.w(TAG, "GitHub App token refresh failed: " + e.getMessage());
            return currentToken;
        }
    }

    private GhAppRefreshResponse.Result refresh(String clientId, String refreshToken)
            throws IOException, GhAppRefreshResponse.RefreshException {
        FormBody body = new FormBody.Builder()
                .add("client_id", clientId)
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .build();
        Request request = new Request.Builder()
                .url(AppConfig.GITHUB_BASE_URL + "login/oauth/access_token")
                .header("Accept", "application/json")
                .post(body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            ResponseBody rb = response.body();
            if (!response.isSuccessful() || rb == null) {
                throw new GhAppRefreshResponse.RefreshException("HTTP " + response.code());
            }
            return GhAppRefreshResponse.parse(rb.string(), System.currentTimeMillis() / 1000L);
        }
    }

    /** Copy of {@code old} carrying the refreshed token and expiries. */
    static AuthUser applyRefresh(AuthUser old, GhAppRefreshResponse.Result result, long nowMillis) {
        AuthUser u = new AuthUser();
        u.setAccessToken(result.accessToken);
        u.setAuthTime(new Date(nowMillis));
        u.setExpireIn(AuthExpiry.expireInFor(result.expEpochSeconds, nowMillis));
        u.setScope(old.getScope());
        u.setSelected(old.getSelected());
        u.setLoginId(old.getLoginId());
        u.setName(old.getName());
        u.setAvatar(old.getAvatar());
        u.setTokenType(old.getTokenType());
        u.setClientId(old.getClientId());
        // GitHub rotates the refresh token; the old one is now invalid.
        u.setRefreshToken(result.refreshToken);
        u.setRefreshExpiresAt(result.refreshExpEpochSeconds);
        return u;
    }
}
