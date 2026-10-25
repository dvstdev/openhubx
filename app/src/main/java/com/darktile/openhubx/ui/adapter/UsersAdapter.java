

package com.darktile.openhubx.ui.adapter;

import android.content.Context;
import androidx.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.darktile.openhubx.R;
import com.darktile.openhubx.common.GlideApp;
import com.darktile.openhubx.mvp.model.User;
import com.darktile.openhubx.ui.adapter.base.BaseAdapter;
import com.darktile.openhubx.ui.adapter.base.BaseViewHolder;
import com.darktile.openhubx.ui.fragment.base.BaseFragment;
import com.darktile.openhubx.util.PrefUtils;

import javax.inject.Inject;
/**
 * Created by ThirtyDegreesRay on 2017/8/16 17:12:05
 */

public class UsersAdapter extends BaseAdapter<UsersAdapter.ViewHolder, User> {

    private boolean cardEnable = true;

    @Inject
    public UsersAdapter(Context context, BaseFragment fragment){
        super(context, fragment);
    }

    public void setCardEnable(boolean cardEnable) {
        this.cardEnable = cardEnable;
    }

    @Override
    protected int getLayoutId(int viewType) {
        return cardEnable ? R.layout.layout_item_user : R.layout.layout_item_user_no_card;
    }

    @Override
    protected ViewHolder getViewHolder(View itemView, int viewType) {
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        GlideApp.with(fragment)
                .load(data.get(position).getAvatarUrl())
                .onlyRetrieveFromCache(!PrefUtils.isLoadImageEnable())
                .into(holder.avatar);
        holder.name.setText(data.get(position).getLogin());
    }

    class ViewHolder extends BaseViewHolder {

        @Override
        protected void bindViews(android.view.View root) {
            super.bindViews(root);
            avatar = root.findViewById(R.id.avatar);
            name = root.findViewById(R.id.name);
        }
         ImageView avatar;
         TextView name;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

}
