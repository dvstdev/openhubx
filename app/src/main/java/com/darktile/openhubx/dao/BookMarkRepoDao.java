package com.darktile.openhubx.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/** Room DAO for {@link BookMarkRepo} (table BOOK_MARK_REPO). */
@Dao
public interface BookMarkRepoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(BookMarkRepo bookMarkRepo);

    @Delete
    void delete(BookMarkRepo bookMarkRepo);

    @Query("SELECT * FROM BOOK_MARK_REPO")
    List<BookMarkRepo> loadAll();

    @Query("SELECT * FROM BOOK_MARK_REPO ORDER BY MARK_TIME DESC LIMIT :limit OFFSET :offset")
    List<BookMarkRepo> loadPaged(int limit, int offset);

    @Query("SELECT COUNT(*) FROM BOOK_MARK_REPO")
    long count();
}
