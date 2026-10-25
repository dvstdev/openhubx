

package com.darktile.openhubx.ui.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.darktile.openhubx.AppData;
import com.darktile.openhubx.R;
import com.darktile.openhubx.common.GlideApp;
import com.darktile.openhubx.inject.component.AppComponent;
import com.darktile.openhubx.inject.component.DaggerActivityComponent;
import com.darktile.openhubx.inject.module.ActivityModule;
import com.darktile.openhubx.mvp.contract.IRepositoryContract;
import com.darktile.openhubx.mvp.model.Branch;
import com.darktile.openhubx.mvp.model.Repository;
import com.darktile.openhubx.mvp.presenter.RepositoryPresenter;
import com.darktile.openhubx.ui.activity.base.PagerActivity;
import com.darktile.openhubx.ui.adapter.BranchesAdapter;
import com.darktile.openhubx.ui.adapter.base.BaseViewHolder;
import com.darktile.openhubx.ui.adapter.base.FragmentPagerModel;
import com.darktile.openhubx.ui.fragment.ActivityFragment;
import com.darktile.openhubx.ui.fragment.CommitsFragment;
import com.darktile.openhubx.ui.fragment.RepoFilesFragment;
import com.darktile.openhubx.ui.fragment.RepoInfoFragment;
import com.darktile.openhubx.util.AppOpener;
import com.darktile.openhubx.util.AppUtils;
import com.darktile.openhubx.util.BundleHelper;
import com.darktile.openhubx.util.PrefUtils;
import com.darktile.openhubx.util.StarWishesHelper;
import com.darktile.openhubx.util.StringUtils;

import java.util.ArrayList;
import java.util.Locale;
/**
 * Created by ThirtyDegreesRay on 2017/8/9 21:39:20
 */
//FIXME fix fragment not showImage from background
public class RepositoryActivity extends PagerActivity<RepositoryPresenter>
        implements IRepositoryContract.View {

        @Override
        protected void bindViews(android.view.View root) {
            super.bindViews(root);
            userImageViewBg = root.findViewById(R.id.user_avatar_bg);
            loader = root.findViewById(R.id.loader);
            desc = root.findViewById(R.id.desc);
            info = root.findViewById(R.id.info);
        }

    public static void show(@NonNull Context activity, @NonNull String owner,
                            @NonNull String repoName) {
        Intent intent = createIntent(activity, owner, repoName);
        activity.startActivity(intent);
    }

    public static void show(@NonNull Context activity, @NonNull Repository repository) {
        Intent intent = new Intent(activity, RepositoryActivity.class);
        intent.putExtra("repository", repository);
        activity.startActivity(intent);
    }

    public static Intent createIntent(@NonNull Context activity, @NonNull String owner,
                            @NonNull String repoName) {
        return new Intent(activity, RepositoryActivity.class)
                .putExtras(BundleHelper.builder()
                        .put("owner", owner)
                        .put("repoName", repoName)
                        .build());
    }

     ImageView userImageViewBg;
     ProgressBar loader;
     TextView desc;
     TextView info;

    @Override
    protected void setupActivityComponent(AppComponent appComponent) {
        DaggerActivityComponent.builder()
                .appComponent(appComponent)
                .activityModule(new ActivityModule(getActivity()))
                .build()
                .inject(this);
    }

    @Nullable
    @Override
    protected int getContentView() {
        return R.layout.activity_repository;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(mPresenter.getRepository() != null){
            getMenuInflater().inflate(R.menu.menu_repository, menu);
            MenuItem starItem = menu.findItem(R.id.action_star);
            MenuItem bookmark = menu.findItem(R.id.action_bookmark);
            starItem.setTitle(mPresenter.isStarred() ? R.string.unstar : R.string.star);
            starItem.setIcon(mPresenter.isStarred() ?
                    R.drawable.ic_star_title : R.drawable.ic_un_star_title);
            menu.findItem(R.id.action_watch).setTitle(mPresenter.isWatched() ?
                    R.string.unwatch : R.string.watch);
            menu.findItem(R.id.action_fork).setTitle(mPresenter.isFork() ?
                    R.string.forked : R.string.fork);
            menu.findItem(R.id.action_fork).setVisible(mPresenter.isForkEnable());
            bookmark.setTitle(mPresenter.isBookmarked() ?
                    getString(R.string.remove_bookmark) : getString(R.string.bookmark));
            bookmark.setIcon(mPresenter.isBookmarked() ?
                    R.drawable.ic_bookmark_title : R.drawable.ic_un_bookmark_title);
            menu.findItem(R.id.action_wiki).setVisible(mPresenter.getRepository().isHasWiki());
        }
        return true;
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        setTransparentStatusBar();
        toolbar.setTitleTextAppearance(getActivity(), R.style.Toolbar_TitleText);
        toolbar.setSubtitleTextAppearance(getActivity(), R.style.Toolbar_Subtitle);
        setToolbarBackEnable();
        setToolbarTitle(mPresenter.getRepoName());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_star) {
            starRepo(!mPresenter.isStarred());
            return true;
        } else if (itemId == R.id.action_branch) {
            mPresenter.loadBranchesAndTags();
            return true;
        } else if (itemId == R.id.action_share) {
            AppOpener.shareText(getActivity(), mPresenter.getRepository().getHtmlUrl());
            return true;
        } else if (itemId == R.id.action_open_in_browser) {
            AppOpener.openInCustomTabsOrBrowser(getActivity(), mPresenter.getRepository().getHtmlUrl());
            return true;
        } else if (itemId == R.id.action_copy_url) {
            AppUtils.copyToClipboard(getActivity(), mPresenter.getRepository().getHtmlUrl());
            return true;
        } else if (itemId == R.id.action_copy_clone_url) {
            AppUtils.copyToClipboard(getActivity(), mPresenter.getRepository().getCloneUrl());
            return true;
        } else if (itemId == R.id.action_watch) {
            mPresenter.watchRepo(!mPresenter.isWatched());
            invalidateOptionsMenu();
            showSuccessToast(mPresenter.isWatched() ?
                    getString(R.string.watched) : getString(R.string.unwatched));
            return true;
        } else if (itemId == R.id.action_fork) {
            if(!mPresenter.getRepository().isFork()) forkRepo();
            return true;
        } else if (itemId == R.id.action_releases) {
            showReleases();
            return true;
        } else if (itemId == R.id.action_wiki) {
            WikiActivity.show(getActivity(), mPresenter.getRepository().getOwner().getLogin(),
                    mPresenter.getRepository().getName());
            return true;
        } else if (itemId == R.id.action_download_source_zip) {
            AppOpener.startDownload(getActivity(), mPresenter.getZipSourceUrl(),
                    mPresenter.getZipSourceName());
            return true;
        } else if (itemId == R.id.action_download_source_tar) {
            AppOpener.startDownload(getActivity(), mPresenter.getTarSourceUrl(),
                    mPresenter.getTarSourceName());
            return true;
        } else if (itemId == R.id.action_bookmark) {
            mPresenter.bookmark(!mPresenter.isBookmarked());
            invalidateOptionsMenu();
            showSuccessToast(mPresenter.isBookmarked() ?
                    getString(R.string.bookmark_saved) : getString(R.string.bookmark_removed));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void starRepo(boolean star){
        mPresenter.starRepo(star);
        invalidateOptionsMenu();
        showSuccessToast(mPresenter.isStarred() ?
                getString(R.string.starred) : getString(R.string.unstarred));
    }

    @Override
    public void showRepo(Repository repo) {
//        setToolbarTitle(repo.getFullName(), repo.getDefaultBranch());
        desc.setText(repo.getDescription());
        String language = StringUtils.isBlank(repo.getLanguage()) ?
                getString(R.string.unknown) : repo.getLanguage();
        info.setText(String.format(Locale.getDefault(), "Language %s, size %s",
                language, StringUtils.getSizeString(repo.getSize() * 1024)));

        if (pagerAdapter.getCount() == 0) {
            pagerAdapter.setPagerList(FragmentPagerModel
                    .createRepoPagerList(getActivity(), repo, getFragments()));
            tabLayout.setVisibility(View.VISIBLE);
            tabLayout.setupWithViewPager(viewPager);
            viewPager.setAdapter(pagerAdapter);
            showFirstPager();

            GlideApp.with(getActivity())
                    .load(repo.getOwner().getAvatarUrl())
                    .onlyRetrieveFromCache(!PrefUtils.isLoadImageEnable())
                    .into(userImageViewBg);
        } else {
            noticeRepositoryUpdated(repo);
        }
        invalidateOptionsMenu();
    }

    @Override
    public void showBranchesAndTags(final ArrayList<Branch> list, Branch curBranch) {
        BranchesAdapter branchesAdapter = new BranchesAdapter(getActivity(), curBranch.getName());
        branchesAdapter.setData(list);

        final RecyclerView recyclerView = new RecyclerView(getActivity());
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(branchesAdapter);

        final AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .setCancelable(true)
                .setTitle(getString(R.string.select_branch))
                .setView(recyclerView)
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .show();

        branchesAdapter.setOnItemClickListener(new BaseViewHolder.OnItemClickListener() {
            @Override
            public void onItemClick(int position, @NonNull View view) {
                Branch branch = list.get(position);
                mPresenter.getRepository().setDefaultBranch(branch.getName());
                mPresenter.setCurBranch(branch);
                noticeBranchChanged(branch);
//                showRepo(mPresenter.getRepository());
                alertDialog.dismiss();
            }
        });

    }

    @Override
    public void showStarWishes() {
        String message = getString(R.string.star_wishes);
        String user = AppData.INSTANCE.getLoggedUser().getName();
        user = StringUtils.isBlank(user) ? AppData.INSTANCE.getLoggedUser().getLogin() : user;
        message = String.format(message, user, StarWishesHelper.getInstalledDays());
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.openhub_wishes)
                .setMessage(message)
                .setNegativeButton(R.string.star_next_time, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton(R.string.star_me, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        starRepo(true);
                        showSuccessToast(getString(R.string.star_thanks));
                    }
                })
                .setCancelable(false)
                .show();
        StarWishesHelper.addStarWishesTipTimes();
    }

    private void forkRepo(){
        new AlertDialog.Builder(getActivity())
                .setCancelable(true)
                .setTitle(R.string.warning_dialog_tile)
                .setMessage(R.string.fork_warning_msg)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setPositiveButton(R.string.fork, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mPresenter.createFork();
                    }
                }).show();
    }

    private void showReleases(){
        ReleasesActivity.show(getActivity(), mPresenter.getRepository().getOwner().getLogin(),
                mPresenter.getRepository().getName());
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

    private void noticeBranchChanged(Branch branch){
        for(FragmentPagerModel pagerModel : pagerAdapter.getPagerList()){
            if(pagerModel.getFragment() instanceof RepositoryListener){
                ((RepositoryListener)pagerModel.getFragment()).onBranchChanged(branch);
            }
        }
    }

    private void noticeRepositoryUpdated(Repository repository){
        for(FragmentPagerModel pagerModel : pagerAdapter.getPagerList()){
            if(pagerModel.getFragment() instanceof RepositoryListener){
                ((RepositoryListener)pagerModel.getFragment()).onRepositoryInfoUpdated(repository);
            }
        }
    }

    public interface RepositoryListener{
        void onRepositoryInfoUpdated(Repository repository);
        void onBranchChanged(Branch branch);
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
    }

    @Override
    public int getPagerSize() {
        return 4;
    }

    @Override
    protected int getFragmentPosition(Fragment fragment) {
        if(fragment instanceof RepoInfoFragment){
            return 0;
        }else if(fragment instanceof RepoFilesFragment){
            return 1;
        }else if(fragment instanceof CommitsFragment){
            return 2;
        }else if(fragment instanceof ActivityFragment){
            return 3;
        }else
            return -1;
    }
}
