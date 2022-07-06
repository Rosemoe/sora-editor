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
package io.github.rosemoe.sora.lsp.editor;

import android.os.Bundle;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.eclipse.lsp4j.TextDocumentSyncKind;

import io.github.rosemoe.sora.lang.EmptyLanguage;
import io.github.rosemoe.sora.lang.Language;
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager;
import io.github.rosemoe.sora.lang.completion.CompletionCancelledException;
import io.github.rosemoe.sora.lang.completion.CompletionPublisher;
import io.github.rosemoe.sora.lang.format.Formatter;
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler;
import io.github.rosemoe.sora.lsp.editor.LspEditor;
import io.github.rosemoe.sora.lsp.operations.format.LspFormattingFeature;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.ContentReference;
import io.github.rosemoe.sora.widget.SymbolPairMatch;

//TODO: implement LspLanguage
public class LspLanguage implements Language {


    protected final String currentFileUri;
    private LspEditor editor;
    private TextDocumentSyncKind syncKind;

    private LspFormatter formatter;

    public LspLanguage(String currentFileUri, LspEditor editor) {
        this.currentFileUri = currentFileUri;
        this.editor = editor;
        this.formatter = new LspFormatter(this);
    }

    @NonNull
    @Override
    public AnalyzeManager getAnalyzeManager() {
        return EmptyLanguage.EmptyAnalyzeManager.INSTANCE;
    }

    @Override
    public int getInterruptionLevel() {
        return 0;
    }

    @Override
    public void requireAutoComplete(@NonNull ContentReference content, @NonNull CharPosition position, @NonNull CompletionPublisher publisher, @NonNull Bundle extraArguments) throws CompletionCancelledException {

    }

    @Override
    public int getIndentAdvance(@NonNull ContentReference content, int line, int column) {
        return 0;
    }

    @Override
    public boolean useTab() {
        return false;
    }

    @NonNull
    @Override
    public Formatter getFormatter() {
        return formatter;
    }


    @Override
    public SymbolPairMatch getSymbolPairs() {
        return EmptyLanguage.EMPTY_SYMBOL_PAIRS;
    }

    @Nullable
    @Override
    public NewlineHandler[] getNewlineHandlers() {
        return new NewlineHandler[0];
    }

    @Override
    public void destroy() {
        editor.destroy();
        getFormatter().destroy();
        editor = null;
    }

    public void setSyncOptions(TextDocumentSyncKind textDocumentSyncKind) {
        this.syncKind = textDocumentSyncKind;
    }

    public TextDocumentSyncKind getSyncOptions() {
        return syncKind == null ? TextDocumentSyncKind.Full : syncKind;
    }

    public LspEditor getEditor() {
        return editor;
    }
}
