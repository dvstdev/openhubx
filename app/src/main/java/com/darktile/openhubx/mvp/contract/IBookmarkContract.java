package com.darktile.openhubx.mvp.contract;

import com.darktile.openhubx.mvp.contract.base.IBaseContract;
import com.darktile.openhubx.mvp.contract.base.IBaseListContract;
import com.darktile.openhubx.mvp.model.BookmarkExt;

import java.util.ArrayList;

/**
 * Created by ThirtyDegreesRay on 2017/11/22 15:58:04
 */

public interface IBookmarkContract {

    interface View extends IBaseContract.View, IBaseListContract.View{
        void showBookmarks(ArrayList<BookmarkExt> bookmarks);
        void notifyItemAdded(int position);
    }

    interface Presenter extends IBaseContract.Presenter<IBookmarkContract.View>{
        void loadBookmarks(int page);
        void removeBookmark(int position);
        void undoRemoveBookmark();
    }

}
