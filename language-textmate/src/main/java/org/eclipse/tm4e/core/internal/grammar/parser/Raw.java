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
package org.eclipse.tm4e.core.internal.grammar.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.tm4e.core.internal.types.IRawCaptures;
import org.eclipse.tm4e.core.internal.types.IRawGrammar;
import org.eclipse.tm4e.core.internal.types.IRawRepository;
import org.eclipse.tm4e.core.internal.types.IRawRule;
import org.eclipse.tm4e.core.internal.utils.CloneUtils;

/**
 * Raw
 *
 */
public class Raw extends HashMap<String, Object> implements IRawRepository, IRawRule, IRawGrammar, IRawCaptures {
    private static final String FIRST_LINE_MATCH = "firstLineMatch";
    private static final String FILE_TYPES = "fileTypes";
    private static final String SCOPE_NAME = "scopeName";
    private static final String APPLY_END_PATTERN_LAST = "applyEndPatternLast";
    private static final String REPOSITORY = "repository";
    private static final String INJECTION_SELECTOR = "injectionSelector";
    private static final String INJECTIONS = "injections";
    private static final String PATTERNS = "patterns";
    private static final String WHILE_CAPTURES = "whileCaptures";
    private static final String END_CAPTURES = "endCaptures";
    private static final String INCLUDE = "include";
    private static final String WHILE = "while";
    private static final String END = "end";
    private static final String BEGIN = "begin";
    private static final String CAPTURES = "captures";
    private static final String MATCH = "match";
    private static final String BEGIN_CAPTURES = "beginCaptures";
    private static final String CONTENT_NAME = "contentName";
    private static final String NAME = "name";
    private static final String ID = "id";
    private static final String DOLLAR_SELF = "$self";
    private static final String DOLLAR_BASE = "$base";
    private static final long serialVersionUID = -2306714541728887963L;
    private List<String> fileTypes;

    @Override
    public IRawRule getProp(String name) {
        return (IRawRule) super.get(name);
    }

    @Override
    public IRawRule getBase() {
        return (IRawRule) super.get(DOLLAR_BASE);
    }

    @Override
    public void setBase(IRawRule base) {
        super.put(DOLLAR_BASE, base);
    }

    @Override
    public IRawRule getSelf() {
        return (IRawRule) super.get(DOLLAR_SELF);
    }

    @Override
    public void setSelf(IRawRule self) {
        super.put(DOLLAR_SELF, self);
    }

    @Override
    public Integer getId() {
        return (Integer) super.get(ID);
    }

    @Override
    public void setId(Integer id) {
        super.put(ID, id);
    }

    @Override
    public String getName() {
        return (String) super.get(NAME);
    }

    @Override
    public void setName(String name) {
        super.put(NAME, name);
    }

    @Override
    public String getContentName() {
        return (String) super.get(CONTENT_NAME);
    }

    @Override
    public void setContentName(String name) {
        super.put(CONTENT_NAME, name);
    }

    @Override
    public String getMatch() {
        return (String) super.get(MATCH);
    }

    @Override
    public void setMatch(String match) {
        super.put(MATCH, match);
    }

    @Override
    public IRawCaptures getCaptures() {
        updateCaptures(CAPTURES);
        return (IRawCaptures) super.get(CAPTURES);
    }

    @Override
    public void setCaptures(IRawCaptures captures) {
        super.put(CAPTURES, captures);
    }

    private void updateCaptures(String name) {
        Object captures = super.get(name);
        if (captures instanceof List) {
            Raw rawCaptures = new Raw();
            int i = 0;
            for (Object capture : (List<?>) captures) {
                i++;
                rawCaptures.put(i + "", capture);
            }
            super.put(name, rawCaptures);
        }
    }

    @Override
    public String getBegin() {
        return (String) super.get(BEGIN);
    }

    @Override
    public void setBegin(String begin) {
        super.put(BEGIN, begin);
    }

    @Override
    public String getWhile() {
        return (String) super.get(WHILE);
    }

    @Override
    public String getInclude() {
        return (String) super.get(INCLUDE);
    }

    @Override
    public void setInclude(String include) {
        super.put(INCLUDE, include);
    }

    @Override
    public IRawCaptures getBeginCaptures() {
        updateCaptures(BEGIN_CAPTURES);
        return (IRawCaptures) super.get(BEGIN_CAPTURES);
    }

    @Override
    public void setBeginCaptures(IRawCaptures beginCaptures) {
        super.put(BEGIN_CAPTURES, beginCaptures);
    }

    @Override
    public String getEnd() {
        return (String) super.get(END);
    }

    @Override
    public void setEnd(String end) {
        super.put(END, end);
    }

    @Override
    public IRawCaptures getEndCaptures() {
        updateCaptures(END_CAPTURES);
        return (IRawCaptures) super.get(END_CAPTURES);
    }

    @Override
    public void setEndCaptures(IRawCaptures endCaptures) {
        super.put(END_CAPTURES, endCaptures);
    }

    @Override
    public IRawCaptures getWhileCaptures() {
        updateCaptures(WHILE_CAPTURES);
        return (IRawCaptures) super.get(WHILE_CAPTURES);
    }

    @Override
    public Collection<IRawRule> getPatterns() {
        return (Collection<IRawRule>) super.get(PATTERNS);
    }

    @Override
    public void setPatterns(Collection<IRawRule> patterns) {
        super.put(PATTERNS, patterns);
    }

    @Override
    public Map<String, IRawRule> getInjections() {
        return (Map<String, IRawRule>) super.get(INJECTIONS);
    }

    @Override
    public String getInjectionSelector() {
        return (String) super.get(INJECTION_SELECTOR);
    }

    @Override
    public IRawRepository getRepository() {
        return (IRawRepository) super.get(REPOSITORY);
    }

    @Override
    public void setRepository(IRawRepository repository) {
        super.put(REPOSITORY, repository);
    }

    @Override
    public boolean isApplyEndPatternLast() {
        Object applyEndPatternLast = super.get(APPLY_END_PATTERN_LAST);
        if (applyEndPatternLast == null) {
            return false;
        }
        if (applyEndPatternLast instanceof Boolean) {
            return (Boolean) applyEndPatternLast;
        }
        if (applyEndPatternLast instanceof Integer) {
            return ((Integer) applyEndPatternLast).equals(1);
        }
        return false;
    }

    @Override
    public void setApplyEndPatternLast(boolean applyEndPatternLast) {
        super.put(APPLY_END_PATTERN_LAST, applyEndPatternLast);
    }

    @Override
    public String getScopeName() {
        return (String) super.get(SCOPE_NAME);
    }

    @Override
    public Collection<String> getFileTypes() {
        if (fileTypes == null) {
            List<String> list = new ArrayList<>();
            Collection<?> unparsedFileTypes = (Collection<?>) super.get(FILE_TYPES);
            if (unparsedFileTypes != null) {
                for (Object o : unparsedFileTypes) {
                    String str = o.toString();
                    // #202
                    if (str.startsWith(".")) {
                        str = str.substring(1);
                    }
                    list.add(str);
                }
            }
            fileTypes = list;
        }
        return fileTypes;
    }
    @Override
    public String getFirstLineMatch() {
        return (String) super.get(FIRST_LINE_MATCH);
    }

    @Override
    public IRawRule getCapture(String captureId) {
        return getProp(captureId);
    }

    @Override
    public Iterator<String> iterator() {
        return super.keySet().iterator();
    }

    @Override
    public Object clone() {
        return CloneUtils.clone(this);
    }

}