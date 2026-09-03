

package com.thirtydegreesray.openhub.ui.activity;

import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.thirtydegreesray.dataautoaccess.annotation.AutoAccess;
import com.thirtydegreesray.openhub.AppData;
import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.common.GlideApp;
import com.thirtydegreesray.openhub.dao.AuthUser;
import com.thirtydegreesray.openhub.inject.component.AppComponent;
import com.thirtydegreesray.openhub.inject.component.DaggerActivityComponent;
import com.thirtydegreesray.openhub.inject.module.ActivityModule;
import com.thirtydegreesray.openhub.mvp.contract.IMainContract;
import com.thirtydegreesray.openhub.mvp.model.User;
import com.thirtydegreesray.openhub.mvp.model.filter.RepositoriesFilter;
import com.thirtydegreesray.openhub.mvp.presenter.MainPresenter;
import com.thirtydegreesray.openhub.ui.activity.base.BaseDrawerActivity;
import com.thirtydegreesray.openhub.ui.fragment.ActivityFragment;
import com.thirtydegreesray.openhub.ui.fragment.BookmarksFragment;
import com.thirtydegreesray.openhub.ui.fragment.CollectionsFragment;
import com.thirtydegreesray.openhub.ui.fragment.RepositoriesFragment;
import com.thirtydegreesray.openhub.ui.fragment.NotificationsFragment;
import com.thirtydegreesray.openhub.mvp.model.filter.TrendingSince;
import com.thirtydegreesray.openhub.ui.fragment.TopicsFragment;
import com.thirtydegreesray.openhub.ui.fragment.TraceFragment;
import com.thirtydegreesray.openhub.ui.fragment.base.BaseFragment;
import com.thirtydegreesray.openhub.ui.widget.NewYearWishesDialog;
import com.thirtydegreesray.openhub.util.PrefUtils;
import com.thirtydegreesray.openhub.util.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;

public class MainActivity extends BaseDrawerActivity<MainPresenter>
        implements IMainContract.View {

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.tab_layout) TabLayout tabLayout;
    @BindView(R.id.frame_layout_content) FrameLayout frameLayoutContent;

    private AppCompatImageView toggleAccountBn;

    private View cardProfile;
    private View cardDiscover;
    private View cardMore;
    private View bnBar;
    private ImageView bnSearch;
    private ImageView bnProfile;
    private ImageView bnDiscover;
    private ImageView bnMore;
    private static final int BN_IDLE = 0xFFFFFFFF;
    private static final int BN_ACTIVE = 0xFFFFCA28;

    private final Map<Integer, String> TAG_MAP = new HashMap<>();

    private final int SETTINGS_REQUEST_CODE = 100;

    @AutoAccess int selectedPage ;
    private boolean isAccountsAdded = false;

    private final List<Integer> FRAGMENT_NAV_ID_LIST = Arrays.asList(
            R.id.nav_news, R.id.nav_owned, R.id.nav_starred, R.id.nav_bookmarks,
            R.id.nav_trace, R.id.nav_public_news, R.id.nav_collections, R.id.nav_topics,
            R.id.nav_bn_trending, R.id.nav_bn_notifications
    );

    private final List<String> FRAGMENT_TAG_LIST = Arrays.asList(
            ActivityFragment.ActivityType.News.name(),
            RepositoriesFragment.RepositoriesType.OWNED.name(),
            RepositoriesFragment.RepositoriesType.STARRED.name(),
            BookmarksFragment.class.getSimpleName(),
            TraceFragment.class.getSimpleName(),
            ActivityFragment.ActivityType.PublicNews.name(),
            CollectionsFragment.class.getSimpleName(),
            TopicsFragment.class.getSimpleName(),
            "TRENDING_TAB",
            "NOTIFICATIONS_TAB"
    );

    private final List<Integer> FRAGMENT_TITLE_LIST = Arrays.asList(
            R.string.news, R.string.my_repos, R.string.starred_repos, R.string.bookmarks,
            R.string.trace, R.string.bn_now, R.string.repo_collections, R.string.topics,
            R.string.trending_repos, R.string.notifications
    );

    {
        for(int i = 0; i < FRAGMENT_NAV_ID_LIST.size(); i++){
            TAG_MAP.put(FRAGMENT_NAV_ID_LIST.get(i), FRAGMENT_TAG_LIST.get(i));
        }
    }

    private NewYearWishesDialog newYearWishesDialog;

    /**
     * 依赖注入的入口
     *
     * @param appComponent appComponent
     */
    @Override
    protected void setupActivityComponent(AppComponent appComponent) {
        DaggerActivityComponent.builder()
                .appComponent(appComponent)
                .activityModule(new ActivityModule(getActivity()))
                .build()
                .inject(this);
    }

    @Override
    protected void initActivity() {
        super.initActivity();
        if (AppData.INSTANCE.getLoggedUser() != null)

        setStartDrawerEnable(true);
        setEndDrawerEnable(true);
        newYearWishesDialog = new NewYearWishesDialog(getActivity());
        newYearWishesDialog.checkStarWishes();
    }

    /**
     * 获取ContentView id
     *
     * @return
     */
    @Override
    protected int getContentView() {
        return R.layout.activity_main;
    }

    /**
     * 初始化view
     *
     * @param savedInstanceState
     */
    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);

        setToolbarScrollAble(false);
        updateStartDrawerContent(R.menu.activity_main_drawer);
        removeEndDrawer();
        if (selectedPage != 0) {
            updateFragmentByNavId(selectedPage);
        } else {
            // Default landing page after login is Trending (Discover tab).
            selectedPage = R.id.nav_bn_trending;
            updateFragmentByNavId(selectedPage);
        }
        if (FRAGMENT_NAV_ID_LIST.contains(selectedPage)
                && navViewStart.getMenu().findItem(selectedPage) != null) {
            navViewStart.setCheckedItem(selectedPage);
        }

        ImageView avatar = navViewStart.getHeaderView(0).findViewById(R.id.avatar);
        TextView name = navViewStart.getHeaderView(0).findViewById(R.id.name);
        TextView mail = navViewStart.getHeaderView(0).findViewById(R.id.mail);

        toggleAccountBn = navViewStart.getHeaderView(0).findViewById(R.id.toggle_account_bn);
        toggleAccountBn.setOnClickListener(v -> {
            toggleAccountLay();
        });

        User loginUser = AppData.INSTANCE.getLoggedUser();
        GlideApp.with(getActivity())
                .load(loginUser.getAvatarUrl())
                .onlyRetrieveFromCache(!PrefUtils.isLoadImageEnable())
                .into(avatar);
        name.setText(StringUtils.isBlank(loginUser.getName()) ? loginUser.getLogin() : loginUser.getName());
        String joinTime = getString(R.string.joined_at).concat(" ")
                .concat(StringUtils.getDateStr(loginUser.getCreatedAt()));
        mail.setText(StringUtils.isBlank(loginUser.getBio()) ? joinTime : loginUser.getBio());

        tabLayout.setVisibility(View.GONE);
        setupBottomBar();
    }

    private void setupBottomBar() {
        cardProfile = findViewById(R.id.card_profile);
        cardDiscover = findViewById(R.id.card_discover);
        cardMore = findViewById(R.id.card_more);
        bnBar = findViewById(R.id.bn_bar);
        bnSearch = (ImageView) findViewById(R.id.bn_search);
        bnProfile = (ImageView) findViewById(R.id.bn_profile);
        bnDiscover = (ImageView) findViewById(R.id.bn_discover);
        bnMore = (ImageView) findViewById(R.id.bn_more);

        // Search goes straight to the search screen; the other three toggle their card.
        bnSearch.setOnClickListener(v -> {
            hideAllCards();
            updateActiveIcon(selectedPage);
            SearchActivity.show(getActivity());
        });
        bnProfile.setOnClickListener(v -> openTab(cardProfile, R.id.nav_owned));
        bnDiscover.setOnClickListener(v -> openTab(cardDiscover, R.id.nav_bn_trending));
        bnMore.setOnClickListener(v -> toggleCard(cardMore));

        // Profile card cells
        findViewById(R.id.cell_trace).setOnClickListener(v -> {
            hideAllCards();
            updateActiveIcon(selectedPage);
            TraceActivity.show(getActivity());
        });
        findViewById(R.id.cell_notifications).setOnClickListener(v -> selectContent(R.id.nav_bn_notifications));
        findViewById(R.id.cell_bookmarks).setOnClickListener(v -> selectContent(R.id.nav_bookmarks));
        findViewById(R.id.cell_my_repos).setOnClickListener(v -> selectContent(R.id.nav_owned));
        findViewById(R.id.cell_issues).setOnClickListener(v -> {
            hideAllCards();
            updateActiveIcon(selectedPage);
            IssuesActivity.showForUser(getActivity());
        });
        findViewById(R.id.cell_starred).setOnClickListener(v -> selectContent(R.id.nav_starred));

        // Discover card cells
        findViewById(R.id.cell_random).setOnClickListener(v -> selectContent(R.id.nav_public_news));
        findViewById(R.id.cell_trending).setOnClickListener(v -> selectContent(R.id.nav_bn_trending));
        findViewById(R.id.cell_topics).setOnClickListener(v -> selectContent(R.id.nav_topics));
        findViewById(R.id.cell_following).setOnClickListener(v -> selectContent(R.id.nav_news));

        // More card cells -> launch existing activities
        findViewById(R.id.cell_settings).setOnClickListener(v -> {
            hideAllCards();
            updateActiveIcon(selectedPage);
            SettingsActivity.show(getActivity(), SETTINGS_REQUEST_CODE);
        });
        findViewById(R.id.cell_about).setOnClickListener(v -> {
            hideAllCards();
            updateActiveIcon(selectedPage);
            AboutActivity.show(getActivity());
        });

        updateActiveIcon(selectedPage);
    }

    private void toggleCard(View card) {
        boolean show = card.getVisibility() != View.VISIBLE;
        hideAllCards();
        card.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void hideAllCards() {
        if (cardProfile != null) cardProfile.setVisibility(View.GONE);
        if (cardDiscover != null) cardDiscover.setVisibility(View.GONE);
        if (cardMore != null) cardMore.setVisibility(View.GONE);
    }

    private boolean isAnyCardVisible() {
        return (cardProfile != null && cardProfile.getVisibility() == View.VISIBLE)
                || (cardDiscover != null && cardDiscover.getVisibility() == View.VISIBLE)
                || (cardMore != null && cardMore.getVisibility() == View.VISIBLE);
    }

    private void selectContent(int navId) {
        hideAllCards();
        navigate(navId);
    }

    private void navigate(int navId) {
        updateTitle(navId);
        loadFragment(navId);
        updateFilter(navId);
        updateActiveIcon(navId);
    }

    private void openTab(View card, int defaultNavId) {
        boolean wasVisible = card.getVisibility() == View.VISIBLE;
        hideAllCards();
        if (!wasVisible) {
            card.setVisibility(View.VISIBLE);
            navigate(defaultNavId);
        }
    }

    private void updateActiveIcon(int navId) {
        if (bnProfile == null) return;
        bnSearch.setColorFilter(BN_IDLE);
        bnMore.setColorFilter(BN_IDLE);
        bnProfile.setColorFilter(isProfileNav(navId) ? BN_ACTIVE : BN_IDLE);
        bnDiscover.setColorFilter(isDiscoverNav(navId) ? BN_ACTIVE : BN_IDLE);
    }

    private boolean isProfileNav(int navId) {
        return navId == R.id.nav_owned || navId == R.id.nav_starred
                || navId == R.id.nav_bookmarks || navId == R.id.nav_bn_notifications;
    }

    private boolean isDiscoverNav(int navId) {
        return navId == R.id.nav_public_news || navId == R.id.nav_bn_trending
                || navId == R.id.nav_topics || navId == R.id.nav_news;
    }

    @Override
    public void onBackPressed() {
        if (isAnyCardVisible()) {
            hideAllCards();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // Touching/scrolling outside an open card (and outside the bar) dismisses it.
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN && isAnyCardVisible()) {
            View card = visibleCard();
            if (!isTouchInside(card, ev) && !isTouchInside(bnBar, ev)) {
                hideAllCards();
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private View visibleCard() {
        if (cardProfile != null && cardProfile.getVisibility() == View.VISIBLE) return cardProfile;
        if (cardDiscover != null && cardDiscover.getVisibility() == View.VISIBLE) return cardDiscover;
        if (cardMore != null && cardMore.getVisibility() == View.VISIBLE) return cardMore;
        return null;
    }

    private boolean isTouchInside(View v, MotionEvent ev) {
        if (v == null || v.getVisibility() != View.VISIBLE) return false;
        int[] loc = new int[2];
        v.getLocationOnScreen(loc);
        float x = ev.getRawX();
        float y = ev.getRawY();
        return x >= loc[0] && x <= loc[0] + v.getWidth()
                && y >= loc[1] && y <= loc[1] + v.getHeight();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_sort, menu);
        MenuItem menuItem = menu.findItem(R.id.nav_sort);
        menuItem.setVisible(selectedPage == R.id.nav_owned || selectedPage == R.id.nav_starred);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        invalidateMainMenu();
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected boolean isEndDrawerMultiSelect() {
        return true;
    }

    @Override
    protected int getEndDrawerToggleMenuItemId() {
        return R.id.nav_sort;
    }

    protected void onNavItemSelected(@NonNull MenuItem item, boolean isStartDrawer) {
        super.onNavItemSelected(item, isStartDrawer);
        if (!isStartDrawer) {
            handlerEndDrawerClick(item);
            return;
        }
        int id = item.getItemId();
        updateFragmentByNavId(id);
    }

    private void updateFragmentByNavId(int id){
        if(FRAGMENT_NAV_ID_LIST.contains(id)){
            updateTitle(id);
            loadFragment(id);
            updateFilter(id);
            return;
        }
        switch (id) {
            case R.id.nav_profile:
                ProfileActivity.show(getActivity(), AppData.INSTANCE.getLoggedUser().getLogin(),
                        AppData.INSTANCE.getLoggedUser().getAvatarUrl());
                break;
            case R.id.nav_issues:
                IssuesActivity.showForUser(getActivity());
                break;
            case R.id.nav_notifications:
                NotificationsActivity.show(getActivity());
                break;
            case R.id.nav_trending:
                TrendingActivity.show(getActivity());
                break;
            case R.id.nav_search:
                SearchActivity.show(getActivity());
                break;
            case R.id.nav_settings:
                SettingsActivity.show(getActivity(), SETTINGS_REQUEST_CODE);
                break;
            case R.id.nav_about:
                AboutActivity.show(getActivity());
                break;

            case R.id.nav_logout:
                logout();
                break;
            case R.id.nav_add_account:
                showLoginPage();
                break;
            default:
                break;
        }
    }

    private void updateFilter(int itemId) {
        if (itemId == R.id.nav_owned) {
            updateEndDrawerContent(R.menu.menu_repositories_filter);
            RepositoriesFilter.initDrawer(navViewEnd, RepositoriesFragment.RepositoriesType.OWNED);
        } else if (itemId == R.id.nav_starred) {
            updateEndDrawerContent(R.menu.menu_repositories_filter);
            RepositoriesFilter.initDrawer(navViewEnd, RepositoriesFragment.RepositoriesType.STARRED);
        } else {
            removeEndDrawer();
        }
        invalidateOptionsMenu();
    }

    private void updateTitle(int itemId) {
        int titleId = FRAGMENT_TITLE_LIST.get(FRAGMENT_NAV_ID_LIST.indexOf(itemId));
        setToolbarTitle(getString(titleId));
    }

    private void loadFragment(int itemId) {
        selectedPage = itemId;
        String fragmentTag = TAG_MAP.get(itemId);
        Fragment showFragment = getSupportFragmentManager().findFragmentByTag(fragmentTag);
        boolean isExist = true;
        if (showFragment == null) {
            isExist = false;
            showFragment = getFragment(itemId);
        }
        if (showFragment.isVisible()) {
            return;
        }

        Fragment visibleFragment = getVisibleFragment();
        if (isExist) {
            showAndHideFragment(showFragment, visibleFragment);
        } else {
            addAndHideFragment(showFragment, visibleFragment, fragmentTag);
        }
    }

    @NonNull
    private Fragment getFragment(int itemId) {
        switch (itemId) {
            case R.id.nav_news:
                return ActivityFragment.create(ActivityFragment.ActivityType.News,
                        AppData.INSTANCE.getLoggedUser().getLogin());
            case R.id.nav_public_news:
                return ActivityFragment.create(ActivityFragment.ActivityType.PublicNews,
                        AppData.INSTANCE.getLoggedUser().getLogin());
            case R.id.nav_owned:
                return RepositoriesFragment.create(RepositoriesFragment.RepositoriesType.OWNED,
                        AppData.INSTANCE.getLoggedUser().getLogin());
            case R.id.nav_starred:
                return RepositoriesFragment.create(RepositoriesFragment.RepositoriesType.STARRED,
                        AppData.INSTANCE.getLoggedUser().getLogin());
            case R.id.nav_bookmarks:
                return BookmarksFragment.create();
            case R.id.nav_trace:
                return TraceFragment.create();
            case R.id.nav_collections:
                return CollectionsFragment.create();
            case R.id.nav_topics:
                return TopicsFragment.create();
            case R.id.nav_bn_trending:
                return RepositoriesFragment.createForTrending(TrendingSince.Daily);
            case R.id.nav_bn_notifications:
                return NotificationsFragment.create(NotificationsFragment.NotificationsType.All);
        }
        return null;
    }

    private void showAndHideFragment(@NonNull Fragment showFragment, @Nullable Fragment hideFragment) {
        if (hideFragment == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .show(showFragment)
                    .commit();
        } else {
            getSupportFragmentManager()
                    .beginTransaction()
                    .show(showFragment)
                    .hide(hideFragment)
                    .commit();
        }

    }

    private void addAndHideFragment(@NonNull Fragment showFragment,
                                    @Nullable Fragment hideFragment, @NonNull String addTag) {
        if (hideFragment == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.frame_layout_content, showFragment, addTag)
                    .commit();
        } else {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.frame_layout_content, showFragment, addTag)
                    .hide(hideFragment)
                    .commit();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SETTINGS_REQUEST_CODE && resultCode == RESULT_OK) {
            recreate();
        }
    }

    @Override
    protected void onToolbarDoubleClick() {
        super.onToolbarDoubleClick();
        Fragment fragment = getVisibleFragment();
        if (fragment != null && fragment instanceof BaseFragment) {
            ((BaseFragment) fragment).scrollToTop();
        }
    }

    private void handlerEndDrawerClick(MenuItem item) {
        Fragment fragment = getVisibleFragment();
        if (fragment != null && fragment instanceof RepositoriesFragment
                && (selectedPage == R.id.nav_owned || selectedPage == R.id.nav_starred)) {
            ((RepositoriesFragment) fragment).onDrawerSelected(navViewEnd, item);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(newYearWishesDialog != null){
            newYearWishesDialog.cancel();
        }
    }

    private boolean isManageAccount = false;
    private void toggleAccountLay(){
        isManageAccount = !isManageAccount;
        toggleAccountBn.setImageResource(isManageAccount ? R.drawable.ic_arrow_drop_up : R.drawable.ic_arrow_drop_down);
        invalidateMainMenu();
    }

    private void invalidateMainMenu(){
        if(navViewStart == null){
            return ;
        }
        Menu menu = navViewStart.getMenu();

        if(!isAccountsAdded){
            isAccountsAdded = true;
            List<AuthUser> users = mPresenter.getLoggedUserList();
            for(AuthUser user : users){
                MenuItem menuItem = menu.add(R.id.manage_accounts, Menu.NONE, 1, user.getLoginId())
                        .setIcon(R.drawable.ic_menu_person)
                        .setOnMenuItemClickListener(item -> {
                            mPresenter.toggleAccount(item.getTitle().toString());
                            return true;
                        });
            }
        }

        menu.setGroupVisible(R.id.my_account, isManageAccount);
        menu.setGroupVisible(R.id.manage_accounts, isManageAccount);

        menu.setGroupVisible(R.id.my, !isManageAccount);
        menu.setGroupVisible(R.id.repositories, !isManageAccount);
        menu.setGroupVisible(R.id.search, !isManageAccount);
        menu.setGroupVisible(R.id.setting, !isManageAccount);

    }

    @Override
    public void restartApp() {
        getActivity().finishAffinity();
        Intent intent = new Intent(getActivity(), SplashActivity.class);
        startActivity(intent);
    }

    private void logout() {
        new AlertDialog.Builder(getActivity())
                .setCancelable(true)
                .setTitle(R.string.warning_dialog_tile)
                .setMessage(R.string.logout_warning)
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.logout, (dialog, which) -> {
                    mPresenter.logout();
                })
                .show();
    }

}
