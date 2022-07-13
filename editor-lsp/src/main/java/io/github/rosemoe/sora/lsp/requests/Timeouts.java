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
package io.github.rosemoe.sora.lsp.requests;

public enum Timeouts {
    CODEACTION(2000), CODELENS(2000), COMPLETION(3000),
    DEFINITION(2000), DOC_HIGHLIGHT(1000),
    EXECUTE_COMMAND(2000), FORMATTING(5000), HOVER(2000),
    INIT(10000), REFERENCES(2000), SIGNATURE(1000),
    SHUTDOWN(5000), SYMBOLS(2000), WILLSAVE(2000);

    private final int defaultTimeout;

    Timeouts(final int defaultTimeout) {
        this.defaultTimeout = defaultTimeout;
    }

    public int getDefaultTimeout() {
        return defaultTimeout;
    }
}
