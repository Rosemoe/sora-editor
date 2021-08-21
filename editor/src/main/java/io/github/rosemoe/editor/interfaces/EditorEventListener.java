/*
 *    CodeEditor - the awesome code editor for Android
 *    Copyright (C) 2020-2021  Rosemoe
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
 */
package io.github.rosemoe.editor.interfaces;

import io.github.rosemoe.editor.text.Content;
import io.github.rosemoe.editor.widget.CodeEditor;

/**
 * Listener for editor
 *
 * @author Rose
 */
public interface EditorEventListener {

    /**
     * Before format
     *
     * @return true if you want to cancel this operation
     */
    boolean onRequestFormat(CodeEditor editor, boolean async);

    /**
     * Format failed
     *
     * @return true if you do not want editor to handle this exception
     */
    boolean onFormatFail(CodeEditor editor, Throwable cause);

    /**
     * Format succeeded
     */
    void onFormatSucceed(CodeEditor editor);

    /**
     * CodeEditor's setText is called
     */
    void onNewTextSet(CodeEditor editor);

    /**
     * @see io.github.rosemoe.editor.text.ContentListener#afterDelete(Content, int, int, int, int, CharSequence)
     * Note:do not change content at this time
     */
    void afterDelete(CodeEditor editor, CharSequence content, int startLine, int startColumn, int endLine, int endColumn, CharSequence deletedContent);

    /**
     * @see io.github.rosemoe.editor.text.ContentListener#afterInsert(Content, int, int, int, int, CharSequence)
     * Note:do not change content at this time
     */
    void afterInsert(CodeEditor editor, CharSequence content, int startLine, int startColumn, int endLine, int endColumn, CharSequence insertedContent);

    /**
     * @see io.github.rosemoe.editor.text.ContentListener#beforeReplace(Content)
     * Note:do not change content at this time
     */
    void beforeReplace(CodeEditor editor, CharSequence content);

}
