package com.liangleionline.poetry.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.liangleionline.poetry.util.DatabaseCopy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CollectionItem(val id: Long, val name: String, val count: Int)

data class PoemItem(
    val id: Long,
    val title: String,
    val author: String,
    val dynasty: String
)

data class PoemDetail(
    val id: Long,
    val title: String,
    val author: String,
    val dynasty: String,
    val content: String,
    val type: String,
    val source: String
)

/** 只读访问预置诗词库 poetry.db（LIKE 全文检索对 1.7 万行规模毫秒级响应） */
class PoetryRepository(private val context: Context) {

    private var db: SQLiteDatabase? = null

    private suspend fun database(): SQLiteDatabase = withContext(Dispatchers.IO) {
        db ?: synchronized(this) {
            db ?: SQLiteDatabase.openDatabase(
                DatabaseCopy.ensure(context).path,
                null,
                SQLiteDatabase.OPEN_READONLY
            ).also { db = it }
        }
    }

    suspend fun isReady(): Boolean = withContext(Dispatchers.IO) {
        DatabaseCopy.isReady(context)
    }

    suspend fun collections(): List<CollectionItem> = withContext(Dispatchers.IO) {
        val d = database()
        val list = ArrayList<CollectionItem>()
        d.rawQuery(
            """
            SELECT c.id, c.name, COUNT(pc.poem_id) AS cnt
            FROM collection c
            LEFT JOIN poem_collection pc ON pc.collection_id = c.id
            GROUP BY c.id
            ORDER BY c.id
            """.trimIndent(), null
        ).use { cur ->
            while (cur.moveToNext()) {
                list.add(CollectionItem(cur.getLong(0), cur.getString(1), cur.getInt(2)))
            }
        }
        list
    }

    suspend fun poemsByCollection(collectionId: Long, offset: Int, limit: Int): List<PoemItem> =
        withContext(Dispatchers.IO) {
            val d = database()
            val list = ArrayList<PoemItem>()
            d.rawQuery(
                """
                SELECT p.id, p.title,
                       COALESCE(po.name, '佚名') AS author,
                       COALESCE(po.dynasty, '')   AS dynasty
                FROM poem_collection pc
                JOIN poem p ON p.id = pc.poem_id
                LEFT JOIN poet po ON p.poet_id = po.id
                WHERE pc.collection_id = ?
                ORDER BY p.id
                LIMIT ? OFFSET ?
                """.trimIndent(),
                arrayOf(collectionId.toString(), limit.toString(), offset.toString())
            ).use { cur ->
                while (cur.moveToNext()) {
                    list.add(PoemItem(cur.getLong(0), cur.getString(1), cur.getString(2), cur.getString(3)))
                }
            }
            list
        }

    suspend fun poemDetail(id: Long): PoemDetail? = withContext(Dispatchers.IO) {
        val d = database()
        d.rawQuery(
            """
            SELECT p.id, p.title, COALESCE(po.name,'佚名') AS author,
                   COALESCE(po.dynasty,'') AS dynasty,
                   p.content, p.type, p.source
            FROM poem p LEFT JOIN poet po ON p.poet_id = po.id
            WHERE p.id = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(id.toString())
        ).use { cur ->
            if (cur.moveToFirst()) {
                PoemDetail(
                    cur.getLong(0), cur.getString(1), cur.getString(2), cur.getString(3),
                    cur.getString(4), cur.getString(5), cur.getString(6)
                )
            } else null
        }
    }

    /** LIKE 子串检索：标题 / 正文 / 作者 */
    suspend fun search(keyword: String): List<PoemItem> = withContext(Dispatchers.IO) {
        val kw = keyword.trim()
        if (kw.isEmpty()) return@withContext emptyList()
        val like = "%$kw%"
        val d = database()
        val list = ArrayList<PoemItem>()
        d.rawQuery(
            """
            SELECT p.id, p.title, COALESCE(po.name,'佚名') AS author,
                   COALESCE(po.dynasty,'') AS dynasty
            FROM poem p LEFT JOIN poet po ON p.poet_id = po.id
            WHERE p.title LIKE ? OR p.content LIKE ? OR COALESCE(po.name,'') LIKE ?
            ORDER BY CASE
                WHEN p.title LIKE ? THEN 0
                WHEN po.name LIKE ? THEN 1
                ELSE 2
            END, p.id
            LIMIT 100
            """.trimIndent(),
            arrayOf(like, like, like, like, like)
        ).use { cur ->
            while (cur.moveToNext()) {
                list.add(PoemItem(cur.getLong(0), cur.getString(1), cur.getString(2), cur.getString(3)))
            }
        }
        list
    }
}
