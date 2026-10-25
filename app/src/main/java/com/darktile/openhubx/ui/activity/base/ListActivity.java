

package com.darktile.openhubx.ui.activity.base;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

import com.darktile.openhubx.R;
import com.darktile.openhubx.mvp.contract.base.IBaseContract;
import com.darktile.openhubx.ui.adapter.base.BaseAdapter;
import com.darktile.openhubx.ui.adapter.base.BaseViewHolder;

import javax.inject.Inject;
/**
 * Created on 2017/8/1.
 *
 * @author ThirtyDegreesRay
 */

public abstract class ListActivity <P extends IBaseContract.Presenter, A extends BaseAdapter>
        extends BaseActivity<P> implements IBaseContract.View,
        BaseViewHolder.OnItemClickListener,
        BaseViewHolder.OnItemLongClickListener {

        @Override
        protected void bindViews(android.view.View root) {
            super.bindViews(root);
            recyclerView = root.findViewById(R.id.recycler_view);
        }

     protected RecyclerView recyclerView;
    @Inject protected A adapter;

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter.setOnItemLongClickListener(this);
        adapter.setOnItemClickListener(this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onItemClick(int position, @NonNull View view) {

    }

    @Override
    public boolean onItemLongClick(int position, @NonNull View view) {
        return false;
    }

}
