

package com.darktile.openhubx.mvp.contract;

import com.darktile.openhubx.mvp.contract.base.IBaseContract;
import com.darktile.openhubx.mvp.model.CommitFile;
import com.darktile.openhubx.mvp.model.CommitFilesPathModel;
import com.darktile.openhubx.ui.adapter.base.DoubleTypesModel;

import java.util.ArrayList;

/**
 * Created by ThirtyDegreesRay on 2017/10/18 15:21:28
 */

public interface ICommitFilesContract {

    interface View extends IBaseContract.View{

    }

    interface Presenter extends IBaseContract.Presenter<ICommitFilesContract.View>{
        ArrayList<DoubleTypesModel<CommitFilesPathModel, CommitFile>> getSortedList(ArrayList<CommitFile> commitFiles);
    }

}
