package com.darktile.openhubx.ui.adapter;

import android.content.Context;
import androidx.annotation.NonNull;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

import com.darktile.openhubx.R;
import com.darktile.openhubx.mvp.model.Release;
import com.darktile.openhubx.ui.adapter.base.BaseAdapter;
import com.darktile.openhubx.ui.adapter.base.BaseViewHolder;
import com.darktile.openhubx.ui.fragment.base.BaseFragment;
import com.darktile.openhubx.util.StringUtils;

import javax.inject.Inject;
/**
 * Created by ThirtyDegreesRay on 2017/9/16 12:14:02
 */

public class ReleasesAdapter extends BaseAdapter<ReleasesAdapter.ViewHolder, Release> {

    @Inject
    public ReleasesAdapter(Context context, BaseFragment fragment) {
        super(context, fragment);
    }

    @Override
    protected int getLayoutId(int viewType) {
        return R.layout.layout_item_release;
    }

    @Override
    protected ViewHolder getViewHolder(View itemView, int viewType) {
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        Release model = data.get(position);
        holder.releaseName.setText(model.getTagName());
        holder.time.setText(StringUtils.getNewsTimeStr(context, model.getPublishedAt()));
        if(!StringUtils.isBlank(model.getBodyHtml())){
            holder.body.setText(Html.fromHtml(model.getBodyHtml()));
            holder.body.setVisibility(View.VISIBLE);
        } else {
            holder.body.setText("");
            holder.body.setVisibility(View.GONE);
        }
    }

    class ViewHolder extends BaseViewHolder {

        @Override
        protected void bindViews(android.view.View root) {
            super.bindViews(root);
            releaseName = root.findViewById(R.id.release_name);
            time = root.findViewById(R.id.time);
            body = root.findViewById(R.id.body);
        }

         TextView releaseName;
         TextView time;
         TextView body;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

}
