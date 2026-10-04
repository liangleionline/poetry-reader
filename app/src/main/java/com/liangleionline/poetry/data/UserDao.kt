package com.liangleionline.poetry.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    // ---------- 收藏 ----------
    @Query("SELECT * FROM favorite ORDER BY add_time DESC")
    fun observeFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorite WHERE poem_id = :poemId LIMIT 1")
    suspend fun getFavorite(poemId: Long): FavoriteEntity?

    @Insert
    suspend fun insertFavorite(f: FavoriteEntity)

    @Query("DELETE FROM favorite WHERE poem_id = :poemId")
    suspend fun deleteFavorite(poemId: Long)

    // ---------- 书签 ----------
    @Query("SELECT * FROM bookmark ORDER BY add_time DESC")
    fun observeBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmark WHERE poem_id = :poemId LIMIT 1")
    suspend fun getBookmark(poemId: Long): BookmarkEntity?

    @Insert
    suspend fun insertBookmark(b: BookmarkEntity)

    @Query("DELETE FROM bookmark WHERE poem_id = :poemId")
    suspend fun deleteBookmark(poemId: Long)

    // ---------- 阅读进度 ----------
    @Query("SELECT * FROM progress WHERE poem_id = :poemId")
    suspend fun getProgress(poemId: Long): ProgressEntity?

    @Upsert
    suspend fun upsertProgress(p: ProgressEntity)

    // ---------- 数据清理 ----------
    @Query("DELETE FROM favorite")
    suspend fun clearFavorites()

    @Query("DELETE FROM bookmark")
    suspend fun clearBookmarks()

    @Query("DELETE FROM progress")
    suspend fun clearProgress()
}
