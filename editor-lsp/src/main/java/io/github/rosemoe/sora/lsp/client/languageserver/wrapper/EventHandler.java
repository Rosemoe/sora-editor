/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2022  Rosemoe
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 *
 *     Please contact Rosemoe by email 2073412493@qq.com if you need
 *     additional information or have any questions
 */
package io.github.rosemoe.sora.lsp.client.languageserver.wrapper;

import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.jsonrpc.MessageConsumer;
import org.eclipse.lsp4j.jsonrpc.messages.Message;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseMessage;
import org.eclipse.lsp4j.services.LanguageServer;
import org.jetbrains.annotations.NotNull;


import java.util.function.BooleanSupplier;
import java.util.function.Function;

import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.ServerListener;

/**
 * A language server and client event handler.
 */
public class EventHandler implements Function<MessageConsumer, MessageConsumer> {

    private EventListener listener;
    private BooleanSupplier isRunning;
    private LanguageServer languageServer;

    EventHandler(@NotNull EventListener listener, @NotNull BooleanSupplier isRunning) {
        this.listener = listener;
        this.isRunning = isRunning;
    }

    @Override
    public MessageConsumer apply(MessageConsumer messageConsumer) {
        return message -> {
            if (isRunning.getAsBoolean()) {
                handleMessage(message);
                messageConsumer.consume(message);
            }
        };

    }



    private void handleMessage(Message message) {
        if (message instanceof ResponseMessage) {
            ResponseMessage responseMessage = (ResponseMessage) message;
            if (responseMessage.getResult() instanceof InitializeResult) {
                listener.initialize(languageServer, (InitializeResult) responseMessage.getResult());
            }
        }
    }

    void setLanguageServer(@NotNull LanguageServer languageServer) {
        this.languageServer = languageServer;
    }

    public interface EventListener extends ServerListener {
        @Override
        default void initialize(LanguageServer server, InitializeResult result) {
            ServerListener.super.initialize(server, result);
        }


        default void onHandlerException(@NotNull Exception exception) {
        }

        EventListener DEFAULT = new EventListener() {
        };

        default void onShowMessage(MessageParams messageParams) {}

        default void onLogMessage(MessageParams messageParams) {}
    }


}
