package android.zero.studio.widget.editor.symbolinput

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * SymbolDataManager 的核心实现。
 *
 * @author android_zero
 * @github msmt2018/zero-Symbol-input-view
 */
object SymbolDataManager {
    private const val PREFS_NAME = "advanced_symbol_prefs"
    private const val KEY_DATA = "symbol_json_data"
    private const val KEY_COLLAPSED_ROWS = "symbol_collapsed_rows"
    private const val KEY_SYMBOLS_PER_ROW = "symbol_per_row"
    private const val KEY_INDICATOR_STYLE = "symbol_indicator_style"
    private const val KEY_REMEMBER_EXPANDED = "symbol_remember_expanded"
    private const val KEY_UNIFORM_GROUP_HEIGHT = "symbol_uniform_group_height"
    private const val KEY_TEXT_SIZE = "symbol_text_size_sp"
    private const val KEY_SHOW_DRAG_HANDLE = "symbol_show_drag_handle"
    private const val KEY_ADVANCED_ACTIONS = "symbol_enable_advanced_actions"
    private const val KEY_REMEMBER_LAST_PAGE = "symbol_remember_last_page"
    private const val KEY_LAST_EXPANDED = "symbol_last_expanded"
    private const val KEY_LAST_PAGE_INDEX = "symbol_last_page_index"
    val gson = Gson()

    /**
     * 加载符号数据。优先从 SharedPreferences(用户自定义存储) 读取。
     * 若未找到（例如首次启动）则使用内置默认配置，并主动写入缓存，避免后续 I/O。
     */
    fun loadData(context: Context): MutableList<SymbolGroup> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_DATA, null)

        if (json.isNullOrEmpty()) {
            val defaults = SymbolDefaults.createFallbackGroups()
            saveData(context, defaults)
            return SymbolDefaults.deepCopy(defaults)
        }

        return try {
            val listType = object : TypeToken<MutableList<SymbolGroup>>() {}.type
            gson.fromJson<MutableList<SymbolGroup>>(json, listType)
                ?.takeIf { it.isNotEmpty() }
                ?: SymbolDefaults.createFallbackGroups().also { saveData(context, it) }
        } catch (e: Exception) {
            e.printStackTrace()
            SymbolDefaults.createFallbackGroups().also { saveData(context, it) }
        }
    }

/**
     * 保存用户修改后的数据
     */
    fun saveData(context: Context, data: List<SymbolGroup>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DATA, gson.toJson(data)).apply()
    }

    /**
     * 执行 getUiSettings 方法。
     */
    fun getUiSettings(context: Context): SymbolUiSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return SymbolUiSettings(
            collapsedRows = prefs.getInt(KEY_COLLAPSED_ROWS, 2).coerceIn(1, 10),
            symbolsPerRow = prefs.getInt(KEY_SYMBOLS_PER_ROW, 10).coerceIn(1, 20),
            indicatorStyle = prefs.getInt(KEY_INDICATOR_STYLE, 0).coerceIn(0, 4),
            rememberExpanded = prefs.getBoolean(KEY_REMEMBER_EXPANDED, false),
            uniformGroupHeight = prefs.getBoolean(KEY_UNIFORM_GROUP_HEIGHT, true),
            symbolTextSizeSp = prefs.getInt(KEY_TEXT_SIZE, 18).coerceIn(12, 28),
            showDragHandle = prefs.getBoolean(KEY_SHOW_DRAG_HANDLE, true),
            enableAdvancedActions = prefs.getBoolean(KEY_ADVANCED_ACTIONS, true),
            rememberLastPage = prefs.getBoolean(KEY_REMEMBER_LAST_PAGE, true)
        )
    }

    /**
     * 执行 saveUiSettings 方法。
     */
    fun saveUiSettings(context: Context, settings: SymbolUiSettings) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_COLLAPSED_ROWS, settings.collapsedRows.coerceIn(1, 10))
            .putInt(KEY_SYMBOLS_PER_ROW, settings.symbolsPerRow.coerceIn(1, 20))
            .putInt(KEY_INDICATOR_STYLE, settings.indicatorStyle.coerceIn(0, 4))
            .putBoolean(KEY_REMEMBER_EXPANDED, settings.rememberExpanded)
            .putBoolean(KEY_UNIFORM_GROUP_HEIGHT, settings.uniformGroupHeight)
            .putInt(KEY_TEXT_SIZE, settings.symbolTextSizeSp.coerceIn(12, 28))
            .putBoolean(KEY_SHOW_DRAG_HANDLE, settings.showDragHandle)
            .putBoolean(KEY_ADVANCED_ACTIONS, settings.enableAdvancedActions)
            .putBoolean(KEY_REMEMBER_LAST_PAGE, settings.rememberLastPage)
            .apply()
    }

    /**
     * 执行 setLastExpanded 方法。
     */
    fun setLastExpanded(context: Context, expanded: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_LAST_EXPANDED, expanded).apply()
    }

    /**
     * 执行 getLastExpanded 方法。
     */
    fun getLastExpanded(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_LAST_EXPANDED, false)
    }

    /**
     * 执行 setLastPageIndex 方法。
     */
    fun setLastPageIndex(context: Context, index: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_LAST_PAGE_INDEX, index.coerceAtLeast(0)).apply()
    }

    /**
     * 执行 getLastPageIndex 方法。
     */
    fun getLastPageIndex(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_LAST_PAGE_INDEX, 0).coerceAtLeast(0)
    }


    /**
     * 执行 shouldTriggerUiRefresh 方法。
     */
    fun shouldTriggerUiRefresh(key: String?): Boolean {
        if (key.isNullOrEmpty()) return false
        return key in setOf(
            KEY_DATA,
            KEY_COLLAPSED_ROWS,
            KEY_SYMBOLS_PER_ROW,
            KEY_INDICATOR_STYLE,
            KEY_REMEMBER_EXPANDED,
            KEY_UNIFORM_GROUP_HEIGHT,
            KEY_TEXT_SIZE,
            KEY_SHOW_DRAG_HANDLE,
            KEY_ADVANCED_ACTIONS,
            KEY_REMEMBER_LAST_PAGE
        )
    }

    /**
     * 根据Action ID获取中文描述（用于副标题）
     */
    fun getActionDesc(context: Context, actionId: Int, text: String?): String {
        val values = context.resources.getIntArray(R.array.symbol_action_values)
        val names = context.resources.getStringArray(R.array.symbol_action_names)
        val index = values.indexOf(actionId)
        val baseName = if (index >= 0) names[index] else context.getString(R.string.action_unknown)
        return if (actionId == 0) "$baseName: ${text?.replace("\n", "\\n")}" else baseName
    }
}
