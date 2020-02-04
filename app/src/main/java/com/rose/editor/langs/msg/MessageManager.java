package com.rose.editor.langs.msg;


import java.util.ArrayList;
import java.util.List;

/**
 * @author Rose
 * 信息管理器
 */
public class MessageManager {

    private List<Message> msgs;

    private int[] counts;

    private MessageListener listener;

    public MessageManager() {
        msgs = new ArrayList<Message>();
        counts = new int[Math.max(Message.LEVEL_ERROR,Math.max(Message.LEVEL_NORMAL,Message.LEVEL_WARNING)) + 1];
    }

    public void addMessage(Message msg) {
        if(!msgs.contains(msg)) {
            msgs.add(msg);
            counts[msg.getMsgLevel()] ++;
            if(listener != null) {
                listener.onNewMessage(msg);
            }
        }
    }

    public boolean handleCancelableError(Message msg,boolean add) {
        if(msg.getMsgLevel() != Message.LEVEL_ERROR) {
            throw new IllegalArgumentException("Not a error");
        }
        if(add) {
            addMessage(msg);
        }
        return (listener != null && listener.onHandleCancelableError(msg));
    }

    public void setListener(MessageListener listener) {
        this.listener = listener;
    }

    public MessageListener getListener() {
        return listener;
    }

    public int getNormalCount() {
        return counts[Message.LEVEL_NORMAL];
    }

    public int getWarningCount() {
        return counts[Message.LEVEL_WARNING];
    }

    public int getErrorCount() {
        return counts[Message.LEVEL_ERROR];
    }

    public Message getMessageAt(int i) {
        return msgs.get(i);
    }

    public int getCount() {
        return msgs.size();
    }

}

