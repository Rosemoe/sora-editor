package android.zero.studio.widget.editor.symbolinput

import androidx.lifecycle.ViewModel
import io.github.rosemoe.sora.widget.CodeEditor

/**
 * SymbolInputViewModel 的核心实现。
 *
 * @author android_zero
 * @github msmt2018/zero-Symbol-input-view
 */
class SymbolInputViewModel : ViewModel() {
    var editor: CodeEditor? = null
    var onOpenManagerListener: (() -> Unit)? = null
    var groups: List<SymbolGroup> = emptyList()
}
