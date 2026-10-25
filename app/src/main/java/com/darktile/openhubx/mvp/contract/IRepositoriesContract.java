

package com.darktile.openhubx.mvp.contract;

import com.darktile.openhubx.mvp.contract.base.IBaseContract;
import com.darktile.openhubx.mvp.contract.base.IBaseListContract;
import com.darktile.openhubx.mvp.contract.base.IBasePagerContract;
import com.darktile.openhubx.mvp.model.Repository;
import com.darktile.openhubx.mvp.model.filter.RepositoriesFilter;

import java.util.ArrayList;

/**
 * Created on 2017/7/18.
 *
 * @author ThirtyDegreesRay
 */

public interface IRepositoriesContract {

    interface View extends IBaseContract.View, IBasePagerContract.View, IBaseListContract.View {

        void showRepositories(ArrayList<Repository> repositoryList);

    }

    interface Presenter extends IBasePagerContract.Presenter<IRepositoriesContract.View> {
        void loadRepositories(boolean isReLoad, int page);
        void loadRepositories(RepositoriesFilter filter);
    }

}
