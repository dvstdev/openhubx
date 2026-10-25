package com.darktile.openhubx.mvp.contract.base;

/**
 * Created by ThirtyDegreesRay on 2017/9/22 10:47:52
 */

public interface IBaseListContract {

    interface View {
        void showLoadError(String errorMsg);

        /** Shows an informative message with no retry button (e.g. a permission limit). */
        void showLoadHint(String hint);

        void setCanLoadMore(boolean canLoadMore);
    }

}
