/*******************************************************************************
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2026  Rosemoe
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 *
 *     Please contact Rosemoe by email 2073412493@qq.com if you need
 *     additional information or have any questions
 ******************************************************************************/

package io.github.rosemoe.sora.compose

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.rosemoe.sora.compose.component.CompletionState
import io.github.rosemoe.sora.lang.completion.CompletionItem
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import kotlin.math.abs

/**
 * Default configurations and UI components for [CodeEditor].
 */
@Stable
object CodeEditorDefaults {

    /**
     * Default implementation of the auto-completion window.
     *
     * @param state The current state of completion, providing items, loading status, and selection.
     * @param onItemClick Callback invoked when a completion item is clicked.
     * @param modifier Modifier to be applied to the window container.
     * @see CompletionState
     */
    @Composable
    fun AutoCompletionWindow(
        state: CompletionState,
        onItemClick: (index: Int) -> Unit,
        modifier: Modifier = Modifier
    ) {
        val listState = rememberLazyListState()

        val colorScheme = LocalEditorColorScheme.current
        val shape = RoundedCornerShape(8.dp)
        val borderColor = Color(colorScheme.getColor(EditorColorScheme.COMPLETION_WND_CORNER))
        val bgColor = Color(colorScheme.getColor(EditorColorScheme.COMPLETION_WND_BACKGROUND))

        val animatedModifier = if (state.enableAnimation) {
            Modifier.animateContentSize()
        } else Modifier

        Surface(
            shape = shape,
            color = bgColor,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, borderColor),
            modifier = modifier.then(animatedModifier)
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 180.dp, max = 320.dp)
                    .heightIn(max = 240.dp)
            ) {

                if (state.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                            .height(4.dp)
                    )
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(state.items) { index, item ->
                        CompletionRow(
                            item = item,
                            selected = index == state.selectedIndex,
                            onClick = { onItemClick(index) },
                            modifier = if (state.enableAnimation) Modifier.animateItem() else Modifier
                        )
                    }
                }
            }
        }


        LaunchedEffect(state.selectedIndex) {
            val index = state.selectedIndex
            if (index < 0) return@LaunchedEffect

            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo

            val item = visibleItems.find { it.index == index }

            if (item != null) {
                val viewportStart = layoutInfo.viewportStartOffset
                val viewportEnd = layoutInfo.viewportEndOffset
                val viewportHeight = viewportEnd - viewportStart

                val itemCenter = item.offset + item.size / 2
                val viewportCenter = viewportStart + viewportHeight / 2

                val delta = (itemCenter - viewportCenter).toFloat()

                if (abs(delta) > item.size / 2) {
                    listState.animateScrollBy(delta)
                }
            } else {
                listState.scrollToItem(index)
            }
        }

        LaunchedEffect(state.items) {
            if (state.items.isNotEmpty()) {
                listState.scrollToItem(0)
            }
        }
    }

    @Composable
    private fun CompletionRow(
        item: CompletionItem,
        selected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier
    ) {
        val fontFamily = LocalEditorFontFamily.current
        val colorScheme = LocalEditorColorScheme.current

        val bg = if (selected) {
            Color(colorScheme.getColor(EditorColorScheme.COMPLETION_WND_ITEM_CURRENT))
        } else {
            Color.Transparent
        }

        Row(
            modifier = modifier
                .clip(RoundedCornerShape(4.dp))
                .background(bg)
                .clickable(onClick = onClick)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            item.kind?.let { kind ->
                val bg = Color(kind.defaultDisplayBackgroundColor)
                val isLight = bg.luminance() > 0.6f

                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(bg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = kind.getDisplayChar(),
                        color = if (isLight) Color.Black else Color.White,
                        fontSize = 12.sp,
                        fontFamily = fontFamily
                    )
                }

                Spacer(Modifier.width(8.dp))
            }

            Column {
                Text(
                    text = item.label.toString(),
                    color = Color(colorScheme.getColor(EditorColorScheme.COMPLETION_WND_TEXT_PRIMARY)),
                    textDecoration = if (item.deprecated) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(),
                    fontSize = 15.sp,
                    fontFamily = fontFamily
                )

                item.detail?.let {
                    Text(
                        text = it.toString(),
                        color = Color(colorScheme.getColor(EditorColorScheme.COMPLETION_WND_TEXT_SECONDARY)),
                        textDecoration = if (item.deprecated) TextDecoration.LineThrough else null,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(),
                        fontSize = 14.sp,
                        fontFamily = fontFamily
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = item.desc.toString(),
                color = Color(colorScheme.getColor(EditorColorScheme.COMPLETION_WND_TEXT_SECONDARY)),
                maxLines = 1,
                modifier = Modifier.basicMarquee(),
                fontSize = 14.sp,
                fontFamily = fontFamily
            )
        }
    }
}
