

package com.darktile.openhubx.mvp.presenter;

import androidx.annotation.NonNull;

import com.darktile.dataautoaccess.annotation.AutoAccess;
import com.darktile.openhubx.R;
import com.darktile.openhubx.dao.DaoSession;
import com.darktile.openhubx.dao.MyTrendingLanguage;
import com.darktile.openhubx.dao.SearchHistory;
import com.darktile.openhubx.dao.SearchHistoryDao;
import com.darktile.openhubx.mvp.contract.ISearchContract;
import com.darktile.openhubx.mvp.model.SearchFilter;
import com.darktile.openhubx.mvp.model.SearchModel;
import com.darktile.openhubx.mvp.model.TrendingLanguage;
import com.darktile.openhubx.mvp.presenter.base.BasePresenter;
import com.darktile.openhubx.util.JSONUtils;
import com.darktile.openhubx.util.PrefUtils;
import com.darktile.openhubx.util.SearchHistoryPolicy;
import com.darktile.openhubx.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Created by ThirtyDegreesRay on 2017/8/25 14:56:23
 */

public class SearchPresenter extends BasePresenter<ISearchContract.View>
        implements ISearchContract.Presenter {

    /** Max suggestions shown in the dropdown. */
    private static final int SUGGESTION_LIMIT = 20;

    @AutoAccess ArrayList<SearchModel> searchModels;

    private boolean legacyHistoryChecked = false;

    @Inject
    public SearchPresenter(DaoSession daoSession) {
        super(daoSession);
    }

    @Override
    public void onViewInitialized() {
        super.onViewInitialized();
        if (searchModels == null) {
            createSearchModels();
        } else {
            mView.showSearches(searchModels);
        }
    }

    private void createSearchModels() {
        SearchFilter filter = new SearchFilter();
        searchModels = new ArrayList<>();
        searchModels.add(new SearchModel(SearchModel.SearchType.Repository)
                .setSort("stars").setDesc(true).setFilter(filter));
        searchModels.add(new SearchModel(SearchModel.SearchType.User).setFilter(filter));
        searchModels.add(new SearchModel(SearchModel.SearchType.Code).setFilter(filter));
        searchModels.add(new SearchModel(SearchModel.SearchType.Issue).setFilter(filter));
    }

    public ArrayList<SearchModel> getSearchModels() {
        return searchModels;
    }

    @Override
    public ArrayList<SearchModel> getQueryModels(@NonNull String query) {
        for (SearchModel searchModel : searchModels) {
            searchModel.setQuery(query);
        }
        return searchModels;
    }

    @Override
    public SearchModel getSortModel(int page, int sortId) {
        return searchModels.get(page).setSortId(sortId);
    }

    @NonNull
    @Override
    public SearchFilter getFilter() {
        SearchFilter filter = searchModels.get(0).getFilter();
        return filter == null ? new SearchFilter() : filter;
    }

    @Override
    public void setFilter(@NonNull SearchFilter filter) {
        for (SearchModel searchModel : searchModels) {
            searchModel.setFilter(filter);
        }
    }

    /** The user's trending languages (or the bundled default list), without "all"/"unknown". */
    @NonNull
    @Override
    public ArrayList<TrendingLanguage> getFilterLanguages() {
        List<MyTrendingLanguage> myLanguages = daoSession.getMyTrendingLanguageDao().loadAllOrderByOrderAsc();
        ArrayList<TrendingLanguage> source = StringUtils.isBlankList(myLanguages)
                ? JSONUtils.jsonToArrayList(getString(R.string.trending_languages), TrendingLanguage.class)
                : TrendingLanguage.generateFromDB(myLanguages);
        ArrayList<TrendingLanguage> languages = new ArrayList<>();
        if (source == null) return languages;
        for (TrendingLanguage language : source) {
            String slug = language.getSlug();
            if (StringUtils.isBlank(slug)) continue;
            if (slug.contains("?")) slug = slug.substring(0, slug.indexOf("?"));
            if (StringUtils.isBlank(slug) || "all".equals(slug) || "unknown".equals(slug)) continue;
            languages.add(new TrendingLanguage(language.getName(), slug));
        }
        return languages;
    }

    @NonNull
    @Override
    public List<String> getSearchRecordList(@NonNull SearchModel.SearchType type, String text) {
        importLegacyHistoryOnce();
        SearchHistoryDao dao = daoSession.getSearchHistoryDao();
        if (StringUtils.isBlank(text)) {
            return dao.loadQueries(type.name(), SUGGESTION_LIMIT);
        }
        return dao.searchQueries(type.name(), SearchHistoryPolicy.containsPattern(text), SUGGESTION_LIMIT);
    }

    @Override
    public void addSearchRecord(@NonNull SearchModel.SearchType type, @NonNull final String record) {
        importLegacyHistoryOnce();
        if (SearchHistoryPolicy.normalize(record) == null) return;
        final SearchHistoryDao dao = daoSession.getSearchHistoryDao();
        final String typeName = type.name();
        daoSession.runInTx(() -> {
            List<String> current = dao.loadQueries(typeName, SearchHistoryPolicy.MAX_PER_TYPE);
            List<String> next = SearchHistoryPolicy.push(current, record, SearchHistoryPolicy.MAX_PER_TYPE);
            dao.deleteByType(typeName);
            dao.insertAll(toEntities(typeName, next));
        });
    }

    @Override
    public void clearSearchRecords() {
        importLegacyHistoryOnce();
        daoSession.getSearchHistoryDao().deleteAll();
    }

    /** One-time move of the old "$$"-joined preference history into SEARCH_HISTORY. */
    private void importLegacyHistoryOnce() {
        if (legacyHistoryChecked) return;
        legacyHistoryChecked = true;
        String legacy = PrefUtils.getSearchRecords();
        if (legacy == null) return;
        final List<String> records = SearchHistoryPolicy.parseLegacy(legacy, SearchHistoryPolicy.MAX_PER_TYPE);
        if (!records.isEmpty()) {
            final SearchHistoryDao dao = daoSession.getSearchHistoryDao();
            daoSession.runInTx(() -> {
                // The old history was shared by the Repository and User tabs.
                for (SearchModel.SearchType type : new SearchModel.SearchType[]{
                        SearchModel.SearchType.Repository, SearchModel.SearchType.User}) {
                    List<String> merged = new ArrayList<>(dao.loadQueries(type.name(),
                            SearchHistoryPolicy.MAX_PER_TYPE));
                    merged.addAll(records);
                    List<String> next = SearchHistoryPolicy.dedupAndCap(merged,
                            SearchHistoryPolicy.MAX_PER_TYPE);
                    dao.deleteByType(type.name());
                    dao.insertAll(toEntities(type.name(), next));
                }
            });
        }
        PrefUtils.clearKey(PrefUtils.SEARCH_RECORDS);
    }

    /** Newest first -> descending LAST_USED so the order survives the round trip. */
    private static List<SearchHistory> toEntities(String type, List<String> newestFirst) {
        long now = System.currentTimeMillis();
        List<SearchHistory> entities = new ArrayList<>();
        for (int i = 0; i < newestFirst.size(); i++) {
            entities.add(new SearchHistory(type, newestFirst.get(i), now - i));
        }
        return entities;
    }

}
