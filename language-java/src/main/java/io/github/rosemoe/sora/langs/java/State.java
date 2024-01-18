/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2024  Rosemoe
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
package io.github.rosemoe.sora.langs.java;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class State {

    public int state = 0;

    public boolean hasBraces = false;

    public List<String> identifiers = null;

    public void addIdentifier(CharSequence idt) {
        if (identifiers == null) {
            identifiers = new ArrayList<>();
        }
        if (idt instanceof String) {
            identifiers.add((String) idt);
        } else {
            identifiers.add(idt.toString());
        }
    }

    @Override
    public boolean equals(Object o) {
        // `identifiers` is ignored because it is unrelated to tokenization for next line
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        State state1 = (State) o;
        return state == state1.state && hasBraces == state1.hasBraces;
    }

    @Override
    public int hashCode() {
        return Objects.hash(state, hasBraces);
    }
}
