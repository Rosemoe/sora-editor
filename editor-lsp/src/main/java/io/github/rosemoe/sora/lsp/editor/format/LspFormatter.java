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
package io.github.rosemoe.sora.lsp.editor.format;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.github.rosemoe.sora.lang.format.AsyncFormatter;
import io.github.rosemoe.sora.lsp.editor.LspLanguage;
import io.github.rosemoe.sora.lsp.operations.format.FullFormattingFeature;
import io.github.rosemoe.sora.lsp.operations.format.RangeFormattingFeature;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.text.TextRange;

public class LspFormatter extends AsyncFormatter {

    private LspLanguage language;

    public LspFormatter(LspLanguage currentLanguage) {
        this.language = currentLanguage;
    }

    @Nullable
    @Override
    public TextRange formatAsync(@NonNull Content text, @NonNull TextRange cursorRange) {
        language.getEditor().safeUseFeature(FullFormattingFeature.class)
                .ifPresent(fullFormattingFeature -> fullFormattingFeature.execute(text));
        return null;
    }

    @Nullable
    @Override
    public TextRange formatRegionAsync(@NonNull Content text, @NonNull TextRange rangeToFormat, @NonNull TextRange cursorRange) {
        language.getEditor().safeUseFeature(RangeFormattingFeature.class)
                .ifPresent(rangeFormattingFeature -> rangeFormattingFeature.execute(new Pair<>(text, cursorRange)));
        return null;
    }

    @Override
    public void destroy() {
        super.destroy();
        language = null;
    }
}
