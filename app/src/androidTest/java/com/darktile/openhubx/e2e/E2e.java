package com.darktile.openhubx.e2e;

import android.content.Context;
import android.content.Intent;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import com.darktile.openhubx.AppApplication;
import com.darktile.openhubx.AppData;
import com.darktile.openhubx.dao.AuthUser;
import com.darktile.openhubx.dao.DaoSession;
import com.darktile.openhubx.ui.activity.LoginActivity;
import com.darktile.openhubx.ui.activity.SplashActivity;
import com.darktile.openhubx.util.PrefUtils;

import java.util.regex.Pattern;

import static org.junit.Assert.assertNotNull;

/**
 * Shared steps for the end-to-end tests. They drive the real app with UiAutomator, so a
 * crash anywhere in a flow kills the app process and fails the test.
 */
final class E2e {

    static final long WAIT_MS = 15_000;

    private E2e() {}

    static UiDevice device() {
        return UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    }

    static Context app() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    static String pkg() {
        return app().getPackageName();
    }

    static DaoSession db() {
        return AppApplication.get().getAppComponent().getDaoSession();
    }

    /** Signs out every account and leaves guest mode, so each test starts at the login screen. */
    static void resetToSignedOut() {
        for (AuthUser user : db().getAuthUserDao().loadAll()) {
            db().getAuthUserDao().delete(user);
        }
        AppData.INSTANCE.setAuthUser(null);
        AppData.INSTANCE.setLoggedUser(null);
        PrefUtils.set(PrefUtils.BROWSE_ANONYMOUS, false);
    }

    /** Starts the app from a clean task, the same way the launcher icon does. */
    static void launchFromIcon() {
        start(SplashActivity.class);
    }

    static void launchLogin() {
        start(LoginActivity.class);
    }

    private static void start(Class<?> activity) {
        Intent intent = new Intent(app(), activity)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        app().startActivity(intent);
        device().waitForIdle();
    }

    /** Matches a view id from any package, since the e2e app id differs from the resource package. */
    static BySelector id(String resId) {
        return By.res(Pattern.compile("^[^:]+:id/" + Pattern.quote(resId) + "$"));
    }

    /** Waits for a view of this app by id and returns it; fails the test if it never shows. */
    static UiObject2 waitFor(String resId) {
        UiObject2 view = device().wait(Until.findObject(id(resId)), WAIT_MS);
        assertNotNull("Never showed: " + resId, view);
        return view;
    }

    static boolean shows(String resId, long ms) {
        return device().wait(Until.hasObject(id(resId)), ms);
    }

    static void tap(String resId) {
        waitFor(resId).click();
        device().waitForIdle();
    }

    static String text(int stringRes) {
        return app().getString(stringRes);
    }

    /** True while this app (not a browser or the launcher) is on screen. */
    static boolean appInFront() {
        return pkg().equals(device().getCurrentPackageName());
    }

    /** GitHub token passed with -e ghToken ..., or null; token tests skip without it. */
    static String token() {
        String tok = InstrumentationRegistry.getArguments().getString("ghToken");
        return tok == null || tok.isEmpty() ? null : tok;
    }

    /** Signs in through the Paste token row and waits for the home screen. */
    static void signInWithToken(String tok) {
        launchLogin();
        tap("row_token");
        waitFor("token_et").setText(tok);
        tap("token_login_bn");
        waitFor("bn_bar");
    }
}
