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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.tm4e.core.internal.grammar.ScopeListElement;
import org.eclipse.tm4e.core.internal.rule.IRuleRegistry;
import org.eclipse.tm4e.core.internal.rule.Rule;

/**
 * Represents a "pushed" state on the stack (as a linked list element).
 *
 * @link https://github.com/Microsoft/vscode-textmate/blob/master/src/grammar.ts
 *
 */
public class StackElement {

    public static final StackElement NULL = new StackElement(null, 0, 0, null, null, null);
    /**
     * The previous state on the stack (or null for the root state).
     */
    public final StackElement parent;
    /**
     * The depth of the stack.
     */
    public final int depth;
    /**
     * The state (rule) that this element represents.
     */
    public final int ruleId;
    /**
     * The "pop" (end) condition for this state in case that it was dynamically generated through captured text.
     */
    public final String endRule;
    /**
     * The list of scopes containing the "name" for this state.
     */
    public final ScopeListElement nameScopesList;
    /**
     * The list of scopes containing the "contentName" (besides "name") for this state.
     * This list **must** contain as an element `scopeName`.
     */
    public final ScopeListElement contentNameScopesList;
    /**
     * The position on the current line where this state was pushed.
     * This is relevant only while tokenizing a line, to detect endless loops.
     * Its value is meaningless across lines.
     */
    private int enterPosition;

    public StackElement(StackElement parent, int ruleId, int enterPos, String endRule, ScopeListElement nameScopesList, ScopeListElement contentNameScopesList) {
        this.parent = parent;
        this.depth = (this.parent != null ? this.parent.depth + 1 : 1);
        this.ruleId = ruleId;
        this.enterPosition = enterPos;
        this.endRule = endRule;
        this.nameScopesList = nameScopesList;
        this.contentNameScopesList = contentNameScopesList;
    }

    /**
     * A structural equals check. Does not take into account `scopes`.
     */
    private static boolean structuralEquals(StackElement a, StackElement b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.depth == b.depth && a.ruleId == b.ruleId && Objects.equals(a.endRule, b.endRule) && structuralEquals(a.parent, b.parent);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (!(other instanceof StackElement)) {
            return false;
        }
        StackElement stackElement = (StackElement) other;
        return structuralEquals(this, stackElement) && this.contentNameScopesList.equals(stackElement.contentNameScopesList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(depth, ruleId, endRule, parent, contentNameScopesList);
    }

    public void reset() {
        StackElement el = this;
        while (el != null) {
            el.enterPosition = -1;
            el = el.parent;
        }
    }

    public StackElement pop() {
        return this.parent;
    }

    public StackElement safePop() {
        if (this.parent != null) {
            return this.parent;
        }
        return this;
    }

    public StackElement push(int ruleId, int enterPos, String endRule, ScopeListElement nameScopesList, ScopeListElement contentNameScopesList) {
        return new StackElement(this, ruleId, enterPos, endRule, nameScopesList, contentNameScopesList);
    }

    public int getEnterPos() {
        return this.enterPosition;
    }

    public Rule getRule(IRuleRegistry grammar) {
        return grammar.getRule(this.ruleId);
    }

    private void appendString(List<String> res) {
        if (this.parent != null) {
            this.parent.appendString(res);
        }
        res.add('(' + Integer.toString(this.ruleId) + ')'); //, TODO-${this.nameScopesList}, TODO-${this.contentNameScopesList})`;
    }

    @Override
    public String toString() {
        List<String> r = new ArrayList<>();
        this.appendString(r);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('[');
        for (String s : r) {
            stringBuilder.append(s);
            stringBuilder.append(",");
        }
        stringBuilder.append(']');
        //return '[' + String.join(", ", r) + ']';
        return stringBuilder.toString();
    }

    public StackElement setContentNameScopesList(ScopeListElement contentNameScopesList) {
        if (this.contentNameScopesList.equals(contentNameScopesList)) {
            return this;
        }
        return this.parent.push(this.ruleId, this.enterPosition, this.endRule, this.nameScopesList, contentNameScopesList);
    }

    public StackElement setEndRule(String endRule) {
        if (this.endRule != null && this.endRule.equals(endRule)) {
            return this;
        }
        return new StackElement(this.parent, this.ruleId, this.enterPosition, endRule, this.nameScopesList, this.contentNameScopesList);
    }

    public boolean hasSameRuleAs(StackElement other) {
        return this.ruleId == other.ruleId;
    }
}
