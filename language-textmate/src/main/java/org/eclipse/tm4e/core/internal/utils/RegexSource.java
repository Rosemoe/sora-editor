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
package org.eclipse.tm4e.core.internal.utils;

import org.eclipse.tm4e.core.internal.oniguruma.IOnigCaptureIndex;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexSource {

    //fix for android
    private static final Pattern CAPTURING_REGEX_SOURCE = Pattern
            .compile("\\$(\\d+)|\\$\\{(\\d+):/(downcase|upcase)\\}");

//	private static final Pattern CAPTURING_REGEX_SOURCE = Pattern
//			.compile("\\$(\\d+)|\\$\\{(\\d+):\\/(downcase|upcase)}");


    /**
     * Helper class, access members statically
     */
    private RegexSource() {
    }

    public static boolean hasCaptures(String regexSource) {
        if (regexSource == null) {
            return false;
        }
        return CAPTURING_REGEX_SOURCE.matcher(regexSource).find();
    }

    public static String replaceCaptures(String regexSource, String captureSource, IOnigCaptureIndex[] captureIndices) {
        Matcher m = CAPTURING_REGEX_SOURCE.matcher(regexSource);
        StringBuffer result = new StringBuffer();
        while (m.find()) {
            String match = m.group();
            String replacement = getReplacement(match, captureSource, captureIndices);
            m.appendReplacement(result, replacement);
        }
        m.appendTail(result);
        return result.toString();
    }

    private static String getReplacement(String match, String captureSource, IOnigCaptureIndex[] captureIndices) {
        int index = -1;
        String command = null;
        int doublePointIndex = match.indexOf(':');
        if (doublePointIndex != -1) {
            index = Integer.parseInt(match.substring(2, doublePointIndex));
            command = match.substring(doublePointIndex + 2, match.length() - 1);
        } else {
            index = Integer.parseInt(match.substring(1, match.length()));
        }
        IOnigCaptureIndex capture = captureIndices.length > index ? captureIndices[index] : null;
        if (capture != null) {
            String result = captureSource.substring(capture.getStart(), capture.getEnd());
            // Remove leading dots that would make the selector invalid
            while (result.length() > 0 && result.charAt(0) == '.') {
                result = result.substring(1);
            }
            if ("downcase".equals(command)) {
                return result.toLowerCase(Locale.ROOT);
            } else if ("upcase".equals(command)) {
                return result.toUpperCase(Locale.ROOT);
            } else {
                return result;
            }
        } else {
            return match;
        }
    }
}
