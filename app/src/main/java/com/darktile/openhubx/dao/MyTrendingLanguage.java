package com.darktile.openhubx.dao;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/** Room entity mapped to table "MY_TRENDING_LANGUAGE" (migrated from greenDAO). */
@Entity(tableName = "MY_TRENDING_LANGUAGE")
public class MyTrendingLanguage {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "SLUG")
    private String slug;

    @NonNull
    @ColumnInfo(name = "NAME")
    private String name;

    /** Column name "ORDER" is a SQL keyword; kept for on-disk parity with greenDAO. */
    @ColumnInfo(name = "ORDER")
    private int order;

    public MyTrendingLanguage() {
    }

    @Ignore
    public MyTrendingLanguage(@NonNull String slug, @NonNull String name, int order) {
        this.slug = slug;
        this.name = name;
        this.order = order;
    }

    @NonNull
    public String getSlug() {
        return slug;
    }

    public void setSlug(@NonNull String slug) {
        this.slug = slug;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
