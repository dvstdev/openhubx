package com.darktile.openhubx.dao;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** One search-history entry, scoped by search type (Repository / User / Code / Issue). */
@Entity(tableName = "SEARCH_HISTORY",
        indices = {@Index(value = {"TYPE", "QUERY"}, unique = true,
                name = "index_SEARCH_HISTORY_TYPE_QUERY")})
public class SearchHistory {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "ID")
    private long id;

    @NonNull
    @ColumnInfo(name = "TYPE")
    private String type = "";

    @NonNull
    @ColumnInfo(name = "QUERY")
    private String query = "";

    /** Ordering key; larger = more recent. */
    @ColumnInfo(name = "LAST_USED")
    private long lastUsed;

    public SearchHistory() {
    }

    @Ignore
    public SearchHistory(@NonNull String type, @NonNull String query, long lastUsed) {
        this.type = type;
        this.query = query;
        this.lastUsed = lastUsed;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getType() {
        return type;
    }

    public void setType(@NonNull String type) {
        this.type = type;
    }

    @NonNull
    public String getQuery() {
        return query;
    }

    public void setQuery(@NonNull String query) {
        this.query = query;
    }

    public long getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(long lastUsed) {
        this.lastUsed = lastUsed;
    }
}
