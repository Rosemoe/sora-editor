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
package io.github.rosemoe.sora.widget;

import android.animation.ValueAnimator;

/**
 * Helper class for cursor animation in editor
 *
 * @author Rosemoe
 */
class MoveCursorAnimator implements CursorAnimator, ValueAnimator.AnimatorUpdateListener {

    private ValueAnimator animatorX;
    private ValueAnimator animatorY;
    private ValueAnimator animatorBgBottom;
    private ValueAnimator animatorBackground;

    private final CodeEditor editor;
    private float startX, startY, startSize, startBottom;
    private long lastAnimateTime;

    public MoveCursorAnimator(CodeEditor editor) {
        this.editor = editor;
        animatorX = new ValueAnimator();
        animatorY = new ValueAnimator();
        animatorBackground = new ValueAnimator();
        animatorBgBottom = new ValueAnimator();
    }

    @Override
    public void markStartPos() {
        var line = editor.getCursor().getLeftLine();
        float[] pos = editor.mLayout.getCharLayoutOffset(line, editor.getCursor().getLeftColumn());
        startX = editor.measureTextRegionOffset() + pos[1];
        startY = pos[0];
        startSize = editor.mLayout.getRowCountForLine(line) * editor.getRowHeight();
        startBottom = editor.mLayout.getCharLayoutOffset(line, editor.getText().getColumnCount(line))[0];
    }

    @Override
    public boolean isRunning() {
        return animatorX.isRunning() || animatorY.isRunning() || animatorBackground.isRunning() || animatorBgBottom.isRunning();
    }

    @Override
    public void cancel() {
        animatorX.cancel();
        animatorY.cancel();
        animatorBackground.cancel();
        animatorBgBottom.cancel();
    }

    @Override
    public void markEndPos() {
        if (!editor.isCursorAnimationEnabled()) {
            return;
        }
        if (isRunning()) {
            startX = animatedX();
            startY = animatedY();
            startSize = (float) animatorBackground.getAnimatedValue();
            startBottom = (float) animatorBgBottom.getAnimatedValue();
            cancel();
        }
        var duration = 120;
        if (System.currentTimeMillis() - lastAnimateTime < 100) {
            return;
        }
        var line = editor.getCursor().getLeftLine();
        animatorX.removeAllUpdateListeners();
        float[] pos = editor.mLayout.getCharLayoutOffset(editor.getCursor().getLeftLine(), editor.getCursor().getLeftColumn());

        animatorX = ValueAnimator.ofFloat(startX, (pos[1] + editor.measureTextRegionOffset()));
        animatorY = ValueAnimator.ofFloat(startY, pos[0]);

        animatorBackground = ValueAnimator.ofFloat(startSize, editor.mLayout.getRowCountForLine(editor.getCursor().getLeftLine()) * editor.getRowHeight());
        animatorBgBottom = ValueAnimator.ofFloat(startBottom, editor.mLayout.getCharLayoutOffset(line, editor.getText().getColumnCount(line))[0]);

        animatorX.addUpdateListener(this);

        animatorX.setDuration(duration);
        animatorY.setDuration(duration);
        animatorBackground.setDuration(duration);
        animatorBgBottom.setDuration(duration);
    }

    @Override
    public void start() {
        animatorX.start();
        animatorY.start();
        animatorBackground.start();
        animatorBgBottom.start();

        lastAnimateTime = System.currentTimeMillis();
    }

    @Override
    public float animatedX() {
        return (float) animatorX.getAnimatedValue();
    }

    @Override
    public float animatedY() {
        return (float) animatorY.getAnimatedValue();
    }

    @Override
    public float animatedLineHeight() {
        return (float) animatorBackground.getAnimatedValue();
    }

    @Override
    public float animatedLineBottom() {
        return (float) animatorBgBottom.getAnimatedValue();
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        editor.postInvalidateOnAnimation();
    }
}
