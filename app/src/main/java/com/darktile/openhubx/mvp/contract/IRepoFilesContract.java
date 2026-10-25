

package com.darktile.openhubx.mvp.contract;

import androidx.annotation.NonNull;

import com.darktile.openhubx.mvp.contract.base.IBaseContract;
import com.darktile.openhubx.mvp.contract.base.IBaseListContract;
import com.darktile.openhubx.mvp.contract.base.IBasePagerContract;
import com.darktile.openhubx.mvp.model.FileModel;
import com.darktile.openhubx.mvp.model.FilePath;

import java.util.ArrayList;

/**
 * Created by ThirtyDegreesRay on 2017/8/14 16:02:28
 */

public interface IRepoFilesContract {

    interface View extends IBaseContract.View, IBasePagerContract.View, IBaseListContract.View{
        void showFiles(ArrayList<FileModel> files);
        void showFilePath(ArrayList<FilePath> filePath);
    }

    interface Presenter extends IBasePagerContract.Presenter<IRepoFilesContract.View>{
        void loadFiles(boolean isReload);
        void loadFiles(@NonNull String path, boolean isReload);
        boolean goBack();
        void goHome();
    }

}
