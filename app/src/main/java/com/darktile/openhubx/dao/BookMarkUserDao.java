package com.darktile.openhubx.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/** Room DAO for {@link BookMarkUser} (table BOOK_MARK_USER). */
@Dao
public interface BookMarkUserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(BookMarkUser bookMarkUser);

    @Delete
    void delete(BookMarkUser bookMarkUser);

    @Query("SELECT * FROM BOOK_MARK_USER")
    List<BookMarkUser> loadAll();

    @Query("SELECT * FROM BOOK_MARK_USER ORDER BY MARK_TIME DESC LIMIT :limit OFFSET :offset")
    List<BookMarkUser> loadPaged(int limit, int offset);

    @Query("SELECT COUNT(*) FROM BOOK_MARK_USER")
    long count();
}
