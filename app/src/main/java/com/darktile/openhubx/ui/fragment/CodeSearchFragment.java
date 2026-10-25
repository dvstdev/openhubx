package com.darktile.openhubx.ui.fragment;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;

import com.darktile.openhubx.R;
import com.darktile.openhubx.inject.component.AppComponent;
import com.darktile.openhubx.inject.component.DaggerFragmentComponent;
import com.darktile.openhubx.inject.module.FragmentModule;
import com.darktile.openhubx.mvp.contract.ICodeSearchContract;
import com.darktile.openhubx.mvp.model.CodeSearchItem;
import com.darktile.openhubx.mvp.model.SearchModel;
import com.darktile.openhubx.mvp.presenter.CodeSearchPresenter;
import com.darktile.openhubx.ui.activity.ViewerActivity;
import com.darktile.openhubx.ui.adapter.CodeSearchAdapter;
import com.darktile.openhubx.ui.fragment.base.ListFragment;
import com.darktile.openhubx.util.BundleHelper;
import com.darktile.openhubx.util.StringUtils;

import java.util.ArrayList;

/** Code tab of the search screen. */
public class CodeSearchFragment extends ListFragment<CodeSearchPresenter, CodeSearchAdapter>
        implements ICodeSearchContract.View {

    public static CodeSearchFragment createForSearch(@NonNull SearchModel searchModel) {
        CodeSearchFragment fragment = new CodeSearchFragment();
        fragment.setArguments(BundleHelper.builder().put("searchModel", searchModel).build());
        return fragment;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_list;
    }

    @Override
    protected void setupFragmentComponent(AppComponent appComponent) {
        DaggerFragmentComponent.builder()
                .appComponent(appComponent)
                .fragmentModule(new FragmentModule(this))
                .build()
                .inject(this);
    }

    @Override
    protected void initFragment(Bundle savedInstanceState) {
        super.initFragment(savedInstanceState);
        setLoadMoreEnable(true);
    }

    @Override
    protected void onReLoadData() {
        mPresenter.loadCode(1);
    }

    @Override
    protected String getEmptyTip() {
        return getString(R.string.no_code_results);
    }

    @Override
    public void showCodeResults(ArrayList<CodeSearchItem> items) {
        adapter.setData(items);
        postNotifyDataSetChanged();
    }

    @Override
    protected void onLoadMore(int page) {
        super.onLoadMore(page);
        mPresenter.loadCode(page);
    }

    @Override
    public void onItemClick(int position, @NonNull View view) {
        super.onItemClick(position, view);
        CodeSearchItem item = adapter.getData().get(position);
        if (StringUtils.isBlank(item.getUrl())) return;
        ViewerActivity.show(getActivity(), item.getUrl(), item.getHtmlUrl());
    }

    @Override
    public void onFragmentShowed() {
        super.onFragmentShowed();
        if (mPresenter != null) mPresenter.prepareLoadData();
    }
}
