package com.darktile.openhubx.e2e;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.Until;

import com.darktile.openhubx.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.darktile.openhubx.e2e.E2e.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * Signed-in flows. They need a real GitHub token and network:
 * pass it with -e ghToken (see tools/e2e/run-e2e.sh). Without one, they are skipped.
 */
@RunWith(AndroidJUnit4.class)
public class AccountE2eTest {

    private String tok;

    @Before
    public void signIn() {
        tok = token();
        assumeTrue("No ghToken given; skipping signed-in tests", tok != null);
        resetToSignedOut();
        signInWithToken(tok);
    }

    /** Bug: More > Add account > Browser crashed the app. */
    @Test
    public void addAccount_browserLogin_doesNotCrash() {
        tap("bn_more");
        tap("cell_add_account");
        tap("row_browser");
        assertTrue("Browser never opened", BrowserLoginE2eTest.leftApp());
        BrowserLoginE2eTest.backToApp();
        assertTrue("App is gone after the browser (crash?)", shows("row_browser", WAIT_MS));
    }

    /** With one account, Switch account offers to add another instead of failing. */
    @Test
    public void switchAccount_withOneAccount_offersAdd() {
        tap("bn_more");
        tap("cell_switch_account");
        assertTrue("No 'add one' prompt",
                device().wait(Until.hasObject(By.text(text(R.string.no_other_accounts))), WAIT_MS));
        device().pressBack();
    }

    /**
     * Bug: Notifications showed "java.lang.Error · Tap to retry". A fine-grained token can't
     * read notifications (GitHub 403), so the page must explain that, with no retry button.
     */
    @Test
    public void notifications_fineGrainedToken_showsHintNoRetry() {
        assumeTrue("Needs a fine-grained token (github_pat_...)", tok.startsWith("github_pat_"));
        tap("bn_profile");
        tap("cell_notifications");
        assertTrue("No permission hint",
                device().wait(Until.hasObject(By.text(text(R.string.notifications_scope_hint))), WAIT_MS));
        assertFalse("Retry button should be hidden", shows("retry_bn", 1_000));
        assertFalse("Raw error text shown",
                device().hasObject(By.textContains("java.lang.Error")));
    }
}
