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
package io.github.rosemoe.sora.langs.textmate.registry.reader;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import org.eclipse.tm4e.core.registry.IGrammarSource;
import org.eclipse.tm4e.languageconfiguration.model.CharacterPair;
import org.eclipse.tm4e.languageconfiguration.model.OnEnterRule;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.model.DefaultLanguageDefinition;
import io.github.rosemoe.sora.langs.textmate.registry.model.LanguageDefinition;

public class LanguageDefinitionReader {

    public static List<LanguageDefinition> read(String path) {
        var stream = FileProviderRegistry.getInstance().tryGetInputStream(path);
        if (stream == null) {
            return Collections.emptyList();
        }
        return read(new BufferedReader(new InputStreamReader(stream)));
    }

    private static List<LanguageDefinition> read(BufferedReader bufferedReader) {
        return new GsonBuilder().registerTypeAdapter(LanguageDefinition.class, (JsonDeserializer<LanguageDefinition>) (json, typeOfT, context) -> {
                    var object = json.getAsJsonObject();
                    var grammarPath = object.get("grammar").getAsString();
                    var name = object.get("name").getAsString();
                    var scopeName = object.get("scopeName").getAsString();

                    var languageConfigurationElement = object.get("languageConfiguration");

                    String languageConfiguration = null;

                    if (!languageConfigurationElement.isJsonNull()) {
                        languageConfiguration = languageConfigurationElement.getAsString();
                    }

                    var grammarSource = IGrammarSource.fromInputStream(FileProviderRegistry.getInstance().tryGetInputStream(
                            grammarPath
                    ), grammarPath, Charset.defaultCharset());

                    return DefaultLanguageDefinition.withLanguageConfiguration(grammarSource, languageConfiguration, name, scopeName);


                })
                .create()
                .fromJson(bufferedReader, LanguageDefinitionList.class).languageDefinition;


    }


    static class LanguageDefinitionList {
        @SerializedName("languages")
        private List<LanguageDefinition> languageDefinition;

        public LanguageDefinitionList(List<LanguageDefinition> languageDefinition) {
            this.languageDefinition = languageDefinition;
        }

        public List<LanguageDefinition> getLanguageDefinition() {
            return languageDefinition;
        }

        public void setLanguageDefinition(List<LanguageDefinition> languageDefinition) {
            this.languageDefinition = languageDefinition;
        }
    }

}
