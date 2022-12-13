/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2022  Rosemoe
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
package io.github.rosemoe.sora.lsp.operations;

import io.github.rosemoe.sora.lsp.editor.LspEditor;
import io.github.rosemoe.sora.lsp.editor.LspProviderManager;
import io.github.rosemoe.sora.lsp.operations.document.DocumentChangeProvider;

/**
 * Language server capability providers which the editor invokes to call the language server.
 * For example, it is useful to notify the language server that the editor's text has changed by invoking {@link DocumentChangeProvider}.
 * We can also manage the capabilities on an editor via {@link LspProviderManager}.
 *
 * @param <T>
 */
public interface Provider<T, R> {

    /**
     * init this feature
     */
    default void init(LspEditor editor) {
    }

    /**
     * Dispose this feature
     */
    default void dispose(LspEditor editor) {
    }

    R execute(T data);

}
