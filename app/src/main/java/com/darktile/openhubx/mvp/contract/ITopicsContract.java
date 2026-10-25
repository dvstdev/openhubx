package com.darktile.openhubx.mvp.contract;

import com.darktile.openhubx.mvp.contract.base.IBaseContract;
import com.darktile.openhubx.mvp.contract.base.IBaseListContract;
import com.darktile.openhubx.mvp.model.Topic;

import java.util.ArrayList;

/**
 * Created by ThirtyDegreesRay on 2017/12/29 11:08:23
 */

public interface ITopicsContract {

    interface View extends IBaseContract.View, IBaseListContract.View{
        void showTopics(ArrayList<Topic> topics);
    }

    interface Presenter extends IBaseContract.Presenter<ITopicsContract.View>{
        void loadTopics(boolean isReload);
    }

}
