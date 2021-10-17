/*
 *    CodeEditor - the awesome code editor for Android
 *    Copyright (C) 2020-2021  Rosemoe
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
package io.github.rosemoe.sora.textmate.languageconfiguration;

import org.junit.Test;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.StringReader;
import io.github.rosemoe.sora.textmate.core.internal.oniguruma.OnigRegExp;
import io.github.rosemoe.sora.textmate.core.internal.oniguruma.OnigResult;
import io.github.rosemoe.sora.textmate.core.internal.oniguruma.OnigString;
import io.github.rosemoe.sora.textmate.languageconfiguration.internal.LanguageConfiguration;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ParsingTest {
    String json="{\n" +
            "  \"comments\": {\n" +
            "    \"lineComment\": \"//\",\n" +
            "    \"blockComment\": [\"/*\", \"*/\"]\n" +
            "  },\n" +
            "  \"brackets\": [\n" +
            "    [\"{\", \"}\"],\n" +
            "    [\"[\", \"]\"],\n" +
            "    [\"(\", \")\"]\n" +
            "  ],\n" +
            "  \"autoClosingPairs\": [\n" +
            "    { \"open\": \"{\", \"close\": \"}\" },\n" +
            "    { \"open\": \"[\", \"close\": \"]\" },\n" +
            "    { \"open\": \"(\", \"close\": \")\" },\n" +
            "    { \"open\": \"'\", \"close\": \"'\", \"notIn\": [\"string\", \"comment\"] },\n" +
            "    { \"open\": \"\\\"\", \"close\": \"\\\"\", \"notIn\": [\"string\"] },\n" +
            "    { \"open\": \"`\", \"close\": \"`\", \"notIn\": [\"string\", \"comment\"] },\n" +
            "    { \"open\": \"/**\", \"close\": \" */\", \"notIn\": [\"string\"] }\n" +
            "  ],\n" +
            "  \"autoCloseBefore\": \";:.,=}])>` \\n\\t\",\n" +
            "  \"surroundingPairs\": [\n" +
            "    [\"{\", \"}\"],\n" +
            "    [\"[\", \"]\"],\n" +
            "    [\"(\", \")\"],\n" +
            "    [\"'\", \"'\"],\n" +
            "    [\"\\\"\", \"\\\"\"],\n" +
            "    [\"`\", \"`\"]\n" +
            "  ],\n" +
            "  \"folding\": {\n" +
            "    \"markers\": {\n" +
            "      \"start\": \"(\\\\{\\\\s*(//.*)?$|^\\\\s*// \\\\{\\\\{\\\\{)\",\n" +
            "      \"end\": \"^\\\\s*(\\\\}|// \\\\}\\\\}\\\\}$)\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"wordPattern\": \"(-?\\\\d*\\\\.\\\\d\\\\w*)|([^\\\\`\\\\~\\\\!\\\\@\\\\#\\\\%\\\\^\\\\&\\\\*\\\\(\\\\)\\\\-\\\\=\\\\+\\\\[\\\\{\\\\]\\\\}\\\\\\\\\\\\|\\\\;\\\\:\\\\'\\\\\\\"\\\\,\\\\.\\\\<\\\\>\\\\/\\\\?\\\\s]+)\",\n" +
            "  \"indentationRules\": {\n" +
            "    \"increaseIndentPattern\": \"^((?!\\\\/\\\\/).)*(\\\\{[^}\\\"'`]*|\\\\([^)\\\"'`]*|\\\\[[^\\\\]\\\"'`]*)$\",\n" +
            "    \"decreaseIndentPattern\": \"^((?!.*?\\\\/\\\\*).*\\\\*/)?\\\\s*[\\\\}\\\\]].*$\"\n" +
            "  }\n" +
            "}";
    @Test
    public void testCanLoadLanguageConfig() throws Exception {
        LanguageConfiguration languageConfiguration = LanguageConfiguration.load(new StringReader(json));
        assertNotNull(languageConfiguration.getComments());
        assertNotNull(languageConfiguration.getBrackets());
        assertNotNull(languageConfiguration.getAutoClosingPairs());
        assertNotNull(languageConfiguration.getSurroundingPairs());
        assertNotNull(languageConfiguration.getFolding());
        assertNotNull(languageConfiguration.getWordPattern());
        assertNotNull(languageConfiguration.getAutoCloseBefore());
        assertNotNull(languageConfiguration.getIndentationRule());
    }
}