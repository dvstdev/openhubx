

package com.darktile.openhubx.mvp.contract;

import com.darktile.openhubx.mvp.contract.base.IBaseContract;
import com.darktile.openhubx.mvp.model.User;

/**
 * Created on 2017/7/18.
 *
 * @author ThirtyDegreesRay
 */

public interface IProfileContract {

    interface View extends IBaseContract.View{
        void showProfileInfo(User user);
        void invalidateOptionsMenu();
    }

    interface Presenter extends IBaseContract.Presenter<IProfileContract.View>{
        void followUser(boolean follow);
        boolean isBookmarked();
        void bookmark(boolean bookmark);
    }

}
