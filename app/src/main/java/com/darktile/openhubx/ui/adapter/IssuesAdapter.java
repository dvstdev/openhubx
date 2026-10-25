package com.darktile.openhubx.ui.adapter;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.darktile.openhubx.R;
import com.darktile.openhubx.common.GlideApp;
import com.darktile.openhubx.mvp.model.Issue;
import com.darktile.openhubx.ui.activity.ProfileActivity;
import com.darktile.openhubx.ui.adapter.base.BaseAdapter;
import com.darktile.openhubx.ui.adapter.base.BaseViewHolder;
import com.darktile.openhubx.ui.fragment.base.BaseFragment;
import com.darktile.openhubx.util.PrefUtils;
import com.darktile.openhubx.util.StringUtils;

import javax.inject.Inject;
/**
 * Created by ThirtyDegreesRay on 2017/9/20 14:58:40
 */

public class IssuesAdapter extends BaseAdapter<IssuesAdapter.ViewHolder, Issue> {

    private boolean isUserIssues = false;

    @Inject
    public IssuesAdapter(Context context, BaseFragment fragment) {
        super(context, fragment);
    }

    public void setUserIssues(boolean userIssues) {
        isUserIssues = userIssues;
    }

    @Override
    protected int getLayoutId(int viewType) {
        return R.layout.layout_item_issue;
    }

    @Override
    protected ViewHolder getViewHolder(View itemView, int viewType) {
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        Issue model = data.get(position);
        GlideApp.with(fragment)
                .load(model.getUser().getAvatarUrl())
                .onlyRetrieveFromCache(!PrefUtils.isLoadImageEnable())
                .into(holder.userAvatar);
        holder.userName.setText(model.getUser().getLogin());
        holder.issueTitle.setText(model.getTitle());
        holder.commentNum.setText(String.valueOf(model.getCommentNum()));
        holder.time.setText(StringUtils.getNewsTimeStr(context, model.getCreatedAt()));
        if(isUserIssues) {
            holder.repoFullName.setText(model.getRepoFullName().concat(" #").concat(String.valueOf(model.getNumber())));
        } else {
            holder.repoFullName.setText(("#").concat(String.valueOf(model.getNumber())));
        }
    }

    class ViewHolder extends BaseViewHolder {

        @Override
        protected void bindViews(android.view.View root) {
            super.bindViews(root);
            userAvatar = root.findViewById(R.id.user_avatar);
            userName = root.findViewById(R.id.user_name);
            time = root.findViewById(R.id.time);
            issueTitle = root.findViewById(R.id.issue_title);
            commentNum = root.findViewById(R.id.comment_num);
            repoFullName = root.findViewById(R.id.repo_full_name);
            root.findViewById(R.id.user_avatar).setOnClickListener(v -> onUserClick());
            root.findViewById(R.id.user_name).setOnClickListener(v -> onUserClick());
        }

         ImageView userAvatar;
         TextView userName;
         TextView time;
         TextView issueTitle;
         TextView commentNum;
         TextView repoFullName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        public void onUserClick(){
            if(getAdapterPosition() != RecyclerView.NO_POSITION) {
                Issue issue = data.get(getAdapterPosition());
                ProfileActivity.show((Activity) context, userAvatar, issue.getUser().getLogin(),
                        issue.getUser().getAvatarUrl());
            }
        }

    }

}
