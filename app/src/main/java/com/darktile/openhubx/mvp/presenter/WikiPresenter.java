package com.darktile.openhubx.mvp.presenter;

import com.darktile.dataautoaccess.annotation.AutoAccess;
import com.darktile.openhubx.dao.DaoSession;
import com.darktile.openhubx.http.core.HttpObserver;
import com.darktile.openhubx.http.core.HttpResponse;
import com.darktile.openhubx.mvp.contract.IWikiContract;
import com.darktile.openhubx.mvp.model.WikiFeedModel;
import com.darktile.openhubx.mvp.model.WikiModel;
import com.darktile.openhubx.mvp.presenter.base.BasePresenter;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;

import javax.inject.Inject;

import okhttp3.ResponseBody;

/**
 * Created by ThirtyDegreesRay on 2017/12/6 16:41:32
 */

public class WikiPresenter extends BasePresenter<IWikiContract.View>
        implements IWikiContract.Presenter {

    @AutoAccess String owner;
    @AutoAccess String repo;
    private ArrayList<WikiModel> wikiList;

    @Inject
    public WikiPresenter(DaoSession daoSession) {
        super(daoSession);
    }

    @Override
    public void onViewInitialized() {
        super.onViewInitialized();
        loadWiki(false);
    }

    @Override
    public void loadWiki(boolean isReload) {
        mView.showLoading();
        HttpObserver<ResponseBody> httpObserver = new HttpObserver<ResponseBody>() {
            @Override
            public void onError(Throwable error) {
                mView.hideLoading();
                if (error.getCause() != null && error.getCause() instanceof XmlPullParserException) {
                    mView.showWiki(null);
                } else {
                    mView.showLoadError(getErrorTip(error));
                }
            }

            @Override
            public void onSuccess(HttpResponse<ResponseBody> response) {
                mView.hideLoading();
                try {
                    WikiFeedModel feedModel = WikiFeedModel.fromAtom(response.body().string());
                    wikiList = feedModel.getWikiList();
                    mView.showWiki(wikiList);
                } catch (XmlPullParserException e) {
                    mView.showWiki(null);
                } catch (IOException e) {
                    mView.showLoadError(getErrorTip(e));
                }
            }
        };
        generalRxHttpExecute(forceNetWork -> getGitHubWebPageService()
                        .getWiki(forceNetWork, owner, repo)
                , httpObserver, !isReload);

    }

}
