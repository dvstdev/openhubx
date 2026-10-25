package com.darktile.openhubx.mvp.contract;

import com.darktile.openhubx.mvp.contract.base.IBaseContract;
import com.darktile.openhubx.mvp.contract.base.IBaseListContract;
import com.darktile.openhubx.mvp.model.WikiModel;

import java.util.ArrayList;

/**
 * Created by ThirtyDegreesRay on 2017/12/6 16:34:22
 */

public interface IWikiContract {

    interface View extends IBaseContract.View, IBaseListContract.View{
        void showWiki(ArrayList<WikiModel> wikiList);
    }

    interface Presenter extends IBaseContract.Presenter<IWikiContract.View>{
        void loadWiki(boolean isReload);
    }

}
