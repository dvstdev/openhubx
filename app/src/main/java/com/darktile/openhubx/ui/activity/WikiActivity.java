package com.darktile.openhubx.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;

import com.darktile.dataautoaccess.annotation.AutoAccess;
import com.darktile.openhubx.R;
import com.darktile.openhubx.mvp.contract.base.IBaseContract;
import com.darktile.openhubx.ui.activity.base.SingleFragmentActivity;
import com.darktile.openhubx.ui.fragment.WikiFragment;
import com.darktile.openhubx.util.BundleHelper;

/**
 * Created by ThirtyDegreesRay on 2017/12/6 16:22:02
 */

public class WikiActivity extends SingleFragmentActivity<IBaseContract.Presenter, WikiFragment> {

    public static void show(@NonNull Activity activity, @NonNull String owner,
                            @NonNull String repo){
        Intent intent = new Intent(activity, WikiActivity.class);
        intent.putExtras(BundleHelper.builder().put("owner", owner).put("repo", repo).build());
        activity.startActivity(intent);
    }

    @AutoAccess String owner;
    @AutoAccess String repo;

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        setToolbarTitle(getString(R.string.recent_wiki_updates), owner.concat("/").concat(repo));
        setToolbarScrollAble(true);
    }

    @Override
    protected WikiFragment createFragment() {
        return WikiFragment.create(owner, repo);
    }
}
