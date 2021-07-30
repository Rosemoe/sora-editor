/*
 *    CodeEditor - the awesome code editor for Android
 *    Copyright (C) 2020-2021  Rosemoe
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 *     Please contact Rosemoe by email roses2020@qq.com if you need
 *     additional information or have any questions
 */
package io.github.rosemoe.editor.text;

/**
 * Indexer without cache
 *
 * @author Rose
 */
public final class NoCacheIndexer extends CachedIndexer implements Indexer {

    /**
     * Create a indexer without cache
     *
     * @param content Target content
     */
    public NoCacheIndexer(Content content) {
        super(content);
        //Disable dynamic indexing
        if (super.getMaxCacheSize() != 0) {
            super.setMaxCacheSize(0);
        }
        if (super.isHandleEvent()) {
            super.setHandleEvent(false);
        }
    }

}

