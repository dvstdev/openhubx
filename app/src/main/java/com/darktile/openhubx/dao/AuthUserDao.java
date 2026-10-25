package com.darktile.openhubx.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/** Room DAO for {@link AuthUser} (table AUTH_USER). */
@Dao
public interface AuthUserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(AuthUser authUser);

    @Update
    void update(AuthUser authUser);

    @Delete
    void delete(AuthUser authUser);

    @Query("SELECT * FROM AUTH_USER")
    List<AuthUser> loadAll();

    @Query("SELECT * FROM AUTH_USER WHERE ACCESS_TOKEN = :accessToken")
    AuthUser load(String accessToken);

    /** The currently selected account, or null if none is selected. */
    @Query("SELECT * FROM AUTH_USER WHERE SELECTED = 1 LIMIT 1")
    AuthUser getSelected();

    /** Any first account (used as a fallback when none is selected). */
    @Query("SELECT * FROM AUTH_USER LIMIT 1")
    AuthUser getFirst();

    @Query("UPDATE AUTH_USER SET SELECTED = 0")
    void clearAllSelected();

    @Query("UPDATE AUTH_USER SET SELECTED = 1 WHERE LOGIN_ID = :loginId")
    void setSelectedByLoginId(String loginId);

    @Query("DELETE FROM AUTH_USER WHERE LOGIN_ID = :loginId")
    void deleteByLoginId(String loginId);

    @Query("SELECT COUNT(*) FROM AUTH_USER")
    long count();
}
