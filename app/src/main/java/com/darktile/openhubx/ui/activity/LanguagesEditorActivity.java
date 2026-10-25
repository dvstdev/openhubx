package com.darktile.openhubx.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.view.Menu;
import android.view.View;

import com.darktile.dataautoaccess.annotation.AutoAccess;
import com.darktile.openhubx.R;
import com.darktile.openhubx.mvp.contract.base.IBaseContract;
import com.darktile.openhubx.mvp.model.TrendingLanguage;
import com.darktile.openhubx.ui.activity.base.SingleFragmentActivity;
import com.darktile.openhubx.ui.fragment.LanguagesEditorFragment;
import com.darktile.openhubx.ui.fragment.base.ListFragment;
import com.darktile.openhubx.ui.widget.ZoomAbleFloatingActionButton;
import com.darktile.openhubx.util.BundleHelper;

import java.util.ArrayList;
/**
 * Created by ThirtyDegreesRay on 2017/11/28 18:55:27
 */

public class LanguagesEditorActivity extends
        SingleFragmentActivity<IBaseContract.Presenter, LanguagesEditorFragment>
        implements ListFragment.ListScrollListener{

        @Override
        protected void bindViews(android.view.View root) {
            super.bindViews(root);
            floatingActionButton = root.findViewById(R.id.float_action_bn);
            root.findViewById(R.id.float_action_bn).setOnClickListener(v -> onAddClick());
        }

    public static void showForChoose(@NonNull Activity activity, @NonNull LanguageEditorMode mode,
                                     @NonNull ArrayList<TrendingLanguage> selectedLanguages, int requestCode) {
        Intent intent = new Intent(activity, LanguagesEditorActivity.class);
        intent.putExtras(BundleHelper.builder().put("mode", mode)
                .put("selectedLanguages", selectedLanguages).build());
        activity.startActivityForResult(intent, requestCode);
    }

    public static void show(@NonNull Activity activity, @NonNull LanguageEditorMode mode,
                            int requestCode) {
        Intent intent = new Intent(activity, LanguagesEditorActivity.class);
        intent.putExtras(BundleHelper.builder().put("mode", mode).build());
        activity.startActivityForResult(intent, requestCode);
    }

     ZoomAbleFloatingActionButton floatingActionButton;
    private final int ADD_LANGUAGE_REQUEST_CODE = 100;

    @Override
    public void onScrollUp() {
        floatingActionButton.zoomIn();
    }

    @Override
    public void onScrollDown() {
        floatingActionButton.zoomOut();
    }

    public enum LanguageEditorMode {
        Sort, Choose
    }

    @AutoAccess LanguageEditorMode mode;
    @AutoAccess ArrayList<TrendingLanguage> selectedLanguages;

    @Override
    protected LanguagesEditorFragment createFragment() {
        if (LanguageEditorMode.Sort.equals(mode)) {
            LanguagesEditorFragment fragment = LanguagesEditorFragment.create(mode);
            fragment.setListScrollListener(this);
            return fragment;
        } else {
            return LanguagesEditorFragment.createForChoose(mode, selectedLanguages);
        }
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        if (LanguageEditorMode.Sort.equals(mode)) {
            floatingActionButton.setVisibility(View.VISIBLE);
            floatingActionButton.setImageResource(R.drawable.ic_add);
            setToolbarTitle(getString(R.string.my_languages));
        } else {
            setToolbarTitle(getString(R.string.choose_languages));
        }
        setToolbarScrollAble(true);
    }

    public void onAddClick() {
        LanguagesEditorActivity.showForChoose(getActivity(), LanguageEditorMode.Choose,
                getFragment().getSelectedLanguages(), ADD_LANGUAGE_REQUEST_CODE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_confirm, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == ADD_LANGUAGE_REQUEST_CODE) {
            getFragment().updateLanguages();
        }

    }
}
