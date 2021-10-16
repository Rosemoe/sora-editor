/*
 * Copyright (c) 2015-2017 Angelo ZERR.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 * <p>
 * Contributors:
 * Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 * Jochen Ulrich <jochenulrich@t-online.de> - exception messages
 */
package io.github.rosemoe.sora.textmate.core.internal.css;

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
