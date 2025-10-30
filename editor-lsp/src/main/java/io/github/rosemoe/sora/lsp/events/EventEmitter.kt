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

package io.github.rosemoe.sora.lsp.events

import androidx.annotation.WorkerThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class EventEmitter {

    private val listeners = HashMap<String, MutableList<EventListener>>()

    var throwableListener: ((Throwable) -> Unit) = { t -> throw t }

    fun addListener(listener: EventListener): EventDisposable {
        listeners.getOrPut(listener.eventName) { ArrayList() }.add(listener)
        return {
            removeListener(listener)
        }
    }

    fun removeListener(listener: EventListener) {
        listeners[listener.eventName]?.remove(listener)
    }

    fun <T : EventListener> getEventListener(clazz: Class<T>): T? {
        return listeners.flatMap { it.value }
            .find { it::class.java == clazz } as? T?
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

    fun emit(event: String, context: EventContext, throwError: Boolean = false): EventContext {
        try {
            listeners[event]?.forEach {
                it.handle(context)
            }
        } catch (t: Throwable) {
            if (throwError) {
                throw t
            }
            throwableListener.invoke(t)
        }
        return context
    }

    suspend fun emitAsync(event: String, context: EventContext, throwError: Boolean = false): EventContext {
        try {
            listeners[event]?.forEach {
                it.handleAsync(context)
            }
        } catch (t: Throwable) {
            if (throwError) {
                throw t
            }
            throwableListener.invoke(t)
        }
        return context
    }

    @WorkerThread
    fun emitBlocking(event: String, context: EventContext): EventContext = runBlocking {
        emitAsync(event, context)
    }

    fun emit(event: String): EventContext {
        return emit(event, EventContext())
    }

    suspend fun emitAsync(event: String): EventContext {
        return emitAsync(event, EventContext())
    }

    @WorkerThread
    fun emitBlocking(event: String): EventContext = runBlocking {
        emitAsync(event)
    }

    fun emit(event: String, vararg args: Any): EventContext {
        val context = EventContext()
        for (i in args) {
            context.put(i::class.java.name, i)
        }
        return emit(event, context)
    }

    suspend fun emitAsync(event: String, vararg args: Any): EventContext {
        val context = EventContext()
        for (i in args) {
            context.put(i::class.java.name, i)
        }
        return emitAsync(event, context)
    }

    @WorkerThread
    fun emitBlocking(event: String, vararg args: Any): EventContext = runBlocking {
        emitAsync(event, *args)
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

    val isAsync: Boolean
        get() = false

    fun handle(context: EventContext)

    suspend fun handleAsync(context: EventContext) = withContext(Dispatchers.Main) {
        handle(context)
    }

    fun dispose() {}
}

abstract class AsyncEventListener : EventListener {

    override val isAsync = true

    override fun handle(context: EventContext) {
        throw IllegalStateException("This listener is async, please use handleAsync instead")
    }

    override suspend fun handleAsync(context: EventContext) {
        super.handleAsync(context)
    }
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
        return data[key] as? T?
    }

    fun put(key: String, value: Any?) {
        data[key] = value ?: return
    }

    fun put(value: Any?) {
        value ?: return
        data[value::class.java.name] = value
    }

    fun <T : Any> remove(key: String): T? {
        return data.remove(key) as? T?
    }

    fun <T : Any> getByClass(clazz: Class<T>): T? {
        return data.values.filterIsInstance(clazz).firstOrNull()
    }

    operator fun plus(context: EventContext): EventContext {
        data.putAll(context.data)
        return this
    }

    fun clear() {
        data.clear()
    }

    override fun toString(): String {
        return data.toString()
    }
}

inline fun <reified T : Any> EventContext.getByClass(): T? {
    return getByClass(T::class.java)
}

inline fun <reified T : Any> EventContext.get(): T {
    return get(T::class.java.name)
}

inline fun <reified T : EventListener> EventEmitter.getEventListener(): T? {
    return getEventListener(T::class.java) as T?
}

object EventType

typealias EventDisposable = () -> Unit
