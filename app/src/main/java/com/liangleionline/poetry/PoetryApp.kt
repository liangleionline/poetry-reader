package com.liangleionline.poetry

import android.app.Application
import androidx.room.Room
import com.liangleionline.poetry.data.UserDatabase

class PoetryApp : Application() {

    lateinit var userDb: UserDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        userDb = Room.databaseBuilder(
            this,
            UserDatabase::class.java,
            "user_db"
        ).build()
    }
}
