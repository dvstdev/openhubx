package com.darktile.openhubx.mvp.contract;

import com.darktile.openhubx.mvp.contract.base.IBaseContract;
import com.darktile.openhubx.mvp.contract.base.IBaseListContract;
import com.darktile.openhubx.mvp.model.Collection;

import java.util.ArrayList;

/**
 * Created by ThirtyDegreesRay on 2017/12/25 15:12:30
 */

public interface ICollectionsContract {

    interface View extends IBaseContract.View, IBaseListContract.View{
        void showCollections(ArrayList<Collection> collections);
    }

    interface Presenter extends IBaseContract.Presenter<ICollectionsContract.View>{
        void loadCollections(boolean isReload);
    }

}
