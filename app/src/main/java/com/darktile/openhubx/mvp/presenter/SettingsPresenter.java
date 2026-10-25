

package com.darktile.openhubx.mvp.presenter;

import com.darktile.openhubx.AppData;
import com.darktile.openhubx.dao.DaoSession;
import com.darktile.openhubx.mvp.contract.ISettingsContract;
import com.darktile.openhubx.mvp.presenter.base.BasePresenter;

import javax.inject.Inject;

/**
 * Created on 2017/8/1.
 *
 * @author ThirtyDegreesRay
 */

public class SettingsPresenter extends BasePresenter<ISettingsContract.View>
        implements ISettingsContract.Presenter{

    @Inject
    public SettingsPresenter(DaoSession daoSession) {
        super(daoSession);
    }

    @Override
    public void logout() {
        daoSession.getAuthUserDao().delete(AppData.INSTANCE.getAuthUser());
        AppData.INSTANCE.setAuthUser(null);
        AppData.INSTANCE.setLoggedUser(null);
        mView.showLoginPage();
    }

}
