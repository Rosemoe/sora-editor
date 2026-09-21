package android.zero.studio.widget.editor.symbolinput

/**
 * SymbolDefaults 的核心实现。
 *
 * @author android_zero
 * @github msmt2018/zero-Symbol-input-view
 */
object SymbolDefaults {

    /**
     * 执行 createFallbackGroups 方法。
     */
    fun createFallbackGroups(): MutableList<SymbolGroup> {
        return mutableListOf(
            SymbolGroup(
                "commonlyUsed", mutableListOf(
                    SymbolItem(18, "←", null, 16, null),
                    SymbolItem(20, "↑", null, 23, null),
                    SymbolItem(19, "→", null, 17, null),
                    SymbolItem(0, "\"", "\""),
                    SymbolItem(0, "'", "'"),
                    SymbolItem(0, ".", "."),
                    SymbolItem(0, ",", ","),
                    SymbolItem(0, "/", "/"),
                    SymbolItem(0, "//", "//"),
                    SymbolItem(0, ":", ":"),
                    SymbolItem(0, ";", ";"),
                    SymbolItem(21, "↓", null, 24, null),
                    SymbolItem(0, "#", "#"),
                    SymbolItem(0, "+", "+"),
                    SymbolItem(0, "-", "-"),
                    SymbolItem(0, "*", "*"),
                    SymbolItem(0, "=", "="),
                    SymbolItem(0, "|", "|"),
                    SymbolItem(0, "~", "~"),
                    SymbolItem(0, "(", "("),
                    SymbolItem(0, ")", ")"),
                    SymbolItem(0, "(\"", "(\""),
                    SymbolItem(0, "\")", "\")"),
                    SymbolItem(0, "{", "{"),
                    SymbolItem(0, "}", "}"),
                    SymbolItem(0, "()", "()", 0, "()"),
                    SymbolItem(0, "[]", "[]", 0, "[]"),
                    SymbolItem(0, "{}", "{}", 0, "{}"),
                    SymbolItem(0, "<", "<"),
                    SymbolItem(0, ">", ">"),
                    SymbolItem(0, "\\", "\\"),
                    SymbolItem(0, "$", "$"),
                    SymbolItem(0, "&", "&"),
                    SymbolItem(0, "/*", "/**"),
                    SymbolItem(0, "*/", "*/"),
                    SymbolItem(0, "\n", "\n"),
                    SymbolItem(0, "\t", "\t"),
                    SymbolItem(0, "\\", "\\"),
                    SymbolItem(22, "settings")
                )
            )
        )
    }

    /**
     * 执行 deepCopy 方法。
     */
    fun deepCopy(groups: List<SymbolGroup>): MutableList<SymbolGroup> {
        return groups.map { group ->
            SymbolGroup(
                group.name,
                group.items.map { item ->
                    SymbolItem(item.shortAction, item.display, item.shortText, item.longAction, item.longText)
                }.toMutableList()
            )
        }.toMutableList()
    }
}
