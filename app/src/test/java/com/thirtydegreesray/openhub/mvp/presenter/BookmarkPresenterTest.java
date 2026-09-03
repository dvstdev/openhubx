package com.thirtydegreesray.openhub.mvp.presenter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.thirtydegreesray.openhub.dao.Bookmark;
import com.thirtydegreesray.openhub.dao.LocalRepo;
import com.thirtydegreesray.openhub.dao.LocalUser;
import com.thirtydegreesray.openhub.mvp.model.BookmarkExt;

import org.junit.Test;

/**
 * Regression tests for the bookmark-list crash fix: a bookmark whose companion
 * LOCAL_USER / LOCAL_REPO row is missing (orphaned) must be skipped, not NPE and
 * blank the entire list. See BookmarkPresenter.rehydrate.
 */
public class BookmarkPresenterTest {

    private static Bookmark userBookmark(String login) {
        Bookmark bookmark = new Bookmark("id-" + login);
        bookmark.setType("user");
        bookmark.setUserId(login);
        return bookmark;
    }

    private static Bookmark repoBookmark(long repoId) {
        Bookmark bookmark = new Bookmark("id-" + repoId);
        bookmark.setType("repo");
        bookmark.setRepoId(repoId);
        return bookmark;
    }

    @Test
    public void userBookmark_missingCompanion_isSkipped() {
        assertNull(BookmarkPresenter.rehydrate(userBookmark("octocat"), null, null));
    }

    @Test
    public void repoBookmark_missingCompanion_isSkipped() {
        assertNull(BookmarkPresenter.rehydrate(repoBookmark(42L), null, null));
    }

    @Test
    public void userBookmark_withCompanion_rehydrates() {
        LocalUser localUser = new LocalUser();
        localUser.setLogin("octocat");
        localUser.setFollowers(0);
        localUser.setFollowing(0);
        BookmarkExt ext = BookmarkPresenter.rehydrate(userBookmark("octocat"), localUser, null);
        assertNotNull(ext);
        assertNotNull(ext.getUser());
        assertEquals("octocat", ext.getUser().getLogin());
    }

    @Test
    public void repoBookmark_withCompanion_rehydrates() {
        LocalRepo localRepo = new LocalRepo();
        localRepo.setId(42L);
        localRepo.setName("Hello-World");
        localRepo.setStargazersCount(0);
        localRepo.setWatchersCount(0);
        localRepo.setForksCount(0);
        localRepo.setFork(false);
        BookmarkExt ext = BookmarkPresenter.rehydrate(repoBookmark(42L), null, localRepo);
        assertNotNull(ext);
        assertNotNull(ext.getRepository());
        assertEquals(42, ext.getRepository().getId());
    }
}
