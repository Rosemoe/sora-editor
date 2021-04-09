/*
 *   Copyright 2020-2021 Rosemoe
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package io.github.rosemoe.editor.debug_logger;

import android.util.Log;

public class Logs {

    private static StringBuilder stringBuilder = new StringBuilder();

    public static void dumpStack() {
        Throwable e = new Throwable();
        stringBuilder.append(e.toString()).append('\n');
        for (StackTraceElement element : e.getStackTrace()) {
            stringBuilder.append(element.toString()).append('\n');
        }
    }

    public static void log(String line) {
        stringBuilder.append(line).append('\n');
        Log.d("CodeEditor", line);
    }

    public static StringBuilder getLogs() {
        return stringBuilder;
    }

}
