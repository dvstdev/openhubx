

package com.darktile.openhubx.mvp.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.darktile.openhubx.R;
import com.darktile.openhubx.util.SearchQueryBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by ThirtyDegreesRay on 2017/8/25 14:31:21
 */

public class SearchModel implements Parcelable {

    public enum SearchType{
        Repository, User, Code, Issue
    }

    public static final List<Integer> REPO_SORT_ID_LIST = Arrays.asList(
            R.id.action_bast_match,
            R.id.action_most_stars, R.id.action_fewest_stars,
            R.id.action_most_forks, R.id.action_fewest_forks,
            R.id.action_recently_updated, R.id.action_least_recently_updated
    );

    public static final List<Integer> USER_SORT_ID_LIST = Arrays.asList(
            R.id.action_bast_match,
            R.id.action_most_followers, R.id.action_fewest_followers,
            R.id.action_most_recently_joined, R.id.action_least_recently_joined,
            R.id.action_most_repositories, R.id.action_fewest_repositories
    );

    public static final List<Integer> SORT_ID_LIST = new ArrayList<>();
    static {
        SORT_ID_LIST.addAll(REPO_SORT_ID_LIST);
        SORT_ID_LIST.addAll(USER_SORT_ID_LIST);
    }


    private SearchType type;
    private String query;
    private String sort = "";
    private boolean desc = true;
    /** Null for internal searches (topic / trending) whose query is already a raw q. */
    private SearchFilter filter;

    public SearchModel(SearchType type) {
        this.type = type;
    }

    public SearchModel(SearchType type, String query) {
        this.type = type;
        this.query = query;
    }

    public String getQuery() {
        return query;
    }

    public SearchModel setQuery(String query) {
        this.query = query;
        return this;
    }

    public SearchFilter getFilter() {
        return filter;
    }

    public SearchModel setFilter(SearchFilter filter) {
        this.filter = filter;
        return this;
    }

    /**
     * The q sent to the API. User searches (filter set) are composed and encoded by
     * {@link SearchQueryBuilder}; internal searches pass their pre-built query through.
     */
    public String getApiQuery() {
        if (filter == null) return query;
        return SearchQueryBuilder.build(query, filter, toTarget(type));
    }

    static SearchQueryBuilder.Target toTarget(SearchType type) {
        if (type == null) return SearchQueryBuilder.Target.REPOSITORIES;
        switch (type) {
            case User:
                return SearchQueryBuilder.Target.USERS;
            case Code:
                return SearchQueryBuilder.Target.CODE;
            case Issue:
                return SearchQueryBuilder.Target.ISSUES;
            default:
                return SearchQueryBuilder.Target.REPOSITORIES;
        }
    }

    public String getSort() {
        return sort;
    }

    public SearchModel setSort(String sort) {
        this.sort = sort;
        return this;
    }

    public SearchModel setSortId(int sortId) {
        if(!SORT_ID_LIST.contains(sortId)) {
            return this;
        }
        setSortWithSortId(sortId);
        setOrderWithSortId(sortId);
        return this;
    }

    public boolean isDesc() {
        return desc;
    }

    public SearchModel setDesc(boolean desc) {
        this.desc = desc;
        return this;
    }

    public String getOrder(){
        return desc ? "desc" : "asc";
    }

    public SearchType getType() {
        return type;
    }

    private void setSortWithSortId(int sortId){
        if (sortId == R.id.action_bast_match) {
            sort = "";
        } else if (sortId == R.id.action_most_stars || sortId == R.id.action_fewest_stars) {
            sort = "stars";
        } else if (sortId == R.id.action_most_forks || sortId == R.id.action_fewest_forks) {
            sort = "forks";
        } else if (sortId == R.id.action_recently_updated || sortId == R.id.action_least_recently_updated) {
            sort = "updated";
        } else if (sortId == R.id.action_most_followers || sortId == R.id.action_fewest_followers) {
            sort = "followers";
        } else if (sortId == R.id.action_most_recently_joined || sortId == R.id.action_least_recently_joined) {
            sort = "joined";
        } else if (sortId == R.id.action_most_repositories || sortId == R.id.action_fewest_repositories) {
            sort = "repositories";
        }
    }

    private void setOrderWithSortId(int sortId){
        if (sortId == R.id.action_bast_match
                || sortId == R.id.action_most_stars
                || sortId == R.id.action_most_forks
                || sortId == R.id.action_recently_updated
                || sortId == R.id.action_most_followers
                || sortId == R.id.action_most_recently_joined
                || sortId == R.id.action_most_repositories) {
            desc = true;
        } else if (sortId == R.id.action_fewest_stars
                || sortId == R.id.action_fewest_forks
                || sortId == R.id.action_least_recently_updated
                || sortId == R.id.action_fewest_followers
                || sortId == R.id.action_least_recently_joined
                || sortId == R.id.action_fewest_repositories) {
            desc = false;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.type == null ? -1 : this.type.ordinal());
        dest.writeString(this.query);
        dest.writeString(this.sort);
        dest.writeByte(this.desc ? (byte) 1 : (byte) 0);
        dest.writeSerializable(this.filter);
    }

    public SearchModel() {
    }

    protected SearchModel(Parcel in) {
        int tmpType = in.readInt();
        this.type = tmpType == -1 ? null : SearchType.values()[tmpType];
        this.query = in.readString();
        this.sort = in.readString();
        this.desc = in.readByte() != 0;
        this.filter = (SearchFilter) in.readSerializable();
    }

    public static final Creator<SearchModel> CREATOR = new Creator<SearchModel>() {
        @Override
        public SearchModel createFromParcel(Parcel source) {
            return new SearchModel(source);
        }

        @Override
        public SearchModel[] newArray(int size) {
            return new SearchModel[size];
        }
    };
}
