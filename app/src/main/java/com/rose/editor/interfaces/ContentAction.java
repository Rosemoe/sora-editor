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
package com.rose.editor.interfaces;

import com.rose.editor.common.Content;

/**
 * For saving modification better
 * @author Rose
 */
public interface ContentAction{

    /**
     * Undo this action
     * @param content On the given object
     */
    void undo(Content content);

    /**
     * Redo this action
     * @param content On the given object
     */
    void redo(Content content);

    /**
     * Get whether the target action can be merged with this action
     * @param action Target action to merge
     * @return Whether can merge
     */
    boolean canMerge(ContentAction action);

    /**
     * Merge with target action
     * @param action Target action to merge
     */
    void merge(ContentAction action);

}
