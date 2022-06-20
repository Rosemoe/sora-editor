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
package org.eclipse.tm4e.core.internal.types;

import java.util.Collection;

public interface IRawRule {

    Integer getId();

    void setId(Integer id);

    String getInclude();

    void setInclude(String include);

    String getName();

    void setName(String name);

    String getContentName();

    void setContentName(String name);

    String getMatch();

    void setMatch(String match);

    IRawCaptures getCaptures();

    void setCaptures(IRawCaptures captures);

    String getBegin();

    void setBegin(String begin);

    IRawCaptures getBeginCaptures();

    void setBeginCaptures(IRawCaptures beginCaptures);

    String getEnd();

    void setEnd(String end);

    String getWhile();

    IRawCaptures getEndCaptures();

    void setEndCaptures(IRawCaptures endCaptures);

    IRawCaptures getWhileCaptures();

    Collection<IRawRule> getPatterns();

    void setPatterns(Collection<IRawRule> patterns);

    IRawRepository getRepository();

    void setRepository(IRawRepository repository);

    boolean isApplyEndPatternLast();

    void setApplyEndPatternLast(boolean applyEndPatternLast);
}
