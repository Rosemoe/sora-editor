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

