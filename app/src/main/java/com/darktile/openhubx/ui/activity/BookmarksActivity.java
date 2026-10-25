package com.darktile.openhubx.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.View;

import com.darktile.openhubx.R;
import com.darktile.openhubx.inject.component.AppComponent;
import com.darktile.openhubx.ui.activity.base.PagerActivity;
import com.darktile.openhubx.ui.adapter.base.FragmentPagerModel;
import com.darktile.openhubx.ui.adapter.base.FragmentViewPagerAdapter;
import com.darktile.openhubx.ui.fragment.RepositoriesFragment;
import com.darktile.openhubx.ui.fragment.UserListFragment;

/**
 * Created by ThirtyDegreesRay on 2017/11/13 13:46:26
 */

public class BookmarksActivity extends PagerActivity {

    public static void show(@NonNull Activity activity){
        Intent intent = new Intent(activity, BookmarksActivity.class);
        activity.startActivity(intent);
    }

    @Override
    protected void initActivity() {
        super.initActivity();
        pagerAdapter = new FragmentViewPagerAdapter(getSupportFragmentManager());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        setToolbarScrollAble(true);
        setToolbarBackEnable();
        setToolbarTitle(getString(R.string.bookmarks));
        pagerAdapter.setPagerList(FragmentPagerModel.createBookmarksPagerList(getActivity(), getFragments()));
        tabLayout.setVisibility(View.VISIBLE);
        tabLayout.setupWithViewPager(viewPager);
        viewPager.setAdapter(pagerAdapter);
        showFirstPager();
    }

    @Override
    protected void setupActivityComponent(AppComponent appComponent) {

    }

    @Override
    protected int getContentView() {
        return R.layout.activity_view_pager;
    }

    @Override
    public int getPagerSize() {
        return 2;
    }

    @Override
    protected int getFragmentPosition(Fragment fragment) {
        if(fragment instanceof RepositoriesFragment){
            return 0;
        } else if(fragment instanceof UserListFragment){
            return 1;
        } else
            return -1;
    }
}
