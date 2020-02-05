package com.rose.editor.simpleclass;

/**
 * The class used to save auto complete result items
 * @author Rose
 */
public class ResultItem {

    public static final int TYPE_KEYWORD = 0;
    public static final int TYPE_LOCAL_METHOD = 1;

    public int type;

    public String commit;

    public String label;

    public String desc;

    public int mask = 0;

    public static final int MASK_SHIFT_LEFT_ONCE = 1;
    public static final int MASK_SHIFT_LEFT_TWICE = 1 << 1;

    public ResultItem(String str,String desc){
        type = TYPE_KEYWORD;
        commit = label = str;
        this.desc = desc;
    }

    public ResultItem(String label, String desc, int type) {
        this.label = this.commit = label;
        this.desc = desc;
        this.type = type;
    }

    public ResultItem(String label,String commit, String desc, int type) {
        this.label = label;
        this.commit = commit;
        this.desc = desc;
        this.type = type;
    }

    public ResultItem mask(int m){
        mask = m;
        return this;
    }

}

