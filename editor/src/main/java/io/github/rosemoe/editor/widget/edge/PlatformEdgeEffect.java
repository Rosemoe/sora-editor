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

package io.github.rosemoe.editor.widget.edge;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;

/**
 * System EdgeEffect
 * @author Rose
 */
public class PlatformEdgeEffect implements EdgeEffect {

    private final android.widget.EdgeEffect systemEdge;

    public PlatformEdgeEffect(Context context) {
        systemEdge = new android.widget.EdgeEffect(context);
    }

    @Override
    public boolean isFinished() {
        return systemEdge.isFinished();
    }

    @Override
    public void finish() {
        systemEdge.finish();
    }

    @Override
    public void onPull(float deltaDistance, float displacement) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            systemEdge.onPull(deltaDistance, displacement);
        } else {
            systemEdge.onPull(deltaDistance);
        }
    }

    @Override
    public void onRelease() {
        systemEdge.onRelease();
    }

    @Override
    public void onAbsorb(int velocity) {
        systemEdge.onAbsorb(velocity);
    }

    @Override
    public void setColor(int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            systemEdge.setColor(color);
        }
    }

    @Override
    public int getColor() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? systemEdge.getColor() : 0;
    }

    @Override
    public boolean draw(Canvas canvas) {
        return systemEdge.draw(canvas);
    }

    @Override
    public void setSize(int width, int height) {
        systemEdge.setSize(width, height);
    }
}
