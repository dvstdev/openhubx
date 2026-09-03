package com.thirtydegreesray.openhub.mvp.presenter;

import com.thirtydegreesray.openhub.AppConfig;
import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.dao.DaoSession;
import com.thirtydegreesray.openhub.http.core.HttpObserver;
import com.thirtydegreesray.openhub.http.core.HttpResponse;
import com.thirtydegreesray.openhub.mvp.contract.ITopicsContract;
import com.thirtydegreesray.openhub.mvp.model.Topic;
import com.thirtydegreesray.openhub.mvp.presenter.base.BasePresenter;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import javax.inject.Inject;

import okhttp3.ResponseBody;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by ThirtyDegreesRay on 2017/12/29 11:10:27
 */

public class TopicsPresenter extends BasePresenter<ITopicsContract.View>
        implements ITopicsContract.Presenter {

    private ArrayList<Topic> topics;

    @Inject
    public TopicsPresenter(DaoSession daoSession) {
        super(daoSession);
    }

    @Override
    public void onViewInitialized() {
        super.onViewInitialized();
        loadTopics(false);
    }

    @Override
    public void loadTopics(boolean isReload) {
        mView.showLoading();
        HttpObserver<ResponseBody> httpObserver = new HttpObserver<ResponseBody>() {
            @Override
            public void onError(Throwable error) {
                mView.hideLoading();
                mView.showLoadError(getErrorTip(error));
            }

            @Override
            public void onSuccess(HttpResponse<ResponseBody> response) {
                try {
                    parsePageData(response.body().string());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        generalRxHttpExecute(forceNetWork -> getGitHubWebPageService().getTopics(forceNetWork),
                httpObserver, !isReload);

    }

    private void parsePageData(String page) {
        Observable.just(page)
                .map(s -> {
                    ArrayList<Topic> topics = new ArrayList<>();
                    try {
                        Document doc = Jsoup.parse(s, AppConfig.GITHUB_BASE_URL);
                        topics.addAll(getTopics(doc));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return topics;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(results -> {
                    if(mView == null) return;
                    if(results.size() != 0){
                        topics = results;
                        mView.hideLoading();
                        mView.showTopics(topics);
                    } else {
                        String errorTip = String.format(getString(R.string.github_page_parse_error),
                                getString(R.string.topics));
                        mView.showLoadError(errorTip);
                        mView.hideLoading();
                    }
                });
    }

    private ArrayList<Topic> getTopics(Document doc) {
        ArrayList<Topic> list = new ArrayList<>();
        HashSet<String> seen = new HashSet<>();
        // Key on the stable /topics/{id} link + its title/desc <p> tags instead of
        // GitHub's churny utility classes.
        Elements anchors = doc.select("a[href^=/topics/]");
        for (Element a : anchors) {
            Elements ps = a.select("p");
            if (ps.size() == 0) continue; // skip the image-only anchor and nav links
            String href = a.attr("href");
            String id = href.substring(href.lastIndexOf("/") + 1);
            if (id.isEmpty() || seen.contains(id)) continue;
            String name = ps.get(0).text().trim();
            if (name.isEmpty()) continue;
            String desc = ps.size() > 1 ? ps.get(1).text().trim() : "";
            String image = null;
            Element parent = a.parent();
            if (parent != null) {
                Element img = parent.select("a[href^=/topics/] > img").first();
                if (img != null) image = img.attr("src");
            }
            seen.add(id);
            list.add(new Topic().setId(id).setName(name).setDesc(desc).setImage(image));
        }
        return list;
    }


}
