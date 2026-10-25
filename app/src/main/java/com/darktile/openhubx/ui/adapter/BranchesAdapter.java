

package com.darktile.openhubx.ui.adapter;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.darktile.openhubx.R;
import com.darktile.openhubx.mvp.model.Branch;
import com.darktile.openhubx.ui.adapter.base.BaseAdapter;
import com.darktile.openhubx.ui.adapter.base.BaseViewHolder;
import com.darktile.openhubx.util.ViewUtils;
/**
 * Created by ThirtyDegreesRay on 2017/8/15 22:50:29
 */

public class BranchesAdapter extends BaseAdapter<BranchesAdapter.ViewHolder, Branch> {

    private String curBranch;

    public BranchesAdapter(Context context, String curBranch) {
        super(context);
        this.curBranch = curBranch;
    }

    @Override
    protected int getLayoutId(int viewType) {
        return R.layout.layout_item_branch;
    }

    @Override
    protected ViewHolder getViewHolder(View itemView, int viewType) {
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        Branch branch = data.get(position);
        holder.icon.setImageResource(branch.isBranch() ? R.drawable.ic_branch : R.drawable.ic_tag);
        holder.name.setText(branch.getName());
        if(branch.getName().equals(curBranch)){
            holder.rootLayout.setBackgroundColor(ViewUtils.getSelectedColor(context));
        }else{
            holder.rootLayout.setBackground(null);
        }
    }

    class ViewHolder extends BaseViewHolder {

        @Override
        protected void bindViews(android.view.View root) {
            super.bindViews(root);
            rootLayout = root.findViewById(R.id.root_layout);
            icon = root.findViewById(R.id.icon);
            name = root.findViewById(R.id.name);
        }

         LinearLayout rootLayout;
         AppCompatImageView icon;
         TextView name;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

}
