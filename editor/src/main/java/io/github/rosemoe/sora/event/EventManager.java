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
package io.github.rosemoe.sora.event;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This class manages event dispatching in editor.
 * Users can either register their event receivers here or dispatch event to
 * the receivers in this manager.
 * <p>
 * There may be several EventManagers in one editor instance. For example, each plugin
 * will have it own EventManager and the editor also has a root event manager for external
 * listeners.
 * <p>
 * Note that the event type must be exact. That's to say, you need to use a terminal class instead
 * of using its parent classes. For instance, if you register a receiver with the event type {@link Event},
 * no event will be sent to your receiver.
 *
 * @author Rosemoe
 */
public final class EventManager {

    @SuppressWarnings("rawtypes")
    private final Map<Class<?>, Receivers> receivers;
    private final ReadWriteLock lock;
    private final EventManager parent;
    private final List<EventManager> children;
    private final EventReceiver<?>[][] caches = new EventReceiver[5][];
    private boolean enabled = true;
    private boolean detached = false;

    /**
     * Create an EventManager with no parent
     */
    public EventManager() {
        this(null);
    }

    /**
     * Create an EventManager with the given parent.
     * Null for no parent.
     */
    public EventManager(@Nullable EventManager parent) {
        receivers = new HashMap<>();
        this.parent = parent;
        lock = new ReentrantReadWriteLock();
        children = new Vector<>();
        if (parent != null) {
            parent.children.add(this);
        }
    }

    /**
     * Check is the manager enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Set enabled.
     * Disabled EventManager will not deliver event to its subscribers or children.
     * Root EventManager can not be disabled.
     */
    public void setEnabled(boolean enabled) {
        if (parent == null && !enabled) {
            throw new IllegalStateException("The event manager is set to be root, and can not be disabled");
        }
        this.enabled = enabled;
    }

    /**
     * Get root node
     */
    @NonNull
    public EventManager getRootManager() {
        checkDetached();
        return parent == null ? this : parent.getRootManager();
    }

    /**
     * Get root manager and dispatch the given event
     *
     * @see #dispatchEvent(Event)
     */
    public <T extends Event> int dispatchEventFromRoot(@NonNull T event) {
        return getRootManager().dispatchEvent(event);
    }

    /**
     * Detached from parent.
     * This manager will not receive future events from parent
     */
    public void detach() {
        if (parent == null) {
            throw new IllegalStateException("root manager can not be detached");
        }
        checkDetached();
        detached = true;
        parent.children.remove(this);
    }

    private void checkDetached() {
        if (detached) {
            throw new IllegalStateException("already detached");
        }
    }

    /**
     * Get receivers container of a given event type safely
     */
    @NonNull
    @SuppressWarnings("unchecked")
    <T extends Event> Receivers<T> getReceivers(@NonNull Class<T> type) {
        lock.readLock().lock();
        Receivers<T> result;
        try {
            result = receivers.get(type);
        } finally {
            lock.readLock().unlock();
        }
        if (result == null) {
            lock.writeLock().lock();
            try {
                result = receivers.get(type);
                if (result == null) {
                    result = new Receivers<>();
                    receivers.put(type, result);
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
        return result;
    }

    /**
     * Register a receiver of the given event.
     *
     * @param eventType Event type to be received
     * @param receiver  Receiver of event
     * @param <T>       Event type
     */
    @NonNull
    public <T extends Event> SubscriptionReceipt<T> subscribeEvent(@NonNull Class<T> eventType, @NonNull EventReceiver<T> receiver) {
        var receivers = getReceivers(eventType);
        receivers.lock.writeLock().lock();
        try {
            var list = receivers.receivers;
            if (list.contains(receiver)) {
                // Simply detect if the event receiver has been added and return the SubscriptionReceipt directly.
                // Even if add multiple subscription, actually send an event, the event receiver will only receive an event once.
                // See also how LiveData does it:
                // https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:lifecycle/lifecycle-livedata-core/src/main/java/androidx/lifecycle/LiveData.java;l=190;drc=b69fe340ccf37160705e6d7dc512b814fd6bb100
                return new SubscriptionReceipt<>(this, eventType, receiver);
            }
            list.add(receiver);
        } finally {
            receivers.lock.writeLock().unlock();
        }
        return new SubscriptionReceipt<>(this, eventType, receiver);
    }

    /**
     * Dispatch the given event to its receivers registered in this manager.
     *
     * @param event Event to dispatch
     * @param <T>   Event type
     * @return <>                           </>he event's intercept targets
     */
    @SuppressWarnings("unchecked")
    public <T extends Event> int dispatchEvent(@NonNull T event) {
        if (!enabled) {
            return event.getInterceptTargets();
        }
        // Safe cast
        var receivers = getReceivers((Class<T>) event.getClass());
        receivers.lock.readLock().lock();
        EventReceiver<T>[] receiverArr;
        int count;
        try {
            count = receivers.receivers.size();
            receiverArr = obtainBuffer(count);
            receivers.receivers.toArray(receiverArr);
        } finally {
            receivers.lock.readLock().unlock();
        }
        List<EventReceiver<T>> unsubscribedReceivers = null;
        try {
            Unsubscribe unsubscribe = new Unsubscribe();
            for (int i = 0; i < count && (event.getInterceptTargets() & InterceptTarget.TARGET_RECEIVERS) == 0; i++) {
                var receiver = receiverArr[i];
                receiver.onReceive(event, unsubscribe);
                if (unsubscribe.isUnsubscribed()) {
                    if (unsubscribedReceivers == null) {
                        unsubscribedReceivers = new LinkedList<>();
                    }
                    unsubscribedReceivers.add(receiver);
                }
                unsubscribe.reset();
            }
        } finally {
            if (unsubscribedReceivers != null) {
                receivers.lock.writeLock().lock();
                try {
                    receivers.receivers.removeAll(unsubscribedReceivers);
                } finally {
                    receivers.lock.writeLock().unlock();
                }
            }
            recycleBuffer(receiverArr);
        }
        for (int i = 0; i < children.size() && (event.getInterceptTargets() & InterceptTarget.TARGET_RECEIVERS) == 0; i++) {
            EventManager sub = null;
            try {
                sub = children.get(i);
            } catch (IndexOutOfBoundsException e) {
                // concurrent mod ignored
            }
            if (sub != null) {
                sub.dispatchEvent(event);
            }
        }
        return event.getInterceptTargets();
    }

    @NonNull
    @SuppressWarnings("unchecked")
    private <V extends Event> EventReceiver<V>[] obtainBuffer(int size) {
        EventReceiver<V>[] res = null;
        synchronized (this) {
            for (int i = 0; i < caches.length; i++) {
                if (caches[i] != null && caches[i].length >= size) {
                    res = (EventReceiver<V>[]) caches[i];
                    caches[i] = null;
                    break;
                }
            }
        }
        if (res == null) {
            res = new EventReceiver[size];
        }
        return res;
    }

    private synchronized void recycleBuffer(@Nullable EventReceiver<?>[] array) {
        if (array == null) {
            return;
        }
        for (int i = 0; i < caches.length; i++) {
            if (caches[i] == null) {
                Arrays.fill(array, null);
                caches[i] = array;
                break;
            }
        }
    }

    /**
     * Internal class for saving receivers of each type
     *
     * @param <T> Event type
     */
    static class Receivers<T extends Event> {

        ReadWriteLock lock = new ReentrantReadWriteLock();

        List<EventReceiver<T>> receivers = new ArrayList<>();

    }

}
