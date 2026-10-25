package com.darktile.openhubx.dao;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.darktile.openhubx.AppConfig;

/**
 * Room database backing the former greenDAO schema. Version 6 = greenDAO's v5
 * on-disk schema plus the per-account bookmark column; the existing "OpenHub.db"
 * file is adopted in place via the ported {@link Migrations}.
 */
@Database(
        entities = {
                AuthUser.class,
                Bookmark.class,
                BookMarkRepo.class,
                BookMarkUser.class,
                LocalRepo.class,
                LocalUser.class,
                MyTrendingLanguage.class,
                Trace.class,
                TraceRepo.class,
                TraceUser.class
        },
        version = 6,
        exportSchema = false
)
@TypeConverters(DateConverter.class)
public abstract class OpenHubRoomDatabase extends RoomDatabase {

    private static volatile OpenHubRoomDatabase instance;

    public abstract AuthUserDao authUserDao();

    public abstract BookmarkDao bookmarkDao();

    public abstract BookMarkRepoDao bookMarkRepoDao();

    public abstract BookMarkUserDao bookMarkUserDao();

    public abstract LocalRepoDao localRepoDao();

    public abstract LocalUserDao localUserDao();

    public abstract MyTrendingLanguageDao myTrendingLanguageDao();

    public abstract TraceDao traceDao();

    public abstract TraceRepoDao traceRepoDao();

    public abstract TraceUserDao traceUserDao();

    /** Opens (or returns the cached) database, adopting the greenDAO on-disk file. */
    public static OpenHubRoomDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (OpenHubRoomDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    OpenHubRoomDatabase.class,
                                    AppConfig.DB_NAME)
                            .addMigrations(Migrations.ALL)
                            // greenDAO performed synchronous DB access on the calling
                            // (often main) thread; call sites still rely on that, so keep
                            // the same semantics rather than forcing a threading rewrite.
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return instance;
    }
}
