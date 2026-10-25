

package com.darktile.openhubx.inject.component;

import com.darktile.openhubx.inject.FragmentScope;
import com.darktile.openhubx.inject.module.FragmentModule;
import com.darktile.openhubx.ui.fragment.CollectionsFragment;
import com.darktile.openhubx.ui.fragment.LabelManageFragment;
import com.darktile.openhubx.ui.fragment.LanguagesEditorFragment;
import com.darktile.openhubx.ui.fragment.ActivityFragment;
import com.darktile.openhubx.ui.fragment.BookmarksFragment;
import com.darktile.openhubx.ui.fragment.CommitFilesFragment;
import com.darktile.openhubx.ui.fragment.CommitsFragment;
import com.darktile.openhubx.ui.fragment.IssueTimelineFragment;
import com.darktile.openhubx.ui.fragment.IssuesFragment;
import com.darktile.openhubx.ui.fragment.NotificationsFragment;
import com.darktile.openhubx.ui.fragment.ProfileInfoFragment;
import com.darktile.openhubx.ui.fragment.ReleasesFragment;
import com.darktile.openhubx.ui.fragment.RepoFilesFragment;
import com.darktile.openhubx.ui.fragment.RepoInfoFragment;
import com.darktile.openhubx.ui.fragment.RepositoriesFragment;
import com.darktile.openhubx.ui.fragment.TopicsFragment;
import com.darktile.openhubx.ui.fragment.TraceFragment;
import com.darktile.openhubx.ui.fragment.UserListFragment;
import com.darktile.openhubx.ui.fragment.ViewerFragment;
import com.darktile.openhubx.ui.fragment.WikiFragment;

import dagger.Component;

/**
 * Created on 2017/7/18.
 *
 * @author ThirtyDegreesRay
 */

@FragmentScope
@Component(modules = FragmentModule.class, dependencies = AppComponent.class)
public interface FragmentComponent {
    void inject(RepositoriesFragment fragment);
    void inject(RepoInfoFragment fragment);
    void inject(RepoFilesFragment fragment);
    void inject(UserListFragment fragment);
    void inject(ViewerFragment fragment);
    void inject(ProfileInfoFragment fragment);
    void inject(ActivityFragment fragment);
    void inject(ReleasesFragment fragment);
    void inject(IssuesFragment fragment);
    void inject(IssueTimelineFragment fragment);
    void inject(CommitsFragment fragment);
    void inject(CommitFilesFragment fragment);
    void inject(NotificationsFragment fragment);
    void inject(BookmarksFragment fragment);
    void inject(TraceFragment fragment);
    void inject(LanguagesEditorFragment fragment);
    void inject(WikiFragment fragment);
    void inject(CollectionsFragment fragment);
    void inject(TopicsFragment fragment);
    void inject(LabelManageFragment fragment);
}
