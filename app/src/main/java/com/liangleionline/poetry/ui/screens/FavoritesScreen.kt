package com.liangleionline.poetry.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.liangleionline.poetry.R
import com.liangleionline.poetry.ui.AppViewModel
import com.liangleionline.poetry.ui.components.PoemListItem

@Composable
fun FavoritesScreen(vm: AppViewModel, onOpenPoem: (Long) -> Unit) {
    val favorites by vm.favorites.collectAsState()
    val bookmarks by vm.bookmarks.collectAsState()

    Column(Modifier.fillMaxSize()) {
        Text(
            stringResource(R.string.favorites),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp)
        )
        HorizontalDivider()

        if (favorites.isEmpty() && bookmarks.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.empty_favorite), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (favorites.isNotEmpty()) {
                item { SectionTitle("收藏") }
                items(favorites, key = { "fav_${it.poemId}" }) { f ->
                    PoemListItem(f.title, f.author, onClick = { onOpenPoem(f.poemId) })
                }
            }
            if (bookmarks.isNotEmpty()) {
                item { SectionTitle("书签") }
                items(bookmarks, key = { "bm_${it.poemId}" }) { b ->
                    PoemListItem(b.title, b.author, onClick = { onOpenPoem(b.poemId) })
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
    )
}
