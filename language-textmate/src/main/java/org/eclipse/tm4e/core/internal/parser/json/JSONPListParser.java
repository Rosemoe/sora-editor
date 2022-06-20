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
package org.eclipse.tm4e.core.internal.parser.json;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.eclipse.tm4e.core.internal.parser.PList;

public class JSONPListParser<T> {

    private final boolean theme;

    public JSONPListParser(boolean theme) {
        this.theme = theme;
    }

    public T parse(InputStream contents) throws Exception {
        PList<T> pList = new PList<T>(theme);
        JsonReader reader = new JsonReader(new InputStreamReader(contents, StandardCharsets.UTF_8));
        // reader.setLenient(true);
        boolean parsing = true;
        while (parsing) {
            JsonToken nextToken = reader.peek();
            switch (nextToken) {
                case BEGIN_ARRAY:
                    pList.startElement(null, "array", null, null);
                    reader.beginArray();
                    break;
                case END_ARRAY:
                    pList.endElement(null, "array", null);
                    reader.endArray();
                    break;
                case BEGIN_OBJECT:
                    pList.startElement(null, "dict", null, null);
                    reader.beginObject();
                    break;
                case END_OBJECT:
                    pList.endElement(null, "dict", null);
                    reader.endObject();
                    break;
                case NAME:
                    String lastName = reader.nextName();
                    pList.startElement(null, "key", null, null);
                    pList.characters(lastName.toCharArray(), 0, lastName.length());
                    pList.endElement(null, "key", null);
                    break;
                case NULL:
                    reader.nextNull();
                    break;
                case BOOLEAN:
                    reader.nextBoolean();
                    break;
                case NUMBER:
                    reader.nextLong();
                    break;
                case STRING:
                    String value = reader.nextString();
                    pList.startElement(null, "string", null, null);
                    pList.characters(value.toCharArray(), 0, value.length());
                    pList.endElement(null, "string", null);
                    break;
                case END_DOCUMENT:
                    parsing = false;
                    break;
                default:
                    break;
            }
        }
        reader.close();
        return pList.getResult();
    }

}
