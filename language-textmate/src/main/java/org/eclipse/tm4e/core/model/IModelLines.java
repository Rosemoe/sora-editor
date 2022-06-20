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

import java.util.function.Consumer;

/**
 * Mode lines API which must be initalize with a document and changed of
 * document.
 *
 */
public interface IModelLines {

    /**
     * Add a new line at specified index line.
     *
     * @param lineIndex (0-based)
     */
    void addLine(int lineIndex);

    /**
     * Remove the line at specified index line.
     *
     * @param line (0-based)
     */
    void removeLine(int lineIndex);

    /**
     * Mark as line is updated.
     *
     * @param line (0-based)
     */
    void updateLine(int lineIndex);

    /**
     * @deprecated use {@link #getNumberOfLines()}
     */
    @Deprecated
    int getSize();

    /**
     *
     * @param lineIndex (0-based)
     * @return
     */
    ModelLine get(int lineIndex);

    void forEach(Consumer<ModelLine> consumer);

    int getNumberOfLines();

    /**
     *
     * @param line (0-based)
     * @return
     * @throws Exception
     */
    String getLineText(int lineIndex) throws Exception;

    int getLineLength(int lineIndex) throws Exception;

    void dispose();


}
