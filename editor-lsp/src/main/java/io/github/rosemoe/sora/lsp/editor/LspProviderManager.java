/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2023  Rosemoe
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
package io.github.rosemoe.sora.lsp.editor;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import io.github.rosemoe.sora.lsp.operations.Provider;

/**
 * Manage the capabilities and attributes of the lsp editor.
 */
public class LspProviderManager {

    private final List<Provider<?, ?>> supportedProviders = new ArrayList<>();

    private final List<Object> options = new ArrayList<>();

    private LspEditor editor;

    protected LspProviderManager(LspEditor currentEditor) {
        editor = currentEditor;
    }

    /**
     * Add a provider
     */
    public void addProvider(Supplier<Provider<?, ?>> providerSupplier) {
        var feature = providerSupplier.get();
        addProvider(feature);
    }

    /**
     * Add a provider
     */
    public void addProvider(Provider<?, ?> feature) {
        supportedProviders.add(feature);
        feature.init(editor);
    }

    /**
     * Add multiple providers
     */
    @SafeVarargs
    public final void addProviders(Supplier<Provider<?, ?>>... providerSupplier) {
        Arrays.stream(providerSupplier).sequential().forEach(this::addProvider);
    }

    /**
     * Remove provider
     */
    public void removeProvider(Class<?> providerClass) {
        for (var feature : supportedProviders) {
            if (feature.getClass() == providerClass) {
                feature.dispose(editor);
                supportedProviders.remove(feature);
                return;
            }
        }
    }

    /**
     * Return a feature instance for the given class
     */
    @Nullable
    public <T extends Provider> T useProvider(Class<T> providerClass) {
        for (var feature : supportedProviders) {
            if (feature.getClass() == providerClass) {
                return (T) feature;
            }
        }
        return null;
    }

    public <T extends Provider> Optional<T> safeUseProvider(Class<T> providerClass) {
        return Optional.ofNullable(useProvider(providerClass));
    }


    /**
     * For language server, some option need to be set, you can get the relevant option and set the values freely by this
     */
    @Nullable
    public <T> T getOption(Class<T> optionClass) {
        for (Object option : options) {
            if (optionClass.isInstance(option)) {
                return (T) option;
            }
        }
        return null;
    }


    /**
     * Dispose Manager
     */
    public void dispose() {
        editor = null;
        supportedProviders.forEach(provider -> provider.dispose(editor));
        supportedProviders.clear();
        options.clear();
    }

    public void addOption(Object object) {
        options.add(object);
    }
}
