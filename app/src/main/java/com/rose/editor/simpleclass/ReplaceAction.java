/*
 Copyright 2020 Rose2073

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package com.rose.editor.simpleclass;

import com.rose.editor.common.Content;
import com.rose.editor.interfaces.ContentAction;

/**
 * Replace action model for UndoManager
 * @author Rose
 */
public final class ReplaceAction implements ContentAction {

    public InsertAction _insert;
    public DeleteAction _delete;

    @Override
    public void undo(Content content) {
        _insert.undo(content);
        _delete.undo(content);
    }

    @Override
    public void redo(Content content) {
        _delete.redo(content);
        _insert.redo(content);
    }

    @Override
    public boolean canMerge(ContentAction action) {
        return false;
    }

    @Override
    public void merge(ContentAction action) {
        throw new UnsupportedOperationException();
    }

}

