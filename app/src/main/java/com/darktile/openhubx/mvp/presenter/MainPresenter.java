

package com.darktile.openhubx.mvp.presenter;

import androidx.annotation.NonNull;

import com.darktile.openhubx.AppData;
import com.darktile.openhubx.dao.AuthUser;
import com.darktile.openhubx.dao.AuthUserDao;
import com.darktile.openhubx.dao.DaoSession;
import com.darktile.openhubx.mvp.contract.IMainContract;
import com.darktile.openhubx.mvp.model.User;
import com.darktile.openhubx.mvp.presenter.base.BasePresenter;
import com.darktile.openhubx.util.PrefUtils;

import java.util.List;

import javax.inject.Inject;

/**
 * Created on 2017/7/18.
 *
 * @author ThirtyDegreesRay
 */

public class MainPresenter extends BasePresenter<IMainContract.View>
        implements IMainContract.Presenter{

    @Inject
    public MainPresenter(DaoSession daoSession) {
        super(daoSession);
    }

    @Override
    public boolean isFirstUseAndNoNewsUser() {
        User user = AppData.INSTANCE.getLoggedUser();
        if(user.getFollowing() == 0
                && user.getPublicRepos() == 0 && user.getPublicGists() == 0
                && PrefUtils.isFirstUse()){
            PrefUtils.set(PrefUtils.FIRST_USE, false);
            return true;
        }
        return false;
    }

    @Override
    public List<AuthUser> getLoggedUserList() {
        List<AuthUser> users = daoSession.getAuthUserDao().loadAll();
        if(users != null){
            for(AuthUser user : users){
                if(AppData.INSTANCE.getLoggedUser().getLogin().equals(user.getLoginId())){
                    users.remove(user);
                    break;
                }
            }
        }
        return users;
    }

    @Override
    public void toggleAccount(@NonNull String loginId) {
        daoSession.getAuthUserDao().clearAllSelected();
        daoSession.getAuthUserDao().setSelectedByLoginId(loginId);
        AppData.INSTANCE.setAuthUser(null);
        AppData.INSTANCE.setLoggedUser(null);
        mView.restartApp();
    }

    @Override
    public void logout() {
        daoSession.getAuthUserDao().delete(AppData.INSTANCE.getAuthUser());
        AppData.INSTANCE.setAuthUser(null);
        AppData.INSTANCE.setLoggedUser(null);
        mView.restartApp();
    }

}
