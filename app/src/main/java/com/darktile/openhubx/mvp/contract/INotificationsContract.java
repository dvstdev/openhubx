

package com.darktile.openhubx.mvp.contract;

import androidx.annotation.NonNull;

import com.darktile.openhubx.mvp.contract.base.IBaseContract;
import com.darktile.openhubx.mvp.contract.base.IBaseListContract;
import com.darktile.openhubx.mvp.contract.base.IBasePagerContract;
import com.darktile.openhubx.mvp.model.Notification;
import com.darktile.openhubx.mvp.model.Repository;
import com.darktile.openhubx.ui.adapter.base.DoubleTypesModel;

import java.util.ArrayList;

/**
 * Created by ThirtyDegreesRay on 2017/11/6 17:44:57
 */

public interface INotificationsContract {

    interface View extends IBaseContract.View, IBasePagerContract.View, IBaseListContract.View{
        void showNotifications(ArrayList<DoubleTypesModel<Repository, Notification>> notifications);
    }

    interface Presenter extends IBasePagerContract.Presenter<INotificationsContract.View> {
        void loadNotifications(int page, boolean isReload);
        void markNotificationAsRead(String threadId);
        void markAllNotificationsAsRead();
        boolean isNotificationsAllRead();
        void markRepoNotificationsAsRead(@NonNull Repository repository);
    }

}
