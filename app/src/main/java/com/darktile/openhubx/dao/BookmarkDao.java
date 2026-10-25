package com.darktile.openhubx.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/** Room DAO for {@link Bookmark} (table BOOKMARK). */
@Dao
public interface BookmarkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Bookmark bookmark);

    @Delete
    void delete(Bookmark bookmark);

    @Query("DELETE FROM BOOKMARK WHERE ID = :id")
    void deleteByKey(String id);

    @Query("SELECT * FROM BOOKMARK")
    List<Bookmark> loadAll();

    @Query("SELECT * FROM BOOKMARK ORDER BY MARK_TIME DESC LIMIT :limit OFFSET :offset")
    List<Bookmark> loadPaged(int limit, int offset);

    @Query("SELECT * FROM BOOKMARK WHERE USER_ID = :userId LIMIT 1")
    Bookmark findOneByUserId(String userId);

    @Query("SELECT * FROM BOOKMARK WHERE REPO_ID = :repoId LIMIT 1")
    Bookmark findOneByRepoId(long repoId);

    @Query("SELECT COUNT(*) FROM BOOKMARK WHERE USER_ID = :userId")
    long countByUserId(String userId);

    @Query("SELECT COUNT(*) FROM BOOKMARK WHERE REPO_ID = :repoId")
    long countByRepoId(long repoId);

    @Query("SELECT COUNT(*) FROM BOOKMARK")
    long count();
}
