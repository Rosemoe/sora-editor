# 符号输入

编辑器输入面板使用 Jetpack Compose：`SymbolInputPanel`、`HorizontalPager`、`ScrollableTabRow` 和 `LazyVerticalGrid`。旧 `AdvancedSymbolInputView`、Fragment 分页器及自定义 ViewGroup 输入容器已移除。

```kotlin
SymbolInputPanel(
    editor = currentSoraEditor, // null 时禁止输入
    onOpenManager = { startActivity(Intent(this, SymbolManagerActivity::class.java)) },
)
```

- 水平滑动或点击指示器切换用户分组，点击箭头展开/收起；箭头也支持垂直拖动。
- 保留符号 JSON、SharedPreferences、短按/长按动作及宏格式，由 `SymbolDataManager` 和 `SymbolActionExecutor` 处理。
- 读取每行数量、折叠行数、字号、统一分组高度、页面和展开状态记忆，以及五种页面指示器设置。
- 较大的分组在限制高度的网格中滚动，避免挤出编辑区域。
- `SymbolManagerActivity` 是独立的管理设置页面，继续维护分组增删、排序、导入导出和动作设置；其 XML 仍有调用方，予以保留。`showDragHandle` 和 `enableAdvancedActions` 属于该管理页面的选项。
- Sora 编辑引擎通过参数传入，本模块不重写引擎。

Compose BOM 和 Kotlin Compose 插件沿用仓库版本目录。
