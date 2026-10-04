package com.liangleionline.poetry.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.liangleionline.poetry.PoetryApp
import com.liangleionline.poetry.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = PoetryRepository(app)
    private val dao: UserDao = (app as PoetryApp).userDb.userDao()

    // 书架
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready
    private val _collections = MutableStateFlow<List<CollectionItem>>(emptyList())
    val collections: StateFlow<List<CollectionItem>> = _collections

    // 列表页
    private val _collectionPoems = MutableStateFlow<List<PoemItem>>(emptyList())
    val collectionPoems: StateFlow<List<PoemItem>> = _collectionPoems
    private var listOffset = 0
    private var currentCollectionId: Long? = null

    // 搜索
    private val _searchResults = MutableStateFlow<List<PoemItem>>(emptyList())
    val searchResults: StateFlow<List<PoemItem>> = _searchResults

    // 阅读页
    private val _detail = MutableStateFlow<PoemDetail?>(null)
    val detail: StateFlow<PoemDetail?> = _detail
    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite
    private val _isBookmarked = MutableStateFlow(false)
    val isBookmarked: StateFlow<Boolean> = _isBookmarked
    private val _savedPosition = MutableStateFlow(0)
    val savedPosition: StateFlow<Int> = _savedPosition

    // 收藏/书签页
    val favorites = dao.observeFavorites().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val bookmarks = dao.observeBookmarks().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val list = repo.collections()
            _collections.value = list
            _ready.value = true
        }
    }

    // ---------- 列表页 ----------
    fun openCollection(id: Long, name: String) {
        currentCollectionId = id
        listOffset = 0
        _collectionPoems.value = emptyList()
        viewModelScope.launch(Dispatchers.IO) {
            val page = repo.poemsByCollection(id, 0, 100)
            listOffset = page.size
            _collectionPoems.value = page
        }
    }

    fun loadMoreCollection() {
        val cid = currentCollectionId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val page = repo.poemsByCollection(cid, listOffset, 100)
            if (page.isNotEmpty()) {
                listOffset += page.size
                _collectionPoems.value = _collectionPoems.value + page
            }
        }
    }

    // ---------- 搜索 ----------
    fun search(kw: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _searchResults.value = repo.search(kw)
        }
    }

    // ---------- 阅读页 ----------
    fun openPoem(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val d = repo.poemDetail(id)
            _detail.value = d
            if (d != null) {
                _isFavorite.value = dao.getFavorite(id) != null
                _isBookmarked.value = dao.getBookmark(id) != null
                _savedPosition.value = dao.getProgress(id)?.position ?: 0
            }
        }
    }

    fun toggleFavorite() {
        val d = _detail.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            if (_isFavorite.value) {
                dao.deleteFavorite(d.id)
                _isFavorite.value = false
            } else {
                dao.insertFavorite(FavoriteEntity(poemId = d.id, title = d.title, author = d.author))
                _isFavorite.value = true
            }
        }
    }

    fun toggleBookmark() {
        val d = _detail.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            if (_isBookmarked.value) {
                dao.deleteBookmark(d.id)
                _isBookmarked.value = false
            } else {
                dao.insertBookmark(BookmarkEntity(poemId = d.id, title = d.title, author = d.author))
                _isBookmarked.value = true
            }
        }
    }

    fun saveProgress(position: Int) {
        val d = _detail.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            dao.upsertProgress(ProgressEntity(poemId = d.id, position = position))
        }
    }

    // ---------- 数据清理 ----------
    fun clearAll() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.clearFavorites()
            dao.clearBookmarks()
            dao.clearProgress()
        }
    }
}
