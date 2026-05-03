/*******************************************************************************
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2026  Rosemoe
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

package io.github.rosemoe.sora.app.compose

import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import io.github.rosemoe.sora.app.R
import io.github.rosemoe.sora.compose.CodeEditor
import io.github.rosemoe.sora.compose.rememberCodeEditorState
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticDetail
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer
import io.github.rosemoe.sora.lang.diagnostic.Quickfix
import io.github.rosemoe.sora.langs.java.JavaLanguage
import io.github.rosemoe.sora.text.ContentIO
import io.github.rosemoe.sora.utils.toast
import io.github.rosemoe.sora.widget.schemes.SchemeEclipse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ComposeEditorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val darkTheme = isSystemInDarkTheme()
            val colorScheme = if (darkTheme) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    dynamicDarkColorScheme(this)
                } else {
                    darkColorScheme()
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    dynamicLightColorScheme(this)
                } else {
                    lightColorScheme()
                }
            }

            MaterialTheme(colorScheme = colorScheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Content()
                }
            }
        }
    }

    @Composable
    private fun Content() {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("Compose") },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(
                                painterResource(R.drawable.round_arrow_back_24),
                                contentDescription = "back"
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->

            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .imePadding()
            ) {
                val state = rememberCodeEditorState()
                val typeface = remember { Typeface.createFromAsset(assets, "JetBrainsMono-Regular.ttf") }

                LaunchedEffect(state) {
                    state.typefaceText = typeface
                    state.typefaceLineNumber = typeface
                    state.editorLanguage = JavaLanguage()
                    state.colorScheme = SchemeEclipse()
                    state.props.showMinimap = false

                    val content = withContext(Dispatchers.IO) {
                        unzipSampleFile().inputStream().use { ContentIO.createFrom(it) }
                    }
                    withContext(Dispatchers.Main) { state.setText(content) }

                    val diagnostics = state.diagnostics ?: DiagnosticsContainer()
                    diagnostics.addDiagnostic(
                        DiagnosticRegion(
                            140, 145, DiagnosticRegion.SEVERITY_WARNING, 0,
                            DiagnosticDetail(
                                "Test Diagnostic",
                                "Hellloooo, Worlddd.",
                                listOf(Quickfix("Umm.", fixAction = { toast("Fixed.") }))
                            )
                        )
                    )
                    state.diagnostics = diagnostics
                }

                CodeEditor(
                    state = state,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    private fun unzipSampleFile(forced: Boolean = false): File {
        filesDir.mkdirs()
        val sourceFile = filesDir.resolve("sample.txt")
        if (sourceFile.exists() && !forced) {
            return sourceFile
        }
        assets.open("samples/sample.txt").use { input ->
            sourceFile.outputStream().use {
                input.copyTo(it)
            }
        }
        return sourceFile
    }
}
