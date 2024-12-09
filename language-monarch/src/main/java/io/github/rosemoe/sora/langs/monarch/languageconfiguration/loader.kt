/*******************************************************************************
 * Copyright (c) 2015-2017 Angelo ZERR.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 * <p>
 * Contributors:
 * Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 * Sebastian Thomschke (Vegard IT GmbH) - add previousLineText support
 *
 ******************************************************************************/

package io.github.rosemoe.sora.langs.monarch.languageconfiguration

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import io.github.dingyi222666.regex.GlobalRegexLib
import io.github.dingyi222666.regex.Regex
import io.github.rosemoe.sora.langs.monarch.languageconfiguration.model.AutoClosingPair
import io.github.rosemoe.sora.langs.monarch.languageconfiguration.model.AutoClosingPairConditional
import io.github.rosemoe.sora.langs.monarch.languageconfiguration.model.BaseAutoClosingPair
import io.github.rosemoe.sora.langs.monarch.languageconfiguration.model.CharacterPair
import io.github.rosemoe.sora.langs.monarch.languageconfiguration.model.CommentRule
import io.github.rosemoe.sora.langs.monarch.languageconfiguration.model.EnterAction
import io.github.rosemoe.sora.langs.monarch.languageconfiguration.model.FoldingMarkers
import io.github.rosemoe.sora.langs.monarch.languageconfiguration.model.FoldingRules
import io.github.rosemoe.sora.langs.monarch.languageconfiguration.model.IndentAction
import io.github.rosemoe.sora.langs.monarch.languageconfiguration.model.IndentationRule
import io.github.rosemoe.sora.langs.monarch.languageconfiguration.model.LanguageConfiguration
import io.github.rosemoe.sora.langs.monarch.languageconfiguration.model.OnEnterRule
import kotlin.properties.Delegates

class LanguageConfigurationAdapter : JsonAdapter<LanguageConfiguration>() {
    override fun fromJson(reader: JsonReader): LanguageConfiguration {
        reader.isLenient = true

        var comments: CommentRule? = null

        var brackets: List<CharacterPair>? = null

        var surroundingPairs: List<BaseAutoClosingPair>? = null

        var wordPattern: Regex? = null

        var indentationRules: IndentationRule? = null

        var onEnterRules: List<OnEnterRule> = listOf()

        var autoCloseBefore: String? = null

        var foldingRules: FoldingRules? = null

        var colorizedBracketPairs: List<CharacterPair>? = null

        var autoClosingPairs: List<AutoClosingPairConditional>? = null

        reader.beginObject()

        while (reader.hasNext()) {
            when (reader.nextName()) {
                "comments" -> {
                    comments = readCommentRule(reader)
                }

                "brackets" -> {
                    brackets = readBrackets(reader)
                }

                "colorizedBracketPairs" -> {
                    colorizedBracketPairs = readBrackets(reader)
                }

                "surroundingPairs" -> {
                    surroundingPairs = readSurroundingPairs(reader)
                }

                "autoClosingPairs" -> {
                    autoClosingPairs = readAutoClosingPairs(reader)
                }

                "wordPattern" -> {
                    wordPattern = readRegex(reader)
                }

                "indentationRules" -> {
                    indentationRules = readIndentationRules(reader)
                }

                "onEnterRules" -> {
                    onEnterRules = readOnEnterRules(reader)
                }

                "autoCloseBefore" -> {
                    autoCloseBefore = reader.nextString()
                }

                "folding" -> {
                    foldingRules = readFoldingRules(reader)
                }

                else -> {
                    reader.skipValue()
                }
            }
        }

        reader.endObject()

        return LanguageConfiguration(
            comments = comments,
            brackets = brackets,
            surroundingPairs = surroundingPairs,
            wordPattern = wordPattern,
            indentationRules = indentationRules,
            onEnterRules = onEnterRules,
            autoCloseBefore = autoCloseBefore,
            colorizedBracketPairs = colorizedBracketPairs,
            folding = foldingRules,
            autoClosingPairs = autoClosingPairs
        )

    }

    private fun readAutoClosingPairs(reader: JsonReader): List<AutoClosingPairConditional> {
        reader.beginArray()

        val list = mutableListOf<AutoClosingPairConditional>()

        while (reader.hasNext()) {
            if (reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                val pair = readAutoClosingPairConditional(reader, false)

                list.add(pair)
            } else {
                val pair = readAutoClosingPair(reader, false)

                list.add(AutoClosingPairConditional(pair.open, pair.close, emptyList()))
            }
        }

        reader.endArray()

        return list
    }

    private fun readFoldingRules(reader: JsonReader): FoldingRules {
        reader.beginObject()

        var offSide: Boolean? = null

        var markers: FoldingMarkers? = null

        while (reader.hasNext()) {
            when (reader.nextName()) {
                "offSide" -> {
                    offSide = reader.nextBoolean()
                }

                "markers" -> {
                    markers = readFoldingMarkers(reader)
                }
            }
        }

        reader.endObject()

        return FoldingRules(
            offSide = offSide,
            markers = markers
        )
    }

    private fun readFoldingMarkers(reader: JsonReader): FoldingMarkers {
        reader.beginObject()

        var start by Delegates.notNull<Regex>()
        var end by Delegates.notNull<Regex>()

        while (reader.hasNext()) {
            when (reader.nextName()) {
                "start" -> {
                    start = readRegex(reader)
                }

                "end" -> {
                    end = readRegex(reader)
                }
            }
        }

        reader.endObject()

        return FoldingMarkers(
            start = start,
            end = end
        )
    }


    private fun readOnEnterRules(reader: JsonReader): List<OnEnterRule> {
        reader.beginArray()

        val onEnterRules = mutableListOf<OnEnterRule>()

        while (reader.hasNext()) {
            reader.beginObject()

            var beforeText by Delegates.notNull<Regex>()
            var afterText: Regex? = null
            var action by Delegates.notNull<EnterAction>()
            var previousLineText: Regex? = null

            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "beforeText" -> {
                        beforeText = readRegex(reader)
                    }

                    "afterText" -> {
                        afterText = readRegex(reader)
                    }

                    "action" -> {
                        action = readEnterAction(reader)
                    }

                    "previousLineText" -> {
                        previousLineText = readRegex(reader)
                    }
                }
            }

            onEnterRules.add(OnEnterRule(beforeText, afterText, previousLineText, action))
            reader.endObject()
        }

        reader.endArray()

        return onEnterRules
    }

    private fun readEnterAction(reader: JsonReader): EnterAction {
        reader.beginObject()
        var indentAction by Delegates.notNull<Int>()
        var appendText: String? = null
        var removeText: Int? = null

        while (reader.hasNext()) {
            when (reader.nextName()) {
                "indent" -> {

                    indentAction = when (reader.nextString()) {
                        "none" -> IndentAction.None
                        "indent" -> IndentAction.Indent
                        "indentOutdent" -> IndentAction.IndentOutdent
                        "outdent" -> IndentAction.Outdent
                        else -> throw IllegalArgumentException("Invalid indentAction")
                    }
                }

                "appendText" -> {
                    appendText = reader.nextString()
                }

                "removeText" -> {
                    removeText = reader.nextInt()
                }
            }
        }
        reader.endObject()
        return EnterAction(indentAction, appendText, removeText)
    }

    private fun readIndentationRules(reader: JsonReader): IndentationRule {
        reader.beginObject()
        var decreaseIndentPattern by Delegates.notNull<Regex>()
        var increaseIndentPattern by Delegates.notNull<Regex>()
        var indentNextLinePattern: Regex? = null
        var unIndentedLinePattern: Regex? = null

        while (reader.hasNext()) {
            when (reader.nextName()) {
                "decreaseIndentPattern" -> {
                    decreaseIndentPattern = readRegex(reader)
                }

                "increaseIndentPattern" -> {
                    increaseIndentPattern = readRegex(reader)
                }

                "indentNextLinePattern" -> {
                    indentNextLinePattern = readRegex(reader)
                }

                "unIndentedLinePattern" -> {
                    unIndentedLinePattern = readRegex(reader)
                }
            }
        }

        reader.endObject()

        return IndentationRule(
            decreaseIndentPattern = decreaseIndentPattern,
            increaseIndentPattern = increaseIndentPattern,
            indentNextLinePattern = indentNextLinePattern,
            unIndentedLinePattern = unIndentedLinePattern
        )
    }


    private fun readRegex(reader: JsonReader): Regex {
        if (reader.peek() == JsonReader.Token.STRING) {
            return GlobalRegexLib.compile(reader.nextString())
        }

        reader.beginObject()

        var pattern by Delegates.notNull<String>()

        while (reader.hasNext()) {
            when (reader.nextName()) {
                "pattern" -> {
                    pattern = reader.nextString()
                }
            }
        }

        reader.endObject()

        return GlobalRegexLib.compile(pattern)
    }

    private fun readSurroundingPairs(reader: JsonReader): List<BaseAutoClosingPair> {
        reader.beginArray()

        val list = mutableListOf<BaseAutoClosingPair>()

        while (reader.hasNext()) {
            list.add(readAutoClosingPair(reader, true))
        }

        reader.endArray()

        return list
    }

    private fun readAutoClosingPair(
        reader: JsonReader,
        isSurroundingPair: Boolean
    ): BaseAutoClosingPair {
        if (reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
            return readAutoClosingPairConditional(reader, isSurroundingPair)
        }

        reader.beginArray()

        val open = reader.nextString()
        val close = reader.nextString()

        if (reader.hasNext()) {
            val notIn = readStringArray(reader)
            return AutoClosingPairConditional(open, close, notIn, isSurroundingPair)
        }

        reader.endArray()

        return AutoClosingPair(open, close, isSurroundingPair)
    }

    private fun readAutoClosingPairConditional(
        reader: JsonReader,
        isSurroundingPair: Boolean
    ): AutoClosingPairConditional {
        reader.beginObject()

        var open by Delegates.notNull<String>()
        var close by Delegates.notNull<String>()
        var notIn: List<String>? = null

        while (reader.hasNext()) {
            when (reader.nextName()) {
                "open" -> {
                    open = reader.nextString()
                }

                "close" -> {
                    close = reader.nextString()
                }

                "notIn" -> {
                    notIn = readStringArray(reader)
                }
            }
        }

        reader.endObject()

        return AutoClosingPairConditional(open, close, notIn ?: emptyList(), isSurroundingPair)
    }

    private fun readStringArray(reader: JsonReader): List<String> {
        reader.beginArray()

        val list = mutableListOf<String>()

        while (reader.hasNext()) {
            list.add(reader.nextString())
        }

        reader.endArray()

        return list
    }

    private fun readBrackets(reader: JsonReader): List<CharacterPair> {
        reader.beginArray()

        val list = mutableListOf<CharacterPair>()

        while (reader.hasNext()) {
            list.add(readCharacterPair(reader))
        }

        reader.endArray()

        return list
    }

    private fun readCommentRule(reader: JsonReader): CommentRule {
        reader.beginObject()

        var lineComment: String? = null
        var blockComment: CharacterPair? = null

        while (reader.hasNext()) {
            when (reader.nextName()) {
                "lineComment" -> {
                    lineComment = reader.nextString()
                }

                "blockComment" -> {
                    blockComment = readCharacterPair(reader)
                }
            }
        }

        reader.endObject()

        return CommentRule(lineComment, blockComment)
    }

    private fun readCharacterPair(reader: JsonReader): CharacterPair {
        reader.beginArray()

        val first = reader.nextString()
        val second = reader.nextString()

        reader.endArray()

        return CharacterPair(first, second)
    }

    override fun toJson(p0: JsonWriter, p1: LanguageConfiguration?) {}

}