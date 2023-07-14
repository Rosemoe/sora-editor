/*******************************************************************************
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
 ******************************************************************************/

package io.github.rosemoe.sora.lsp2.editor

import io.github.rosemoe.sora.lsp2.events.EventContext
import io.github.rosemoe.sora.lsp2.events.EventListener
import java.util.function.Supplier


class LspEventManager(
    private val project: LspProject,
    private val editor: LspEditor
) {

    private val eventEmitter = project.eventEmitter
    private val options = arrayListOf<Any>()

    fun addEventListener(eventListenerSupplier: Supplier<EventListener>) {
        val eventListener = eventListenerSupplier.get()
        addEventListener(eventListener)
    }

    /**
     * Add a provider
     */
    fun addEventListener(eventListener: EventListener) {
        eventEmitter.addListener(eventListener)
    }

    /**
     * Add multiple providers
     */
    @SafeVarargs
    fun addEventListeners(vararg eventListenerSupplier: Supplier<EventListener>) {
        eventListenerSupplier.forEach { addEventListener(it) }
    }

    /**
     * Remove provider
     */
    fun <T : EventListener> removeEventListener(eventClass: Class<T>) {
        eventEmitter.removeListener(eventClass)
    }

    fun emit(eventName: String, vararg args: Any) {
        val eventContext = EventContext()

        eventContext.put("lsp-editor", editor)
        eventContext.put("lsp-project", project)

        for (i in args.indices) {
            eventContext.put("arg$i", args[i])
        }

        eventEmitter.emit(eventName, eventContext)
    }

    /**
     * For language server, some option need to be set, you can get the relevant option and set the values freely by this
     */
    fun <T> getOption(optionClass: Class<T>): T? {
        for (option in options) {
            if (optionClass.isInstance(option)) {
                return option as T
            }
        }
        return null
    }


    fun addOption(option: Any) {
        options.add(option)
    }

    fun removeOption(option: Any) {
        options.remove(option)
    }
}