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
