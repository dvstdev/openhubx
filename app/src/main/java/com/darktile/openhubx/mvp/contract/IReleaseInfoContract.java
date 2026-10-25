package com.darktile.openhubx.mvp.contract;

import com.darktile.openhubx.mvp.contract.base.IBaseContract;
import com.darktile.openhubx.mvp.model.Release;

/**
 * Created by ThirtyDegreesRay on 2017/9/16 13:09:26
 */

public interface IReleaseInfoContract {

    interface View extends IBaseContract.View{
        void showReleaseInfo(Release release);
    }

    interface Presenter extends IBaseContract.Presenter<IReleaseInfoContract.View>{

    }

}
