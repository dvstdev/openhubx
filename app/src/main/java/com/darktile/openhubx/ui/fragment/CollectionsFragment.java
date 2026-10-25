package com.darktile.openhubx.ui.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.View;

import com.darktile.openhubx.R;
import com.darktile.openhubx.inject.component.AppComponent;
import com.darktile.openhubx.inject.component.DaggerFragmentComponent;
import com.darktile.openhubx.inject.module.FragmentModule;
import com.darktile.openhubx.mvp.contract.ICollectionsContract;
import com.darktile.openhubx.mvp.model.Collection;
import com.darktile.openhubx.mvp.presenter.CollectionsPresenter;
import com.darktile.openhubx.ui.activity.RepoListActivity;
import com.darktile.openhubx.ui.adapter.CollectionAdapter;
import com.darktile.openhubx.ui.fragment.base.ListFragment;
import com.darktile.openhubx.util.PrefUtils;

import java.util.ArrayList;

/**
 * Created by ThirtyDegreesRay on 2017/12/25 15:17:38
 */

public class CollectionsFragment extends ListFragment<CollectionsPresenter, CollectionAdapter>
        implements ICollectionsContract.View {

    public static Fragment create(){
        return new CollectionsFragment();
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
        setLoadMoreEnable(false);
    }

    @Override
    protected void onReLoadData() {
        mPresenter.loadCollections(true);
    }

    @Override
    protected String getEmptyTip() {
        return getString(R.string.no_repo_collections);
    }

    @Override
    public void showCollections(ArrayList<Collection> collections) {
        adapter.setData(collections);
        postNotifyDataSetChanged();
        if(collections != null && collections.size() > 0 && PrefUtils.isCollectionsTipAble()){
            showOperationTip(R.string.collections_tip);
            PrefUtils.set(PrefUtils.COLLECTIONS_TIP_ABLE, false);
        }
    }

    @Override
    public void onItemClick(int position, @NonNull View view) {
        super.onItemClick(position, view);
        RepoListActivity.showCollection(getActivity(), adapter.getData().get(position));
    }
}
