package com.darktile.openhubx.ui.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.View;

import com.darktile.openhubx.R;
import com.darktile.openhubx.inject.component.AppComponent;
import com.darktile.openhubx.inject.component.DaggerFragmentComponent;
import com.darktile.openhubx.inject.module.FragmentModule;
import com.darktile.openhubx.mvp.contract.ITopicsContract;
import com.darktile.openhubx.mvp.model.Topic;
import com.darktile.openhubx.mvp.presenter.TopicsPresenter;
import com.darktile.openhubx.ui.activity.RepoListActivity;
import com.darktile.openhubx.ui.adapter.TopicsAdapter;
import com.darktile.openhubx.ui.fragment.base.ListFragment;
import com.darktile.openhubx.util.PrefUtils;

import java.util.ArrayList;

/**
 * Created by ThirtyDegreesRay on 2017/12/29 11:12:41
 */

public class TopicsFragment extends ListFragment<TopicsPresenter, TopicsAdapter>
        implements ITopicsContract.View {

    public static Fragment create(){
        return new TopicsFragment();
    }

    @Override
    protected void initFragment(Bundle savedInstanceState) {
        super.initFragment(savedInstanceState);
        setLoadMoreEnable(false);
    }

    @Override
    public void showTopics(ArrayList<Topic> topics) {
        adapter.setData(topics);
        postNotifyDataSetChanged();
        if(topics != null && topics.size() > 0 && PrefUtils.isTopicsTipEnable()){
            showOperationTip(R.string.topics_tip);
            PrefUtils.set(PrefUtils.TOPICS_TIP_ABLE, false);
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
    protected void onReLoadData() {
        mPresenter.loadTopics(true);
    }

    @Override
    protected String getEmptyTip() {
        return getString(R.string.no_topics);
    }

    @Override
    public void onItemClick(int position, @NonNull View view) {
        super.onItemClick(position, view);
        RepoListActivity.showTopic(getActivity(), adapter.getData().get(position));
    }
}
