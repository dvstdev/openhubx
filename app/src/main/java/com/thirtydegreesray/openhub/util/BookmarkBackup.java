package com.thirtydegreesray.openhub.util;

import com.google.gson.Gson;
import com.thirtydegreesray.openhub.dao.Bookmark;
import com.thirtydegreesray.openhub.dao.BookmarkDao;
import com.thirtydegreesray.openhub.dao.DaoSession;
import com.thirtydegreesray.openhub.dao.LocalRepo;
import com.thirtydegreesray.openhub.dao.LocalUser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Serializes bookmarks (and their companion LocalRepo/LocalUser rows) to JSON and
 * restores them, so users can back up / move bookmarks across installs or devices.
 */
public final class BookmarkBackup {

    private BookmarkBackup() {}

    public static class Backup {
        int version = 1;
        List<Bookmark> bookmarks = new ArrayList<>();
        List<LocalRepo> repos = new ArrayList<>();
        List<LocalUser> users = new ArrayList<>();
    }

    public static String export(DaoSession dao) {
        Backup backup = new Backup();
        backup.bookmarks = dao.getBookmarkDao().loadAll();

        Set<Long> repoIds = new HashSet<>();
        Set<String> userIds = new HashSet<>();
        for (Bookmark bm : backup.bookmarks) {
            if ("user".equals(bm.getType())) {
                if (bm.getUserId() != null) userIds.add(bm.getUserId());
            } else if (bm.getRepoId() != null) {
                repoIds.add(bm.getRepoId());
            }
        }
        for (Long id : repoIds) {
            LocalRepo r = dao.getLocalRepoDao().load(id);
            if (r != null) backup.repos.add(r);
        }
        for (String uid : userIds) {
            LocalUser u = dao.getLocalUserDao().load(uid);
            if (u != null) backup.users.add(u);
        }
        return new Gson().toJson(backup);
    }

    /** Returns the number of newly-added bookmarks (existing ones are skipped). */
    public static int importJson(DaoSession dao, String json) {
        Backup backup = new Gson().fromJson(json, Backup.class);
        if (backup == null) return 0;

        if (backup.repos != null) {
            for (LocalRepo r : backup.repos) dao.getLocalRepoDao().insertOrReplace(r);
        }
        if (backup.users != null) {
            for (LocalUser u : backup.users) dao.getLocalUserDao().insertOrReplace(u);
        }

        int added = 0;
        if (backup.bookmarks != null) {
            for (Bookmark bm : backup.bookmarks) {
                boolean exists;
                if ("user".equals(bm.getType())) {
                    exists = dao.getBookmarkDao().queryBuilder()
                            .where(BookmarkDao.Properties.UserId.eq(bm.getUserId()))
                            .count() > 0;
                } else {
                    exists = bm.getRepoId() != null && dao.getBookmarkDao().queryBuilder()
                            .where(BookmarkDao.Properties.RepoId.eq(bm.getRepoId()))
                            .count() > 0;
                }
                if (!exists) {
                    dao.getBookmarkDao().insertOrReplace(bm);
                    added++;
                }
            }
        }
        return added;
    }
}
