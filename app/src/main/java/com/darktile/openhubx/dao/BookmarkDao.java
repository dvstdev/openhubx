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

    // All reads below are scoped to one account (OWNER_LOGIN_ID) so bookmarks are
    // per-account: switching accounts shows that account's own bookmarks, and never
    // mixes or drops another account's rows.

    @Query("SELECT * FROM BOOKMARK WHERE OWNER_LOGIN_ID = :owner")
    List<Bookmark> loadAllByOwner(String owner);

    @Query("SELECT * FROM BOOKMARK WHERE OWNER_LOGIN_ID = :owner ORDER BY MARK_TIME DESC LIMIT :limit OFFSET :offset")
    List<Bookmark> loadPaged(String owner, int limit, int offset);

    @Query("SELECT * FROM BOOKMARK WHERE USER_ID = :userId AND OWNER_LOGIN_ID = :owner LIMIT 1")
    Bookmark findOneByUserId(String userId, String owner);

    @Query("SELECT * FROM BOOKMARK WHERE REPO_ID = :repoId AND OWNER_LOGIN_ID = :owner LIMIT 1")
    Bookmark findOneByRepoId(long repoId, String owner);

    @Query("SELECT COUNT(*) FROM BOOKMARK WHERE USER_ID = :userId AND OWNER_LOGIN_ID = :owner")
    long countByUserId(String userId, String owner);

    @Query("SELECT COUNT(*) FROM BOOKMARK WHERE REPO_ID = :repoId AND OWNER_LOGIN_ID = :owner")
    long countByRepoId(long repoId, String owner);

    @Query("SELECT COUNT(*) FROM BOOKMARK")
    long count();
}
