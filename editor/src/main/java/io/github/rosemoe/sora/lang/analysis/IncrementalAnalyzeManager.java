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
package io.github.rosemoe.sora.lang.analysis;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import io.github.rosemoe.sora.lang.styling.MappedSpans;
import io.github.rosemoe.sora.lang.styling.Span;
import io.github.rosemoe.sora.lang.styling.Styles;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.text.ContentReference;

/**
 * Simple implementation of incremental analyze manager.
 * This class saves states at line endings. It is for simple token-based highlighting, and it can also
 * save tokens on lines so that they can be reused.
 *
 * Note that the analysis is done on UI thread currently.
 *
 * @param <S> State type at line endings
 * @param <T> Token type
 */
public abstract class IncrementalAnalyzeManager<S, T> implements AnalyzeManager {

    private StyleReceiver receiver;
    private Content shadowed;
    private List<LineTokenizeResult<S, T>> states = new ArrayList<>();
    private Styles sentStyles;

    @Override
    public void setReceiver(@Nullable StyleReceiver receiver) {
        this.receiver = receiver;
    }

    @Override
    public void reset(@NonNull ContentReference content, @NonNull Bundle extraArguments) {
        shadowed = content.getReference().copyText();
        shadowed.setUndoEnabled(false);
        shadowed.beginStreamCharGetting(0);
        rerun();
    }

    @Override
    public void insert(CharPosition start, CharPosition end, CharSequence insertedContent) {
        shadowed.insert(start.line, start.column, insertedContent);
        S state = start.line == 0 ? getInitialState() : states.get(start.line - 1).state;
        int line = start.line;
        var spans = sentStyles.spans.modify();
        while (line <= end.line) {
            var res = tokenizeLine(shadowed.getLine(line), state);
            Log.d("T", "Generate for:" + line);
            if (line == start.line) {
                states.set(line, res);
                spans.setSpansOnLine(line, generateSpansForLine(res));
            } else {
                states.add(line, res);
                spans.addLineAt(line, generateSpansForLine(res));
            }
            state = res.state;
            line++;
        }
        // line = end.line + 1, check whether the state equals
        while (line < shadowed.getLineCount()) {
            var res = tokenizeLine(shadowed.getLine(line), state);
            if (stateEquals(res.state, states.get(line).state)) {
                break;
            } else {
                states.set(line, res);
                spans.setSpansOnLine(line, generateSpansForLine(res));
            }
            Log.d("T", "Generate for:" + line);
            line ++;
        }
        receiver.setStyles(this, sentStyles);
    }

    @Override
    public void delete(CharPosition start, CharPosition end, CharSequence deletedContent) {
        shadowed.delete(start.line, start.column, end.line, end.column);
        S state = start.line == 0 ? getInitialState() : states.get(start.line - 1).state;
        // Remove states
        if (end.line >= start.line + 1) {
            states.subList(start.line + 1, end.line + 1).clear();
        }
        int line = start.line;
        while (line < shadowed.getLineCount()){
            var res = tokenizeLine(shadowed.getLine(line), state);
            var old = states.set(line, res);
            var spans = sentStyles.spans.modify();
            spans.setSpansOnLine(line, generateSpansForLine(res));
            Log.d("T", "Generate for:" + line);
            if (stateEquals(old.state, res.state)) {
                break;
            }
            state = res.state;
            line ++;
        }
        receiver.setStyles(this, sentStyles);
    }

    @Override
    public void rerun() {
        states.clear();
        S state = getInitialState();
        var builder = new MappedSpans.Builder(shadowed.getLineCount());
        for (int i = 0;i < shadowed.getLineCount();i++) {
            var result = tokenizeLine(shadowed.getLine(i), state);
            states.add(result);
            state = result.state;
            var spans = generateSpansForLine(result);
            for (var span : spans) {
                builder.add(i, span);
            }
        }
        sentStyles = new Styles(builder.build());
        final var r = receiver;
        if (r != null) {
            r.setStyles(this, sentStyles);
        }
    }

    @Override
    public void destroy() {
        states = null;
        receiver = null;
        sentStyles = null;
        shadowed = null;
    }

    protected abstract S getInitialState();

    protected abstract boolean stateEquals(S state, S another);

    protected abstract LineTokenizeResult<S, T> tokenizeLine(CharSequence line, S state);

    protected abstract List<Span> generateSpansForLine(LineTokenizeResult<S, T> tokens);

    protected static class LineTokenizeResult<S_, T_> {

        public S_ state;

        public List<T_> tokens;

        public LineTokenizeResult(@NonNull S_ state, @Nullable List<T_> tokens) {
            this.state = state;
            this.tokens = tokens;
        }

    }

}
