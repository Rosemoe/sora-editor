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