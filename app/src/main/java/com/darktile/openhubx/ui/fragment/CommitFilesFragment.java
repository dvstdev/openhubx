

package com.darktile.openhubx.ui.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import android.view.View;

import com.darktile.openhubx.R;
import com.darktile.openhubx.inject.component.AppComponent;
import com.darktile.openhubx.inject.component.DaggerFragmentComponent;
import com.darktile.openhubx.inject.module.FragmentModule;
import com.darktile.openhubx.mvp.model.CommitFile;
import com.darktile.openhubx.mvp.presenter.CommitFilesPresenter;
import com.darktile.openhubx.ui.activity.ViewerActivity;
import com.darktile.openhubx.ui.adapter.CommitFilesAdapter;
import com.darktile.openhubx.ui.fragment.base.ListFragment;
import com.darktile.openhubx.util.GitHubHelper;

import java.util.ArrayList;

/**
 * Created by ThirtyDegreesRay on 2017/10/18 14:38:13
 */

public class CommitFilesFragment extends ListFragment<CommitFilesPresenter, CommitFilesAdapter> {

    public static CommitFilesFragment create(@NonNull ArrayList<CommitFile> commitFiles){
        CommitFilesFragment fragment = new CommitFilesFragment();
        fragment.setCommitFiles(commitFiles);
        return fragment;
    }

    private ArrayList<CommitFile> commitFiles;

    public void setCommitFiles(ArrayList<CommitFile> commitFiles) {
        this.commitFiles = commitFiles;
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
    protected void onReLoadData() {

    }

    @Override
    protected String getEmptyTip() {
        return getString(R.string.no_file);
    }

    @Override
    protected void initFragment(Bundle savedInstanceState) {
        super.initFragment(savedInstanceState);
        setLoadMoreEnable(false);
        setRefreshEnable(false);
        if(commitFiles != null){
            adapter.setData(mPresenter.getSortedList(commitFiles));
            postNotifyDataSetChanged();
        }
    }

    @Override
    public void onItemClick(int position, @NonNull View view) {
        super.onItemClick(position, view);
        if(adapter.getData().get(position).getTypePosition() == 1){
            CommitFile commitFile = adapter.getData().get(position).getM2();
            if(GitHubHelper.isImage(commitFile.getFileName())){
                ViewerActivity.showImage(getActivity(), commitFile.getRawUrl());
            } else {
                ViewerActivity.showForDiff(getActivity(), commitFile);
            }
        }
    }

    public void showCommitFiles(@NonNull ArrayList<CommitFile> commitFiles){
        this.commitFiles = commitFiles;
        adapter.setData(mPresenter.getSortedList(commitFiles));
        postNotifyDataSetChanged();
    }

}
