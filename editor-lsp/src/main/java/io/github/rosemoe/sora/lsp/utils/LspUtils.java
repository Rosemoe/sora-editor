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
package io.github.rosemoe.sora.lsp.utils;

import android.net.Uri;

import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentColorParams;
import org.eclipse.lsp4j.DocumentHighlightParams;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;

import java.util.HashMap;
import java.util.Map;

public class LspUtils {

    private static Map<String, Integer> versionMap = new HashMap<>();

    public static DidCloseTextDocumentParams createDidCloseTextDocumentParams(String uri) {
        DidCloseTextDocumentParams params = new DidCloseTextDocumentParams();
        params.setTextDocument(createTextDocumentIdentifier(uri));
        return params;
    }

    public static DidOpenTextDocumentParams createDidOpenTextDocumentParams(String uri, String languageId, String content) {
        DidOpenTextDocumentParams params = new DidOpenTextDocumentParams();
        int version = versionMap.getOrDefault(uri, 0);
        version++;
        versionMap.put(uri, version);
        params.setTextDocument(new TextDocumentItem(uri, languageId, version, content));
        return params;
    }

    public static TextDocumentIdentifier createTextDocumentIdentifier(String uri) {
        TextDocumentIdentifier identifier = new TextDocumentIdentifier();
        identifier.setUri(uri);
        return identifier;
    }

    public static DocumentHighlightParams createDocumentHighlightParams(String uri, int line, int character) {
        DocumentHighlightParams params = new DocumentHighlightParams();
        params.setTextDocument(createTextDocumentIdentifier(uri));
        params.setPosition(new Position(line, character));
        return params;
    }

    public static DocumentColorParams createDocumentColorParams(String uri, int line, int character) {
        DocumentColorParams params = new DocumentColorParams();
        params.setTextDocument(createTextDocumentIdentifier(uri));
        return params;
    }

    public static DocumentSymbolParams createDocumentSymbolParams(String uri) {
        DocumentSymbolParams params = new DocumentSymbolParams();
        params.setTextDocument(createTextDocumentIdentifier(uri));

        return params;
    }

    public static DidSaveTextDocumentParams createDidSaveTextDocumentParams(String currentFileUri, String str) {
        DidSaveTextDocumentParams params = new DidSaveTextDocumentParams();
        params.setTextDocument(createTextDocumentIdentifier(currentFileUri));
        params.setText(str);
        return params;
    }
}
