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
