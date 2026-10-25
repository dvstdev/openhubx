package com.darktile.openhubx.ui.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.darktile.openhubx.R;
import com.darktile.openhubx.common.GlideApp;
import com.darktile.openhubx.mvp.model.BookmarkExt;
import com.darktile.openhubx.mvp.model.Repository;
import com.darktile.openhubx.mvp.model.User;
import com.darktile.openhubx.ui.activity.ProfileActivity;
import com.darktile.openhubx.ui.adapter.base.BaseAdapter;
import com.darktile.openhubx.ui.adapter.base.BaseViewHolder;
import com.darktile.openhubx.ui.fragment.base.BaseFragment;
import com.darktile.openhubx.util.LanguageColorsHelper;
import com.darktile.openhubx.util.PrefUtils;
import com.darktile.openhubx.util.StringUtils;
import com.darktile.openhubx.util.ViewUtils;

import javax.inject.Inject;
/**
 * Created by ThirtyDegreesRay on 2017/11/22 16:15:52
 */

public class BookmarksAdapter extends BaseAdapter<BaseViewHolder, BookmarkExt> {

    @Inject
    public BookmarksAdapter(Context context, BaseFragment fragment) {
        super(context, fragment);
    }

    @Override
    protected int getLayoutId(int viewType) {
        if(viewType == 0){
            return R.layout.layout_item_user;
        } else {
            return R.layout.layout_item_repository;
        }
    }

    @Override
    protected BaseViewHolder getViewHolder(View itemView, int viewType) {
        if(viewType == 0){
            return new UserViewHolder(itemView);
        } else {
            return new RepoViewHolder(itemView);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if("user".equals(data.get(position).getType())){
            return 0;
        }else {
            return 1;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull BaseViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        BookmarkExt model = data.get(position);
        if(getItemViewType(position) == 0){
            User user = model.getUser();
            UserViewHolder userViewHolder = (UserViewHolder) holder;
            GlideApp.with(fragment)
                    .load(user.getAvatarUrl())
                    .onlyRetrieveFromCache(!PrefUtils.isLoadImageEnable())
                    .into(userViewHolder.avatar);
            userViewHolder.name.setText(user.getLogin());
            userViewHolder.name.setTextColor(ViewUtils.getAccentColor(context));
        } else {
            Repository repository = data.get(position).getRepository();
            RepoViewHolder repoViewHolder = (RepoViewHolder) holder;
            repoViewHolder.tvRepoName.setText(repository.getName());
            repoViewHolder.tvRepoDescription.setText(repository.getDescription());
            repoViewHolder.tvStarNum.setText(String.valueOf(repository.getStargazersCount()));
            repoViewHolder.tvForkNum.setText(String.valueOf(repository.getForksCount()));
            repoViewHolder.tvOwnerName.setText(repository.getOwner().getLogin());
            GlideApp.with(fragment)
                    .load(repository.getOwner().getAvatarUrl())
                    .onlyRetrieveFromCache(!PrefUtils.isLoadImageEnable())
                    .into(repoViewHolder.ivUserAvatar);
            repoViewHolder.forkMark.setVisibility(repository.isFork() ? View.VISIBLE : View.GONE);

            if(StringUtils.isBlank(repository.getLanguage())){
                repoViewHolder.tvLanguage.setText("");
                repoViewHolder.languageColor.setVisibility(View.INVISIBLE);
            } else {
                repoViewHolder.languageColor.setVisibility(View.VISIBLE);
                repoViewHolder.tvLanguage.setText(repository.getLanguage());
                int languageColor = LanguageColorsHelper.INSTANCE.getColor(context, repository.getLanguage());
                repoViewHolder.languageColor.setImageTintList(ColorStateList.valueOf(languageColor));
            }
        }

    }

    class UserViewHolder extends BaseViewHolder {

        @Override
        protected void bindViews(android.view.View root) {
            super.bindViews(root);
            avatar = root.findViewById(R.id.avatar);
            name = root.findViewById(R.id.name);
        }
         ImageView avatar;
         TextView name;
        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public class RepoViewHolder extends BaseViewHolder {

        @Override
        protected void bindViews(android.view.View root) {
            super.bindViews(root);
            ivUserAvatar = root.findViewById(R.id.iv_user_avatar);
            languageColor = root.findViewById(R.id.language_color);
            tvRepoName = root.findViewById(R.id.tv_repo_name);
            tvLanguage = root.findViewById(R.id.tv_language);
            tvRepoDescription = root.findViewById(R.id.tv_repo_description);
            tvStarNum = root.findViewById(R.id.tv_star_num);
            tvForkNum = root.findViewById(R.id.tv_fork_num);
            tvOwnerName = root.findViewById(R.id.tv_owner_name);
            forkMark = root.findViewById(R.id.fork_mark);
            root.findViewById(R.id.iv_user_avatar).setOnClickListener(v -> onUserClick());
        }

         ImageView ivUserAvatar;
         ImageView languageColor;
         TextView tvRepoName;
         TextView tvLanguage;
         TextView tvRepoDescription;
         TextView tvStarNum;
         TextView tvForkNum;
         TextView tvOwnerName;
         View forkMark;

        public RepoViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        public void onUserClick(){
            if(getAdapterPosition() != RecyclerView.NO_POSITION) {
                ProfileActivity.show((Activity) context, ivUserAvatar,
                        data.get(getAdapterPosition()).getRepository().getOwner().getLogin(),
                        data.get(getAdapterPosition()).getRepository().getOwner().getAvatarUrl());
            }
        }
    }

}
