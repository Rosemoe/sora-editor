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
package io.github.rosemoe.sora.widget.snippet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

import io.github.rosemoe.sora.lang.completion.snippet.ConditionalFormat;
import io.github.rosemoe.sora.lang.completion.snippet.FormatString;
import io.github.rosemoe.sora.lang.completion.snippet.NextUpperCaseFormat;
import io.github.rosemoe.sora.lang.completion.snippet.NoFormat;
import io.github.rosemoe.sora.lang.completion.snippet.Transform;

public class TransformApplier {

    public static String doTransform(@NonNull String text, @Nullable Transform transform) {
        if (transform == null) {
            return text;
        }
        var sb = new StringBuilder();
        var matcher = transform.regexp.matcher(text);
        int loopCount = 0;
        int limit = transform.globalMode ? Integer.MAX_VALUE : 1;
        int nextIndex = 0;
        while (loopCount < limit && nextIndex < text.length()) {
            if (matcher.find(nextIndex)) {
                int start = matcher.start();
                int end = matcher.end();
                sb.append(text, nextIndex, start);
                sb.append(applySingle(text, matcher, transform.format));
                nextIndex = end;
            } else {
                break;
            }
            loopCount++;
        }
        if (nextIndex < text.length()) {
            sb.append(text, nextIndex, text.length());
        }
        return sb.toString();
    }

    private static CharSequence applySingle(String text, Matcher matcher, List<FormatString> formatStringList) {
        var sb = new StringBuilder();
        var nextUpperCase = false;
        for (FormatString formatString : formatStringList) {
            if (formatString instanceof NoFormat) {
                sb.append(applyFirstUpperCase(((NoFormat) formatString).getText(), nextUpperCase));
            } else if (formatString instanceof ConditionalFormat) {
                var format = (ConditionalFormat) formatString;
                var group = matcher.group(format.getGroup());
                if (format.getShorthand() != null) {
                    if (group != null) {
                        switch (format.getShorthand()) {
                            case "upcase":
                                sb.append(applyFirstUpperCase(group.toUpperCase(Locale.ROOT), nextUpperCase));
                                break;
                            case "lowcase":
                                sb.append(applyFirstUpperCase(group.toLowerCase(Locale.ROOT), nextUpperCase));
                                break;
                            default:
                                //not supported
                                sb.append(applyFirstUpperCase(group, nextUpperCase));
                        }
                    }
                } else {
                    var ifValue = format.getIfValue() != null ? format.getIfValue() : group;
                    var elseValue = format.getElseValue() != null ? format.getElseValue() : "";
                    sb.append(applyFirstUpperCase(group != null ? ifValue : elseValue, nextUpperCase));
                }
            }
            nextUpperCase = formatString instanceof NextUpperCaseFormat;
        }
        return sb;
    }

    private static String applyFirstUpperCase(String text, boolean apply) {
        if (apply && text != null && text.length() > 0 && isAlpha(text.charAt(0))) {
            return Character.toUpperCase(text.charAt(0)) + text.substring(1, text.length());
        }
        return text;
    }

    private static boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

}
