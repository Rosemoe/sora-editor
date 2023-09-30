/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2023  Rosemoe
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

import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

import io.github.rosemoe.sora.event.EditorKeyEvent;
import io.github.rosemoe.sora.event.InterceptTarget;
import io.github.rosemoe.sora.event.KeyBindingEvent;
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.text.Cursor;
import io.github.rosemoe.sora.text.method.KeyMetaStates;
import io.github.rosemoe.sora.util.Chars;
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion;

/**
 * Handles {@link KeyEvent}s in editor.
 *
 * <p>
 * <strong>This is for internal use only.</strong>
 * </p>
 *
 * @author Rose
 * @author Akash Yadav
 */
public class EditorKeyEventHandler {

    private static final String TAG = "EditorKeyEventHandler";
    private final CodeEditor editor;
    private final KeyMetaStates keyMetaStates;

    EditorKeyEventHandler(CodeEditor editor) {
        Objects.requireNonNull(editor, "Cannot setup KeyEvent with null editor instance.");
        this.editor = editor;
        this.keyMetaStates = new KeyMetaStates(editor);
    }

    /**
     * Check if the given {@link KeyEvent} is a key binding event.
     * {@link EditorKeyEventHandler#keyMetaStates} must be notified about the key event before this
     * method is called.
     *
     * @param keyCode The keycode.
     * @param event   The key event.
     * @return <code>true</code> if the event is a key binding event. <code>false</code> otherwise.
     */
    private boolean isKeyBindingEvent(int keyCode, KeyEvent event) {

        // These keys must be pressed for the key event to be a key binding event
        if (!(keyMetaStates.isShiftPressed() || keyMetaStates.isAltPressed() || event.isCtrlPressed())) {
            return false;
        }

        // Any alphabet key
        if (keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z) {
            return true;
        }

        // Other key combinations
        return keyCode == KeyEvent.KEYCODE_ENTER
                || keyCode == KeyEvent.KEYCODE_DPAD_UP
                || keyCode == KeyEvent.KEYCODE_DPAD_DOWN
                || keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                || keyCode == KeyEvent.KEYCODE_MOVE_HOME
                || keyCode == KeyEvent.KEYCODE_MOVE_END;
    }

    /**
     * Get the {@link KeyMetaStates} instance.
     *
     * @return The {@link KeyMetaStates} instance.
     */
    @NonNull
    public KeyMetaStates getKeyMetaStates() {
        return keyMetaStates;
    }

    /**
     * Called by editor in {@link CodeEditor#onKeyDown(int, KeyEvent)}.
     *
     * @param keyCode The key code.
     * @param event   The key event.
     * @return <code>true</code> if the event was handled, <code>false</code> otherwise.
     */
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        keyMetaStates.onKeyDown(event);
        final var editor = this.editor;
        final var eventManager = editor.eventManager;

        final var editorKeyEvent = new EditorKeyEvent(editor, event, EditorKeyEvent.Type.DOWN);
        final var keybindingEvent =
                new KeyBindingEvent(editor,
                        event,
                        EditorKeyEvent.Type.DOWN,
                        editor.canHandleKeyBinding(keyCode, event.isCtrlPressed(), keyMetaStates.isShiftPressed(), keyMetaStates.isAltPressed()));
        if ((eventManager.dispatchEvent(editorKeyEvent) & InterceptTarget.TARGET_EDITOR) != 0) {
            return editorKeyEvent.result(false);
        }

        final var isShiftPressed = keyMetaStates.isShiftPressed();
        final var isAltPressed = keyMetaStates.isAltPressed();
        final var isCtrlPressed = event.isCtrlPressed();

        // Currently, KeyBindingEvent is triggered only for (Shift | Ctrl | Alt) + alphabet keys
        // Should we add support for more keys?
        if (isKeyBindingEvent(keyCode, event)) {
            if ((eventManager.dispatchEvent(keybindingEvent) & InterceptTarget.TARGET_EDITOR) != 0) {
                return keybindingEvent.result(false) || editorKeyEvent.result(false);
            }
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_MOVE_HOME:
            case KeyEvent.KEYCODE_MOVE_END:
                final var cursor = editor.getCursor();
                if (isShiftPressed && (!cursor.isSelected())) {
                    editor.selectionAnchor = cursor.left();
                } else if (!isShiftPressed && editor.selectionAnchor != null) {
                    editor.selectionAnchor = null;
                }
                keyMetaStates.adjust();
        }

        Boolean result = handleKeyEvent(event, editorKeyEvent, keybindingEvent, keyCode, isShiftPressed, isAltPressed, isCtrlPressed);
        if (result != null) {
            return result;
        }

        return editorKeyEvent.result(editor.onSuperKeyDown(keyCode, event));
    }

    private Boolean handleKeyEvent(KeyEvent event,
                                   EditorKeyEvent editorKeyEvent,
                                   KeyBindingEvent keybindingEvent,
                                   int keyCode,
                                   boolean isShiftPressed,
                                   boolean isAltPressed,
                                   boolean isCtrlPressed
    ) {
        final var connection = editor.inputConnection;
        final var editorCursor = editor.getCursor();
        final var editorText = editor.getText();
        final var completionWindow = editor.getComponent(EditorAutoCompletion.class);
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK: {
                if (editorCursor.isSelected()) {
                    editor.setSelection(editorCursor.getLeftLine(), editorCursor.getLeftColumn());
                    return editorKeyEvent.result(true);
                }
                if (editor.isInLongSelect()) {
                    editor.endLongSelect();
                    return editorKeyEvent.result(true);
                }
                return editorKeyEvent.result(false);
            }
            case KeyEvent.KEYCODE_DEL:
                if (editor.isEditable()) {
                    editor.deleteText();
                    editor.notifyIMEExternalCursorChange();
                }
                return editorKeyEvent.result(true);
            case KeyEvent.KEYCODE_FORWARD_DEL: {
                if (editor.isEditable()) {
                    connection.deleteSurroundingText(0, 1);
                    editor.notifyIMEExternalCursorChange();
                }
                return editorKeyEvent.result(true);
            }
            case KeyEvent.KEYCODE_ENTER: {
                return handleEnterKeyEvent(editorKeyEvent, keybindingEvent, isShiftPressed, isAltPressed, isCtrlPressed);
            }
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (isCtrlPressed) {
                    if (isShiftPressed) {
                        final var left = editorCursor.left();
                        final var right = editorCursor.right();
                        final var lines = editorText.getLineCount();
                        if (right.line == lines - 1) {
                            // last line, cannot move down
                            return editorKeyEvent.result(true);
                        }

                        final var next = editorText.getLine(right.line + 1).toString();
                        editorText.beginBatchEdit();
                        editorText.delete(right.line, editorText.getColumnCount(right.line), right.line + 1, next.length());
                        editorText.insert(left.line, 0, next.concat(editor.getLineSeparator().getContent()));
                        editorText.endBatchEdit();

                        // Update selection
                        final var newLeft = new CharPosition(left.line + 1, left.column);
                        final var newRight = new CharPosition(right.line + 1, right.column);
                        if (left.index != right.index) {
                            editor.setSelectionRegion(newLeft.line, newLeft.column, newRight.line, newRight.column);
                            if (editor.selectionAnchor.equals(left)) {
                                editor.selectionAnchor = newLeft;
                            } else {
                                editor.selectionAnchor = newRight;
                            }
                        } else {
                            editor.setSelection(newLeft.line, newLeft.column);
                        }

                        return editorKeyEvent.result(true);
                    }
                    final var dy = editor.getOffsetY() + editor.getRowHeight() > editor.getScrollMaxY()
                            ? editor.getScrollMaxY() - editor.getOffsetY()
                            : editor.getRowHeight();
                    editor.getScroller().startScroll(editor.getOffsetX(), editor.getOffsetY(), 0, dy, 0);
                    return editorKeyEvent.result(true);
                }
                editor.moveSelectionDown();
                return editorKeyEvent.result(true);
            case KeyEvent.KEYCODE_DPAD_UP:
                if (isCtrlPressed) {
                    if (isShiftPressed) {
                        final var left = editorCursor.left();
                        final var right = editorCursor.right();
                        final var lines = editorText.getLineCount();
                        if (left.line == 0) {
                            // first line, cannot move up
                            return editorKeyEvent.result(true);
                        }

                        final var prev = editorText.getLine(left.line - 1).toString();
                        editorText.beginBatchEdit();
                        editorText.delete(left.line - 1, 0, left.line, 0);
                        editorText.insert(right.line - 1, editorText.getColumnCount(right.line - 1), editor.getLineSeparator().getContent().concat(prev));
                        editorText.endBatchEdit();

                        // Update selection
                        final var newLeft = new CharPosition(left.line - 1, left.column);
                        final var newRight = new CharPosition(right.line - 1, right.column);
                        if (left.index != right.index) {
                            editor.setSelectionRegion(newLeft.line, newLeft.column, newRight.line, newRight.column);
                            if (editor.selectionAnchor.equals(left)) {
                                editor.selectionAnchor = newLeft;
                            } else {
                                editor.selectionAnchor = newRight;
                            }
                        } else {
                            editor.setSelection(newLeft.line, newLeft.column);
                        }

                        return editorKeyEvent.result(true);
                    }
                    if (editor.getOffsetY() == 0) {
                        return editorKeyEvent.result(true);
                    }
                    var dy = -editor.getRowHeight();
                    if (editor.getOffsetY() - editor.getRowHeight() < 0) {
                        dy = -editor.getOffsetY();
                    }
                    editor.getScroller().startScroll(editor.getOffsetX(), editor.getOffsetY(), 0, dy, 0);
                    return editorKeyEvent.result(true);
                }
                editor.moveSelectionUp();
                return editorKeyEvent.result(true);
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (isCtrlPressed) {
                    final var handle = editorCursor.left().equals(editor.selectionAnchor) ? editorCursor.right() : editorCursor.left();
                    final var prevStart = Chars.prevWordStart(handle, editorText);
                    if (editor.selectionAnchor != null) {
                        editor.setSelectionRegion(editor.selectionAnchor.line, editor.selectionAnchor.column, prevStart.line, prevStart.column);
                        editor.ensureSelectingTargetVisible();
                        return editorKeyEvent.result(true);
                    }
                    editor.setSelection(prevStart.line, prevStart.column);
                    return editorKeyEvent.result(true);
                }
                editor.moveSelectionLeft();
                return editorKeyEvent.result(true);
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (isCtrlPressed) {
                    final var handle = editorCursor.left().equals(editor.selectionAnchor) ? editorCursor.right() : editorCursor.left();
                    final var nextEnd = Chars.nextWordEnd(handle, editorText);
                    if (editor.selectionAnchor != null) {
                        editor.setSelectionRegion(editor.selectionAnchor.line, editor.selectionAnchor.column, nextEnd.line, nextEnd.column);
                        editor.ensureSelectingTargetVisible();
                        return editorKeyEvent.result(true);
                    }
                    editor.setSelection(nextEnd.line, nextEnd.column);
                    return editorKeyEvent.result(true);
                }
                editor.moveSelectionRight();
                return editorKeyEvent.result(true);
            case KeyEvent.KEYCODE_MOVE_END:
                final var lastLine = editorText.getLineCount() - 1;
                final var lastColumn = editorText.getColumnCount(lastLine);
                if (isCtrlPressed) {
                    if (editor.selectionAnchor != null) {
                        editor.setSelectionRegion(editor.selectionAnchor.line, editor.selectionAnchor.column, lastLine, lastColumn);
                        editor.ensureSelectingTargetVisible();
                        return editorKeyEvent.result(true);
                    }
                    editor.setSelection(lastLine, lastColumn);
                    return editorKeyEvent.result(true);
                }
                editor.moveSelectionEnd();
                return editorKeyEvent.result(true);
            case KeyEvent.KEYCODE_MOVE_HOME:
                if (isCtrlPressed) {
                    if (editor.selectionAnchor != null) {
                        editor.setSelectionRegion(0, 0, editor.selectionAnchor.line, editor.selectionAnchor.column);
                        editor.ensureSelectingTargetVisible();
                        return editorKeyEvent.result(true);
                    }
                    editor.setSelection(0, 0);
                    return editorKeyEvent.result(true);
                }
                editor.moveSelectionHome();
                return editorKeyEvent.result(true);
            case KeyEvent.KEYCODE_PAGE_DOWN:
                editor.movePageDown();
                return editorKeyEvent.result(true);
            case KeyEvent.KEYCODE_PAGE_UP:
                editor.movePageUp();
                return editorKeyEvent.result(true);
            case KeyEvent.KEYCODE_TAB:
                if (editor.isEditable()) {
                    if (completionWindow.isShowing() && !isShiftPressed) {
                        completionWindow.select();
                    } else if (editor.getSnippetController().isInSnippet() && !isAltPressed && !isCtrlPressed) {
                        if (isShiftPressed) {
                            editor.getSnippetController().shiftToPreviousTabStop();
                        } else {
                            editor.getSnippetController().shiftToNextTabStop();
                        }
                    } if (isShiftPressed) {
                        // Shift + TAB -> unindent the [selected] lines
                        editor.unindentSelection();
                    } else {
                        editor.indentOrCommitTab();
                    }
                }
                return editorKeyEvent.result(true);
            case KeyEvent.KEYCODE_PASTE:
                if (editor.isEditable()) {
                    editor.pasteText();
                }
                return editorKeyEvent.result(true);
            case KeyEvent.KEYCODE_COPY:
                editor.copyText();
                return editorKeyEvent.result(true);
            case KeyEvent.KEYCODE_SPACE:
                if (editor.isEditable()) {
                    editor.commitText(" ");
                    editor.notifyIMEExternalCursorChange();
                }
                return editorKeyEvent.result(true);
            case KeyEvent.KEYCODE_ESCAPE:
                if (editorCursor.isSelected()) {
                    final var newPosition = editor.getProps().positionOfCursorWhenExitSelecting ? editorCursor.right() : editorCursor.left();
                    editor.setSelection(newPosition.line, newPosition.column, true);
                }
                return editorKeyEvent.result(true);
            default:
                if (event.isCtrlPressed() && !event.isAltPressed()) {
                    return handleCtrlKeyBinding(editorKeyEvent, keybindingEvent, keyCode, isShiftPressed);
                }

                if (!event.isCtrlPressed() && !event.isAltPressed()) {
                    return handlePrintingKey(event, editorKeyEvent, keyCode);
                }
        }
        return null;
    }

    @NonNull
    private Boolean handlePrintingKey(
            KeyEvent event,
            EditorKeyEvent editorKeyEvent,
            int keyCode) {
        final var editorText = this.editor.getText();
        final var editorCursor = this.editor.getCursor();
        if (event.isPrintingKey() && editor.isEditable()) {
            String text = new String(Character.toChars(event.getUnicodeChar(event.getMetaState())));
            // replace text
            SymbolPairMatch.SymbolPair pair = null;
            if (editor.getProps().symbolPairAutoCompletion) {

                var firstCharFromText = text.charAt(0);

                char[] inputText = null;

                //size > 1
                if (text.length() > 1) {
                    inputText = text.toCharArray();
                }

                pair = editor.languageSymbolPairs.matchBestPair(
                        editor.getText(), editor.getCursor().left(),
                        inputText, firstCharFromText
                );
            }
            if (pair == null || pair == SymbolPairMatch.SymbolPair.EMPTY_SYMBOL_PAIR
                    || pair.shouldNotReplace(editor)) {
                editor.commitText(text);
                editor.notifyIMEExternalCursorChange();
            } else {

                // QuickQuoteHandler can easily implement the feature of AutoSurround
                // and is at a higher level (customizable),
                // so if the language implemented QuickQuoteHandler,
                // the AutoSurround feature is not considered needed because it can be implemented through QuickQuoteHandler

                if (pair.shouldDoAutoSurround(editorText) && editor.getEditorLanguage().getQuickQuoteHandler() == null) {
                    editorText.beginBatchEdit();
                    // insert left
                    editorText.insert(editorCursor.getLeftLine(), editorCursor.getLeftColumn(), pair.open);
                    // editorText.insert(editorCursor.getLeftLine(),editorCursor.getLeftColumn(),selectText);
                    // insert right
                    editorText.insert(editorCursor.getRightLine(), editorCursor.getRightColumn(), pair.close);
                    editorText.endBatchEdit();

                    // setSelection
                    editor.setSelectionRegion(editorCursor.getLeftLine(), editorCursor.getLeftColumn(),
                            editorCursor.getRightLine(), editorCursor.getRightColumn() - pair.close.length());
                } else if (editorCursor.isSelected() && editor.getEditorLanguage().getQuickQuoteHandler() != null) {
                    editor.commitText(text);
                } else {
                    editorText.beginBatchEdit();

                    var insertPosition = editorText
                            .getIndexer()
                            .getCharPosition(pair.getInsertOffset());

                    editorText.replace(insertPosition.line, insertPosition.column, editorCursor.getRightLine(), editorCursor.getRightColumn(), pair.open);
                    editorText.insert(insertPosition.line, insertPosition.column + pair.open.length(), pair.close);
                    editorText.endBatchEdit();

                    var cursorPosition = editorText
                            .getIndexer()
                            .getCharPosition(pair.getCursorOffset());

                    editor.setSelection(cursorPosition.line, cursorPosition.column);
                }
                editor.notifyIMEExternalCursorChange();
            }
        } else {
            return editor.onSuperKeyDown(keyCode, event);
        }
        return editorKeyEvent.result(true);
    }

    @Nullable
    private Boolean handleCtrlKeyBinding(
            EditorKeyEvent e,
            KeyBindingEvent keybindingEvent,
            int keyCode,
            boolean isShiftPressed) {
        final var editor = this.editor;
        final var connection = editor.inputConnection;
        final var editorText = editor.getText();
        final var editorCursor = editor.getCursor();
        switch (keyCode) {
            case KeyEvent.KEYCODE_V:
                if (editor.isEditable()) {
                    editor.pasteText();
                }
                return keybindingEvent.result(true) || e.result(true);
            case KeyEvent.KEYCODE_C:
                editor.copyText();
                return keybindingEvent.result(true) || e.result(true);
            case KeyEvent.KEYCODE_X:
                if (editor.isEditable()) {
                    editor.cutText();
                } else {
                    editor.copyText();
                }
                return keybindingEvent.result(true) || e.result(true);
            case KeyEvent.KEYCODE_A:
                editor.selectAll();
                return keybindingEvent.result(true) || e.result(true);
            case KeyEvent.KEYCODE_Z:
                if (editor.isEditable()) {
                    editor.undo();
                }
                return keybindingEvent.result(true) || e.result(true);
            case KeyEvent.KEYCODE_Y:
                if (editor.isEditable()) {
                    editor.redo();
                }
                return keybindingEvent.result(true) || e.result(true);
            case KeyEvent.KEYCODE_D:
                if (editor.isEditable()) {
                    editor.duplicateLine();
                }
                return keybindingEvent.result(true) || e.result(true);
            case KeyEvent.KEYCODE_W:
                editor.selectCurrentWord();
                return keybindingEvent.result(true) || e.result(true);
            case KeyEvent.KEYCODE_J:
                if (!isShiftPressed || editorCursor.isSelected()) {
                    // TODO If the cursor is selected, then the selected lines must be joined.
                    return keybindingEvent.result(false) || e.result(false);
                }

                final var line = editorCursor.getLeftLine();
                editor.setSelection(line, editorText.getColumnCount(line));
                connection.deleteSurroundingText(0, 1);
                editor.ensureSelectionVisible();
                return keybindingEvent.result(true) || e.result(true);
        }
        return null;
    }

    @NonNull
    private Boolean handleEnterKeyEvent(
            EditorKeyEvent editorKeyEvent,
            KeyBindingEvent keybindingEvent,
            boolean isShiftPressed,
            boolean isAltPressed,
            boolean isCtrlPressed) {
        final var editor = this.editor;
        final var editorCursor = editor.getCursor();
        final var editorText = editor.getText();
        final var completionWindow = editor.getComponent(EditorAutoCompletion.class);
        if (editor.isEditable()) {
            var lineSeparator = editor.getLineSeparator().getContent();
            final var editorLanguage = editor.getEditorLanguage();
            if (completionWindow.isShowing() && completionWindow.select()) {
                return true;
            }

            if (isShiftPressed && !isAltPressed && !isCtrlPressed) {
                // Shift + Enter
                return startNewLIne(editor, editorCursor, editorText, editorKeyEvent, keybindingEvent);
            }

            if (isCtrlPressed && !isShiftPressed) {
                if (isAltPressed) {
                    // Ctrl + Alt + Enter
                    var line = editorCursor.left().line;
                    if (line == 0) {
                        editorText.insert(0, 0, lineSeparator);
                        editor.setSelection(0, 0);
                        editor.ensureSelectionVisible();
                        return keybindingEvent.result(true) || editorKeyEvent.result(true);
                    } else {
                        line--;
                        editor.setSelection(line, editorText.getColumnCount(line));
                        return startNewLIne(editor, editorCursor, editorText, editorKeyEvent, keybindingEvent);
                    }
                }

                // Ctrl + Enter
                final var left = editorCursor.left().fromThis();
                editor.commitText(lineSeparator);
                editor.setSelection(left.line, left.column);
                editor.ensureSelectionVisible();
                return keybindingEvent.result(true) || editorKeyEvent.result(true);
            }

            NewlineHandler[] handlers = editorLanguage.getNewlineHandlers();
            if (handlers == null || editorCursor.isSelected()) {
                editor.commitText(lineSeparator, true);
            } else {
                boolean consumed = false;
                for (NewlineHandler handler : handlers) {
                    if (handler != null) {
                        if (handler.matchesRequirement(editorText, editorCursor.left(), editor.getStyles())) {
                            try {
                                var result = handler.handleNewline(editorText, editorCursor.left(), editor.getStyles(), editor.getTabWidth());
                                editor.commitText(result.text, false);
                                int delta = result.shiftLeft;
                                if (delta != 0) {
                                    int newSel = Math.max(editorCursor.getLeft() - delta, 0);
                                    var charPosition = editorCursor.getIndexer().getCharPosition(newSel);
                                    editor.setSelection(charPosition.line, charPosition.column);
                                }
                                consumed = true;
                            } catch (Exception ex) {
                                Log.w(TAG, "Error occurred while calling Language's NewlineHandler", ex);
                            }
                            break;
                        }
                    }
                }
                if (!consumed) {
                    editor.commitText(lineSeparator, true);
                }
            }
            editor.notifyIMEExternalCursorChange();
        }
        return editorKeyEvent.result(true);
    }

    private boolean startNewLIne(CodeEditor editor, Cursor editorCursor, Content
            editorText, EditorKeyEvent e, KeyBindingEvent keybindingEvent) {
        final var line = editorCursor.right().line;
        editor.setSelection(line, editorText.getColumnCount(line));
        editor.commitText(editor.getLineSeparator().getContent());
        editor.ensureSelectionVisible();
        return keybindingEvent.result(true) || e.result(true);
    }

    /**
     * Called by editor in {@link CodeEditor#onKeyUp(int, KeyEvent)}.
     *
     * @param keyCode The key code.
     * @param event   The key event.
     * @return <code>true</code> if the event was handled, <code>false</code> otherwise.
     */
    public boolean onKeyUp(int keyCode, @NonNull KeyEvent event) {
        keyMetaStates.onKeyUp(event);

        final var eventManager = this.editor.eventManager;
        final var cursor = this.editor.getCursor();

        var e = new EditorKeyEvent(this.editor, event, EditorKeyEvent.Type.UP);
        if ((eventManager.dispatchEvent(e) & InterceptTarget.TARGET_EDITOR) != 0) {
            return e.result(false);
        }

        if (isKeyBindingEvent(keyCode, event)) {
            final var keybindingEvent =
                    new KeyBindingEvent(editor,
                            event,
                            EditorKeyEvent.Type.UP,
                            editor.canHandleKeyBinding(keyCode, event.isCtrlPressed(), keyMetaStates.isShiftPressed(), keyMetaStates.isAltPressed()));

            if ((eventManager.dispatchEvent(keybindingEvent) & InterceptTarget.TARGET_EDITOR) != 0) {
                return keybindingEvent.result(false) || e.result(false);
            }
        }

        if (!keyMetaStates.isShiftPressed() && this.editor.selectionAnchor != null && !cursor.isSelected()) {
            this.editor.selectionAnchor = null;
            return e.result(true);
        }
        return e.result(this.editor.onSuperKeyUp(keyCode, event));
    }

    /**
     * Called by editor in {@link CodeEditor#onKeyMultiple(int, int, KeyEvent)}.
     *
     * @param keyCode     The key code.
     * @param repeatCount The repeat count.
     * @param event       The key event.
     * @return <code>true</code> if the event was handled, <code>false</code> otherwise.
     */
    public boolean onKeyMultiple(int keyCode, int repeatCount, @NonNull KeyEvent event) {
        final var e = new EditorKeyEvent(this.editor, event, EditorKeyEvent.Type.MULTIPLE);
        final var eventManager = this.editor.eventManager;
        if ((eventManager.dispatchEvent(e) & InterceptTarget.TARGET_EDITOR) != 0) {
            return e.result(false);
        }

        return e.result(this.editor.onSuperKeyMultiple(keyCode, repeatCount, event));
    }
}
