package com.darktile.openhubx.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/** Room DAO for {@link TraceRepo} (table TRACE_REPO). */
@Dao
public interface TraceRepoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(TraceRepo traceRepo);

    @Delete
    void delete(TraceRepo traceRepo);

    @Query("SELECT * FROM TRACE_REPO")
    List<TraceRepo> loadAll();

    @Query("SELECT * FROM TRACE_REPO ORDER BY LATEST_TIME DESC LIMIT :limit OFFSET :offset")
    List<TraceRepo> loadPaged(int limit, int offset);

    @Query("SELECT COUNT(*) FROM TRACE_REPO")
    long count();
}
