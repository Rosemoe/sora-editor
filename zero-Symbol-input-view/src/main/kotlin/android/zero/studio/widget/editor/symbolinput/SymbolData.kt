package android.zero.studio.widget.editor.symbolinput

import com.google.gson.annotations.SerializedName


// 符号分组数据模型
/**
 * SymbolGroup 的核心实现。
 *
 * @author android_zero
 * @github msmt2018/zero-Symbol-input-view
 */
data class SymbolGroup(
    @SerializedName("n") var name: String = "",
    @SerializedName("d") var items: MutableList<SymbolItem> = mutableListOf()
)

// 具体符号属性数据模型
/**
 * SymbolItem 的核心实现。
 *
 * @author android_zero
 * @github msmt2018/zero-Symbol-input-view
 */
data class SymbolItem(
    @SerializedName("a") var shortAction: Int = 0,
    @SerializedName("b") var display: String = "",
    @SerializedName("c") var shortText: String? = null,
    @SerializedName("d") var longAction: Int? = null,
    @SerializedName("e") var longText: String? = null
)

/**
 * SymbolUiSettings 的核心实现。
 *
 * @author android_zero
 * @github msmt2018/zero-Symbol-input-view
 */
data class SymbolUiSettings(
    val collapsedRows: Int = 2,
    val symbolsPerRow: Int = 10,
    val indicatorStyle: Int = 0,
    val rememberExpanded: Boolean = false,
    val uniformGroupHeight: Boolean = true,
    val symbolTextSizeSp: Int = 8,
    val showDragHandle: Boolean = true,
    val enableAdvancedActions: Boolean = true,
    val rememberLastPage: Boolean = true
)
