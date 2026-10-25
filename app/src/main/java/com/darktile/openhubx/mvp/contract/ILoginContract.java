

package com.darktile.openhubx.mvp.contract;

import android.content.Intent;

import com.darktile.openhubx.mvp.contract.base.IBaseContract;
import com.darktile.openhubx.mvp.model.BasicToken;

/**
 * Created on 2017/7/12.
 *
 * @author ThirtyDegreesRay
 */

public interface ILoginContract {

    interface View extends IBaseContract.View{

        void onGetTokenSuccess(BasicToken basicToken);

        void onGetTokenError(String errorMsg);

        void onLoginComplete();

        void onQrTokenRejected();

    }

    interface Presenter extends IBaseContract.Presenter<ILoginContract.View>{

        void getToken(String code, String state);

        String getOAuth2Url();

        void basicLogin(String userName, String password);

        void handleOauth(Intent intent);

        void getUserInfo(BasicToken basicToken);

        void loginWithToken(BasicToken basicToken);

    }

}
