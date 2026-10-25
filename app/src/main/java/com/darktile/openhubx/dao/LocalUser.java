package com.darktile.openhubx.dao;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/** Room entity mapped to table "LOCAL_USER" (migrated from greenDAO). */
@Entity(tableName = "LOCAL_USER")
public class LocalUser {

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

    public LocalUser() {
    }

    @Ignore
    public LocalUser(@NonNull String login) {
        this.login = login;
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
}
