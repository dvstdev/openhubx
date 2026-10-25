package com.darktile.openhubx.mvp.presenter;

import com.darktile.dataautoaccess.annotation.AutoAccess;
import com.darktile.openhubx.AppData;
import com.darktile.openhubx.R;
import com.darktile.openhubx.common.Event;
import com.darktile.openhubx.dao.DaoSession;
import com.darktile.openhubx.http.core.HttpObserver;
import com.darktile.openhubx.http.core.HttpResponse;
import com.darktile.openhubx.http.error.HttpPageNoFoundError;
import com.darktile.openhubx.mvp.contract.ICodeSearchContract;
import com.darktile.openhubx.mvp.model.CodeSearchItem;
import com.darktile.openhubx.mvp.model.SearchModel;
import com.darktile.openhubx.mvp.model.SearchResult;
import com.darktile.openhubx.mvp.presenter.base.BasePagerPresenter;
import com.darktile.openhubx.util.StringUtils;

import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;

import javax.inject.Inject;

/** GET search/code. GitHub requires an authenticated user for code search. */
public class CodeSearchPresenter extends BasePagerPresenter<ICodeSearchContract.View>
        implements ICodeSearchContract.Presenter {

    @AutoAccess SearchModel searchModel;

    private ArrayList<CodeSearchItem> items;

    @Inject
    public CodeSearchPresenter(DaoSession daoSession) {
        super(daoSession);
    }

    @Override
    public void onViewInitialized() {
        super.onViewInitialized();
        setEventSubscriber(true);
    }

    @Override
    protected void loadData() {
        if (searchModel != null) loadCode(1);
    }

    @Override
    public void loadCode(final int page) {
        if (searchModel == null) return;
        if (StringUtils.isBlank(AppData.INSTANCE.getAccessToken())) {
            mView.hideLoading();
            mView.showLoadError(getString(R.string.code_search_login_required));
            return;
        }
        mView.showLoading();
        HttpObserver<SearchResult<CodeSearchItem>> httpObserver =
                new HttpObserver<SearchResult<CodeSearchItem>>() {
                    @Override
                    public void onError(Throwable error) {
                        mView.hideLoading();
                        if (!StringUtils.isBlankList(items)) {
                            mView.showErrorToast(getErrorTip(error));
                        } else if (error instanceof HttpPageNoFoundError) {
                            mView.showCodeResults(new ArrayList<>());
                        } else {
                            mView.showLoadError(getErrorTip(error));
                        }
                    }

                    @Override
                    public void onSuccess(HttpResponse<SearchResult<CodeSearchItem>> response) {
                        mView.hideLoading();
                        ArrayList<CodeSearchItem> result = response.body().getItems();
                        if (result == null) result = new ArrayList<>();
                        if (items == null || page == 1) {
                            items = result;
                        } else {
                            items.addAll(result);
                        }
                        if (result.size() == 0 && items.size() != 0) {
                            mView.setCanLoadMore(false);
                        } else {
                            mView.showCodeResults(items);
                        }
                    }
                };
        final String queryStr = searchModel.getApiQuery();
        generalRxHttpExecute(forceNetWork -> getSearchService().searchCode(queryStr, page), httpObserver);
    }

    @Subscribe
    public void onSearchEvent(Event.SearchEvent searchEvent) {
        if (!SearchModel.SearchType.Code.equals(searchEvent.searchModel.getType())) return;
        setLoaded(false);
        this.searchModel = searchEvent.searchModel;
        prepareLoadData();
    }
}
