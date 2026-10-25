package com.darktile.openhubx.dao;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

/** Room entity mapped to table "BOOK_MARK_REPO" (migrated from greenDAO). */
@Entity(tableName = "BOOK_MARK_REPO")
public class BookMarkRepo {

    @PrimaryKey
    @ColumnInfo(name = "ID")
    private long id;

    @NonNull
    @ColumnInfo(name = "NAME")
    private String name;

    @ColumnInfo(name = "DESCRIPTION")
    private String description;

    @ColumnInfo(name = "LANGUAGE")
    private String language;

    @ColumnInfo(name = "STARGAZERS_COUNT")
    private Integer stargazersCount;

    @ColumnInfo(name = "WATCHERS_COUNT")
    private Integer watchersCount;

    @ColumnInfo(name = "FORKS_COUNT")
    private Integer forksCount;

    @ColumnInfo(name = "FORK")
    private Boolean fork;

    @ColumnInfo(name = "OWNER_LOGIN")
    private String ownerLogin;

    @ColumnInfo(name = "OWNER_AVATAR_URL")
    private String ownerAvatarUrl;

    @ColumnInfo(name = "MARK_TIME")
    private Date markTime;

    public BookMarkRepo() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Integer getStargazersCount() {
        return stargazersCount;
    }

    public void setStargazersCount(Integer stargazersCount) {
        this.stargazersCount = stargazersCount;
    }

    public Integer getWatchersCount() {
        return watchersCount;
    }

    public void setWatchersCount(Integer watchersCount) {
        this.watchersCount = watchersCount;
    }

    public Integer getForksCount() {
        return forksCount;
    }

    public void setForksCount(Integer forksCount) {
        this.forksCount = forksCount;
    }

    public Boolean getFork() {
        return fork;
    }

    public void setFork(Boolean fork) {
        this.fork = fork;
    }

    public String getOwnerLogin() {
        return ownerLogin;
    }

    public void setOwnerLogin(String ownerLogin) {
        this.ownerLogin = ownerLogin;
    }

    public String getOwnerAvatarUrl() {
        return ownerAvatarUrl;
    }

    public void setOwnerAvatarUrl(String ownerAvatarUrl) {
        this.ownerAvatarUrl = ownerAvatarUrl;
    }

    public Date getMarkTime() {
        return markTime;
    }

    public void setMarkTime(Date markTime) {
        this.markTime = markTime;
    }
}
