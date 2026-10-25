package com.darktile.openhubx.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/** Room DAO for {@link LocalRepo} (table LOCAL_REPO). */
@Dao
public interface LocalRepoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(LocalRepo localRepo);

    @Update
    void update(LocalRepo localRepo);

    @Delete
    void delete(LocalRepo localRepo);

    @Query("SELECT * FROM LOCAL_REPO WHERE ID = :id")
    LocalRepo load(long id);

    @Query("SELECT * FROM LOCAL_REPO")
    List<LocalRepo> loadAll();

    @Query("SELECT COUNT(*) FROM LOCAL_REPO")
    long count();
}
