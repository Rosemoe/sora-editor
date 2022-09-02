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
import androidx.annotation.WorkerThread;

import org.eclipse.tm4e.core.registry.IGrammarSource;
import org.eclipse.tm4e.core.registry.IThemeSource;

import java.io.Reader;
import java.util.Objects;

import io.github.rosemoe.sora.lang.EmptyLanguage;
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager;
import io.github.rosemoe.sora.lang.completion.CompletionHelper;
import io.github.rosemoe.sora.lang.completion.CompletionPublisher;
import io.github.rosemoe.sora.lang.completion.IdentifierAutoComplete;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.ContentReference;
import io.github.rosemoe.sora.util.MyCharacter;
import io.github.rosemoe.sora.widget.SymbolPairMatch;

public class TextMateLanguage extends EmptyLanguage {

    private TextMateAnalyzer textMateAnalyzer;
    private int tabSize = 4;
    private final IdentifierAutoComplete autoComplete = new IdentifierAutoComplete();
    boolean autoCompleteEnabled;
    final boolean createIdentifiers;

    protected TextMateLanguage(IGrammarSource grammarSource, Reader languageConfiguration, IThemeSource themeSource, boolean createIdentifiers) {
        try {
            textMateAnalyzer = new TextMateAnalyzer(this, grammarSource, languageConfiguration, themeSource);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.createIdentifiers = createIdentifiers;
        autoCompleteEnabled = true;
    }

    public static TextMateLanguage create(IGrammarSource grammarSource, Reader languageConfiguration, IThemeSource themeSource) {
        return new TextMateLanguage(grammarSource, languageConfiguration, themeSource, true);
    }

    public static TextMateLanguage create(IGrammarSource grammarSource, IThemeSource themeSource) {
        return new TextMateLanguage(grammarSource, null, themeSource, true);
    }

    public static TextMateLanguage createNoCompletion(IGrammarSource grammarSource, Reader languageConfiguration, IThemeSource themeSource) {
        return new TextMateLanguage(grammarSource, languageConfiguration, themeSource, false);
    }

    public static TextMateLanguage createNoCompletion(IGrammarSource grammarSource, IThemeSource themeSource) {
        return new TextMateLanguage(grammarSource, null, themeSource, true);
    }

    /**
     * When you update the {@link TextMateColorScheme} for editor, you need to synchronize the updates here
     *
     * @param theme IThemeSource creates from file
     */
    @WorkerThread
    public void updateTheme(IThemeSource theme) throws Exception {
        if (textMateAnalyzer != null) {
            textMateAnalyzer.updateTheme(theme);
        }
    }

    @NonNull
    @Override
    public AnalyzeManager getAnalyzeManager() {
        return Objects.requireNonNullElse(textMateAnalyzer, EmptyAnalyzeManager.INSTANCE);
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

    public void setCompleterKeywords(String[] keywords) {
        autoComplete.setKeywords(keywords, false);
    }
}
