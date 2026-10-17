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
 ******************************************************************************/
package io.github.rosemoe.sora.lang.folding;

import androidx.annotation.NonNull;

/**
 * An immutable, line based region of the source code that can be folded (collapsed).
 * <p>
 * When the region is collapsed, the first line (the header, {@link #getStartLine()}) stays
 * visible while all lines from {@code startLine + 1} to {@link #getEndLine()} (inclusive)
 * are removed from the screen.
 * <p>
 * Two regions are equal when they span the same lines, so the folding state of a region can be
 * tracked in a set even though regions are recreated when the analyze results are refreshed.
 *
 * @author nullij
 * @see <a href="https://github.com/nullij">https://github.com/nullij</a>
 */
public class FoldingRegion implements Comparable<FoldingRegion> {

    private final int startLine;
    private final int endLine;

    /**
     * Create a region spanning the given lines (both inclusive)
     *
     * @param startLine The header line of the region
     * @param endLine   The last line of the region, must not be smaller than {@code startLine}
     */
    public FoldingRegion(int startLine, int endLine) {
        if (startLine < 0 || endLine < startLine) {
            throw new IllegalArgumentException("Illegal folding region: " + startLine + ".." + endLine);
        }
        this.startLine = startLine;
        this.endLine = endLine;
    }

    /**
     * First line of the region. It is the only line that stays visible when the region is collapsed.
     */
    public int getStartLine() {
        return startLine;
    }

    /**
     * Last line of the region (inclusive). It is hidden when the region is collapsed.
     */
    public int getEndLine() {
        return endLine;
    }

    /**
     * Number of lines that are hidden when this region is collapsed
     */
    public int getHiddenLineCount() {
        return Math.max(0, endLine - startLine);
    }

    /**
     * Whether the region hides anything when collapsed. Single line regions can not be folded.
     */
    public boolean isFoldable() {
        return endLine > startLine;
    }

    /**
     * Whether the given line is inside this region (header included)
     */
    public boolean contains(int line) {
        return line >= startLine && line <= endLine;
    }

    /**
     * Whether the given line is hidden when this region is collapsed
     */
    public boolean containsHiddenLine(int line) {
        return line > startLine && line <= endLine;
    }

    @Override
    public int compareTo(@NonNull FoldingRegion other) {
        int res = Integer.compare(startLine, other.startLine);
        return res != 0 ? res : Integer.compare(endLine, other.endLine);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof FoldingRegion)) {
            return false;
        }
        var other = (FoldingRegion) obj;
        return startLine == other.startLine && endLine == other.endLine;
    }

    @Override
    public int hashCode() {
        return 31 * startLine + endLine;
    }

    @NonNull
    @Override
    public String toString() {
        return "FoldingRegion{" + startLine + ".." + endLine + '}';
    }

}
