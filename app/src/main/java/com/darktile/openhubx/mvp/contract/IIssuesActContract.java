package com.darktile.openhubx.mvp.contract;

import com.darktile.openhubx.mvp.contract.base.IBaseContract;

/**
 * Created by ThirtyDegreesRay on 2017/9/20 17:21:14
 */

public interface IIssuesActContract {

    interface View extends IBaseContract.View{

    }

    interface Presenter extends IBaseContract.Presenter<IIssuesActContract.View>{

    }

}
