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
package io.github.rosemoe.sora.lsp.operations.document;

import android.util.Pair;

import org.eclipse.lsp4j.TextEdit;

import java.util.List;

import io.github.rosemoe.sora.lsp.operations.RunOnlyProvider;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.util.Logger;

/**
 *
 */
public class ApplyEditsProvider extends RunOnlyProvider<Pair<List<? extends TextEdit>, Content>> {

    @Override
    public void run(Pair<List<? extends TextEdit>, Content> contentPair) {

        var editList = contentPair.first;
        var content = contentPair.second;

        editList.forEach(textEdit -> {
            var range = textEdit.getRange();
            var text = textEdit.getNewText();
            var startIndex = content.getCharIndex(range.getStart().getLine(), range.getStart().getCharacter());
            var endIndex = content.getCharIndex(range.getEnd().getLine(), range.getEnd().getCharacter());

            if (endIndex < startIndex) {
                Logger.instance(this.getClass().getName())
                        .w("Invalid location information found applying edits from %s to %s", range.getStart(), range.getEnd());
                var diff = startIndex - endIndex;
                endIndex = startIndex;
                startIndex = endIndex - diff;
            }

            content.replace(startIndex, endIndex, text);
        });

    }
}

