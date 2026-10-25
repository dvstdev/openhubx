package com.darktile.openhubx.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/** Room DAO for {@link MyTrendingLanguage} (table MY_TRENDING_LANGUAGE). */
@Dao
public interface MyTrendingLanguageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<MyTrendingLanguage> languages);

    @Query("DELETE FROM MY_TRENDING_LANGUAGE")
    void deleteAll();

    @Query("SELECT * FROM MY_TRENDING_LANGUAGE ORDER BY \"ORDER\" ASC")
    List<MyTrendingLanguage> loadAllOrderByOrderAsc();

    @Query("SELECT COUNT(*) FROM MY_TRENDING_LANGUAGE")
    long count();
}
