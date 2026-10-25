

package com.darktile.openhubx.mvp.contract;

import androidx.annotation.NonNull;

import com.darktile.openhubx.mvp.contract.base.IBaseContract;
import com.darktile.openhubx.mvp.model.SearchFilter;
import com.darktile.openhubx.mvp.model.SearchModel;
import com.darktile.openhubx.mvp.model.TrendingLanguage;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ThirtyDegreesRay on 2017/8/25 14:55:29
 */

public interface ISearchContract {

    interface View extends IBaseContract.View{
        void showSearches(ArrayList<SearchModel> searchModels);
    }

    interface Presenter extends IBaseContract.Presenter<ISearchContract.View>{
        ArrayList<SearchModel> getQueryModels(@NonNull String query);
        SearchModel getSortModel(int page, int sortId);
        /** History for {@code type}, newest first, filtered by {@code text} when not blank. */
        @NonNull List<String> getSearchRecordList(@NonNull SearchModel.SearchType type, String text);
        void addSearchRecord(@NonNull SearchModel.SearchType type, @NonNull String record);
        void clearSearchRecords();
        @NonNull SearchFilter getFilter();
        void setFilter(@NonNull SearchFilter filter);
        @NonNull ArrayList<TrendingLanguage> getFilterLanguages();
    }

}
