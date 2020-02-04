package com.rose.editor.langs.msg;


/**
 * @author Rose
 * 信息
 * 等级：平常，警告，错误
 */
public class Message {

    public final static int LEVEL_NORMAL = 0;

    public final static int LEVEL_WARNING = 1;

    public final static int LEVEL_ERROR = 2;

    private int level;

    private String msg;

    private Advice advice;

    private Throwable cause;

    public Message(int level) {
        this(level,null);
    }

    public Message(int level,String msg) {
        this(level,msg,null);
    }

    public Message(int level,String msg,Advice advice) {
        setLevel(level);
        setMsgContent(msg);
        setAdvice(advice);
    }

    public void setCause(Throwable cause) {
        this.cause = cause;
    }

    public Throwable getCause() {
        return cause;
    }

    public void setMsgContent(String msg) {
        this.msg = msg;
    }

    public void setLevel(int level) {
        if(level < LEVEL_NORMAL || level > LEVEL_ERROR) {
            throw new IllegalArgumentException("Not a valid level");
        }
        this.level = level;
    }

    public void setAdvice(Advice advice) {
        this.advice = advice;
    }

    public int getMsgLevel() {
        return this.level;
    }

    public String getMsgContent() {
        return this.msg;
    }

    public Advice getAdvice() {
        return this.advice;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " { Level = " + getMsgLevel()
                + " , Content = " + getMsgContent()
                + " , Advice = " + (getAdvice() == null ? "null" : getAdvice().toString())
                + " }";
    }

}