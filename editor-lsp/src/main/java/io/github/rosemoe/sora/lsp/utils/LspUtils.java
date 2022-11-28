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

import org.eclipse.lsp4j.CompletionContext;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentColorParams;
import org.eclipse.lsp4j.DocumentDiagnosticParams;
import org.eclipse.lsp4j.DocumentHighlightParams;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.github.rosemoe.sora.lsp.editor.LspEditor;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.TextRange;

public class LspUtils {

    private static final Map<String, Integer> versionMap = new HashMap<>();

    private static ReentrantReadWriteLock lock;

    public static DidCloseTextDocumentParams createDidCloseTextDocumentParams(String uri) {
        DidCloseTextDocumentParams params = new DidCloseTextDocumentParams();
        params.setTextDocument(createTextDocumentIdentifier(uri));
        return params;
    }

    public static DidChangeTextDocumentParams createDidChangeTextDocumentParams(String uri, List<TextDocumentContentChangeEvent> events) {
        DidChangeTextDocumentParams params = new DidChangeTextDocumentParams();
        params.setContentChanges(events);
        params.setTextDocument(new VersionedTextDocumentIdentifier(uri, getVersion(uri)));
        return params;
    }

    public static TextDocumentContentChangeEvent createTextDocumentContentChangeEvent(String text) {
        return new TextDocumentContentChangeEvent(text);
    }

    public static TextDocumentContentChangeEvent createTextDocumentContentChangeEvent(Range range, int rangeLength, String text) {
        return new TextDocumentContentChangeEvent(range, rangeLength, text);
    }

    public static Range createRange(Position start, Position end) {
        return new Range(start, end);
    }

    public static Position createPosition(int line, int character) {
        return new Position(line, character);
    }

    public static Position createPosition(CharPosition position) {
        return createPosition(position.line, position.column);
    }

    public static Range createRange(CharPosition start, CharPosition end) {
        return createRange(createPosition(start), createPosition(end));
    }

    public static Range createRange(TextRange range) {
        return createRange(range.getStart(), range.getEnd());
    }


    private static int getVersion(String uri) {
        if (lock == null) {
            lock = new ReentrantReadWriteLock();
        }
        var readLock = lock.readLock();
        readLock.lock();
        Integer version = versionMap.getOrDefault(uri, 0);
        if (version == null) {
            version = 0;
        }
        version++;
        readLock.unlock();
        var writeLock = lock.writeLock();
        writeLock.lock();
        versionMap.put(uri, version);
        writeLock.unlock();
        return version;
    }

    public static DidOpenTextDocumentParams createDidOpenTextDocumentParams(String uri, String languageId, String content) {
        var params = new DidOpenTextDocumentParams();

        params.setTextDocument(new TextDocumentItem(uri, languageId, getVersion(uri), content));
        return params;
    }

    public static TextDocumentIdentifier createTextDocumentIdentifier(String uri) {
        TextDocumentIdentifier identifier = new TextDocumentIdentifier();
        identifier.setUri(uri);
        return identifier;
    }

    public static DocumentDiagnosticParams createDocumentDiagnosticParams(String uri) {
        return new DocumentDiagnosticParams(createTextDocumentIdentifier(uri));
    }


    public static CompletionParams createCompletionParams(LspEditor editor, Position position) {
        CompletionParams params = new CompletionParams();
        params.setTextDocument(createTextDocumentIdentifier(editor.getCurrentFileUri()));
        params.setPosition(position);
        params.setContext(new CompletionContext());
        params.getContext().setTriggerCharacter(null);
        return params;
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

    public static void clearVersions() {
        versionMap.clear();
    }


}
