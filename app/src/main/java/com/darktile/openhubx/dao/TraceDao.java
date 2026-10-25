package com.darktile.openhubx.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/** Room DAO for {@link Trace} (table TRACE). */
@Dao
public interface TraceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Trace trace);

    @Update
    void update(Trace trace);

    @Delete
    void delete(Trace trace);

    @Query("DELETE FROM TRACE WHERE ID = :id")
    void deleteByKey(String id);

    @Query("SELECT * FROM TRACE")
    List<Trace> loadAll();

    @Query("SELECT * FROM TRACE ORDER BY LATEST_TIME DESC LIMIT :limit OFFSET :offset")
    List<Trace> loadPaged(int limit, int offset);

    @Query("SELECT * FROM TRACE WHERE USER_ID = :userId LIMIT 1")
    Trace findOneByUserId(String userId);

    @Query("SELECT * FROM TRACE WHERE REPO_ID = :repoId LIMIT 1")
    Trace findOneByRepoId(long repoId);

    @Query("SELECT COUNT(*) FROM TRACE")
    long count();
}
