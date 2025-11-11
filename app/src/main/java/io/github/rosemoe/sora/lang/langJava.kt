/*******************************************************************************
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2024  Rosemoe
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

package io.github.rosemoe.sora.lang

import android.graphics.Color
import android.util.Log
import androidx.core.graphics.ColorUtils
import com.itsaky.androidide.treesitter.TSQuery
import com.itsaky.androidide.treesitter.TSQueryCapture
import com.itsaky.androidide.treesitter.java.TSLanguageJava
import io.github.rosemoe.sora.editor.ts.LocalsCaptureSpec
import io.github.rosemoe.sora.editor.ts.TsAnalyzeManager
import io.github.rosemoe.sora.editor.ts.TsLanguage
import io.github.rosemoe.sora.editor.ts.TsLanguageSpec
import io.github.rosemoe.sora.editor.ts.TsTheme
import io.github.rosemoe.sora.editor.ts.TsThemeBuilder
import io.github.rosemoe.sora.editor.ts.spans.DefaultSpanFactory
import io.github.rosemoe.sora.lang.styling.Span
import io.github.rosemoe.sora.lang.styling.SpanFactory
import io.github.rosemoe.sora.lang.styling.Styles
import io.github.rosemoe.sora.lang.styling.TextStyle.makeStyle
import io.github.rosemoe.sora.lang.styling.span.SpanConstColorResolver
import io.github.rosemoe.sora.lang.styling.span.SpanExtAttrs
import io.github.rosemoe.sora.lang.styling.textStyle
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.COMMENT
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.FUNCTION_NAME
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.IDENTIFIER_NAME
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.IDENTIFIER_VAR
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.KEYWORD
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.LITERAL
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.OPERATOR

/**
 * Tree Sitter language for Java.
 *
 * @author Akash Yadav
 */
class TsLanguageJava(
    languageSpec: JavaLanguageSpec,
    tab: Boolean = false
) : TsLanguage(languageSpec, tab, { buildTheme() }) {

    override val analyzer: TsAnalyzeManager by lazy {
        TsJavaAnalyzeManager(languageSpec, tsTheme)
    }
}

class TsJavaAnalyzeManager(
    languageSpec: TsLanguageSpec,
    theme: TsTheme
) : TsAnalyzeManager(languageSpec, theme) {

    override var styles: Styles = Styles()
        set(value) {
            field = value
            spanFactory = TsJavaSpanFactory(reference, languageSpec.tsQuery, value)
        }
}

class TsJavaSpanFactory(
    private var content: ContentReference?,
    private var query: TSQuery?,
    private var styles: Styles?
) : DefaultSpanFactory() {

    companion object {
        @JvmStatic
        private val HEX_REGEX = "#\\b([0-9a-fA-F]{3}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})\\b".toRegex()
    }

    override fun createSpans(
        capture: TSQueryCapture, column: Int, spanStyle: Long
    ): List<Span> {
        val content = requireNotNull(content?.reference)
        val query = requireNotNull(query)

        val captureName = query.getCaptureNameForId(capture.index)
        if (captureName != "string") {
            return super.createSpans(capture, column, spanStyle)
        }

        val (start, end) = content.indexer.run {
            getCharPosition(capture.node.startByte / 2) to getCharPosition(capture.node.endByte / 2)
        }

        if (start.line != end.line || start.column != column) {
            return super.createSpans(capture, column, spanStyle)
        }

        val text =
            content.subContent(start.line, start.column, end.line, end.column, false).toString()
        val results = HEX_REGEX.findAll(text)

        val spans = mutableListOf<Span>()
        var s = -1
        var e = -1
        results.forEach { result ->
            if (e != -1 && e < result.range.first) {
                // there is some interval between previous color span
                // and this color span
                // fill the gap
                spans.add(
                    SpanFactory.obtain(
                        column + e + 1,
                        spanStyle
                    )
                )
            }

            if (s == -1) {
                s = result.range.first
            }
            e = result.range.last

            val color = try {
                var str = result.groupValues[1]
                if (str.length == 3) {
                    // HEX color is in the form of #FFF
                    // convert it to #FFFFFF format (6 character long)
                    val r = str[0]
                    val g = str[1]
                    val b = str[2]
                    str = "$r$r$g$g$b$b"
                }

                if (str.length == 6) {
                    // Prepend alpha value
                    str = "FF${str}"
                }

                java.lang.Long.parseLong(str, 16)
            } catch (e: Exception) {
                Log.e("JavaSpanFactory", "Failed to parse hex color. color=$text", e)
                return@forEach
            }.toInt()

            val textColor = if (ColorUtils.calculateLuminance(color) > 0.5f) {
                Color.BLACK
            } else {
                Color.WHITE
            }

            val col = column + result.range.first
            val span = SpanFactory.obtain(
                col,
                makeStyle(
                    EditorColorScheme.STATIC_SPAN_FOREGROUND,
                    EditorColorScheme.STATIC_SPAN_BACKGROUND,
                    false,
                    false,
                    false,
                    true
                )
            )
            span.setSpanExt(
                SpanExtAttrs.EXT_COLOR_RESOLVER,
                SpanConstColorResolver(textColor, color)
            )

            spans.add(span)
        }

        if (spans.isEmpty()) {
            return super.createSpans(capture, column, spanStyle)
        }

        // make sure that the default style is used for unmatched regions
        if (s != 0) {
            spans.add(
                0,
                SpanFactory.obtain(
                    column,
                    spanStyle
                )
            )
        }

        if (e != text.lastIndex) {
            spans.add(
                SpanFactory.obtain(
                    column + e + 1,
                    spanStyle
                )
            )
        }

        return spans
    }

    override fun close() {
        content = null
        query = null
        styles = null
    }
}

fun TsThemeBuilder.buildTheme() {
    textStyle(COMMENT, italic = true) applyTo "comment"
    textStyle(KEYWORD, bold = true) applyTo "keyword"
    makeStyle(LITERAL) applyTo arrayOf("constant.builtin", "string", "number")
    makeStyle(IDENTIFIER_VAR) applyTo arrayOf(
        "variable.builtin", "variable",
        "constant"
    )
    makeStyle(IDENTIFIER_NAME) applyTo arrayOf(
        "type.builtin", "type",
        "attribute"
    )
    makeStyle(FUNCTION_NAME) applyTo arrayOf(
        "function.method",
        "function.builtin", "variable.field"
    )
    makeStyle(OPERATOR) applyTo "operator"
}

class JavaLanguageSpec(
    highlightScmSource: String,
    codeBlocksScmSource: String = "",
    bracketsScmSource: String = "",
    localsScmSource: String = "",
) : TsLanguageSpec(
    TSLanguageJava.getInstance(),
    highlightScmSource,
    codeBlocksScmSource,
    bracketsScmSource,
    localsScmSource,
    localsCaptureSpec
)

private val localsCaptureSpec = object : LocalsCaptureSpec() {

    override fun isScopeCapture(captureName: String): Boolean {
        return captureName == "scope"
    }

    override fun isReferenceCapture(captureName: String): Boolean {
        return captureName == "reference"
    }

    override fun isDefinitionCapture(captureName: String): Boolean {
        return captureName == "definition.var" || captureName == "definition.field"
    }

    override fun isMembersScopeCapture(captureName: String): Boolean {
        return captureName == "scope.members"
    }
}