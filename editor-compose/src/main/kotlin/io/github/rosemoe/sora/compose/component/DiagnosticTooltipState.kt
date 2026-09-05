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

package io.github.rosemoe.sora.compose.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticDetail
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion

/**
 * State object representing the information needed to display a diagnostic tooltip.
 *
 * @property diagnostic The detailed diagnostic information to be displayed.
 * @property region The specific region in the text that this diagnostic refers to, or null if not applicable.
 * @property onDismiss Callback to be invoked when the tooltip should be dismissed.
 * @property onMenuShowingChanged Callback to be invoked when the visibility of any sub-menus (like quick fixes) changes.
 */
@Immutable
data class DiagnosticTooltipState(
    val diagnostic: DiagnosticDetail,
    val region: DiagnosticRegion?,
    val onDismiss: () -> Unit,
    val onMenuShowingChanged: (Boolean) -> Unit,
)

/**
 * Composable function type for rendering the content of a diagnostic tooltip.
 */
typealias DiagnosticTooltipContent = @Composable (DiagnosticTooltipState) -> Unit
