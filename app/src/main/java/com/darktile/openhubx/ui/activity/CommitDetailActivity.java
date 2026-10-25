

package com.darktile.openhubx.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.darktile.openhubx.R;
import com.darktile.openhubx.common.GlideApp;
import com.darktile.openhubx.inject.component.AppComponent;
import com.darktile.openhubx.inject.component.DaggerActivityComponent;
import com.darktile.openhubx.inject.module.ActivityModule;
import com.darktile.openhubx.mvp.contract.ICommitDetailContract;
import com.darktile.openhubx.mvp.model.RepoCommit;
import com.darktile.openhubx.mvp.model.RepoCommitExt;
import com.darktile.openhubx.mvp.presenter.CommitDetailPresenter;
import com.darktile.openhubx.ui.activity.base.BaseActivity;
import com.darktile.openhubx.ui.fragment.CommitFilesFragment;
import com.darktile.openhubx.util.AppOpener;
import com.darktile.openhubx.util.AppUtils;
import com.darktile.openhubx.util.BundleHelper;
import com.darktile.openhubx.util.PrefUtils;
import com.darktile.openhubx.util.StringUtils;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by ThirtyDegreesRay on 2017/10/18 11:50:11
 */

public class CommitDetailActivity extends BaseActivity<CommitDetailPresenter>
        implements ICommitDetailContract.View {

        @Override
        protected void bindViews(android.view.View root) {
            super.bindViews(root);
            userAvatar = root.findViewById(R.id.user_avatar);
            commitMessage = root.findViewById(R.id.commit_message);
            changedFileCount = root.findViewById(R.id.changed_files_count);
            addtionsCount = root.findViewById(R.id.additions_count);
            deletionsCount = root.findViewById(R.id.deletions_count);
            loader = root.findViewById(R.id.loader);
            commentBn = root.findViewById(R.id.comment_bn);
            root.findViewById(R.id.comment_bn).setOnClickListener(v -> onViewClicked());
            root.findViewById(R.id.commit_message).setOnClickListener(v -> onCommitMessageClick());
            root.findViewById(R.id.user_avatar).setOnClickListener(v -> onUserAvatarClick());
        }

    public static void show(@NonNull Activity activity, @NonNull String commitUrl) {
        Intent intent = new Intent(activity, CommitDetailActivity.class);
        intent.putExtras(BundleHelper.builder().put("commitUrl", commitUrl).build());
        activity.startActivity(intent);
    }

    public static void show(@NonNull Activity activity, @NonNull String user, @NonNull String repo,
                            @NonNull RepoCommit commit, @NonNull View userAvatar) {
        Intent intent = new Intent(activity, CommitDetailActivity.class);
        intent.putExtras(BundleHelper.builder().put("user", user).put("repo", repo)
                .put("commit", commit).build());
        ActivityOptionsCompat optionsCompat = ActivityOptionsCompat
                .makeSceneTransitionAnimation(activity, userAvatar, "userAvatar");
        activity.startActivity(intent, optionsCompat.toBundle());
    }

    public static void show(@NonNull Activity activity, @NonNull String user, @NonNull String repo,
                            @NonNull String commitSha, @NonNull View userAvatar, @NonNull String userAvatarUrl) {
        Intent intent = createIntent(activity, user, repo, commitSha, userAvatarUrl);
        ActivityOptionsCompat optionsCompat = ActivityOptionsCompat
                .makeSceneTransitionAnimation(activity, userAvatar, "userAvatar");
        activity.startActivity(intent, optionsCompat.toBundle());
    }

    public static Intent createIntent(@NonNull Activity activity, @NonNull String user, @NonNull String repo,
                                      @NonNull String commitSha) {
        return createIntent(activity, user, repo, commitSha, null);

    }

    public static Intent createIntent(@NonNull Activity activity, @NonNull String user, @NonNull String repo,
                                      @NonNull String commitSha, @NonNull String userAvatarUrl) {
        return new Intent(activity, CommitDetailActivity.class)
                .putExtras(BundleHelper.builder()
                        .put("user", user)
                        .put("repo", repo)
                        .put("commitSha", commitSha)
                        .put("userAvatarUrl", userAvatarUrl).build());

    }

     CircleImageView userAvatar;
     TextView commitMessage;
     TextView changedFileCount;
     TextView addtionsCount;
     TextView deletionsCount;

     ProgressBar loader;
     FloatingActionButton commentBn;

    @Override
    protected void setupActivityComponent(AppComponent appComponent) {
        DaggerActivityComponent.builder()
                .appComponent(appComponent)
                .activityModule(new ActivityModule(this))
                .build()
                .inject(this);
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        setToolbarBackEnable();
        setToolbarTitle(getString(R.string.commit));
        commentBn.setVisibility(View.GONE);
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_commit_detail;
    }

    @Override
    public void showCommit(RepoCommit commit) {
        setToolbarTitle(getString(R.string.commit).concat(" ").concat(commit.getShortSha()));
        if (commit.getAuthor() != null) {
            GlideApp.with(getActivity())
                    .load(commit.getAuthor().getAvatarUrl())
                    .onlyRetrieveFromCache(!PrefUtils.isLoadImageEnable())
                    .into(userAvatar);
        } else {
            userAvatar.setImageResource(R.drawable.ic_question);
        }
        String commitDateStr = getString(R.string.committed).concat(" ").concat(
                StringUtils.getNewsTimeStr(getActivity(), commit.getCommit().getAuthor().getDate()));
        commitMessage.setText(commitDateStr.concat("\n").concat(commit.getCommit().getMessage()));
        invalidateOptionsMenu();
    }

    private CommitFilesFragment commitFilesFragment;

    @Override
    public void showCommitInfo(RepoCommitExt commitExt) {
        showCommit(commitExt);

        changedFileCount.setText(String.valueOf(commitExt.getFiles().size()));
        addtionsCount.setText(String.valueOf(commitExt.getStats().getAdditions()));
        deletionsCount.setText(String.valueOf(commitExt.getStats().getDeletions()));

        if (commitFilesFragment == null) {
            commitFilesFragment = CommitFilesFragment.create(commitExt.getFiles());
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.container, commitFilesFragment)
                    .commitAllowingStateLoss();
        } else {
            commitFilesFragment.showCommitFiles(commitExt.getFiles());
        }

    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if (fragment instanceof CommitFilesFragment) {
            commitFilesFragment = (CommitFilesFragment) fragment;
        }
    }

    @Override
    public void showUserAvatar(String userAvatarUrl) {
        GlideApp.with(getActivity())
                .load(userAvatarUrl)
                .onlyRetrieveFromCache(!PrefUtils.isLoadImageEnable())
                .into(userAvatar);
    }

    public void onViewClicked() {

    }

    @Override
    public void finishActivity() {
        supportFinishAfterTransition();
    }

    @Override
    public void showLoading() {
        super.showLoading();
        loader.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideLoading() {
        super.hideLoading();
        loader.setVisibility(View.GONE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_open_in_browser) {
            AppOpener.openInCustomTabsOrBrowser(getActivity(), mPresenter.getCommit().getHtmlUrl());
            return true;
        } else if (itemId == R.id.action_share) {
            AppOpener.shareText(getActivity(), mPresenter.getCommit().getHtmlUrl());
            return true;
        } else if (itemId == R.id.action_copy_url) {
            AppUtils.copyToClipboard(getActivity(), mPresenter.getCommit().getHtmlUrl());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mPresenter.getCommit() != null)
            getMenuInflater().inflate(R.menu.menu_commit_detail, menu);
        return true;
    }

    public void onCommitMessageClick() {
        commitMessage.setMaxLines(commitMessage.getMaxLines() == 6 ? 20 : 6);
    }

    public void onUserAvatarClick() {
        if (mPresenter.getCommit() != null && mPresenter.getCommit().getAuthor() != null) {
            RepoCommit commit = mPresenter.getCommit();
            ProfileActivity.show(getActivity(), userAvatar,
                    commit.getAuthor().getLogin(), commit.getAuthor().getAvatarUrl());
        }
    }

    @Override
    protected void onToolbarDoubleClick() {
        super.onToolbarDoubleClick();
        if (commitFilesFragment != null) commitFilesFragment.scrollToTop();
    }

}
