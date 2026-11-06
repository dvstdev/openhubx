package com.darktile.openhubx.e2e;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.darktile.openhubx.dao.AuthUser;
import com.darktile.openhubx.dao.Bookmark;
import com.darktile.openhubx.dao.BookmarkDao;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.darktile.openhubx.e2e.E2e.db;
import static org.junit.Assert.assertEquals;

/**
 * Bug: bookmarks were shared by all accounts and looked lost after switching.
 * Runs on the device's real database; needs no token or network.
 */
@RunWith(AndroidJUnit4.class)
public class BookmarkPerAccountE2eTest {

    private static final String ALICE = "e2e-alice-" + UUID.randomUUID();
    private static final String BOB = "e2e-bob-" + UUID.randomUUID();

    private BookmarkDao dao;
    private Bookmark aliceMark;
    private Bookmark bobMark;
    private AuthUser alice;
    private AuthUser bob;

    @Before
    public void setUp() {
        dao = db().getBookmarkDao();
        aliceMark = mark(ALICE, 900_000_001L);
        bobMark = mark(BOB, 900_000_002L);
        alice = account(ALICE, true);
        bob = account(BOB, false);
    }

    @After
    public void tearDown() {
        dao.deleteByKey(aliceMark.getId());
        dao.deleteByKey(bobMark.getId());
        db().getAuthUserDao().deleteByLoginId(ALICE);
        db().getAuthUserDao().deleteByLoginId(BOB);
    }

    @Test
    public void eachAccountSeesOnlyItsOwnBookmarks() {
        assertOwnOnly(ALICE, aliceMark);
        assertOwnOnly(BOB, bobMark);
    }

    @Test
    public void switchingAccountsKeepsBookmarks() {
        // Same steps the app runs on Switch account (MainPresenter.toggleAccount).
        db().getAuthUserDao().clearAllSelected();
        db().getAuthUserDao().setSelectedByLoginId(BOB);
        assertOwnOnly(BOB, bobMark);

        db().getAuthUserDao().clearAllSelected();
        db().getAuthUserDao().setSelectedByLoginId(ALICE);
        assertOwnOnly(ALICE, aliceMark);
        assertOwnOnly(BOB, bobMark);
    }

    private void assertOwnOnly(String owner, Bookmark expected) {
        List<Bookmark> list = dao.loadPaged(owner, 30, 0);
        assertEquals(owner + " bookmark count", 1, list.size());
        assertEquals(expected.getId(), list.get(0).getId());
    }

    private Bookmark mark(String owner, long repoId) {
        Bookmark b = new Bookmark(UUID.randomUUID().toString());
        b.setType("repo");
        b.setRepoId(repoId);
        b.setMarkTime(new Date());
        b.setOwnerLoginId(owner);
        dao.insert(b);
        return b;
    }

    private AuthUser account(String login, boolean selected) {
        AuthUser u = new AuthUser();
        u.setAccessToken("e2e-token-" + login);
        u.setLoginId(login);
        u.setAuthTime(new Date());
        u.setExpireIn(3600);
        u.setSelected(selected);
        db().getAuthUserDao().insert(u);
        return u;
    }
}
