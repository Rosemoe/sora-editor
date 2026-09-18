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

package io.github.rosemoe.sora.widget

/**
 * Returns the [CodeEditor] instance associated with this delegate, or null if the editor
 * is not an instance of [CodeEditor].
 *
 * Note: When using the editor within a Jetpack Compose environment (e.g., via `CodeEditor`
 * composable), this method will return **null** because the internal implementation uses
 * a different view structure. In standard View-based layouts, this will return the
 * non-null [CodeEditor] instance.
 */
fun CodeEditorDelegate.asCodeEditor(): CodeEditor? = host.attachedView as? CodeEditor

/**
 * Returns true if the delegate is attached to a [CodeEditor] view.
 * This will return false in Jetpack Compose environments.
 */
val CodeEditorDelegate.isViewMode: Boolean
    get() = host.attachedView is CodeEditor

internal inline val CodeEditorDelegate.isComposeMode get() = !isViewMode
