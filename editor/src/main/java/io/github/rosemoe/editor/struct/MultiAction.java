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
package io.github.rosemoe.editor.struct;


import java.util.ArrayList;
import java.util.List;

import io.github.rosemoe.editor.text.Content;
import io.github.rosemoe.editor.text.ContentAction;

/**
 * MultiAction saves several actions for UndoManager
 * @author Rose
 */
public final class MultiAction implements ContentAction {

    private final List<ContentAction> _actions = new ArrayList<>();

    public void addAction(ContentAction action) {
        if(_actions.isEmpty()) {
            _actions.add(action);
        }else {
            ContentAction last = _actions.get(_actions.size() - 1);
            if(last.canMerge(action)) {
                last.merge(action);
            }else {
                _actions.add(action);
            }
        }
    }

    @Override
    public void undo(Content content) {
        for(int i = _actions.size() - 1;i >= 0;i--) {
            _actions.get(i).undo(content);
        }
    }

    @Override
    public void redo(Content content) {
        for(int i = 0;i < _actions.size();i++) {
            _actions.get(i).redo(content);
        }
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
