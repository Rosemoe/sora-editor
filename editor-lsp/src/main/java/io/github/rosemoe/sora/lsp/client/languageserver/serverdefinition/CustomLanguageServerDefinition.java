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

import org.checkerframework.checker.units.qual.C;

import java.util.HashMap;
import java.util.Map;

import io.github.rosemoe.sora.lsp.client.connection.StreamConnectionProvider;

public class CustomLanguageServerDefinition extends LanguageServerDefinition {


    protected ConnectProvider connectProvider;

    /**
     * Creates new instance with the given language id which is different from the file extension.
     *
     * @param ext             The extension.
     * @param languageIds     The language server ids mapping to extension(s).
     * @param connectProvider The connect provider.
     */
    @SuppressWarnings("WeakerAccess")
    public CustomLanguageServerDefinition(String ext, Map<String, String> languageIds, ConnectProvider connectProvider) {
        this.ext = ext;
        this.languageIds = languageIds;
        this.connectProvider = connectProvider;
    }

    /**
     * Creates new instance.
     *
     * @param ext             The extension.
     * @param connectProvider The connect provider.
     */
    @SuppressWarnings("unused")
    public CustomLanguageServerDefinition(String ext, ConnectProvider connectProvider) {
        this(ext, new HashMap<>(), connectProvider);
    }

    public String toString() {
        return "CustomLanguageServerDefinition : " + String.join(" ", connectProvider.toString());
    }

    @Override
    public StreamConnectionProvider createConnectionProvider(String workingDir) {
        return connectProvider.createConnectionProvider(workingDir);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CustomLanguageServerDefinition) {
            CustomLanguageServerDefinition other = (CustomLanguageServerDefinition) obj;
            return other.connectProvider.equals(connectProvider);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return ext.hashCode() + 3 * connectProvider.hashCode();
    }

    public interface ConnectProvider {
        /**
         * Creates a StreamConnectionProvider given the working directory
         *
         * @param workingDir The root directory
         * @return The stream connection provider
         */
        StreamConnectionProvider createConnectionProvider(String workingDir);
    }
}
