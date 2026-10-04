package com.liangleionline.poetry.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liangleionline.poetry.R
import com.liangleionline.poetry.data.CollectionItem
import com.liangleionline.poetry.data.PoemItem
import com.liangleionline.poetry.ui.AppViewModel
import com.liangleionline.poetry.ui.components.PoemListItem
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookshelfScreen(
    vm: AppViewModel,
    onOpenCollection: (Long, String) -> Unit,
    onOpenPoem: (Long) -> Unit
) {
    val collections by vm.collections.collectAsState()
    val ready by vm.ready.collectAsState()
    val results by vm.searchResults.collectAsState()
    var query by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        // 顶部标题
        Text(
            "中国古典诗词",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 4.dp)
        )
        // 搜索框
        OutlinedTextField(
            value = query,
            onValueChange = { query = it; vm.search(it) },
            placeholder = { Text(stringResource(R.string.search_hint), fontSize = 14.sp) },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
        )

        if (!ready) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.loading))
                }
            }
            return@Column
        }

        if (query.isNotBlank()) {
            SearchResultList(results, onOpenPoem)
        } else {
            CollectionGrid(collections, onOpenCollection)
        }
    }
}

@Composable
private fun SearchResultList(results: List<PoemItem>, onOpenPoem: (Long) -> Unit) {
    if (results.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.search_no_result), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(1),
        contentPadding = PaddingValues(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(results, key = { it.id }) { poem ->
            PoemListItem(
                title = poem.title,
                subtitle = poem.author + if (poem.dynasty.isNotEmpty()) " · ${poem.dynasty}" else "",
                onClick = { onOpenPoem(poem.id) }
            )
        }
    }
}

@Composable
private fun CollectionGrid(collections: List<CollectionItem>, onOpenCollection: (Long, String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(collections, key = { it.id }) { c ->
            CollectionCard(c) { onOpenCollection(c.id, c.name) }
        }
    }
}

@Composable
private fun CollectionCard(c: CollectionItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth().height(120.dp)
    ) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                c.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${c.count} 首",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
    }
}
