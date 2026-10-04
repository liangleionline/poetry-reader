package com.liangleionline.poetry.util

import android.content.Context
import java.io.File
import java.io.FileOutputStream

/** 首次启动把 assets 中的 poetry.db 拷贝到应用私有目录（仅执行一次） */
object DatabaseCopy {

    const val DB_NAME = "poetry.db"

    /** 返回数据库文件；若不存在则从 assets 拷贝。阻塞调用，需在 IO 线程执行。 */
    @Synchronized
    fun ensure(context: Context): File {
        val dir = context.getDatabasePath(DB_NAME).parentFile
        val dbFile = context.getDatabasePath(DB_NAME)
        if (dbFile.exists() && dbFile.length() > 0L) return dbFile

        dir?.mkdirs()
        context.assets.open(DB_NAME).use { input ->
            FileOutputStream(dbFile).use { output ->
                val buffer = ByteArray(64 * 1024)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                }
                output.flush()
            }
        }
        return dbFile
    }

    fun isReady(context: Context): Boolean =
        context.getDatabasePath(DB_NAME).exists() &&
            context.getDatabasePath(DB_NAME).length() > 0L
}
