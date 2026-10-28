package android.zero.studio.widget.editor.symbolinput

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/** Native Compose symbol pager. Stored groups and action IDs retain their existing format. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SymbolInputPanel(editor: CodeEditor?, onOpenManager: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var revision by remember { mutableIntStateOf(0) }
    DisposableEffect(context) {
        val preferences = context.getSharedPreferences("advanced_symbol_prefs", Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (SymbolDataManager.shouldTriggerUiRefresh(key)) revision++
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val settings = remember(revision) { SymbolDataManager.getUiSettings(context) }
    val groups = remember(revision) {
        SymbolDataManager.loadData(context).filter { it.items.isNotEmpty() }
            .ifEmpty { SymbolDefaults.createFallbackGroups() }
    }
    var expanded by rememberSaveable { mutableStateOf(settings.rememberExpanded && SymbolDataManager.getLastExpanded(context)) }
    val pager = rememberPagerState(
        initialPage = if (settings.rememberLastPage) SymbolDataManager.getLastPageIndex(context).coerceIn(0, groups.lastIndex.coerceAtLeast(0)) else 0,
        pageCount = { groups.size },
    )
    val scope = rememberCoroutineScope()
    LaunchedEffect(expanded, settings.rememberExpanded) {
        if (settings.rememberExpanded) SymbolDataManager.setLastExpanded(context, expanded)
    }
    LaunchedEffect(pager, settings.rememberLastPage) {
        snapshotFlow { pager.settledPage }.distinctUntilChanged().collect {
            if (settings.rememberLastPage) SymbolDataManager.setLastPageIndex(context, it)
        }
    }
    LaunchedEffect(groups.size) {
        if (groups.isNotEmpty() && pager.currentPage > groups.lastIndex) pager.scrollToPage(groups.lastIndex)
    }
    if (groups.isEmpty()) return
    val columns = settings.symbolsPerRow.coerceIn(1, 20)
    val pageRows = { index: Int -> (groups[index].items.size + columns - 1) / columns }
    val rows = if (!expanded) settings.collapsedRows else if (settings.uniformGroupHeight) groups.indices.maxOf(pageRows) else pageRows(pager.currentPage.coerceAtMost(groups.lastIndex))
    val viewportRows = rows.coerceAtLeast(settings.collapsedRows).coerceIn(1, 6)
    // A bounded viewport remains scrollable for large user-defined groups and large fonts.
    val height by animateDpAsState((viewportRows * 40).dp, label = "symbolPanelHeight")
    Surface(modifier) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (settings.indicatorStyle == 0) {
                    Box(Modifier.weight(1f)) {
                    key(groups.map { it.name }) {
                        ScrollableTabRow(selectedTabIndex = pager.currentPage.coerceIn(0, groups.lastIndex), edgePadding = 0.dp) {
                            groups.forEachIndexed { index, group ->
                                Tab(selected = pager.currentPage == index, onClick = { scope.launch { pager.animateScrollToPage(index) } }, text = { Text(group.name, maxLines = 1) })
                            }
                        }
                    }
                }
                }
                if (settings.indicatorStyle in listOf(1, 3, 4)) {
                    Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.Center) {
                        groups.forEachIndexed { index, group ->
                            val selected = pager.currentPage == index
                            IconButton(onClick = { scope.launch { pager.animateScrollToPage(index) } }) {
                                Box(Modifier.size(if (settings.indicatorStyle == 3) 22.dp else 8.dp, if (settings.indicatorStyle == 3) 3.dp else 8.dp)
                                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        if (settings.indicatorStyle == 1) CircleShape else RoundedCornerShape(1.dp))
                                    .semantics { contentDescription = group.name })
                            }
                        }
                    }
                }
                if (settings.indicatorStyle == 2) Spacer(Modifier.weight(1f))
                IconButton(onOpenManager) { Icon(Icons.Default.Settings, context.getString(android.zero.studio.widget.editor.symbolinput.R.string.symbol_manager_title)) }
                IconButton(onClick = { expanded = !expanded }, modifier = Modifier.pointerInput(Unit) {
                    detectVerticalDragGestures { change, amount -> change.consume(); expanded = amount < 0 }
                }) { Icon(if (expanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess, context.getString(if (expanded) R.string.symbol_collapse else R.string.symbol_expand)) }
            }
            HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth().height(height)) { page ->
                LazyVerticalGrid(columns = GridCells.Fixed(columns), modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(groups[page].items) { _, item ->
                        Box(
                            Modifier.heightIn(min = 40.dp).combinedClickable(
                                enabled = editor != null,
                                onClick = { editor?.let { SymbolActionExecutor.execute(it, item.shortAction, item.shortText, onOpenManager) } },
                                onLongClick = item.longAction?.let { action -> { editor?.let { SymbolActionExecutor.execute(it, action, item.longText, onOpenManager) }; Unit } },
                            ).padding(2.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text(item.display, fontSize = settings.symbolTextSizeSp.sp, maxLines = 1) }
                    }
                }
            }
        }
    }
}
