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
package io.github.rosemoe.sora.langs.textmate.registry;

import org.eclipse.jdt.annotation.Nullable;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import io.github.rosemoe.sora.langs.textmate.registry.provider.FileProvider;

public class FileProviderRegistry {


    private final List<FileProvider> allFileProviders = new ArrayList<>();

    private static FileProviderRegistry fileProviderRegistry;

    private FileProviderRegistry() {
        allFileProviders.add(FileProvider.DEFAULT);
    }

    public static synchronized FileProviderRegistry getInstance() {
        if (fileProviderRegistry == null)
            fileProviderRegistry = new FileProviderRegistry();
        return fileProviderRegistry;
    }

    public synchronized void addFileProvider(FileProvider fileProvider) {
        if (fileProvider != FileProvider.DEFAULT) {
            allFileProviders.add(fileProvider);
        }
    }

    public synchronized void removeFileProvider(FileProvider fileProvider) {
        if (fileProvider != FileProvider.DEFAULT) {
            allFileProviders.remove(fileProvider);
        }
    }

    @Nullable
    public InputStream tryGetInputStream(String path) {
        for (var provider : allFileProviders) {
            var stream = provider.provideStream(path);
            if (stream!=null) {
                return stream;
            }
        }
        return null;
    }

    public void dispose() {
        for (var provider : allFileProviders) {
            provider.dispose();
        }

        allFileProviders.clear();
    }

}
