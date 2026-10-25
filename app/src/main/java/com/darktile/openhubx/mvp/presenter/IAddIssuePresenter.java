package com.darktile.openhubx.mvp.presenter;

import com.darktile.openhubx.dao.DaoSession;
import com.darktile.openhubx.mvp.contract.IAddIssueContract;
import com.darktile.openhubx.mvp.presenter.base.BasePresenter;

import javax.inject.Inject;

/**
 * Created by ThirtyDegreesRay on 2017/9/26 16:56:35
 */

public class IAddIssuePresenter extends BasePresenter<IAddIssueContract.View>
        implements IAddIssueContract.Presenter{

    @Inject
    public IAddIssuePresenter(DaoSession daoSession) {
        super(daoSession);
    }

}
