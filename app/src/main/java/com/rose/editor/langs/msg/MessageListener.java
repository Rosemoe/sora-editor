package com.rose.editor.langs.msg;

/**
 * @author Rose
 *
 */
public interface MessageListener {

    /**
     * Whether the error provider should cancel the error
     * Or it may be with advice
     * @param msg
     * @return Whether cancel
     */
    boolean onHandleCancelableError(Message msg);

    /**
     * You are notified that the given message has been added to list just now
     * @param msg
     */
    void onNewMessage(Message msg);

}
