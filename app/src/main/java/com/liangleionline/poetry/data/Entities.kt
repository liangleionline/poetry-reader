package com.liangleionline.poetry.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite")
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "poem_id") val poemId: Long,
    val title: String,
    val author: String,
    @ColumnInfo(name = "add_time") val addTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "bookmark")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "poem_id") val poemId: Long,
    val title: String,
    val author: String,
    @ColumnInfo(name = "add_time") val addTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "progress")
data class ProgressEntity(
    @PrimaryKey @ColumnInfo(name = "poem_id") val poemId: Long,
    val position: Int,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
