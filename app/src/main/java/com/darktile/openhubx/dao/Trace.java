package com.darktile.openhubx.dao;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Date;

/** Room entity mapped to table "TRACE" (migrated from greenDAO). */
@Entity(tableName = "TRACE")
public class Trace {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "ID")
    private String id;

    @ColumnInfo(name = "TYPE")
    private String type;

    @ColumnInfo(name = "USER_ID")
    private String userId;

    @ColumnInfo(name = "REPO_ID")
    private Long repoId;

    @ColumnInfo(name = "START_TIME")
    private Date startTime;

    @ColumnInfo(name = "LATEST_TIME")
    private Date latestTime;

    @ColumnInfo(name = "TRACE_NUM")
    private Integer traceNum;

    public Trace() {
    }

    @Ignore
    public Trace(@NonNull String id) {
        this.id = id;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
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
