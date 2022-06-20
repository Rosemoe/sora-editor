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
package io.github.rosemoe.sora.lang.diagnostic;

/**
 * Class for describing a diagnostic region.
 *
 * @author Rosemoe
 */
public final class DiagnosticRegion implements Comparable<DiagnosticRegion> {

    public final static short SEVERITY_NONE = 0;
    public final static short SEVERITY_TYPO = 1;
    public final static short SEVERITY_WEAK_WARNING = 2;
    public final static short SEVERITY_WARNING = 3;
    public final static short SEVERITY_ERROR = 4;

    public long id;
    public String description;
    public int startIndex;
    public int endIndex;
    public short severity;

    public DiagnosticRegion(int startIndex, int endIndex, short severity) {
        this(startIndex, endIndex, severity, null, 0);
    }

    public DiagnosticRegion(int startIndex, int endIndex, short severity, String description, long id) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.severity = severity;
        this.description = description;
        this.id = id;
    }

    public int getSeverity() {
        return severity;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public String getDescription() {
        return description;
    }

    public long getId() {
        return id;
    }

    @Override
    public int compareTo(DiagnosticRegion o) {
        var cmp = Integer.compare(startIndex, o.startIndex);
        if (cmp == 0) {
            cmp = Integer.compare(endIndex, o.endIndex);
            if (cmp == 0) {
                cmp = Short.compare(severity, o.severity);
                if (cmp == 0) {
                    cmp = Long.compare(id, o.id);
                }
            }
        }
        return cmp;
    }
}
