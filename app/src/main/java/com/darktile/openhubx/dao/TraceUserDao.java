package com.darktile.openhubx.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/** Room DAO for {@link TraceUser} (table TRACE_USER). */
@Dao
public interface TraceUserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(TraceUser traceUser);

    @Delete
    void delete(TraceUser traceUser);

    @Query("SELECT * FROM TRACE_USER")
    List<TraceUser> loadAll();

    @Query("SELECT * FROM TRACE_USER ORDER BY LATEST_TIME DESC LIMIT :limit OFFSET :offset")
    List<TraceUser> loadPaged(int limit, int offset);

    @Query("SELECT COUNT(*) FROM TRACE_USER")
    long count();
}
