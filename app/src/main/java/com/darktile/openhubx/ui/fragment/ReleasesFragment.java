package com.darktile.openhubx.ui.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import android.view.View;

import com.darktile.openhubx.R;
import com.darktile.openhubx.inject.component.AppComponent;
import com.darktile.openhubx.inject.component.DaggerFragmentComponent;
import com.darktile.openhubx.inject.module.FragmentModule;
import com.darktile.openhubx.mvp.contract.IReleasesContract;
import com.darktile.openhubx.mvp.model.Release;
import com.darktile.openhubx.mvp.presenter.ReleasesPresenter;
import com.darktile.openhubx.ui.activity.ReleaseInfoActivity;
import com.darktile.openhubx.ui.adapter.ReleasesAdapter;
import com.darktile.openhubx.ui.fragment.base.ListFragment;
import com.darktile.openhubx.ui.widget.DownloadSourceDialog;
import com.darktile.openhubx.util.BundleHelper;
import com.darktile.openhubx.util.PrefUtils;
import com.darktile.openhubx.util.StringUtils;

import java.util.ArrayList;

/**
 * Created by ThirtyDegreesRay on 2017/9/16 11:33:07
 */

public class ReleasesFragment extends ListFragment<ReleasesPresenter, ReleasesAdapter>
        implements IReleasesContract.View{

    public static ReleasesFragment create(String owner, String repo){
        ReleasesFragment fragment = new ReleasesFragment();
        fragment.setArguments(BundleHelper.builder().put("owner", owner).put("repo", repo).build());
        return fragment;
    }

    @Override
    public void showReleases(ArrayList<Release> releases) {
        adapter.setData(releases);
        postNotifyDataSetChanged();
        if(getCurPage() == 1 && !StringUtils.isBlankList(releases)
                && PrefUtils.isReleasesLongClickTipAble()){
            showOperationTip(R.string.releases_click_tip);
            PrefUtils.set(PrefUtils.RELEASES_LONG_CLICK_TIP_ABLE, false);
        }
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
        mPresenter.loadReleases(1, true);
    }

    @Override
    protected String getEmptyTip() {
        return getString(R.string.no_releases);
    }

    @Override
    public void onItemClick(int position, @NonNull View view) {
        super.onItemClick(position, view);
        ReleaseInfoActivity.show(getActivity(), mPresenter.getOwner(), mPresenter.getRepoName(),
                adapter.getData().get(position));
    }

    @Override
    public boolean onItemLongClick(int position, @NonNull View view) {
        Release release = adapter.getData().get(position);
        DownloadSourceDialog.show(getActivity(), mPresenter.getRepoName(),
                release.getTagName(), release);
        return true;
    }

    @Override
    protected void onLoadMore(int page) {
        super.onLoadMore(page);
        mPresenter.loadReleases(page, false);
    }
}
