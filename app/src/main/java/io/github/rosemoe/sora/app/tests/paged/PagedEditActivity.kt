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

package io.github.rosemoe.sora.app.tests.paged

import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.addCallback
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.rosemoe.sora.app.BaseEditorActivity
import io.github.rosemoe.sora.langs.java.JavaLanguage
import io.github.rosemoe.sora.utils.toast
import io.github.rosemoe.sora.widget.schemes.SchemeEclipse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PagedEditActivity : BaseEditorActivity() {

    companion object {
        const val MyPageSize = 512 * 1024
    }

    private var pagedEditSession: PagedEditSession? = null

    private var pageIndex = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val typeface = Typeface.createFromAsset(assets, "JetBrainsMono-Regular.ttf")
        editor.typefaceText = typeface
        editor.typefaceLineNumber = typeface
        editor.setEditorLanguage(JavaLanguage())
        editor.colorScheme = SchemeEclipse()

        val sourceFile = filesDir.resolve("big_sample.txt")
        runTaskWithModalDialog {
            runCatching {
                unzipSampleFile(forced = false)
                val tmpDir = filesDir.resolve("session")
                sourceFile.reader().use {
                    pagedEditSession =
                        PagedEditSession(it, tmpDir, MyPageSize)
                }
                pagedEditSession?.loadPageToEditor(0, editor)
                pageIndex = 0
                updateUiPageIndex()
            }.onFailure {
                it.printStackTrace()
                pagedEditSession?.close()
                pagedEditSession = null
                pageIndex = -1
                withContext(Dispatchers.Main) {
                    toast("Failed to setup paged edit session")
                }
            }
        }

        onBackPressedDispatcher.addCallback {
            handleSaveOnBack()
        }
    }

    fun runTaskWithModalDialog(title: CharSequence = "Loading", task: suspend () -> Unit) {
        val pd = MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(RelativeLayout(this).also { layout ->
                ProgressBar(this).also { pb ->
                    layout.addView(
                        pb,
                        RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT
                        ).also {
                            it.addRule(RelativeLayout.CENTER_IN_PARENT)
                            it.topMargin = TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP,
                                16f,
                                resources.displayMetrics
                            ).toInt()
                            it.bottomMargin = it.topMargin
                        })
                }
            })
            .setCancelable(false)
            .show()
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                task()
            }
            withContext(Dispatchers.Main) {
                pd.dismiss()
            }
        }
    }

    private fun unzipSampleFile(forced: Boolean = false) {
        filesDir.mkdirs()
        val sourceFile = filesDir.resolve("big_sample.txt")
        if (sourceFile.exists() && !forced) {
            return
        }
        assets.open("samples/big_sample.txt").use { input ->
            sourceFile.outputStream().use {
                input.copyTo(it)
            }
        }
    }

    private fun updateUiPageIndex() {
        runOnUiThread {
            Toast.makeText(
                this,
                "Page ${pageIndex + 1} of ${pagedEditSession?.pageCount}",
                Toast.LENGTH_SHORT
            ).show()
            setTitle("Page ${pageIndex + 1} of ${pagedEditSession?.pageCount}")
        }
    }

    private fun handleSaveOnBack() {
        if (pagedEditSession == null || pageIndex == -1) {
            finish()
            return
        }
        runTaskWithModalDialog(title = "Saving") {
            pagedEditSession?.unloadPageFromEditor(pageIndex, editor)
            val sourceFile = filesDir.resolve("big_sample.txt")
            pagedEditSession?.writeTo(sourceFile)
            pageIndex = -1
            withContext(Dispatchers.Main) {
                toast("Changes saved")
                finish()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            handleSaveOnBack()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add("Previous Page")?.setOnMenuItemClickListener {
            if (pagedEditSession == null || pageIndex == -1) {
                return@setOnMenuItemClickListener true
            }
            if (pageIndex == 0) {
                updateUiPageIndex()
            } else {
                runTaskWithModalDialog {
                    pagedEditSession?.apply {
                        unloadPageFromEditor(pageIndex, editor)
                        loadPageToEditor(--pageIndex, editor)
                    }
                    updateUiPageIndex()
                }
            }
            return@setOnMenuItemClickListener true
        }
        menu?.add("Next Page")?.setOnMenuItemClickListener {
            if (pagedEditSession == null || pageIndex == -1) {
                return@setOnMenuItemClickListener true
            }
            if (pageIndex == pagedEditSession!!.pageCount - 1) {
                updateUiPageIndex()
            } else {
                runTaskWithModalDialog {
                    pagedEditSession?.apply {
                        unloadPageFromEditor(pageIndex, editor)
                        loadPageToEditor(++pageIndex, editor)
                    }
                    updateUiPageIndex()
                }
            }
            return@setOnMenuItemClickListener true
        }
        menu?.add("Reset File")?.setOnMenuItemClickListener {
            runTaskWithModalDialog {
                pagedEditSession?.close()
                pagedEditSession = null
                pageIndex = -1
                unzipSampleFile(forced = true)
                withContext(Dispatchers.Main) {
                    toast("File is reset, Please reopen activity")
                    finish()
                }
            }
            return@setOnMenuItemClickListener true
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onDestroy() {
        super.onDestroy()
        pagedEditSession?.close()
        editor.release()
    }

}