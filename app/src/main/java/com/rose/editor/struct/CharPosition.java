/*
 *   Copyright 2020 Rose2073
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.rose.editor.struct;


/**
 * This a model of a character position in Content
 * @author Rose
 */
public final class CharPosition{

    //Packaged due to make changes

    public int index;

    public int line;

    public int column;

    /**
     * Get the index
     * @return index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Get column
     * @return column
     */
    public int getColumn() {
        return column;
    }

    /**
     * Get line
     * @return line
     */
    public int getLine() {
        return line;
    }

    /**
     * Make this CharPosition zero and return self
     * @return self
     */
    public CharPosition zero() {
        index = line = column = 0;
        return this;
    }

    /**
     * Make a copy of this CharPosition and return the copy
     * @return New CharPosition including info of this CharPosition
     */
    public CharPosition fromThis() {
        CharPosition pos = new CharPosition();
        pos.index = index;
        pos.line = line;
        pos.column = column;
        return pos;
    }

    @Override
    public String toString() {
        return "CharPosition(line = " + line + ",column = " + column + ",index = " + index +")";
    }

}
