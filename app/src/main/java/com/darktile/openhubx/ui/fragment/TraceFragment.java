package com.darktile.openhubx.ui.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.snackbar.Snackbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.view.View;

import com.darktile.openhubx.R;
import com.darktile.openhubx.inject.component.AppComponent;
import com.darktile.openhubx.inject.component.DaggerFragmentComponent;
import com.darktile.openhubx.inject.module.FragmentModule;
import com.darktile.openhubx.mvp.contract.ITraceContract;
import com.darktile.openhubx.mvp.model.TraceExt;
import com.darktile.openhubx.mvp.presenter.TracePresenter;
import com.darktile.openhubx.ui.activity.ProfileActivity;
import com.darktile.openhubx.ui.activity.RepositoryActivity;
import com.darktile.openhubx.ui.adapter.TraceAdapter;
import com.darktile.openhubx.ui.adapter.base.ItemTouchHelperCallback;
import com.darktile.openhubx.ui.fragment.base.ListFragment;
import com.darktile.openhubx.ui.widget.stickyheader.StickyHeaderDecoration;

import java.util.ArrayList;

/**
 * Created by ThirtyDegreesRay on 2017/11/23 10:56:40
 */

public class TraceFragment extends ListFragment<TracePresenter, TraceAdapter>
        implements ITraceContract.View, ItemTouchHelperCallback.ItemGestureListener {

    public static TraceFragment create(){
        return new TraceFragment();
    }

    private ItemTouchHelper itemTouchHelper;

    @Override
    public void showTraceList(ArrayList<TraceExt> traceList) {
        adapter.setData(traceList);
        postNotifyDataSetChanged();
    }

    @Override
    public void notifyItemAdded(int position) {
        if(adapter.getData().size() == 1){
            postNotifyDataSetChanged();
        } else {
            adapter.notifyItemInserted(position);
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_list;
    }

    @Override
    protected void initFragment(Bundle savedInstanceState) {
        super.initFragment(savedInstanceState);
        setLoadMoreEnable(true);
        ItemTouchHelperCallback callback = new ItemTouchHelperCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, this);
        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
        StickyHeaderDecoration headersDecor = new StickyHeaderDecoration(adapter);
        recyclerView.addItemDecoration(headersDecor);
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
        mPresenter.loadTraceList(1);
    }

    @Override
    protected void onLoadMore(int page) {
        super.onLoadMore(page);
        mPresenter.loadTraceList(page);
    }

    @Override
    protected String getEmptyTip() {
        return getString(R.string.no_trace);
    }

    @Override
    public void onItemClick(int position, @NonNull View view) {
        super.onItemClick(position, view);
        TraceExt trace = adapter.getData().get(position);
        if("user".equals(trace.getType())){
            View userAvatar = view.findViewById(R.id.avatar);
            ProfileActivity.show(getActivity(), userAvatar, trace.getUser().getLogin(),
                    trace.getUser().getAvatarUrl());
        } else {
            RepositoryActivity.show(getActivity(), trace.getRepository().getOwner().getLogin(),
                    trace.getRepository().getName());
        }
    }

    @Override
    public boolean onItemMoved(int fromPosition, int toPosition) {
        return false;
    }

    @Override
    public void onItemSwiped(int position, int direction) {
        mPresenter.removeTrace(position);
        if(adapter.getData().size() == 0){
            postNotifyDataSetChanged();
        } else {
            adapter.notifyItemRemoved(position);
        }
        Snackbar.make(recyclerView, R.string.trace_deleted, Snackbar.LENGTH_SHORT)
                .setAction(R.string.undo, v -> mPresenter.undoRemoveTrace() )
                .show();
    }
}
