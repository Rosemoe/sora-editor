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
package io.github.rosemoe.sora.lsp.requests;


import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An object containing the Timeout for the various requests
 */
public class Timeout {

    private static Map<Timeouts, Integer> timeouts = new ConcurrentHashMap<>();

    static {
        Arrays.stream(Timeouts.values()).forEach(t -> timeouts.put(t, t.getDefaultTimeout()));
    }

    public static int getTimeout(Timeouts type) {
        return timeouts.get(type);
    }

    public static Map<Timeouts, Integer> getTimeouts() {
        return timeouts;
    }

    public static void setTimeouts(Map<Timeouts, Integer> loaded) {
        loaded.forEach((t, v) -> timeouts.replace(t, v));
    }
}