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

import java.util.List;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Class for containing diagnostics
 *
 * @author Rosemoe
 */
public class DiagnosticsContainer {

    private final SortedSet<DiagnosticRegion> regions = new TreeSet<>();

    public synchronized void addDiagnostic(DiagnosticRegion diagnostic) {
        regions.add(diagnostic);
    }

    public List<DiagnosticRegion> queryInRegion(int startIndex, int endIndex) {
        return null;
    }

    public synchronized void shiftOnInsert(int insertStart, int insertEnd) {

    }

    public synchronized void shiftOnDelete(int deleteStart, int deleteEnd) {

    }

    public void reset() {
        regions.clear();
    }

}
