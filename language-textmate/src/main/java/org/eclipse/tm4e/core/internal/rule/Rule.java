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
package org.eclipse.tm4e.core.internal.rule;

import org.eclipse.tm4e.core.internal.oniguruma.IOnigCaptureIndex;
import org.eclipse.tm4e.core.internal.utils.RegexSource;

public abstract class Rule {

    public final int id;

    private boolean nameIsCapturing;
    private String name;

    private boolean contentNameIsCapturing;
    private String contentName;

    public Rule(int id, String name, String contentName) {
        this.id = id;
        this.name = name;
        this.nameIsCapturing = RegexSource.hasCaptures(this.name);
        this.contentName = contentName;
        this.contentNameIsCapturing = RegexSource.hasCaptures(this.contentName);
    }

    public String getName(String lineText, IOnigCaptureIndex[] captureIndices) {
        if (!this.nameIsCapturing) {
            return this.name;
        }
        return RegexSource.replaceCaptures(this.name, lineText, captureIndices);
    }

    public String getContentName(String lineText, IOnigCaptureIndex[] captureIndices) {
        if (!this.contentNameIsCapturing) {
            return this.contentName;
        }
        return RegexSource.replaceCaptures(this.contentName, lineText, captureIndices);
    }

    public abstract void collectPatternsRecursive(IRuleRegistry grammar, RegExpSourceList out, boolean isFirst);

    public abstract ICompiledRule compile(IRuleRegistry grammar, String endRegexSource, boolean allowA, boolean allowG);

}