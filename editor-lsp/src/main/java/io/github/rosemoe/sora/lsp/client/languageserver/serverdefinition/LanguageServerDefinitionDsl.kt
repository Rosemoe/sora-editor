/*******************************************************************************
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2024  Rosemoe
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
 ******************************************************************************/

package io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition

import io.github.rosemoe.sora.lsp.client.connection.ConnectionDefinitionDsl
import io.github.rosemoe.sora.lsp.client.connection.connectionDefinition
import io.github.rosemoe.sora.lsp.client.languageserver.LspFeature
import io.github.rosemoe.sora.lsp.client.languageserver.wrapper.EventHandler
import org.eclipse.lsp4j.ServerCapabilities
import java.net.URI

/**
 * Marker annotation used to scope the language server definition DSL.
 */
@DslMarker
annotation class LanguageServerDefinitionDslMarker

/**
 * Entry point for the language server definition DSL. Use this to register servers from code.
 */
inline fun languageServerDefinition(block: LanguageServerDefinitionDsl.() -> Unit): LanguageServerDefinition {
    return LanguageServerDefinitionDsl().apply(block).build()
}

/**
 * Entry point for the language server definition DSL. Use this to register servers from code.
 */
inline fun languageServerDefinition(
    ext: String,
    block: LanguageServerDefinitionDsl.() -> Unit
): LanguageServerDefinition {
    return LanguageServerDefinitionDsl().apply {
        ext(ext)
        block()
    }.build()
}

/**
 * DSL for configuring a [LanguageServerDefinition].
 */
@LanguageServerDefinitionDslMarker
class LanguageServerDefinitionDsl {
    private var extension: String? = null
    private var extensions: MutableList<String>? = null
    private var serverName: String? = null
    private var expectedCapabilities: ServerCapabilities? = null
    private var connectProvider: CustomLanguageServerDefinition.ServerConnectProvider? = null
    private var initializationOptionsProvider: ((URI?) -> Any?)? = null
    private var eventListenerOverride: EventHandler.EventListener? = null
    private var disabledFeatures: MutableSet<LspFeature> = mutableSetOf()

    /**
     * Sets the extension under which the server should be registered.
     */
    fun extension(value: String) {
        extension = value
    }

    /**
     * Alias for [extension].
     */
    fun ext(value: String) = extension(value)

    /**
     * Sets multiple file extensions for the server. Includes the primary extension.
     */
    fun extensions(vararg values: String) {
        if (extensions == null) {
            extensions = mutableListOf()
        }
        extensions!!.addAll(values)
    }

    /**
     * Alias for [extensions].
     */
    fun exts(vararg values: String) = extensions(*values)

    /**
     * Overrides the user-facing name of the server definition.
     */
    fun name(value: String) {
        serverName = value
    }

    /**
     * Provides the expected capabilities directly.
     */
    fun expectedCapabilities(capabilities: ServerCapabilities) {
        expectedCapabilities = capabilities
    }

    fun initOptions(provider: (URI?) -> Any?) {
        initializationOptionsProvider = provider
    }

    fun eventListener(listener: EventHandler.EventListener) {
        eventListenerOverride = listener
    }

    fun disabledFeatures(vararg features: LspFeature) {
        disabledFeatures.addAll(features)
    }

    /**
     * Supplies the connection configuration using the connection DSL.
     */
    fun connection(block: ConnectionDefinitionDsl.() -> Unit) {
        connectProvider = connectionDefinition(block).asServerConnectProvider()
    }

    /**
     * Provides a raw [CustomLanguageServerDefinition.ServerConnectProvider] when more control is required.
     */
    fun connect(provider: CustomLanguageServerDefinition.ServerConnectProvider) {
        connectProvider = provider
    }

    fun build(): LanguageServerDefinition {
        val actualExtension = extension
            ?: throw IllegalStateException("Extension must be provided when building a server definition")
        val connection = connectProvider
            ?: throw IllegalStateException("Connection must be provided when building a server definition")

        val allExtensions = if (extensions != null) {
            mutableListOf(actualExtension).apply { addAll(extensions!!) }
        } else null

        return object : CustomLanguageServerDefinition(
            actualExtension,
            connection,
            name = serverName ?: actualExtension,
            expectedCapabilitiesOverride = expectedCapabilities,
            extensionsOverride = allExtensions
        ) {
            override val disabledFeatures: Set<LspFeature>
                get() = this@LanguageServerDefinitionDsl.disabledFeatures

            override fun getInitializationOptions(uri: URI?): Any? {
                return initializationOptionsProvider?.invoke(uri) ?: super.getInitializationOptions(
                    uri
                )
            }

            override val eventListener: EventHandler.EventListener
                get() = eventListenerOverride ?: super.eventListener
        }
    }
}
