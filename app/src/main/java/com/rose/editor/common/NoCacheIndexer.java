package com.rose.editor.common;

import com.rose.editor.interfaces.Indexer;

/**
 * Indexer without cache
 * @author Rose
 */
final class NoCacheIndexer extends CachedIndexer implements Indexer{

    /**
     * Create a indexer without cache
     * @param content Target content
     */
    public NoCacheIndexer(Content content) {
        super(content);
        //Disable dynamic indexing
        if(super.getMaxCacheSize() != 0) {
            super.setMaxCacheSize(0);
        }
        if(super.isHandleEvent()) {
            super.setHandleEvent(false);
        }
    }

    @Override
    protected void _throw() {
        //Override this to make super class not throw exception after text changes
    }

}

