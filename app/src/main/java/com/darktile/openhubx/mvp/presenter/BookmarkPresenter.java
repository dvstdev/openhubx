package com.darktile.openhubx.mvp.presenter;

import com.darktile.openhubx.dao.Bookmark;
import com.darktile.openhubx.dao.BookmarkDao;
import com.darktile.openhubx.dao.DaoSession;
import com.darktile.openhubx.dao.LocalRepo;
import com.darktile.openhubx.dao.LocalUser;
import com.darktile.openhubx.mvp.contract.IBookmarkContract;
import com.darktile.openhubx.mvp.model.BookmarkExt;
import com.darktile.openhubx.mvp.model.Repository;
import com.darktile.openhubx.mvp.model.User;
import com.darktile.openhubx.mvp.presenter.base.BasePresenter;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

/**
 * Created by ThirtyDegreesRay on 2017/11/22 15:59:45
 */

public class BookmarkPresenter extends BasePresenter<IBookmarkContract.View>
        implements IBookmarkContract.Presenter {

    private ArrayList<BookmarkExt> bookmarks;
    private BookmarkExt removedBookmark;
    private int removedPosition;

    @Inject
    public BookmarkPresenter(DaoSession daoSession) {
        super(daoSession);
    }

    public DaoSession getDaoSession() {
        return daoSession;
    }

    @Override
    public void onViewInitialized() {
        super.onViewInitialized();
        loadBookmarks(1);
    }

    @Override
    public void loadBookmarks(int page) {
        mView.showLoading();
        final ArrayList<BookmarkExt> tempBookmarks = new ArrayList<>();
        daoSession.rxTx()
                .run(() -> {
                    List<Bookmark> bookmarkList = daoSession.getBookmarkDao()
                            .loadPaged(page * 30, (page - 1) * 30);
                    for(Bookmark bookmark : bookmarkList){
                        BookmarkExt ext = "user".equals(bookmark.getType())
                                ? rehydrate(bookmark, daoSession.getLocalUserDao().load(bookmark.getUserId()), null)
                                : rehydrate(bookmark, null, daoSession.getLocalRepoDao().load(bookmark.getRepoId()));
                        // Skip orphaned bookmarks (missing LOCAL_* companion) rather than
                        // NPEing and blanking the whole list.
                        if(ext != null){
                            tempBookmarks.add(ext);
                        }
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aVoid -> {
                    if(bookmarks == null || page == 1){
                        bookmarks = tempBookmarks;
                    } else {
                        bookmarks.addAll(tempBookmarks);
                    }
                    mView.showBookmarks(bookmarks);
                    mView.hideLoading();
                }, throwable -> {
                    // Never let an unexpected DB error blank the list silently: recover
                    // the UI with whatever loaded rather than leaving it stuck on loading.
                    if(bookmarks == null || page == 1){
                        bookmarks = tempBookmarks;
                    } else {
                        bookmarks.addAll(tempBookmarks);
                    }
                    mView.showBookmarks(bookmarks);
                    mView.hideLoading();
                });

    }

    /**
     * Rehydrates a bookmark row into a display model, or returns null when its
     * companion LOCAL_USER / LOCAL_REPO row is missing (orphaned bookmark) so the
     * caller skips it instead of crashing the whole list.
     */
    static BookmarkExt rehydrate(Bookmark bookmark, LocalUser localUser, LocalRepo localRepo){
        BookmarkExt ext = BookmarkExt.generate(bookmark);
        if("user".equals(bookmark.getType())){
            if(localUser == null) return null;
            ext.setUser(User.generateFromLocalUser(localUser));
        }else{
            if(localRepo == null) return null;
            ext.setRepository(Repository.generateFromLocalRepo(localRepo));
        }
        return ext;
    }

    @Override
    public void removeBookmark(int position) {
        removedBookmark = bookmarks.remove(position);
        removedPosition = position;
        rxDBExecute(() -> daoSession.getBookmarkDao().deleteByKey(removedBookmark.getId()));
    }

    @Override
    public void undoRemoveBookmark() {
        bookmarks.add(removedPosition, removedBookmark);
        mView.notifyItemAdded(removedPosition);
        rxDBExecute(() -> daoSession.getBookmarkDao().insert(removedBookmark));
    }

}
