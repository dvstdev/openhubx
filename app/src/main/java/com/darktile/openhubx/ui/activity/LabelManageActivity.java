package com.darktile.openhubx.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.view.View;

import com.darktile.dataautoaccess.annotation.AutoAccess;
import com.darktile.openhubx.R;
import com.darktile.openhubx.mvp.contract.base.IBaseContract;
import com.darktile.openhubx.ui.activity.base.SingleFragmentActivity;
import com.darktile.openhubx.ui.fragment.LabelManageFragment;
import com.darktile.openhubx.util.BundleHelper;
/**
 * Created by ThirtyDegreesRay on 2018/1/11 10:50:09
 */

public class LabelManageActivity extends SingleFragmentActivity<IBaseContract.Presenter, LabelManageFragment> {

        @Override
        protected void bindViews(android.view.View root) {
            super.bindViews(root);
            floatingActionButton = root.findViewById(R.id.float_action_bn);
            root.findViewById(R.id.float_action_bn).setOnClickListener(v -> onCreateLabelClick());
        }

    public static void show(@NonNull Activity activity, @NonNull String owner, @NonNull String repo){
        Intent intent = new Intent(activity, LabelManageActivity.class);
        intent.putExtras(BundleHelper.builder().put("owner", owner).put("repo", repo).build());
        activity.startActivity(intent);
    }

    @AutoAccess String owner;
    @AutoAccess String repo;
     FloatingActionButton floatingActionButton;


    @Override
    protected LabelManageFragment createFragment() {
        return LabelManageFragment.create(owner, repo);
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        setToolbarTitle(getString(R.string.labels), owner + "/" + repo);
        floatingActionButton.setVisibility(View.VISIBLE);
        setToolbarScrollAble(true);
    }

     public void onCreateLabelClick(){
        getFragment().onCreateLabelClick();
    }

}
