package com.darktile.openhubx.dao;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

/** Room entity mapped to table "TRACE_USER" (migrated from greenDAO). */
@Entity(tableName = "TRACE_USER")
public class TraceUser {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "LOGIN")
    private String login;

    @ColumnInfo(name = "NAME")
    private String name;

    @ColumnInfo(name = "AVATAR_URL")
    private String avatarUrl;

    @ColumnInfo(name = "FOLLOWERS")
    private Integer followers;

    @ColumnInfo(name = "FOLLOWING")
    private Integer following;

    @ColumnInfo(name = "START_TIME")
    private Date startTime;

    @ColumnInfo(name = "LATEST_TIME")
    private Date latestTime;

    @ColumnInfo(name = "TRACE_NUM")
    private Integer traceNum;

    public TraceUser() {
    }

    @NonNull
    public String getLogin() {
        return login;
    }

    public void setLogin(@NonNull String login) {
        this.login = login;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Integer getFollowers() {
        return followers;
    }

    public void setFollowers(Integer followers) {
        this.followers = followers;
    }

    public Integer getFollowing() {
        return following;
    }

    public void setFollowing(Integer following) {
        this.following = following;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getLatestTime() {
        return latestTime;
    }

    public void setLatestTime(Date latestTime) {
        this.latestTime = latestTime;
    }

    public Integer getTraceNum() {
        return traceNum;
    }

    public void setTraceNum(Integer traceNum) {
        this.traceNum = traceNum;
    }
}
