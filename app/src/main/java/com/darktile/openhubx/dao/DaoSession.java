package com.darktile.openhubx.dao;

import java.util.concurrent.Callable;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Room-backed replacement for greenDAO's generated DaoSession. Keeps the same
 * type and getXxxDao() surface so injected presenters are unchanged, and adds
 * transaction helpers (runInTx / rxTx) matching the greenDAO API the presenters
 * relied on.
 */
public class DaoSession {

    private final OpenHubRoomDatabase db;

    public DaoSession(OpenHubRoomDatabase db) {
        this.db = db;
    }

    public AuthUserDao getAuthUserDao() {
        return db.authUserDao();
    }

    public BookmarkDao getBookmarkDao() {
        return db.bookmarkDao();
    }

    public BookMarkRepoDao getBookMarkRepoDao() {
        return db.bookMarkRepoDao();
    }

    public BookMarkUserDao getBookMarkUserDao() {
        return db.bookMarkUserDao();
    }

    public LocalRepoDao getLocalRepoDao() {
        return db.localRepoDao();
    }

    public LocalUserDao getLocalUserDao() {
        return db.localUserDao();
    }

    public MyTrendingLanguageDao getMyTrendingLanguageDao() {
        return db.myTrendingLanguageDao();
    }

    public TraceDao getTraceDao() {
        return db.traceDao();
    }

    public TraceRepoDao getTraceRepoDao() {
        return db.traceRepoDao();
    }

    public TraceUserDao getTraceUserDao() {
        return db.traceUserDao();
    }

    /** Runs the work synchronously inside a database transaction. */
    public void runInTx(Runnable runnable) {
        db.runInTransaction(runnable);
    }

    /** greenDAO-style reactive transaction wrapper (runs on the io scheduler). */
    public RxTransaction rxTx() {
        return new RxTransaction(db);
    }

    /** Minimal stand-in for greenDAO's RxTransaction: run(Runnable) -> Observable. */
    public static final class RxTransaction {

        private final OpenHubRoomDatabase db;

        RxTransaction(OpenHubRoomDatabase db) {
            this.db = db;
        }

        public Observable<Object> run(final Runnable runnable) {
            return Observable.fromCallable(new Callable<Object>() {
                @Override
                public Object call() {
                    db.runInTransaction(runnable);
                    // RxJava 3 disallows null emissions; emit a non-null sentinel.
                    return Boolean.TRUE;
                }
            }).subscribeOn(Schedulers.io());
        }
    }
}
