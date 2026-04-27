package android.zero.studio.widget.editor.symbolinput

import androidx.lifecycle.ViewModel
import io.github.rosemoe.sora.widget.CodeEditor

class SymbolInputViewModel : ViewModel() {
    var editor: CodeEditor? = null
    var onOpenManagerListener: (() -> Unit)? = null
    var groups: List<SymbolGroup> = emptyList()
}