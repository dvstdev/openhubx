package com.darktile.openhubx.mvp.contract;

import com.darktile.openhubx.mvp.contract.base.IBaseContract;
import com.darktile.openhubx.mvp.contract.base.IBaseListContract;
import com.darktile.openhubx.mvp.contract.base.IBasePagerContract;
import com.darktile.openhubx.mvp.model.CodeSearchItem;

import java.util.ArrayList;

/** Code search tab. */
public interface ICodeSearchContract {

    interface View extends IBaseContract.View, IBasePagerContract.View, IBaseListContract.View {
        void showCodeResults(ArrayList<CodeSearchItem> items);
    }

    interface Presenter extends IBasePagerContract.Presenter<ICodeSearchContract.View> {
        void loadCode(int page);
    }
}
