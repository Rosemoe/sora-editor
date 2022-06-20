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
package org.eclipse.tm4e.core.internal.css;

import org.w3c.css.sac.CSSException;
import org.w3c.css.sac.CharacterDataSelector;
import org.w3c.css.sac.Condition;
import org.w3c.css.sac.ConditionalSelector;
import org.w3c.css.sac.DescendantSelector;
import org.w3c.css.sac.ElementSelector;
import org.w3c.css.sac.NegativeSelector;
import org.w3c.css.sac.ProcessingInstructionSelector;
import org.w3c.css.sac.Selector;
import org.w3c.css.sac.SelectorFactory;
import org.w3c.css.sac.SiblingSelector;
import org.w3c.css.sac.SimpleSelector;

public class CSSSelectorFactory implements SelectorFactory {

    public static final SelectorFactory INSTANCE = new CSSSelectorFactory();

    @Override
    public SimpleSelector createAnyNodeSelector() throws CSSException {
        throw new UnsupportedOperationException("CSS any selector is not supported");
    }

    @Override
    public CharacterDataSelector createCDataSectionSelector(String arg0) throws CSSException {
        throw new UnsupportedOperationException("CSS CDATA section is not supported");
    }

    @Override
    public DescendantSelector createChildSelector(Selector arg0, SimpleSelector arg1) throws CSSException {
        throw new UnsupportedOperationException("CSS child selector is not supported");
    }

    @Override
    public CharacterDataSelector createCommentSelector(String arg0) throws CSSException {
        throw new UnsupportedOperationException("CSS comment is not supported");
    }

    @Override
    public ConditionalSelector createConditionalSelector(SimpleSelector selector, Condition condition)
            throws CSSException {
        return new CSSConditionalSelector(selector, condition);
    }

    @Override
    public DescendantSelector createDescendantSelector(Selector arg0, SimpleSelector arg1) throws CSSException {
        throw new UnsupportedOperationException("CSS descendant selector is not supported");
    }

    @Override
    public SiblingSelector createDirectAdjacentSelector(short arg0, Selector arg1, SimpleSelector arg2)
            throws CSSException {
        throw new UnsupportedOperationException("CSS direct adjacent selector is not supported");
    }

    @Override
    public ElementSelector createElementSelector(String uri, String name) throws CSSException {
        return new CSSElementSelector(uri, name);
    }

    @Override
    public NegativeSelector createNegativeSelector(SimpleSelector arg0) throws CSSException {
        throw new UnsupportedOperationException("CSS negative selector is not supported");
    }

    @Override
    public ProcessingInstructionSelector createProcessingInstructionSelector(String arg0, String arg1)
            throws CSSException {
        throw new UnsupportedOperationException("CSS processing instruction is not supported");
    }

    @Override
    public ElementSelector createPseudoElementSelector(String arg0, String arg1) throws CSSException {
        throw new UnsupportedOperationException("CSS pseudo element selector is not supported");
    }

    @Override
    public SimpleSelector createRootNodeSelector() throws CSSException {
        throw new UnsupportedOperationException("CSS root node selector is not supported");
    }

    @Override
    public CharacterDataSelector createTextNodeSelector(String arg0) throws CSSException {
        throw new UnsupportedOperationException("CSS text node selector is not supported");
    }

}
