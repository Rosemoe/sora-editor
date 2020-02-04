package com.rose.editor.langs.msg;

/**
 * @author Rose
 *
 */
public class StringAdvice implements Advice {

    private String adv;

    public StringAdvice(String advice) {
        this.adv = advice;
    }

    public String getAdvice() {
        return adv;
    }

    public void setAdvice(String adv) {
        this.adv = adv;
    }

    @Override
    public String toString() {
        return "StringAdvice {" + adv + "}";
    }

}
