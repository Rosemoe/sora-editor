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
package org.eclipse.tm4e.core.grammar;

import org.eclipse.tm4e.core.internal.types.IRawGrammar;

import java.util.List;
import java.util.function.Predicate;

public class Injection {

    public final int priority; // -1 | 0 | 1; // 0 is the default. -1 for 'L' and 1 for 'R'
    public final int ruleId;
    public final IRawGrammar grammar;
    private final Predicate<List<String>> matcher;

    public Injection(Predicate<List<String>> matcher, int ruleId, IRawGrammar grammar, int priority) {
        this.matcher = matcher;
        this.ruleId = ruleId;
        this.grammar = grammar;
        this.priority = priority;
    }

    public boolean match(List<String> states) {
        return matcher.test(states);
    }
}
