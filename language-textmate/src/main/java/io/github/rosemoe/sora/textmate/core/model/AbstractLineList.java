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
 */
package io.github.rosemoe.sora.textmate.core.model;

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