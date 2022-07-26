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
package io.github.rosemoe.sora.langs.textmate;

import android.os.Bundle;

import androidx.annotation.NonNull;

import java.io.InputStream;
import java.io.Reader;

import io.github.rosemoe.sora.annotations.Experimental;
import io.github.rosemoe.sora.lang.EmptyLanguage;
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager;
import io.github.rosemoe.sora.lang.completion.CompletionHelper;
import io.github.rosemoe.sora.lang.completion.CompletionPublisher;
import io.github.rosemoe.sora.lang.completion.IdentifierAutoComplete;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.ContentReference;
import io.github.rosemoe.sora.util.MyCharacter;
import io.github.rosemoe.sora.widget.SymbolPairMatch;

import org.eclipse.tm4e.core.theme.IRawTheme;

public class TextMateLanguage extends EmptyLanguage {

    private TextMateAnalyzer textMateAnalyzer;
    private int tabSize = 4;
    private final IdentifierAutoComplete autoComplete = new IdentifierAutoComplete();
    boolean autoCompleteEnabled;
    final boolean createIdentifiers;

    protected TextMateLanguage(String grammarName, InputStream grammarIns, Reader languageConfiguration, IRawTheme theme, boolean createIdentifiers) {
        try {
            textMateAnalyzer = new TextMateAnalyzer(this,grammarName, grammarIns,languageConfiguration, theme);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.createIdentifiers = createIdentifiers;
        autoCompleteEnabled = true;
    }

    public static TextMateLanguage create(String grammarName, InputStream grammarIns,Reader languageConfiguration, IRawTheme theme) {
        return new TextMateLanguage(grammarName, grammarIns,languageConfiguration, theme, true);
    }

    public static TextMateLanguage create(String grammarName, InputStream grammarIns, IRawTheme theme) {
        return new TextMateLanguage(grammarName, grammarIns,null, theme, true);
    }

    public static TextMateLanguage createNoCompletion(String grammarName, InputStream grammarIns,Reader languageConfiguration, IRawTheme theme) {
        return new TextMateLanguage(grammarName, grammarIns,languageConfiguration, theme, false);
    }

    public static TextMateLanguage createNoCompletion(String grammarName, InputStream grammarIns, IRawTheme theme) {
        return new TextMateLanguage(grammarName, grammarIns,null, theme, false);
    }

    /**
     * When you update the {@link TextMateColorScheme} for editor, you need to synchronize the updates here
     *
     * @param theme IRawTheme creates from file
     */
    public void updateTheme(IRawTheme theme) {
        if (textMateAnalyzer != null) {
            textMateAnalyzer.updateTheme(theme);
        }
    }

    @NonNull
    @Override
    public AnalyzeManager getAnalyzeManager() {
        if (textMateAnalyzer != null) {
            return textMateAnalyzer;
        }
        return EmptyAnalyzeManager.INSTANCE;
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    /**
     * Set tab size. The tab size is used to compute code blocks.
     */
    public void setTabSize(int tabSize) {
        this.tabSize = tabSize;
    }

    public int getTabSize() {
        return tabSize;
    }

    @Override
    public SymbolPairMatch getSymbolPairs() {
        return new SymbolPairMatch.DefaultSymbolPairs();
    }

    public boolean isAutoCompleteEnabled() {
        return autoCompleteEnabled;
    }

    public void setAutoCompleteEnabled(boolean autoCompleteEnabled) {
        this.autoCompleteEnabled = autoCompleteEnabled;
    }

    @Override
    public void requireAutoComplete(@NonNull ContentReference content, @NonNull CharPosition position, @NonNull CompletionPublisher publisher, @NonNull Bundle extraArguments) {
        if (!autoCompleteEnabled) {
            return;
        }
        var prefix = CompletionHelper.computePrefix(content, position, MyCharacter::isJavaIdentifierPart);
        final var idt = textMateAnalyzer.syncIdentifiers;
        autoComplete.requireAutoComplete(prefix, publisher, idt);
    }

    public IdentifierAutoComplete getAutoCompleter() {
        return autoComplete;
    }
}
