package io.github.rosemoe.sora.widget.style.builtin;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.TypedValue;

import androidx.annotation.NonNull;

import io.github.rosemoe.sora.widget.style.SelectionHandleStyle;

public class HandleStyleSideDrop extends HandleStyleDrop {
    
    private final int size;
    private final Paint paint;

    public HandleStyleSideDrop(Context context) {
        super(context);
        size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 22f, context.getResources().getDisplayMetrics());
        paint = new Paint();
        paint.setAntiAlias(true);
    }
    
    @Override
    public void draw(@NonNull Canvas canvas, int handleType, float x, float y, int rowHeight, int color, @NonNull HandleDescriptor descriptor) {
        float radius = size / 2;
        paint.setColor(color);
        if (handleType == HANDLE_TYPE_INSERT || handleType == HANDLE_TYPE_UNDEFINED) {
            super.draw(canvas, handleType, x, y, rowHeight, color, descriptor);
        } else {
            boolean type = handleType == HANDLE_TYPE_LEFT;
            float cx = type ? x - radius : x + radius;
            canvas.drawCircle(cx, y + radius, radius, paint);
            canvas.drawRect(type ? cx : cx - radius, y, type ? cx + radius : cx, y + radius, paint);
            descriptor.set(cx - radius, y, cx + radius, y + 2 * radius, type ? ALIGN_LEFT : ALIGN_RIGHT);
        }
    }
    
}
