package com.darktile.openhubx.dao;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Date;

/** Room entity mapped to table "BOOKMARK" (migrated from greenDAO). */
@Entity(tableName = "BOOKMARK")
public class Bookmark {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "ID")
    private String id;

    @NonNull
    @ColumnInfo(name = "TYPE")
    private String type;

    @ColumnInfo(name = "USER_ID")
    private String userId;

    @ColumnInfo(name = "REPO_ID")
    private Long repoId;

    @ColumnInfo(name = "MARK_TIME")
    private Date markTime;

    /** Login id of the account that owns this bookmark (per-account scoping, schema v6). */
    @ColumnInfo(name = "OWNER_LOGIN_ID")
    private String ownerLoginId;

    public Bookmark() {
    }

    @Ignore
    public Bookmark(@NonNull String id) {
        this.id = id;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    @NonNull
    public String getType() {
        return type;
    }

    public void setType(@NonNull String type) {
        this.type = type;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Long getRepoId() {
        return repoId;
    }

    public void setRepoId(Long repoId) {
        this.repoId = repoId;
    }

    public Date getMarkTime() {
        return markTime;
    }

    public void setMarkTime(Date markTime) {
        this.markTime = markTime;
    }

    public String getOwnerLoginId() {
        return ownerLoginId;
    }

    public void setOwnerLoginId(String ownerLoginId) {
        this.ownerLoginId = ownerLoginId;
    }
}
