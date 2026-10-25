

package com.darktile.openhubx.ui.activity.base;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.CallSuper;
import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import com.bumptech.glide.Glide;
import com.darktile.dataautoaccess.DataAutoAccess;
import com.darktile.dataautoaccess.annotation.AutoAccess;
import com.darktile.openhubx.AppApplication;
import com.darktile.openhubx.AppData;
import com.darktile.openhubx.R;
import com.darktile.openhubx.dao.DaoSession;
import com.darktile.openhubx.inject.component.AppComponent;
import com.darktile.openhubx.mvp.contract.base.IBaseContract;
import com.darktile.openhubx.ui.activity.LoginActivity;
import com.darktile.openhubx.ui.activity.SplashActivity;
import com.darktile.openhubx.ui.widget.DoubleClickHandler;
import com.darktile.openhubx.util.AppUtils;
import com.darktile.openhubx.util.PrefUtils;
import com.darktile.openhubx.util.ThemeHelper;
import com.darktile.openhubx.util.WindowUtil;

import java.util.List;

import javax.inject.Inject;

import es.dmoral.toasty.Toasty;

/**
 * This is base activity to set some common things.
 * Created by ThirtyDegreesRay on 2016/7/13 18:13
 */
public abstract class
BaseActivity<P extends IBaseContract.Presenter>
        extends AppCompatActivity implements IBaseContract.View{

    @Inject
    protected P mPresenter;
    private ProgressDialog mProgressDialog;
    private static BaseActivity curActivity;

    protected boolean isAlive = true;
    @Nullable protected Toolbar toolbar;
    @Nullable protected CollapsingToolbarLayout toolbarLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if((AppData.INSTANCE.getAuthUser() == null || AppData.INSTANCE.getLoggedUser() == null)
                && !this.getClass().equals(SplashActivity.class)
                && !this.getClass().equals(LoginActivity.class)){
            super.onCreate(savedInstanceState);
            finishAffinity();
            startActivity(new Intent(getActivity(), SplashActivity.class));
            return;
        }

        ThemeHelper.apply(this);
        AppUtils.updateAppLanguage(getActivity());
        super.onCreate(savedInstanceState);
        isAlive = true;
        setupActivityComponent(getAppComponent());
        DataAutoAccess.getData(this, savedInstanceState);
        if(mPresenter != null) {
            mPresenter.onRestoreInstanceState(savedInstanceState == null ?
                    getIntent().getExtras() : savedInstanceState);
            mPresenter.attachView(this);
        }
        if(savedInstanceState != null && AppData.INSTANCE.getAuthUser() == null){
            DataAutoAccess.getData(AppData.INSTANCE, savedInstanceState);
        }
        getScreenSize();

        if(getContentView() != 0){
            setContentView(getContentView());
            bindViews(getWindow().getDecorView());
        }

        initActivity();
        initView(savedInstanceState);
        if(mPresenter != null) mPresenter.onViewInitialized();
        if(isFullScreen){
            intoFullScreen();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //系统由于内存不足而杀死activity，此时保存数据
        DataAutoAccess.saveData(this, outState);
        if(mPresenter != null) mPresenter.onSaveInstanceState(outState);
        if(curActivity.equals(this)){
            DataAutoAccess.saveData(AppData.INSTANCE, outState);
        }
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    /**
     * 依赖注入的入口
     * @param appComponent appComponent
     */
    protected abstract void setupActivityComponent(AppComponent appComponent);

    /**
     * 获取ContentView id
     * @return
     */
    @LayoutRes protected abstract int getContentView();

    /**
     * View binding hook (ButterKnife replacement). Subclasses override, call
     * super, then assign their own views via {@code root.findViewById(...)} and
     * wire click listeners. Invoked after setContentView.
     */
    @CallSuper
    protected void bindViews(View root) {
        toolbar = root.findViewById(R.id.toolbar);
        toolbarLayout = root.findViewById(R.id.toolbar_layout);
    }

    /**
     * 初始化activity
     */
    @CallSuper
    protected void initActivity(){

    }
    /**
     * 初始化view
     */
    @CallSuper
    protected void initView(Bundle savedInstanceState){
        if(toolbar != null){
            setSupportActionBar(toolbar);
            DoubleClickHandler.setDoubleClickListener(toolbar, new DoubleClickHandler.DoubleClickListener() {
                @Override
                public void onDoubleClick(View view) {
                    onToolbarDoubleClick();
                }
            });
        }
    }

    protected void onToolbarDoubleClick(){
        PrefUtils.set(PrefUtils.DOUBLE_CLICK_TITLE_TIP_ABLE, false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        curActivity = getActivity();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mPresenter != null) mPresenter.detachView();
        if(this.equals(curActivity)) curActivity = null;
        isAlive = false;
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Glide.with(this).onTrimMemory(level);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Glide.with(this).onLowMemory();
    }

    protected void setToolbarBackEnable() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    protected void setToolbarTitle(String title) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
        if(toolbarLayout != null){
            toolbarLayout.setTitle(title);
        }
    }

    protected void setToolbarTitle(String title, String subTitle) {
        setToolbarTitle(title);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(subTitle);
        }
    }

    protected void setToolbarSubTitle(String subTitle) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(subTitle);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home){
            finishActivity();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void finishActivity(){
        finish();
    }

    protected Fragment getVisibleFragment(){
        @SuppressLint("RestrictedApi")
        List<Fragment> fragmentList = getSupportFragmentManager().getFragments();
        if(fragmentList != null ){
            for(Fragment fragment : fragmentList){
                if(fragment != null && fragment.isVisible()){
                    return fragment;
                }
            }
        }
        return null;
    }

    @Override
    public void showLoading() {

    }

    @Override
    public void hideLoading() {

    }

    @Override
    public void showProgressDialog(String content) {
        getProgressDialog(content);
        mProgressDialog.show();
    }

    @Override
    public void dismissProgressDialog() {
        if(mProgressDialog != null){
            mProgressDialog.dismiss();
        }else{
            throw new NullPointerException("dismissProgressDialog: can't dismiss a null dialog, must showForRepo dialog first!");
        }
    }

    @Override
    public ProgressDialog getProgressDialog(String content){
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setCancelable(false);
        }
        mProgressDialog.setMessage(content);
        return mProgressDialog;
    }

    @Override
    public void showToast(String message){
        Toasty.normal(getActivity(), message).show();
    }

    @Override
    public void showInfoToast(String message) {
        Toasty.info(getActivity(), message).show();
    }

    @Override
    public void showSuccessToast(String message) {
        Toasty.success(getActivity(), message).show();
    }

    @Override
    public void showErrorToast(String message) {
        Toasty.error(getActivity(), message).show();
    }

    @Override
    public void showWarningToast(String message) {
        Toasty.warning(getActivity(), message).show();
    }

    @Override
    public void showTipDialog(String content) {
        new AlertDialog.Builder(getActivity())
                .setCancelable(true)
                .setTitle("提示")
                .setMessage(content)
                .setCancelable(true)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(@NonNull DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .show();
    }

    @Override
    public void showConfirmDialog(String msn, String title, String confirmText
            , DialogInterface.OnClickListener confirmListener) {
        new AlertDialog.Builder(getActivity())
                .setCancelable(true)
                .setTitle(title)
                .setMessage(msn)
                .setCancelable(true)
                .setPositiveButton(confirmText, confirmListener)
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(@NonNull DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .show();
    }

    protected void postDelayFinish(int time){
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, time);
    }

    public static BaseActivity getCurActivity() {
        return curActivity;
    }


    @NonNull
    protected BaseActivity getActivity() {
        return this;
    }

    @NonNull
    protected AppApplication getAppApplication() {
        return (AppApplication) getApplication();
    }

    protected AppComponent getAppComponent(){
        return getAppApplication().getAppComponent();
    }

    protected DaoSession getDaoSession(){
        return getAppComponent().getDaoSession();
    }

    public void onRefreshWebPage(){

    }

    /**
     * @param id  viewId
     * @param <T> View
     * @return View
     * @Description: 优化activity的方法，添加类型自动转换
     * @author: Yuyunhao
     */
    @Nullable
    protected <T extends View> T findViewByViewId(@IdRes int id){
        return (T) findViewById(id);
    }

    private void getScreenSize(){
        if(WindowUtil.screenHeight == 0 ||
                WindowUtil.screenWidth == 0){
            Display display = getWindowManager().getDefaultDisplay();
            WindowUtil.screenWidth = display.getWidth();
            WindowUtil.screenHeight = display.getHeight();
        }
    }

    public String getAppVersionName() {
        String versionName = "";
        try {
            versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionName;
    }

    /**
     * 引用资源文件中的字符串
     *
     * @param strId
     * @see [类、类#方法、类#成员]
     */
    @NonNull
    protected String getResuceString(int strId) {
        return getResources().getString(strId);
    }

    protected void delayFinish(){
        delayFinish(1000);
    }

    protected void delayFinish(int mills){
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, mills);
    }

    protected void setToolbarScrollAble(boolean scrollAble) {
        if(toolbar == null) return;
        int flags = scrollAble ? (AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
                | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
                | AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP) : 0;
        AppBarLayout.LayoutParams layoutParams = (AppBarLayout.LayoutParams) toolbar.getLayoutParams();
        layoutParams.setScrollFlags(flags);
        toolbar.setLayoutParams(layoutParams);
    }

    protected void setTransparentStatusBar(){
        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            decorView.setSystemUiVisibility(option);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
    }


    @Override
    public void showLoginPage() {
        getActivity().finishAffinity();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        startActivity(intent);
    }

    public boolean isAlive() {
        return isAlive;
    }

    @AutoAccess boolean isFullScreen = false;

    protected void exitFullScreen() {
        showStatusBar();
        if(toolbar != null) toolbar.setVisibility(View.VISIBLE);
        isFullScreen = false;
    }

    protected void intoFullScreen() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if(toolbar != null) toolbar.setVisibility(View.GONE);
        isFullScreen = true;
    }

    private void showStatusBar() {
        final WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setAttributes(attrs);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    }

    @Override
    public void onBackPressed() {
        if(isFullScreen){
            exitFullScreen();
        } else {
            super.onBackPressed();
        }
    }
}
