/*
 *   Copyright 2020 Rosemoe
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

import java.util.Comparator;

/**
 * The class used to save auto complete result items
 *
 * @author Rose
 */
@SuppressWarnings("CanBeFinal")
public class ResultItem {

    public final static Comparator<ResultItem> COMPARATOR_BY_NAME = (p1, p2) -> p1.label.compareTo(p2.label);

    public static final int TYPE_KEYWORD = 0;
    public static final int TYPE_LOCAL_METHOD = 1;

    public int type;

    public String commit;

    public String label;

    public String desc;

    public int mask = 0;

    public static final int MASK_SHIFT_LEFT_ONCE = 1;
    public static final int MASK_SHIFT_LEFT_TWICE = 1 << 1;

    public ResultItem(String str, String desc) {
        type = TYPE_KEYWORD;
        commit = label = str;
        this.desc = desc;
    }

    public ResultItem(String label, String desc, int type) {
        this.label = this.commit = label;
        this.desc = desc;
        this.type = type;
    }

    public ResultItem(String label, String commit, String desc, int type) {
        this.label = label;
        this.commit = commit;
        this.desc = desc;
        this.type = type;
    }

    public ResultItem mask(int m) {
        mask = m;
        return this;
    }

}

