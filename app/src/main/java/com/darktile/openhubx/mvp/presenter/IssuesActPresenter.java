package com.darktile.openhubx.mvp.presenter;

import com.darktile.openhubx.dao.DaoSession;
import com.darktile.openhubx.mvp.contract.IIssuesActContract;
import com.darktile.openhubx.mvp.presenter.base.BasePresenter;

import javax.inject.Inject;

/**
 * Created by ThirtyDegreesRay on 2017/9/20 17:22:16
 */

public class IssuesActPresenter extends BasePresenter<IIssuesActContract.View>
        implements IIssuesActContract.Presenter{

    @Inject
    public IssuesActPresenter(DaoSession daoSession) {
        super(daoSession);
    }

}
