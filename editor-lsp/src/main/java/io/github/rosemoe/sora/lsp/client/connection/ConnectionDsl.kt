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

package io.github.rosemoe.sora.lsp.client.connection

import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.CustomLanguageServerDefinition

/**
 * Marker annotation for the connection DSL to avoid nested builder bleed-through.
 */
@DslMarker
annotation class ConnectionDslMarker

/**
 * Holds the factory used to create [StreamConnectionProvider] instances for a working directory.
 */
class ConnectionDefinition internal constructor(
    private val factory: (workingDir: String) -> StreamConnectionProvider
) {

    /**
     * Creates a new [StreamConnectionProvider] for the provided directory.
     */
    fun create(workingDir: String): StreamConnectionProvider = factory(workingDir)

    /**
     * Wraps this definition as a [CustomLanguageServerDefinition.ServerConnectProvider].
     */
    fun asServerConnectProvider(): CustomLanguageServerDefinition.ServerConnectProvider {
        return CustomLanguageServerDefinition.ServerConnectProvider(factory)
    }
}

/**
 * Entry point for the connection DSL. Use this to describe how to connect to a language server.
 */
fun connectionDefinition(block: ConnectionDefinitionDsl.() -> Unit): ConnectionDefinition {
    return ConnectionDefinitionDsl().apply(block).build()
}

/**
 * DSL for configuring connection providers.
 */
@ConnectionDslMarker
class ConnectionDefinitionDsl {
    private var factory: ((String) -> StreamConnectionProvider)? = null

    /**
     * Builds a [SocketStreamConnectionProvider] that connects to the given host and port.
     */
    fun socket(port: Int, host: String? = null) {
        factory = { SocketStreamConnectionProvider(port, host) }
    }

    /**
     * Builds a [LocalSocketStreamConnectionProvider] targeting the provided abstract socket name.
     */
    fun local(name: String) {
        factory = { LocalSocketStreamConnectionProvider(name) }
    }

    /**
     * Wraps a custom [CustomConnectProvider.StreamProvider] that does not depend on the working directory.
     */
    fun custom(streamProvider: CustomConnectProvider.StreamProvider) {
        factory = { CustomConnectProvider(streamProvider) }
    }

    /**
     * Wraps a custom provider factory that can take the working directory into account.
     */
    fun custom(streamProviderFactory: (workingDir: String) -> CustomConnectProvider.StreamProvider) {
        factory = { workingDir -> CustomConnectProvider(streamProviderFactory(workingDir)) }
    }

    /**
     * Provides a raw factory for instances that do not fit the helpers above.
     */
    fun provider(factory: (workingDir: String) -> StreamConnectionProvider) {
        this.factory = factory
    }

    internal fun build(): ConnectionDefinition {
        val actualFactory = factory
        require(actualFactory != null) { "Connection type must be specified in the DSL" }
        return ConnectionDefinition(actualFactory)
    }
}
