/*
 *   Copyright 2020 Rosemoe
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
package io.github.rosemoe.editor.widget;

import android.graphics.Canvas;
import android.graphics.Paint;
import io.github.rosemoe.editor.struct.CharPosition;
import io.github.rosemoe.editor.text.LineRemoveListener;
import io.github.rosemoe.editor.text.ContentListener;

/**
  * Layout is a manager class for editor to display text
  * Different layout may display texts in different way
  * Implementations of this interface should manage 'row's in editor.
  *
  * @author Rose
  */
public interface Layout extends LineRemoveListener, ContentListener {
    
    void destroyLayout();
    
    int getLineNumberForRow(int row);
    
    RowIterator obtainRowIterator(int initialRow);
    
    int getLayoutWidth();
    
    int getLayoutHeight();
    
    CharPosition getCharPositionForLayoutOffset(float xOffset, float yOffset);
    
    float[] getCharLayoutOffset(int line, int column);
    
}
