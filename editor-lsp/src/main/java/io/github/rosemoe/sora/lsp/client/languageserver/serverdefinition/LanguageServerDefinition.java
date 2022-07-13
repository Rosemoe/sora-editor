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
package io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition;

import android.util.Log;
import android.util.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collections;
import java.util.EventListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.rosemoe.sora.lsp.client.connection.StreamConnectionProvider;
import io.github.rosemoe.sora.lsp.client.languageserver.wrapper.EventHandler;

/*
 * A trait representing a ServerDefinition
 */
public class LanguageServerDefinition {

    public String ext;
    protected Map<String, String> languageIds = Collections.emptyMap();
    private Map<String, StreamConnectionProvider> streamConnectionProviders = new ConcurrentHashMap<>();
    public static final String SPLIT_CHAR = ",";

    /**
     * Starts a Language server for the given directory and returns a tuple (InputStream, OutputStream)
     *
     * @param workingDir The root directory
     * @return The input and output streams of the server
     * @throws IOException if the stream connection provider is crashed
     */
    public Pair<InputStream, OutputStream> start(String workingDir) throws IOException {
        StreamConnectionProvider streamConnectionProvider = streamConnectionProviders.get(workingDir);
        if (streamConnectionProvider != null) {
            return new Pair<>(streamConnectionProvider.getInputStream(), streamConnectionProvider.getOutputStream());
        } else {
            streamConnectionProvider = createConnectionProvider(workingDir);
            streamConnectionProvider.start();
            streamConnectionProviders.put(workingDir, streamConnectionProvider);
            return new Pair<>(streamConnectionProvider.getInputStream(), streamConnectionProvider.getOutputStream());
        }
    }



    public boolean callExitForLanguageServer() { return false; }

    /**
     * Stops the Language server corresponding to the given working directory
     *
     * @param workingDir The root directory
     */
    public void stop(String workingDir) {
        StreamConnectionProvider streamConnectionProvider = streamConnectionProviders.get(workingDir);
        if (streamConnectionProvider != null) {
            streamConnectionProvider.close();
            streamConnectionProviders.remove(workingDir);
        } else {
            Log.w("LanguageServerDefinition", "No connection for workingDir " + workingDir + " and ext " + ext);
        }
    }

    public Object getInitializationOptions(URI uri) {
        return null;
    }

    @Override
    public String toString() {
        return "ServerDefinition for " + ext;
    }

    /**
     * Creates a StreamConnectionProvider given the working directory
     *
     * @param workingDir The root directory
     * @return The stream connection provider
     */
    public StreamConnectionProvider createConnectionProvider(String workingDir) {
        throw new UnsupportedOperationException();
    }

    public EventHandler.EventListener getEventListener() {
        return EventHandler.EventListener.DEFAULT;
    }

    /**
     * Return language id for the given extension. if there is no langauge ids registered then the
     * return value will be the value of <code>extension</code>.
     */
    public String languageIdFor(String extension) {
        return languageIds.getOrDefault(extension, extension);
    }
}