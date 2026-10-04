package com.liangleionline.poetry.ui.screens

import android.view.WindowManager
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liangleionline.poetry.ui.AppViewModel
import com.liangleionline.poetry.ui.theme.PoetryTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(vm: AppViewModel, poemId: Long, onBack: () -> Unit) {
    val detail by vm.detail.collectAsState()
    val isFavorite by vm.isFavorite.collectAsState()
    val isBookmarked by vm.isBookmarked.collectAsState()

    var readerDark by remember { mutableStateOf(false) }
    var fontSize by remember { mutableStateOf(20) }
    var lineGap by remember { mutableStateOf(14) }

    // 屏幕常亮
    val view = LocalView.current
    DisposableEffect(Unit) {
        val keep = (view.context as? android.app.Activity)?.window
        keep?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            keep?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LaunchedEffect(poemId) { vm.openPoem(poemId) }

    PoetryTheme(darkTheme = readerDark) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text(detail?.title ?: "", style = MaterialTheme.typography.titleMedium) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        IconButton(onClick = { vm.toggleFavorite() }) {
                            Icon(
                                if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "收藏",
                                tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { vm.toggleBookmark() }) {
                            Icon(
                                if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "书签",
                                tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            },
            bottomBar = {
                Surface(tonalElevation = 2.dp) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("字号", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Slider(
                                value = fontSize.toFloat(), onValueChange = { fontSize = it.toInt() },
                                valueRange = 14f..34f, modifier = Modifier.weight(1f)
                            )
                            Text("行距", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Slider(
                                value = lineGap.toFloat(), onValueChange = { lineGap = it.toInt() },
                                valueRange = 4f..30f, modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { readerDark = !readerDark }) {
                                Text(if (readerDark) "浅色" else "深色")
                            }
                        }
                    }
                }
            }
        ) { padding ->
            val d = detail
            if (d == null) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Scaffold
            }
            ReaderContent(
                detail = d,
                fontSize = fontSize,
                lineGap = lineGap,
                savedPosition = vm.savedPosition.collectAsState().value,
                padding = padding,
                onPosition = { vm.saveProgress(it) }
            )
        }
    }
}

@Composable
private fun ReaderContent(
    detail: com.liangleionline.poetry.data.PoemDetail,
    fontSize: Int,
    lineGap: Int,
    savedPosition: Int,
    padding: PaddingValues,
    onPosition: (Int) -> Unit
) {
    val scrollState = rememberScrollState()
    var restored by remember { mutableStateOf(false) }

    // 恢复上次位置
    LaunchedEffect(Unit) {
        if (savedPosition > 0 && !restored) {
            scrollState.scrollTo(savedPosition)
            restored = true
        }
    }

    // 滚动停止时保存位置
    LaunchedEffect(Unit) {
        var last = 0
        while (true) {
            kotlinx.coroutines.delay(600)
            if (scrollState.value != last) {
                last = scrollState.value
                onPosition(scrollState.value)
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(scrollState)
            .padding(horizontal = 28.dp, vertical = 16.dp)
    ) {
        // 标题
        Text(
            detail.title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        // 作者·朝代
        val authorLine = buildString {
            if (detail.dynasty.isNotEmpty()) append(detail.dynasty).append(" · ")
            append(detail.author)
            if (detail.source.isNotEmpty()) append("  ·  ").append(detail.source)
        }
        Text(
            authorLine,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))

        // 正文（逐行居中）
        detail.content.split("\n").forEach { line ->
            if (line.isBlank()) {
                Spacer(Modifier.height((lineGap / 2).dp))
            } else {
                Text(
                    line,
                    fontFamily = FontFamily.Serif,
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize + lineGap).sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(lineGap.dp))
            }
        }
        Spacer(Modifier.height(120.dp))
    }
}
