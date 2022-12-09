/*******************************************************************************
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
 ******************************************************************************/

package io.github.rosemoe.sora.editor.ts

import android.os.Bundle
import android.util.Log
import com.itsaky.androidide.treesitter.TSInputEdit
import com.itsaky.androidide.treesitter.TSInputEncoding
import com.itsaky.androidide.treesitter.TSParser
import com.itsaky.androidide.treesitter.TSTree
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager
import io.github.rosemoe.sora.lang.analysis.StyleReceiver
import io.github.rosemoe.sora.lang.styling.Styles
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference

class TsAnalyzeManager(val parser: TSParser, val theme: TsTheme) : AnalyzeManager {

    var currentReceiver: StyleReceiver? = null
    var content: StringBuilder? = null
    var tree: TSTree? = null
    var styles: Styles? = null
    var reference: ContentReference? = null

    override fun setReceiver(receiver: StyleReceiver?) {
        currentReceiver = receiver
    }

    override fun reset(content: ContentReference, extraArguments: Bundle) {
        this.content = content.reference.toStringBuilder()
        reference = content
        styles = Styles()
        rerun(null)
    }

    override fun insert(start: CharPosition, end: CharPosition, insertedContent: CharSequence) {
        content!!.insert(start.index, insertedContent)
        rerun(TSInputEdit(start.index, start.index, end.index, start.toTSPoint(), start.toTSPoint(), end.toTSPoint()))
    }

    override fun delete(start: CharPosition, end: CharPosition, deletedContent: CharSequence) {
        content!!.delete(start.index, end.index)
        rerun(TSInputEdit(start.index, end.index, start.index, start.toTSPoint(), end.toTSPoint(), start.toTSPoint()))
    }

    override fun rerun() {
        rerun(null)
    }

    private fun rerun(edition: TSInputEdit?) {
        val oldTree = if (edition != null) tree else null
        val source = content.toString()
        tree = if (oldTree != null) {
            oldTree.edit(edition)
            parser.parseString(oldTree, source, TSInputEncoding.TSInputEncodingUTF16).also {
                oldTree.close()
            }
        } else {
            parser.parseString(source, TSInputEncoding.TSInputEncodingUTF16)
        }
        Log.d("Test", tree!!.rootNode.nodeString)
        styles!!.spans = LineSpansGenerator(tree!!, tree!!.rootNode.endPoint.row + 1, reference!!, theme)
        currentReceiver?.setStyles(this, styles)
    }

    override fun destroy() {
        setReceiver(null)
        content = null
        reference = null
        tree?.close()
        tree = null
        parser.close()
    }

}