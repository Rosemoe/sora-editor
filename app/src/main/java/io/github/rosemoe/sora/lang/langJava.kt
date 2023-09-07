/*******************************************************************************
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
 ******************************************************************************/

package io.github.rosemoe.sora.lang

import android.graphics.Color
import com.itsaky.androidide.treesitter.TSQuery
import com.itsaky.androidide.treesitter.TSQueryCapture
import com.itsaky.androidide.treesitter.java.TSLanguageJava
import io.github.rosemoe.sora.editor.ts.LocalsCaptureSpec
import io.github.rosemoe.sora.editor.ts.TsAnalyzeManager
import io.github.rosemoe.sora.editor.ts.TsLanguage
import io.github.rosemoe.sora.editor.ts.TsLanguageSpec
import io.github.rosemoe.sora.editor.ts.TsTheme
import io.github.rosemoe.sora.editor.ts.TsThemeBuilder
import io.github.rosemoe.sora.editor.ts.spans.TsSpanFactory
import io.github.rosemoe.sora.lang.styling.Span
import io.github.rosemoe.sora.lang.styling.Styles
import io.github.rosemoe.sora.lang.styling.TextStyle.makeStyle
import io.github.rosemoe.sora.lang.styling.line.LineAnchorStyle
import io.github.rosemoe.sora.lang.styling.line.LineGutterBackground
import io.github.rosemoe.sora.text.ContentReference
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

  init {
    spanFactory = TsJavaSpanFactory(reference, languageSpec.tsQuery, styles)
  }

  override fun rerun() {
    super.rerun()
    spanFactory = TsJavaSpanFactory(reference, languageSpec.tsQuery, styles)
  }
}

class TsJavaSpanFactory(
  private var content : ContentReference?,
  private var query: TSQuery?,
  private var styles: Styles?
) : TsSpanFactory {

  companion object {
    @JvmStatic
    private val HEX_REGEX = "#\\b([0-9a-fA-F]{3}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})\\b".toRegex()
  }

  override fun createSpan(capture: TSQueryCapture, column: Int, spanStyle: Long
  ): Span {
    val styles = requireNotNull(styles)
    val lineAnchorStyle = createColorLineAnchorStyle(capture)
    if (lineAnchorStyle != null) {
      styles.addLineStyle(lineAnchorStyle)
    }

    return Span.obtain(column, spanStyle)
  }

  private fun createColorLineAnchorStyle(capture: TSQueryCapture) : LineAnchorStyle? {
    val content = requireNotNull(content?.reference)
    val query = requireNotNull(query)
    val styles = requireNotNull(styles)

    val captureName = query.getCaptureNameForId(capture.index)
    if (captureName != "string") {
      return null
    }

    val (start, end) = content.indexer.run {
      getCharPosition(capture.node.startByte / 2) to getCharPosition(capture.node.endByte / 2)
    }

    if (start.line != end.line) {
      styles.eraseLineStyle(start.line, LineGutterBackground::class.java)
      styles.eraseLineStyle(end.line, LineGutterBackground::class.java)
      return null
    }

    val text = content.subContent(start.line, start.column, end.line, end.column)
    val result = HEX_REGEX.find(text) ?: run {
      styles.eraseLineStyle(start.line, LineGutterBackground::class.java)
      styles.eraseLineStyle(end.line, LineGutterBackground::class.java)
      return null
    }

    val color = try {
      Color.parseColor(result.groupValues[0])
    } catch (err: Exception) {
      styles.eraseLineStyle(start.line, LineGutterBackground::class.java)
      styles.eraseLineStyle(end.line, LineGutterBackground::class.java)
      return null
    }

    return LineGutterBackground(start.line) { color }
  }

  override fun close() {
    content = null
    query = null
    styles = null
  }
}

fun TsThemeBuilder.buildTheme() {
  makeStyle(COMMENT, 0, false, true, false) applyTo "comment"
  makeStyle(KEYWORD, 0, true, false, false) applyTo "keyword"
  makeStyle(LITERAL) applyTo arrayOf("constant.builtin", "string", "number")
  makeStyle(IDENTIFIER_VAR) applyTo arrayOf("variable.builtin", "variable",
    "constant")
  makeStyle(IDENTIFIER_NAME) applyTo arrayOf("type.builtin", "type",
    "attribute")
  makeStyle(FUNCTION_NAME) applyTo arrayOf("function.method",
    "function.builtin", "variable.field")
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