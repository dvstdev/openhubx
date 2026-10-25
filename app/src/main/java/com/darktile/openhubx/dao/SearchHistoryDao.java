package com.darktile.openhubx.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/** Room DAO for {@link SearchHistory} (table SEARCH_HISTORY). */
@Dao
public interface SearchHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<SearchHistory> entries);

    @Query("SELECT QUERY FROM SEARCH_HISTORY WHERE TYPE = :type "
            + "ORDER BY LAST_USED DESC, ID DESC LIMIT :limit")
    List<String> loadQueries(String type, int limit);

    /** {@code pattern} is a LIKE pattern escaped with '\' (see SearchHistoryPolicy). */
    @Query("SELECT QUERY FROM SEARCH_HISTORY WHERE TYPE = :type AND QUERY LIKE :pattern ESCAPE '\\' "
            + "ORDER BY LAST_USED DESC, ID DESC LIMIT :limit")
    List<String> searchQueries(String type, String pattern, int limit);

    @Query("DELETE FROM SEARCH_HISTORY WHERE TYPE = :type")
    void deleteByType(String type);

    @Query("DELETE FROM SEARCH_HISTORY")
    void deleteAll();
}
