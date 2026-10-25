package com.darktile.openhubx.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/** Room DAO for {@link LocalUser} (table LOCAL_USER). */
@Dao
public interface LocalUserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(LocalUser localUser);

    @Update
    void update(LocalUser localUser);

    @Delete
    void delete(LocalUser localUser);

    @Query("SELECT * FROM LOCAL_USER WHERE LOGIN = :login")
    LocalUser load(String login);

    @Query("SELECT * FROM LOCAL_USER")
    List<LocalUser> loadAll();

    @Query("SELECT COUNT(*) FROM LOCAL_USER")
    long count();
}
