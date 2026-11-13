package android.zero.studio.widget.editor.symbolinput

import androidx.lifecycle.ViewModel
import io.github.rosemoe.sora.widget.CodeEditor

/**
 * 符号输入页共享状态模型。
 *
 * 用于在 Fragment/Activity 间共享编辑器引用、回调与分组数据。
 *
 * @author android_zero
 * @github msmt2018/zero-Symbol-input-view
 */
class SymbolInputViewModel : ViewModel() {
    var editor: CodeEditor? = null
    var onOpenManagerListener: (() -> Unit)? = null
    var groups: List<SymbolGroup> = emptyList()
}
