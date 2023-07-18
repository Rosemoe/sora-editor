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

package io.github.rosemoe.sora.lsp2.events

class EventEmitter {

    private val listeners = HashMap<String, MutableList<EventListener>>()

    fun addListener(listener: EventListener) {
        listeners.getOrPut(listener.eventName) { ArrayList() }.add(listener)
    }

    fun removeListener(listener: EventListener) {
        listeners[listener.eventName]?.remove(listener)
    }

    fun <T : EventListener> removeListener(listenerClass: Class<T>) {
        listeners.values.forEach { eventListeners ->
            val iterator = eventListeners.iterator()
            iterator.forEach {
                if (it::class.java == listenerClass) {
                    iterator.remove()
                }
            }
        }
    }

    suspend fun emit(event: String, context: EventContext): EventContext {
        listeners[event]?.forEach {
            it.handle(context)
        }
        return context
    }

    suspend fun emit(event: String): EventContext {
        return emit(event, EventContext())
    }

    suspend fun emit(event: String, vararg args: Any): EventContext {
        val context = EventContext()
        for (i in args) {
            context.put(i::class.java.name, i)
        }
        return emit(event, context)
    }

    fun clear(dispose: Boolean = false) {
        if (dispose) {
            listeners.values.flatten().forEach { eventListeners ->
                eventListeners.dispose()
            }
        }
        listeners.clear()

    }
}


interface EventListener {

    val eventName: String

    suspend fun handle(context: EventContext): EventContext

    fun dispose() {}
}

class EventContext {
    private val data = HashMap<String, Any>()

    fun <T : Any> get(key: String): T {
        return data[key] as T
    }

    fun <T : Any> getOrDefault(key: String, default: T): T {
        return data.getOrDefault(key, default) as T
    }

    fun <T : Any> getOrNull(key: String): T? {
        return data[key] as T?
    }

    fun put(key: String, value: Any) {
        data[key] = value
    }

    fun put(value: Any) {
        data[value::class.java.name] = value
    }

    fun <T : Any> remove(key: String): T? {
        return data.remove(key) as T?
    }

    fun <T : Any> getByClass(clazz: Class<T>): T? {
        for (value in data.values) {
            if (clazz.isInstance(value)) {
                return value as T
            }
        }
        return null
    }

    fun clear() {
        data.clear()
    }

}

inline fun <reified T : Any> EventContext.getByClass(): T? {
   return getByClass(T::class.java)
}

object EventType {}
