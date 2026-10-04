package com.liangleionline.poetry.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.liangleionline.poetry.ui.AppViewModel
import com.liangleionline.poetry.ui.components.PoemListItem

@Composable
fun CollectionScreen(
    vm: AppViewModel,
    collectionId: Long,
    name: String,
    onBack: () -> Unit,
    onOpenPoem: (Long) -> Unit
) {
    val poems by vm.collectionPoems.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(collectionId) {
        vm.openCollection(collectionId, name)
    }

    Column(Modifier.fillMaxSize()) {
        // 顶栏
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            Text(name, style = MaterialTheme.typography.titleMedium)
        }
        HorizontalDivider()

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(poems, key = { it.id }) { p ->
                PoemListItem(
                    title = p.title,
                    subtitle = p.author + if (p.dynasty.isNotEmpty()) " · ${p.dynasty}" else "",
                    onClick = { onOpenPoem(p.id) }
                )
            }
            if (poems.isNotEmpty()) {
                item {
                    LaunchedEffect(listState) {
                        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                            .collect { last ->
                                if (last != null && last >= poems.lastIndex - 3) {
                                    vm.loadMoreCollection()
                                }
                            }
                    }
                }
            }
        }
    }
}
