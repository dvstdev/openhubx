package com.darktile.openhubx.dao;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Room migrations ported from the greenDAO DBOpenHelper upgrade chain
 * (schema versions 2 -> 3 -> 4 -> 5), extended with the v5 -> 6 per-account
 * bookmark column. Version 5 matches the original greenDAO on-disk schema so an
 * existing "OpenHub.db" is adopted in place; these objects then carry it forward
 * now that greenDAO is removed.
 *
 * Divergence from the greenDAO original: the 3 -> 4 step here does NOT drop the
 * TRACE_USER / TRACE_REPO (or BOOK_MARK_USER / BOOK_MARK_REPO) staging tables,
 * because the modern Room schema keeps all four as entities; dropping them would
 * leave a v5 database missing tables Room requires at open. The 4 -> 5 step
 * converges any older database onto the full modern 10-table schema (CREATE TABLE
 * IF NOT EXISTS for every entity), so a database arriving at v5 by any path
 * validates against the Room entities. All CREATE statements are idempotent
 * (IF NOT EXISTS) and data transfers use INSERT OR IGNORE so re-entrant runs
 * cannot fail on existing rows.
 */
public final class Migrations {

    private Migrations() {
    }

    /** v2 -> v3: introduce the trace + bookmark staging tables. */
    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS \"TRACE_USER\" ("
                    + "\"LOGIN\" TEXT PRIMARY KEY NOT NULL ,"
                    + "\"NAME\" TEXT,"
                    + "\"AVATAR_URL\" TEXT,"
                    + "\"FOLLOWERS\" INTEGER,"
                    + "\"FOLLOWING\" INTEGER,"
                    + "\"START_TIME\" INTEGER,"
                    + "\"LATEST_TIME\" INTEGER,"
                    + "\"TRACE_NUM\" INTEGER);");
            db.execSQL("CREATE TABLE IF NOT EXISTS \"TRACE_REPO\" ("
                    + "\"ID\" INTEGER PRIMARY KEY NOT NULL ,"
                    + "\"NAME\" TEXT NOT NULL ,"
                    + "\"DESCRIPTION\" TEXT,"
                    + "\"LANGUAGE\" TEXT,"
                    + "\"STARGAZERS_COUNT\" INTEGER,"
                    + "\"WATCHERS_COUNT\" INTEGER,"
                    + "\"FORKS_COUNT\" INTEGER,"
                    + "\"FORK\" INTEGER,"
                    + "\"OWNER_LOGIN\" TEXT,"
                    + "\"OWNER_AVATAR_URL\" TEXT,"
                    + "\"START_TIME\" INTEGER,"
                    + "\"LATEST_TIME\" INTEGER,"
                    + "\"TRACE_NUM\" INTEGER);");
            db.execSQL("CREATE TABLE IF NOT EXISTS \"BOOK_MARK_USER\" ("
                    + "\"LOGIN\" TEXT PRIMARY KEY NOT NULL ,"
                    + "\"NAME\" TEXT,"
                    + "\"AVATAR_URL\" TEXT,"
                    + "\"FOLLOWERS\" INTEGER,"
                    + "\"FOLLOWING\" INTEGER,"
                    + "\"MARK_TIME\" INTEGER);");
            db.execSQL("CREATE TABLE IF NOT EXISTS \"BOOK_MARK_REPO\" ("
                    + "\"ID\" INTEGER PRIMARY KEY NOT NULL ,"
                    + "\"NAME\" TEXT NOT NULL ,"
                    + "\"DESCRIPTION\" TEXT,"
                    + "\"LANGUAGE\" TEXT,"
                    + "\"STARGAZERS_COUNT\" INTEGER,"
                    + "\"WATCHERS_COUNT\" INTEGER,"
                    + "\"FORKS_COUNT\" INTEGER,"
                    + "\"FORK\" INTEGER,"
                    + "\"OWNER_LOGIN\" TEXT,"
                    + "\"OWNER_AVATAR_URL\" TEXT,"
                    + "\"MARK_TIME\" INTEGER);");
        }
    };

    /**
     * v3 -> v4: introduce the consolidated LOCAL_USER / LOCAL_REPO / TRACE /
     * BOOKMARK tables and move the staged rows across (deduped by primary key via
     * INSERT OR IGNORE, mirroring the greenDAO Java transfer). No staging tables
     * are dropped: TRACE_USER / TRACE_REPO (and BOOK_MARK_USER / BOOK_MARK_REPO)
     * all remain Room entities in the modern schema, so their tables are retained.
     */
    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS \"LOCAL_USER\" ("
                    + "\"LOGIN\" TEXT PRIMARY KEY NOT NULL ,"
                    + "\"NAME\" TEXT,"
                    + "\"AVATAR_URL\" TEXT,"
                    + "\"FOLLOWERS\" INTEGER,"
                    + "\"FOLLOWING\" INTEGER);");
            db.execSQL("CREATE TABLE IF NOT EXISTS \"LOCAL_REPO\" ("
                    + "\"ID\" INTEGER PRIMARY KEY NOT NULL ,"
                    + "\"NAME\" TEXT NOT NULL ,"
                    + "\"DESCRIPTION\" TEXT,"
                    + "\"LANGUAGE\" TEXT,"
                    + "\"STARGAZERS_COUNT\" INTEGER,"
                    + "\"WATCHERS_COUNT\" INTEGER,"
                    + "\"FORKS_COUNT\" INTEGER,"
                    + "\"FORK\" INTEGER,"
                    + "\"OWNER_LOGIN\" TEXT,"
                    + "\"OWNER_AVATAR_URL\" TEXT);");
            db.execSQL("CREATE TABLE IF NOT EXISTS \"TRACE\" ("
                    + "\"ID\" TEXT PRIMARY KEY NOT NULL ,"
                    + "\"TYPE\" TEXT,"
                    + "\"USER_ID\" TEXT,"
                    + "\"REPO_ID\" INTEGER,"
                    + "\"START_TIME\" INTEGER,"
                    + "\"LATEST_TIME\" INTEGER,"
                    + "\"TRACE_NUM\" INTEGER);");
            db.execSQL("CREATE TABLE IF NOT EXISTS \"BOOKMARK\" ("
                    + "\"ID\" TEXT PRIMARY KEY NOT NULL ,"
                    + "\"TYPE\" TEXT NOT NULL ,"
                    + "\"USER_ID\" TEXT,"
                    + "\"REPO_ID\" INTEGER,"
                    + "\"MARK_TIME\" INTEGER);");

            // Transfer repos/users into the consolidated LOCAL_* tables (dedupe by PK).
            db.execSQL("INSERT OR IGNORE INTO \"LOCAL_REPO\" "
                    + "(\"ID\",\"NAME\",\"DESCRIPTION\",\"LANGUAGE\",\"STARGAZERS_COUNT\","
                    + "\"WATCHERS_COUNT\",\"FORKS_COUNT\",\"FORK\",\"OWNER_LOGIN\",\"OWNER_AVATAR_URL\") "
                    + "SELECT \"ID\",\"NAME\",\"DESCRIPTION\",\"LANGUAGE\",\"STARGAZERS_COUNT\","
                    + "\"WATCHERS_COUNT\",\"FORKS_COUNT\",\"FORK\",\"OWNER_LOGIN\",\"OWNER_AVATAR_URL\" "
                    + "FROM \"TRACE_REPO\";");
            db.execSQL("INSERT OR IGNORE INTO \"LOCAL_REPO\" "
                    + "(\"ID\",\"NAME\",\"DESCRIPTION\",\"LANGUAGE\",\"STARGAZERS_COUNT\","
                    + "\"WATCHERS_COUNT\",\"FORKS_COUNT\",\"FORK\",\"OWNER_LOGIN\",\"OWNER_AVATAR_URL\") "
                    + "SELECT \"ID\",\"NAME\",\"DESCRIPTION\",\"LANGUAGE\",\"STARGAZERS_COUNT\","
                    + "\"WATCHERS_COUNT\",\"FORKS_COUNT\",\"FORK\",\"OWNER_LOGIN\",\"OWNER_AVATAR_URL\" "
                    + "FROM \"BOOK_MARK_REPO\";");
            db.execSQL("INSERT OR IGNORE INTO \"LOCAL_USER\" "
                    + "(\"LOGIN\",\"NAME\",\"AVATAR_URL\",\"FOLLOWERS\",\"FOLLOWING\") "
                    + "SELECT \"LOGIN\",\"NAME\",\"AVATAR_URL\",\"FOLLOWERS\",\"FOLLOWING\" "
                    + "FROM \"TRACE_USER\";");
            db.execSQL("INSERT OR IGNORE INTO \"LOCAL_USER\" "
                    + "(\"LOGIN\",\"NAME\",\"AVATAR_URL\",\"FOLLOWERS\",\"FOLLOWING\") "
                    + "SELECT \"LOGIN\",\"NAME\",\"AVATAR_URL\",\"FOLLOWERS\",\"FOLLOWING\" "
                    + "FROM \"BOOK_MARK_USER\";");

            // Materialise trace rows with generated string ids (greenDAO used UUIDs).
            db.execSQL("INSERT INTO \"TRACE\" "
                    + "(\"ID\",\"TYPE\",\"USER_ID\",\"REPO_ID\",\"START_TIME\",\"LATEST_TIME\",\"TRACE_NUM\") "
                    + "SELECT lower(hex(randomblob(16))),'repo',NULL,\"ID\",\"START_TIME\",\"LATEST_TIME\",\"TRACE_NUM\" "
                    + "FROM \"TRACE_REPO\";");
            db.execSQL("INSERT INTO \"TRACE\" "
                    + "(\"ID\",\"TYPE\",\"USER_ID\",\"REPO_ID\",\"START_TIME\",\"LATEST_TIME\",\"TRACE_NUM\") "
                    + "SELECT lower(hex(randomblob(16))),'user',\"LOGIN\",NULL,\"START_TIME\",\"LATEST_TIME\",\"TRACE_NUM\" "
                    + "FROM \"TRACE_USER\";");

            // Materialise bookmark rows with generated string ids.
            db.execSQL("INSERT INTO \"BOOKMARK\" "
                    + "(\"ID\",\"TYPE\",\"USER_ID\",\"REPO_ID\",\"MARK_TIME\") "
                    + "SELECT lower(hex(randomblob(16))),'repo',NULL,\"ID\",\"MARK_TIME\" "
                    + "FROM \"BOOK_MARK_REPO\";");
            db.execSQL("INSERT INTO \"BOOKMARK\" "
                    + "(\"ID\",\"TYPE\",\"USER_ID\",\"REPO_ID\",\"MARK_TIME\") "
                    + "SELECT lower(hex(randomblob(16))),'user',\"LOGIN\",NULL,\"MARK_TIME\" "
                    + "FROM \"BOOK_MARK_USER\";");
        }
    };

    /**
     * v4 -> v5: add MY_TRENDING_LANGUAGE, and converge onto the full modern
     * 10-table schema by ensuring every table the Room entities own exists
     * (CREATE TABLE IF NOT EXISTS). This covers databases that reached v5 via an
     * old greenDAO upgrade path where some staging tables were dropped, so the
     * result always validates against the Room entities.
     */
    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS \"MY_TRENDING_LANGUAGE\" ("
                    + "\"SLUG\" TEXT PRIMARY KEY NOT NULL ,"
                    + "\"NAME\" TEXT NOT NULL ,"
                    + "\"ORDER\" INTEGER NOT NULL );");

            db.execSQL("CREATE TABLE IF NOT EXISTS \"AUTH_USER\" ("
                    + "\"ACCESS_TOKEN\" TEXT PRIMARY KEY NOT NULL ,"
                    + "\"AUTH_TIME\" INTEGER NOT NULL ,"
                    + "\"EXPIRE_IN\" INTEGER NOT NULL ,"
                    + "\"SCOPE\" TEXT NOT NULL ,"
                    + "\"SELECTED\" INTEGER NOT NULL ,"
                    + "\"LOGIN_ID\" TEXT NOT NULL ,"
                    + "\"NAME\" TEXT,"
                    + "\"AVATAR\" TEXT);");
            db.execSQL("CREATE TABLE IF NOT EXISTS \"BOOKMARK\" ("
                    + "\"ID\" TEXT PRIMARY KEY NOT NULL ,"
                    + "\"TYPE\" TEXT NOT NULL ,"
                    + "\"USER_ID\" TEXT,"
                    + "\"REPO_ID\" INTEGER,"
                    + "\"MARK_TIME\" INTEGER);");
            db.execSQL("CREATE TABLE IF NOT EXISTS \"BOOK_MARK_REPO\" ("
                    + "\"ID\" INTEGER PRIMARY KEY NOT NULL ,"
                    + "\"NAME\" TEXT NOT NULL ,"
                    + "\"DESCRIPTION\" TEXT,"
                    + "\"LANGUAGE\" TEXT,"
                    + "\"STARGAZERS_COUNT\" INTEGER,"
                    + "\"WATCHERS_COUNT\" INTEGER,"
                    + "\"FORKS_COUNT\" INTEGER,"
                    + "\"FORK\" INTEGER,"
                    + "\"OWNER_LOGIN\" TEXT,"
                    + "\"OWNER_AVATAR_URL\" TEXT,"
                    + "\"MARK_TIME\" INTEGER);");
            db.execSQL("CREATE TABLE IF NOT EXISTS \"BOOK_MARK_USER\" ("
                    + "\"LOGIN\" TEXT PRIMARY KEY NOT NULL ,"
                    + "\"NAME\" TEXT,"
                    + "\"AVATAR_URL\" TEXT,"
                    + "\"FOLLOWERS\" INTEGER,"
                    + "\"FOLLOWING\" INTEGER,"
                    + "\"MARK_TIME\" INTEGER);");
            db.execSQL("CREATE TABLE IF NOT EXISTS \"LOCAL_USER\" ("
                    + "\"LOGIN\" TEXT PRIMARY KEY NOT NULL ,"
                    + "\"NAME\" TEXT,"
                    + "\"AVATAR_URL\" TEXT,"
                    + "\"FOLLOWERS\" INTEGER,"
                    + "\"FOLLOWING\" INTEGER);");
            db.execSQL("CREATE TABLE IF NOT EXISTS \"LOCAL_REPO\" ("
                    + "\"ID\" INTEGER PRIMARY KEY NOT NULL ,"
                    + "\"NAME\" TEXT NOT NULL ,"
                    + "\"DESCRIPTION\" TEXT,"
                    + "\"LANGUAGE\" TEXT,"
                    + "\"STARGAZERS_COUNT\" INTEGER,"
                    + "\"WATCHERS_COUNT\" INTEGER,"
                    + "\"FORKS_COUNT\" INTEGER,"
                    + "\"FORK\" INTEGER,"
                    + "\"OWNER_LOGIN\" TEXT,"
                    + "\"OWNER_AVATAR_URL\" TEXT);");
            db.execSQL("CREATE TABLE IF NOT EXISTS \"TRACE\" ("
                    + "\"ID\" TEXT PRIMARY KEY NOT NULL ,"
                    + "\"TYPE\" TEXT,"
                    + "\"USER_ID\" TEXT,"
                    + "\"REPO_ID\" INTEGER,"
                    + "\"START_TIME\" INTEGER,"
                    + "\"LATEST_TIME\" INTEGER,"
                    + "\"TRACE_NUM\" INTEGER);");
            db.execSQL("CREATE TABLE IF NOT EXISTS \"TRACE_REPO\" ("
                    + "\"ID\" INTEGER PRIMARY KEY NOT NULL ,"
                    + "\"NAME\" TEXT NOT NULL ,"
                    + "\"DESCRIPTION\" TEXT,"
                    + "\"LANGUAGE\" TEXT,"
                    + "\"STARGAZERS_COUNT\" INTEGER,"
                    + "\"WATCHERS_COUNT\" INTEGER,"
                    + "\"FORKS_COUNT\" INTEGER,"
                    + "\"FORK\" INTEGER,"
                    + "\"OWNER_LOGIN\" TEXT,"
                    + "\"OWNER_AVATAR_URL\" TEXT,"
                    + "\"START_TIME\" INTEGER,"
                    + "\"LATEST_TIME\" INTEGER,"
                    + "\"TRACE_NUM\" INTEGER);");
            db.execSQL("CREATE TABLE IF NOT EXISTS \"TRACE_USER\" ("
                    + "\"LOGIN\" TEXT PRIMARY KEY NOT NULL ,"
                    + "\"NAME\" TEXT,"
                    + "\"AVATAR_URL\" TEXT,"
                    + "\"FOLLOWERS\" INTEGER,"
                    + "\"FOLLOWING\" INTEGER,"
                    + "\"START_TIME\" INTEGER,"
                    + "\"LATEST_TIME\" INTEGER,"
                    + "\"TRACE_NUM\" INTEGER);");
        }
    };

    /**
     * v5 -> v6: add the per-account bookmark column BOOKMARK.OWNER_LOGIN_ID and
     * backfill existing rows with the currently selected account's login id
     * (AUTH_USER.LOGIN_ID where SELECTED = 1). The column is nullable TEXT
     * (ALTER TABLE ADD COLUMN cannot add NOT NULL without a default), matching the
     * entity field.
     */
    public static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE \"BOOKMARK\" ADD COLUMN \"OWNER_LOGIN_ID\" TEXT;");
            db.execSQL("UPDATE \"BOOKMARK\" SET \"OWNER_LOGIN_ID\" = "
                    + "(SELECT \"LOGIN_ID\" FROM \"AUTH_USER\" WHERE \"SELECTED\" = 1 LIMIT 1) "
                    + "WHERE \"OWNER_LOGIN_ID\" IS NULL;");
        }
    };

    /** All migrations in ascending order, for wiring into the database builder. */
    public static final Migration[] ALL = {
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
    };
}
