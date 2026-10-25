

package com.darktile.openhubx.mvp.presenter;

import android.os.Handler;

import com.darktile.dataautoaccess.annotation.AutoAccess;
import com.darktile.openhubx.R;
import com.darktile.openhubx.dao.DaoSession;
import com.darktile.openhubx.http.core.HttpObserver;
import com.darktile.openhubx.http.core.HttpResponse;
import com.darktile.openhubx.mvp.contract.ICommitDetailContract;
import com.darktile.openhubx.mvp.model.GitHubName;
import com.darktile.openhubx.mvp.model.RepoCommit;
import com.darktile.openhubx.mvp.model.RepoCommitExt;
import com.darktile.openhubx.mvp.presenter.base.BasePresenter;
import com.darktile.openhubx.util.GitHubHelper;
import com.darktile.openhubx.util.StringUtils;

import javax.inject.Inject;

import retrofit2.Response;
import io.reactivex.rxjava3.core.Observable;

/**
 * Created by ThirtyDegreesRay on 2017/10/18 11:31:06
 */

public class CommitDetailPresenter extends BasePresenter<ICommitDetailContract.View>
        implements ICommitDetailContract.Presenter {

    @AutoAccess String user;
    @AutoAccess String repo;
    @AutoAccess RepoCommit commit;
    @AutoAccess String commitSha;
    @AutoAccess String userAvatarUrl;
    @AutoAccess String commitUrl;
    private RepoCommitExt commitExt;


    @Inject
    public CommitDetailPresenter(DaoSession daoSession) {
        super(daoSession);
    }

    @Override
    public void onViewInitialized() {
        super.onViewInitialized();
        if(!StringUtils.isBlank(commitUrl)){
            if(getInfoFromCommitUrl()) loadCommitInfo();
            return;
        }
        if(commit != null) {
            mView.showCommit(commit);
            commitSha = commit.getSha();
        }else{
            mView.showUserAvatar(userAvatarUrl);
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(mView == null) return;
                loadCommitInfo();
            }
        }, 500);
    }

    @Override
    public void loadCommitInfo() {
        mView.showLoading();
        HttpObserver<RepoCommitExt> httpObserver = new HttpObserver<RepoCommitExt>() {
            @Override
            public void onError(Throwable error) {
                mView.hideLoading();
                mView.showErrorToast(getErrorTip(error));
            }

            @Override
            public void onSuccess(HttpResponse<RepoCommitExt> response) {
                mView.hideLoading();
                commitExt = response.body();
                mView.showCommitInfo(response.body());
            }
        };
        generalRxHttpExecute(new IObservableCreator<RepoCommitExt>() {
            @Override
            public Observable<Response<RepoCommitExt>> createObservable(boolean forceNetWork) {
                return getCommitService().getCommitInfo(forceNetWork, user, repo, commitSha);
            }
        }, httpObserver, true);
    }

    public RepoCommit getCommit() {
        return commit == null ? commitExt : commit;
    }

    //https://api.github.com/repos/dvstdev/openhubx/commits/c4dbb2eade18b510378e46c7281822fc88517105",
    private boolean getInfoFromCommitUrl(){
        commitUrl = commitUrl.replace("api.github.com/repos", "github.com");
        GitHubName gitHubName = GitHubName.fromUrl(commitUrl);
        if(!GitHubHelper.isCommitUrl(commitUrl) || gitHubName == null){
            mView.showErrorToast(getString(R.string.invalid_url));
            return false;
        }
        user = gitHubName.getUserName();
        repo = gitHubName.getRepoName();
        commitSha = gitHubName.getCommitShaName();
        return true;
    }

}
