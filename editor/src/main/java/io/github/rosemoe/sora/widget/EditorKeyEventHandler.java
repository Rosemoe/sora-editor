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

import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.NonNull;

import java.util.Objects;

import io.github.rosemoe.sora.event.EditorKeyEvent;
import io.github.rosemoe.sora.event.InterceptTarget;
import io.github.rosemoe.sora.event.KeyBindingEvent;
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandleResult;
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.text.ContentLine;
import io.github.rosemoe.sora.text.Cursor;
import io.github.rosemoe.sora.text.method.KeyMetaStates;
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
class EditorKeyEventHandler {

    private static final String TAG = "EditorKeyEventHandler";
    private final CodeEditor editor;
    private final KeyMetaStates mKeyMetaStates;

    EditorKeyEventHandler(CodeEditor editor) {
        Objects.requireNonNull(editor, "Cannot setup KeyEvent with null editor instance.");
        this.editor = editor;
        this.mKeyMetaStates = new KeyMetaStates(editor);
    }

    /**
     * Check if the given {@link KeyEvent} is a key binding event.
     * {@link EditorKeyEventHandler#mKeyMetaStates} must be notified about the key event before this
     * method is called.
     *
     * @param keyCode The keycode.
     * @param event   The key event.
     * @return <code>true</code> if the event is a key binding event. <code>false</code> otherwise.
     */
    private boolean isKeyBindingEvent(int keyCode, KeyEvent event) {
        return (mKeyMetaStates.isShiftPressed()
                || mKeyMetaStates.isAltPressed()
                || event.isCtrlPressed())
                && ((keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z) || keyCode == KeyEvent.KEYCODE_ENTER);
    }

    /**
     * Get the {@link KeyMetaStates} instance.
     *
     * @return The {@link KeyMetaStates} instance.
     */
    @NonNull
    public KeyMetaStates getKeyMetaStates() {
        return mKeyMetaStates;
    }

    /**
     * Called by editor in {@link CodeEditor#onKeyDown(int, KeyEvent)}.
     *
     * @param keyCode The key code.
     * @param event   The key event.
     * @return <code>true</code> if the event was handled, <code>false</code> otherwise.
     */
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        mKeyMetaStates.onKeyDown(event);
        final var editor = this.editor;
        final var eventManager = editor.mEventManager;
        final var connection = editor.mConnection;
        final var editorCursor = editor.getCursor();
        final var editorText = editor.getText();
        final var completionWindow = editor.getComponent(EditorAutoCompletion.class);

        final var e = new EditorKeyEvent(editor, event, EditorKeyEvent.Type.DOWN);
        final var keybindingEvent =
                new KeyBindingEvent(editor,
                        event,
                        EditorKeyEvent.Type.DOWN,
                        keyCode,
                        editor.canHandleKeyBinding(keyCode, event.isCtrlPressed(), mKeyMetaStates.isShiftPressed(), mKeyMetaStates.isAltPressed()));
        if ((eventManager.dispatchEvent(e) & InterceptTarget.TARGET_EDITOR) != 0) {
            return e.result(false);
        }

        final var isShiftPressed = mKeyMetaStates.isShiftPressed();
        final var isAltPressed = mKeyMetaStates.isAltPressed();
        final var isCtrlPressed = event.isCtrlPressed();

        // Currently, KeyBindingEvent is triggered only for (Shift | Ctrl | Alt) + alphabet keys
        // Should we add support for more keys?
        if (isKeyBindingEvent(keyCode, event)) {
            if ((eventManager.dispatchEvent(keybindingEvent) & InterceptTarget.TARGET_EDITOR) != 0) {
                return keybindingEvent.result(false) || e.result(false);
            }
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_MOVE_HOME:
            case KeyEvent.KEYCODE_MOVE_END:
                if (isShiftPressed && (!editorCursor.isSelected())) {
                    editor.mSelectionAnchor = editorCursor.left();
                } else if (!isShiftPressed && editor.mSelectionAnchor != null) {
                    editor.mSelectionAnchor = null;
                }
                mKeyMetaStates.adjust();
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK: {
                if (editorCursor.isSelected()) {
                    editor.setSelection(editorCursor.getLeftLine(), editorCursor.getLeftColumn());
                    return e.result(true);
                }
                return e.result(false);
            }
            case KeyEvent.KEYCODE_DEL:
                if (editor.isEditable()) {
                    editor.deleteText();
                    editor.notifyIMEExternalCursorChange();
                }
                return e.result(true);
            case KeyEvent.KEYCODE_FORWARD_DEL: {
                if (editor.isEditable()) {
                    connection.deleteSurroundingText(0, 1);
                    editor.notifyIMEExternalCursorChange();
                }
                return e.result(true);
            }
            case KeyEvent.KEYCODE_ENTER: {
                if (editor.isEditable()) {
                    final var editorLanguage = editor.getEditorLanguage();
                    if (completionWindow.isShowing()) {
                        completionWindow.select();
                        return true;
                    }

                    if (isShiftPressed && !isAltPressed && !isCtrlPressed) {
                        // Shift + Enter
                        return startNewLIne(editor, editorCursor, editorText, e, keybindingEvent);
                    }

                    if (isCtrlPressed && !isShiftPressed) {
                        if (isAltPressed) {
                            // Ctrl + Alt + Enter
                            var line = editorCursor.left().line;
                            if (line == 0) {
                                editorText.insert(0, 0, "\n");
                                editor.setSelection(0, 0);
                                editor.ensureSelectionVisible();
                                return keybindingEvent.result(true) || e.result(true);
                            } else {
                                line--;
                                editor.setSelection(line, editorText.getColumnCount(line));
                                return startNewLIne(editor, editorCursor, editorText, e, keybindingEvent);
                            }
                        }

                        // Ctrl + Enter
                        final var left = editorCursor.left().fromThis();
                        editor.commitText("\n");
                        editor.setSelection(left.line, left.column);
                        editor.ensureSelectionVisible();
                        return keybindingEvent.result(true) || e.result(true);
                    }

                    NewlineHandler[] handlers = editorLanguage.getNewlineHandlers();
                    if (handlers == null || editorCursor.isSelected()) {
                        editor.commitText("\n", true);
                    } else {
                        ContentLine line = editorText.getLine(editorCursor.getLeftLine());
                        int index = editorCursor.getLeftColumn();
                        String beforeText = line.subSequence(0, index).toString();
                        String afterText = line.subSequence(index, line.length()).toString();
                        boolean consumed = false;
                        for (NewlineHandler handler : handlers) {
                            if (handler != null) {
                                if (handler.matchesRequirement(beforeText, afterText)) {
                                    try {
                                        NewlineHandleResult result = handler.handleNewline(beforeText, afterText, editor.getTabWidth());
                                        if (result != null) {
                                            editor.commitText(result.text, false);
                                            int delta = result.shiftLeft;
                                            if (delta != 0) {
                                                int newSel = Math.max(editorCursor.getLeft() - delta, 0);
                                                CharPosition charPosition = editorCursor.getIndexer().getCharPosition(newSel);
                                                editor.setSelection(charPosition.line, charPosition.column);
                                            }
                                            consumed = true;
                                        } else {
                                            continue;
                                        }
                                    } catch (Exception ex) {
                                        Log.w(TAG, "Error occurred while calling Language's NewlineHandler", ex);
                                    }
                                    break;
                                }
                            }
                        }
                        if (!consumed) {
                            editor.commitText("\n", true);
                        }
                    }
                    editor.notifyIMEExternalCursorChange();
                }
                return e.result(true);
            }
            case KeyEvent.KEYCODE_DPAD_DOWN:
                editor.moveSelectionDown();
                return e.result(true);
            case KeyEvent.KEYCODE_DPAD_UP:
                editor.moveSelectionUp();
                return e.result(true);
            case KeyEvent.KEYCODE_DPAD_LEFT:
                editor.moveSelectionLeft();
                return e.result(true);
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                editor.moveSelectionRight();
                return e.result(true);
            case KeyEvent.KEYCODE_MOVE_END:
                editor.moveSelectionEnd();
                return e.result(true);
            case KeyEvent.KEYCODE_MOVE_HOME:
                editor.moveSelectionHome();
                return e.result(true);
            case KeyEvent.KEYCODE_PAGE_DOWN:
                editor.movePageDown();
                return e.result(true);
            case KeyEvent.KEYCODE_PAGE_UP:
                editor.movePageUp();
                return e.result(true);
            case KeyEvent.KEYCODE_TAB:
                if (editor.isEditable()) {
                    if (completionWindow.isShowing()) {
                        completionWindow.select();
                    } else {
                        editor.commitTab();
                    }
                }
                return e.result(true);
            case KeyEvent.KEYCODE_PASTE:
                if (editor.isEditable()) {
                    editor.pasteText();
                }
                return e.result(true);
            case KeyEvent.KEYCODE_COPY:
                editor.copyText();
                return e.result(true);
            case KeyEvent.KEYCODE_SPACE:
                if (editor.isEditable()) {
                    editor.commitText(" ");
                    editor.notifyIMEExternalCursorChange();
                }
                return e.result(true);
            case KeyEvent.KEYCODE_ESCAPE:
                if (editorCursor.isSelected()) {
                    final var newPosition = editor.getProps().positionOfCursorWhenExitSelecting ? editorCursor.right() : editorCursor.left();
                    editor.setSelection(newPosition.line, newPosition.column, true);
                }
                return e.result(true);
            default:
                if (event.isCtrlPressed() && !event.isAltPressed()) {
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
                } else if (!event.isCtrlPressed() && !event.isAltPressed()) {
                    if (event.isPrintingKey() && editor.isEditable()) {
                        String text = new String(Character.toChars(event.getUnicodeChar(event.getMetaState())));
                        SymbolPairMatch.Replacement replacement = null;
                        if (text.length() == 1 && editor.getProps().symbolPairAutoCompletion) {
                            replacement = editor.mLanguageSymbolPairs.getCompletion(text.charAt(0));
                        }
                        if (replacement == null || replacement == SymbolPairMatch.Replacement.NO_REPLACEMENT
                                || (replacement.shouldNotDoReplace(editorText) && replacement.notHasAutoSurroundPair())) {
                            editor.commitText(text);
                            editor.notifyIMEExternalCursorChange();
                        } else {
                            String[] autoSurroundPair;
                            if (editorCursor.isSelected() && (autoSurroundPair = replacement.getAutoSurroundPair()) != null) {
                                editorText.beginBatchEdit();
                                //insert left
                                editorText.insert(editorCursor.getLeftLine(), editorCursor.getLeftColumn(), autoSurroundPair[0]);
                                //insert right
                                editorText.insert(editorCursor.getRightLine(), editorCursor.getRightColumn(), autoSurroundPair[1]);
                                editorText.endBatchEdit();
                                //cancel selected
                                editor.setSelection(editorCursor.getLeftLine(), editorCursor.getLeftColumn() + autoSurroundPair[0].length() - 1);

                                editor.notifyIMEExternalCursorChange();
                            } else {
                                editor.commitText(replacement.text);
                                int delta = (replacement.text.length() - replacement.selection);
                                if (delta != 0) {
                                    int newSel = Math.max(editorCursor.getLeft() - delta, 0);
                                    CharPosition charPosition = editorCursor.getIndexer().getCharPosition(newSel);
                                    editor.setSelection(charPosition.line, charPosition.column);
                                    editor.notifyIMEExternalCursorChange();
                                }
                            }

                        }
                    } else {
                        return editor.onSuperKeyDown(keyCode, event);
                    }
                    return e.result(true);
                }
        }
        return e.result(editor.onSuperKeyDown(keyCode, event));
    }

    private boolean startNewLIne(CodeEditor editor, Cursor editorCursor, Content editorText, EditorKeyEvent e, KeyBindingEvent keybindingEvent) {
        final var line = editorCursor.right().line;
        editor.setSelection(line, editorText.getColumnCount(line));
        editor.commitText("\n");
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
        mKeyMetaStates.onKeyUp(event);

        final var eventManager = this.editor.mEventManager;
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
                            keyCode,
                            editor.canHandleKeyBinding(keyCode, event.isCtrlPressed(), mKeyMetaStates.isShiftPressed(), mKeyMetaStates.isAltPressed()));

            if ((eventManager.dispatchEvent(keybindingEvent) & InterceptTarget.TARGET_EDITOR) != 0) {
                return keybindingEvent.result(false) || e.result(false);
            }
        }

        if (!mKeyMetaStates.isShiftPressed() && this.editor.mSelectionAnchor != null && !cursor.isSelected()) {
            this.editor.mSelectionAnchor = null;
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
        final var eventManager = this.editor.mEventManager;
        if ((eventManager.dispatchEvent(e) & InterceptTarget.TARGET_EDITOR) != 0) {
            return e.result(false);
        }

        return e.result(this.editor.onSuperKeyMultiple(keyCode, repeatCount, event));
    }
}
