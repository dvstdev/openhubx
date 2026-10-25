

package com.darktile.openhubx.inject.component;

import com.darktile.openhubx.inject.ActivityScope;
import com.darktile.openhubx.inject.module.ActivityModule;
import com.darktile.openhubx.ui.activity.CommitDetailActivity;
import com.darktile.openhubx.ui.activity.EditIssueActivity;
import com.darktile.openhubx.ui.activity.IssueDetailActivity;
import com.darktile.openhubx.ui.activity.IssuesActivity;
import com.darktile.openhubx.ui.activity.LoginActivity;
import com.darktile.openhubx.ui.activity.MainActivity;
import com.darktile.openhubx.ui.activity.ProfileActivity;
import com.darktile.openhubx.ui.activity.ReleaseInfoActivity;
import com.darktile.openhubx.ui.activity.RepositoryActivity;
import com.darktile.openhubx.ui.activity.SearchActivity;
import com.darktile.openhubx.ui.activity.SettingsActivity;
import com.darktile.openhubx.ui.activity.SplashActivity;
import com.darktile.openhubx.ui.activity.TrendingActivity;

import dagger.Component;

/**
 * ActivityComponent
 * Created by ThirtyDegreesRay on 2016/8/30 14:56
 */
@ActivityScope
@Component(modules = ActivityModule.class, dependencies = AppComponent.class)
public interface ActivityComponent {
    void inject(SplashActivity activity);
    void inject(LoginActivity activity);
    void inject(MainActivity activity);
    void inject(SettingsActivity activity);
    void inject(RepositoryActivity activity);
    void inject(ProfileActivity activity);
    void inject(SearchActivity activity);
    void inject(ReleaseInfoActivity activity);
    void inject(IssuesActivity activity);
    void inject(IssueDetailActivity activity);
    void inject(EditIssueActivity activity);
    void inject(CommitDetailActivity activity);
    void inject(TrendingActivity activity);
}
