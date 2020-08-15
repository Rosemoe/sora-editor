package io.github.rosemoe.editor.widget;

import android.graphics.Canvas;
import android.graphics.Paint;
import io.github.rosemoe.editor.struct.CharPosition;
import io.github.rosemoe.editor.text.LineRemoveListener;
import io.github.rosemoe.editor.text.ContentListener;

interface Layout extends LineRemoveListener, ContentListener {
    
    void destroyLayout();
    
    int getLineNumberForRow(int row);
    
    //RowIterator obtainRowIterator();
    
    int computeHorizontalScrollLimit();
    
    int computeVerticalScrollLimit();
    
    CharPosition getCharPositionForLayoutOffset(float xOffset, float yOffset);
    
    float[] getCharLayoutOffset(int line, int column);
    
}
