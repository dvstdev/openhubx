package com.darktile.openhubx.e2e;

import android.app.Activity;
import android.view.WindowManager;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.Until;

import com.darktile.openhubx.BuildConfig;
import com.darktile.openhubx.R;
import com.darktile.openhubx.ui.activity.SearchActivity;
import com.darktile.openhubx.util.PrefUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;

import static com.darktile.openhubx.e2e.E2e.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Guest mode ("Just browse"): no account, public pages only. Needs no token. */
@RunWith(AndroidJUnit4.class)
public class GuestModeE2eTest {

    @Before
    public void signedOut() {
        resetToSignedOut();
    }

    /** Bug: guest mode crashed (null user in the account menu) and then kept restarting. */
    @Test
    public void justBrowse_opensHomeAndStays() {
        launchLogin();
        tap("row_browse");
        waitFor("bn_bar");
        device().waitForIdle();
        assertTrue("Home screen did not stay up", shows("bn_bar", 3_000));
        assertTrue("App left the screen (crash?)", appInFront());
    }

    /** Bug: with guest mode saved, every start bounced Splash -> Home -> Splash forever. */
    @Test
    public void guestRelaunch_landsOnHomeWithoutLoop() throws Exception {
        PrefUtils.set(PrefUtils.BROWSE_ANONYMOUS, true);
        launchFromIcon();
        waitFor("bn_bar");
        Thread.sleep(3_000);
        assertTrue("Home screen did not stay up", shows("bn_bar", 1_000));
        assertFalse("Fell back to the login screen", shows("row_browse", 500));
    }

    /** A page that needs an account asks the guest to sign in instead of failing. */
    @Test
    public void guest_accountPage_asksToSignIn() {
        PrefUtils.set(PrefUtils.BROWSE_ANONYMOUS, true);
        launchFromIcon();
        tap("bn_profile");
        assertTrue("No sign-in prompt",
                device().wait(Until.hasObject(By.text(text(R.string.sign_in_required))), WAIT_MS));
        device().pressBack();
    }

    /** Bug: About force-closed (its cards need a MaterialComponents theme). */
    @Test
    public void about_opens() {
        PrefUtils.set(PrefUtils.BROWSE_ANONYMOUS, true);
        launchFromIcon();
        tap("bn_more");
        tap("cell_about");
        assertTrue("About page did not show the version",
                device().wait(Until.hasObject(By.text(BuildConfig.VERSION_NAME)), WAIT_MS));
        assertTrue("App left the screen (crash?)", appInFront());
        device().pressBack();
    }

    /** The search box sits at the bottom and the screen resizes, so the keyboard can't cover it. */
    @Test
    public void search_boxStaysVisibleWithKeyboard() throws Exception {
        PrefUtils.set(PrefUtils.BROWSE_ANONYMOUS, true);
        launchFromIcon();
        tap("bn_search");
        waitFor("search_input").click();
        waitFor("search_input").setText("android");
        device().waitForIdle();
        Thread.sleep(1_000);   // let the keyboard finish opening
        assertTrue("Search box hidden after typing", shows("search_input", 1_000));
        assertTrue("Search button hidden after typing", shows("search_go", 1_000));

        final int[] mode = new int[1];
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Collection<Activity> resumed = ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED);
            for (Activity a : resumed) {
                if (a instanceof SearchActivity) {
                    mode[0] = a.getWindow().getAttributes().softInputMode
                            & WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST;
                }
            }
        });
        assertEquals("Search screen must resize for the keyboard",
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE, mode[0]);
        device().pressBack();
        device().pressBack();
    }
}
