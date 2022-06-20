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
package org.eclipse.tm4e.core.model;

import org.eclipse.tm4e.core.grammar.StackElement;

import java.util.Objects;

public class TMState {

    private TMState parentEmbedderState;
    private StackElement ruleStack;

    public TMState(TMState parentEmbedderState, StackElement ruleStatck) {
        this.parentEmbedderState = parentEmbedderState;
        this.ruleStack = ruleStatck;
    }

    public StackElement getRuleStack() {
        return ruleStack;
    }

    public void setRuleStack(StackElement ruleStack) {
        this.ruleStack = ruleStack;
    }

    @Override
    public TMState clone() {
        TMState parentEmbedderStateClone = this.parentEmbedderState != null ? this.parentEmbedderState.clone() : null;
        return new TMState(parentEmbedderStateClone, this.ruleStack);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof TMState)) {
            return false;
        }
        TMState otherState = (TMState) other;
        return Objects.equals(this.parentEmbedderState, otherState.parentEmbedderState) &&
                Objects.equals(this.ruleStack, otherState.ruleStack);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.parentEmbedderState, this.ruleStack);
    }

}
