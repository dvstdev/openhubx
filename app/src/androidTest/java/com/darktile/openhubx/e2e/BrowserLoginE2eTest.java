package com.darktile.openhubx.e2e;

import android.content.Intent;
import android.net.Uri;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.darktile.openhubx.e2e.E2e.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** "Browser" sign-in. Needs a browser on the device, but no token. */
@RunWith(AndroidJUnit4.class)
public class BrowserLoginE2eTest {

    @Before
    public void signedOut() {
        resetToSignedOut();
    }

    /** Bug: on Android 11+ the app could not see any browser ("no browser clients installed"). */
    @Test
    public void appCanSeeABrowser() {
        Intent view = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com"))
                .addCategory(Intent.CATEGORY_BROWSABLE);
        assertFalse("No browser visible to the app: missing <queries> in the manifest,"
                        + " or no browser installed on this device",
                app().getPackageManager().queryIntentActivities(view, 0).isEmpty());
    }

    /** Bug: tapping Browser crashed on Android 12+ (menu PendingIntents had no mutability flag). */
    @Test
    public void browserRow_opensBrowserWithoutCrash() {
        launchLogin();
        tap("row_browser");
        assertTrue("Browser never opened", leftApp());
        backToApp();
        assertTrue("App is gone after the browser (crash?)", shows("row_browser", WAIT_MS));
    }

    static boolean leftApp() {
        long end = System.currentTimeMillis() + WAIT_MS;
        while (System.currentTimeMillis() < end) {
            if (!appInFront()) return true;
            device().waitForIdle(500);
        }
        return false;
    }

    static void backToApp() {
        for (int i = 0; i < 4 && !appInFront(); i++) {
            device().pressBack();
            device().waitForIdle();
        }
    }
}
