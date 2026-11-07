package com.darktile.openhubx.util;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.darktile.openhubx.AppData;
import com.darktile.openhubx.dao.Bookmark;
import com.darktile.openhubx.dao.DaoSession;
import com.darktile.openhubx.dao.LocalRepo;
import com.darktile.openhubx.dao.LocalUser;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Serializes bookmarks (and their companion LocalRepo/LocalUser rows) to JSON and
 * restores them, so users can back up / move bookmarks across installs or devices.
 *
 * Import accepts any export: from any account, app version, device or language. It
 * reads fields by hand so one bad row or an unreadable date never fails the whole file.
 */
public final class BookmarkBackup {

    private BookmarkBackup() {}

    public static class Backup {
        int version = 1;
        List<Bookmark> bookmarks = new ArrayList<>();
        List<LocalRepo> repos = new ArrayList<>();
        List<LocalUser> users = new ArrayList<>();
    }

    /** What an import did: rows added, and rows skipped (duplicates or unusable). */
    public static final class ImportResult {
        public final int added;
        public final int skipped;

        ImportResult(int added, int skipped) {
            this.added = added;
            this.skipped = skipped;
        }
    }

    /** A parsed backup file, before anything is written to the database. */
    static final class Parsed {
        final List<Bookmark> bookmarks = new ArrayList<>();
        final Map<Long, LocalRepo> repos = new HashMap<>();
        final Map<String, LocalUser> users = new HashMap<>();
        /** Logins whose user row was made up here (login only), not read from the file. */
        final Set<String> placeholderUsers = new HashSet<>();
        int unusable;
    }

    public static String export(DaoSession dao) {
        Backup backup = new Backup();
        // Export only the current account's bookmarks (per-account scoping).
        String owner = AppData.INSTANCE.getLoggedUser() != null
                ? AppData.INSTANCE.getLoggedUser().getLogin() : null;
        backup.bookmarks = dao.getBookmarkDao().loadAllByOwner(owner);

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
        return toJson(backup);
    }

    /**
     * Writes the backup. Each bookmark keeps "markTime" as text, which older app versions
     * read, and adds "markTimeMs" (epoch millis), which reads the same on every phone.
     */
    static String toJson(Backup backup) {
        JsonObject root = new Gson().toJsonTree(backup).getAsJsonObject();
        JsonArray rows = array(root, "bookmarks");
        for (int i = 0; rows != null && i < rows.size() && i < backup.bookmarks.size(); i++) {
            Date mark = backup.bookmarks.get(i).getMarkTime();
            if (mark != null && rows.get(i).isJsonObject()) {
                rows.get(i).getAsJsonObject().addProperty("markTimeMs", mark.getTime());
            }
        }
        return root.toString();
    }

    /**
     * Imports a backup into the current account. Every imported bookmark gets a new id, so a
     * file exported by another account on this phone never overwrites that account's rows.
     * Duplicates (same repo or user already bookmarked by this account) are skipped.
     *
     * @throws IllegalArgumentException when the text is not a bookmark backup at all.
     */
    public static ImportResult importJson(DaoSession dao, String json) {
        final Parsed parsed = parse(json);
        final String owner = AppData.INSTANCE.getLoggedUser() != null
                ? AppData.INSTANCE.getLoggedUser().getLogin() : null;
        final int[] added = {0};
        final int[] skipped = {parsed.unusable};

        dao.runInTx(() -> {
            for (LocalRepo r : parsed.repos.values()) dao.getLocalRepoDao().insert(r);
            for (LocalUser u : parsed.users.values()) {
                // Never replace a saved user's details with a login-only placeholder.
                if (parsed.placeholderUsers.contains(u.getLogin())
                        && dao.getLocalUserDao().load(u.getLogin()) != null) continue;
                dao.getLocalUserDao().insert(u);
            }

            Set<String> seen = new HashSet<>();
            for (Bookmark bm : parsed.bookmarks) {
                boolean isUser = "user".equals(bm.getType());
                String key = isUser ? "u:" + bm.getUserId() : "r:" + bm.getRepoId();
                boolean exists = !seen.add(key) || (isUser
                        ? dao.getBookmarkDao().countByUserId(bm.getUserId(), owner) > 0
                        : dao.getBookmarkDao().countByRepoId(bm.getRepoId(), owner) > 0);
                // A repo bookmark with no saved repo details can't be shown in the list.
                boolean showable = isUser || parsed.repos.containsKey(bm.getRepoId())
                        || dao.getLocalRepoDao().load(bm.getRepoId()) != null;
                if (exists || !showable) {
                    skipped[0]++;
                    continue;
                }
                bm.setId(UUID.randomUUID().toString());
                bm.setOwnerLoginId(owner);
                dao.getBookmarkDao().insert(bm);
                added[0]++;
            }
        });
        return new ImportResult(added[0], skipped[0]);
    }

    /**
     * Reads a backup without touching the database. Accepts the {bookmarks, repos, users}
     * object written by every app version, or a bare bookmark array; ignores unknown fields
     * and a leading byte-order mark. Drops rows missing what the database requires, and adds
     * a minimal user row for user bookmarks whose details were not exported.
     */
    static Parsed parse(String json) {
        if (json == null) throw new IllegalArgumentException("empty file");
        json = json.replace("\uFEFF", "").trim();
        JsonElement root;
        try {
            root = JsonParser.parseString(json);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("not JSON", e);
        }
        JsonArray bookmarkRows;
        JsonArray repoRows = null;
        JsonArray userRows = null;
        if (root.isJsonArray()) {
            bookmarkRows = root.getAsJsonArray();
        } else if (root.isJsonObject() && array(root.getAsJsonObject(), "bookmarks") != null) {
            JsonObject obj = root.getAsJsonObject();
            bookmarkRows = array(obj, "bookmarks");
            repoRows = array(obj, "repos");
            userRows = array(obj, "users");
        } else {
            throw new IllegalArgumentException("no bookmarks in file");
        }

        Parsed out = new Parsed();
        if (repoRows != null) {
            for (JsonElement el : repoRows) {
                LocalRepo r = toRepo(el);
                if (r != null) out.repos.put(r.getId(), r);
            }
        }
        if (userRows != null) {
            for (JsonElement el : userRows) {
                LocalUser u = toUser(el);
                if (u != null) out.users.put(u.getLogin(), u);
            }
        }
        for (JsonElement el : bookmarkRows) {
            Bookmark bm = toBookmark(el);
            if (bm == null) {
                out.unusable++;
                continue;
            }
            if ("user".equals(bm.getType()) && !out.users.containsKey(bm.getUserId())) {
                out.users.put(bm.getUserId(), new LocalUser(bm.getUserId()));
                out.placeholderUsers.add(bm.getUserId());
            }
            out.bookmarks.add(bm);
        }
        return out;
    }

    @Nullable
    private static Bookmark toBookmark(JsonElement el) {
        if (el == null || !el.isJsonObject()) return null;
        JsonObject o = el.getAsJsonObject();
        String userId = str(o, "userId");
        Long repoId = num(o, "repoId");
        String type = str(o, "type");
        if (type == null) type = userId != null ? "user" : "repo";
        if ("user".equals(type) ? userId == null : repoId == null) return null;

        Bookmark bm = new Bookmark(UUID.randomUUID().toString());
        bm.setType("user".equals(type) ? "user" : "repo");
        bm.setUserId(userId);
        bm.setRepoId(repoId);
        Date mark = date(o.get("markTimeMs"));
        if (mark == null) mark = date(o.get("markTime"));
        bm.setMarkTime(mark != null ? mark : new Date());
        return bm;
    }

    @Nullable
    private static LocalRepo toRepo(JsonElement el) {
        if (el == null || !el.isJsonObject()) return null;
        JsonObject o = el.getAsJsonObject();
        Long id = num(o, "id");
        String name = str(o, "name");
        if (id == null || name == null) return null;
        LocalRepo r = new LocalRepo();
        r.setId(id);
        r.setName(name);
        r.setDescription(str(o, "description"));
        r.setLanguage(str(o, "language"));
        r.setStargazersCount(integer(o, "stargazersCount"));
        r.setWatchersCount(integer(o, "watchersCount"));
        r.setForksCount(integer(o, "forksCount"));
        JsonPrimitive fork = prim(o, "fork");
        r.setFork(fork != null && fork.isBoolean() ? fork.getAsBoolean() : null);
        r.setOwnerLogin(str(o, "ownerLogin"));
        r.setOwnerAvatarUrl(str(o, "ownerAvatarUrl"));
        return r;
    }

    @Nullable
    private static LocalUser toUser(JsonElement el) {
        if (el == null || !el.isJsonObject()) return null;
        JsonObject o = el.getAsJsonObject();
        String login = str(o, "login");
        if (login == null) return null;
        LocalUser u = new LocalUser(login);
        u.setName(str(o, "name"));
        u.setAvatarUrl(str(o, "avatarUrl"));
        u.setFollowers(integer(o, "followers"));
        u.setFollowing(integer(o, "following"));
        return u;
    }

    /**
     * Reads a date in any form an export may hold: epoch millis, ISO 8601, or Gson's default
     * text, which depends on the exporting phone's language and Android version (newer ones
     * put a narrow no-break space before AM/PM). Returns null when nothing matches.
     */
    @Nullable
    static Date date(@Nullable JsonElement el) {
        if (el == null || !el.isJsonPrimitive()) return null;
        JsonPrimitive p = el.getAsJsonPrimitive();
        if (p.isNumber()) return new Date(p.getAsLong());
        String s = p.getAsString().replace('\u202F', ' ').replace('\u00A0', ' ').trim();
        if (s.isEmpty()) return null;
        if (s.matches("-?\\d{9,}")) return new Date(Long.parseLong(s));
        if (formats == null) formats = candidates();
        for (int i = 0; i < formats.size(); i++) {
            DateFormat f = formats.get(i);
            try {
                Date d = f.parse(s);
                // One file uses one format: try the one that worked first next time.
                if (i > 0) formats.add(0, formats.remove(i));
                return d;
            } catch (ParseException ignored) {
                // try the next format
            }
        }
        return null;
    }

    /** Built once on first use; import runs on one thread. */
    private static List<DateFormat> formats;

    private static List<DateFormat> candidates() {
        List<DateFormat> list = new ArrayList<>();
        for (String iso : new String[]{"yyyy-MM-dd'T'HH:mm:ss.SSSX", "yyyy-MM-dd'T'HH:mm:ssX",
                "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd"}) {
            SimpleDateFormat f = new SimpleDateFormat(iso, Locale.US);
            f.setTimeZone(TimeZone.getTimeZone("UTC"));
            f.setLenient(false);
            list.add(f);
        }
        Set<Locale> locales = new java.util.LinkedHashSet<>();
        locales.add(Locale.US);
        locales.add(Locale.getDefault());
        for (Locale l : DateFormat.getAvailableLocales()) locales.add(l);
        int[] styles = {DateFormat.DEFAULT, DateFormat.MEDIUM, DateFormat.LONG, DateFormat.SHORT};
        for (Locale l : locales) {
            for (int st : styles) list.add(DateFormat.getDateTimeInstance(st, st, l));
        }
        return list;
    }

    @Nullable
    private static JsonArray array(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonArray() ? e.getAsJsonArray() : null;
    }

    @Nullable
    private static JsonPrimitive prim(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsJsonPrimitive() : null;
    }

    @Nullable
    private static String str(JsonObject o, String key) {
        JsonPrimitive p = prim(o, key);
        if (p == null) return null;
        String s = p.getAsString().trim();
        return s.isEmpty() ? null : s;
    }

    @Nullable
    private static Long num(JsonObject o, String key) {
        JsonPrimitive p = prim(o, key);
        if (p == null) return null;
        try {
            return p.isNumber() ? p.getAsLong() : Long.parseLong(p.getAsString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Nullable
    private static Integer integer(JsonObject o, String key) {
        Long n = num(o, key);
        return n == null ? null : n.intValue();
    }
}
