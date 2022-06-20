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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract class for Model lines used by the TextMate model. Implementation
 * class must :
 *
 * <ul>
 * <li>synchronizes lines with the lines of the editor content when it changed.</li>
 * <li>call {@link AbstractLineList#invalidateLine(int)} with the first changed line.</li>
 * </ul>
 *
 */
public abstract class AbstractLineList implements IModelLines {

    private static final Logger LOGGER = Logger.getLogger(AbstractLineList.class.getName());

    private final List<ModelLine> list = Collections.synchronizedList(new ArrayList<>());

    private TMModel model;

    public AbstractLineList() {
    }

    void setModel(TMModel model) {
        this.model = model;
    }

    @Override
    public void addLine(int line) {
        try {
            this.list.add(line, new ModelLine());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeLine(int line) {
        this.list.remove(line);
    }

    @Override
    public void updateLine(int line) {
        try {
            // this.list.get(line).text = this.lineToTextResolver.apply(line);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    @Override
    public ModelLine get(int index) {
        return this.list.get(index);
    }

    @Override
    public void forEach(Consumer<ModelLine> consumer) {
        this.list.forEach(consumer);
    }

    protected void invalidateLine(int lineIndex) {
        if (model != null) {
            model.invalidateLine(lineIndex);
        }
    }

    @Override
    @Deprecated
    public int getSize() {
        return getNumberOfLines();
    }
}