package android.zero.studio.widget.editor.symbolinput

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import io.github.rosemoe.sora.widget.CodeEditor
import kotlin.math.roundToInt

/**
 * MT 风格的符号输入控件：
 * Root(ViewGroup)
 *  ├─ GroupIndicatorBar(HorizontalScrollView > LinearLayout > (LinearLayout > TextView))
 *  └─ SymbolPagerHost(ViewGroup > ViewPager > SymbolPageGridView)
 */
/**
 * 高级符号输入控件（纯代码构建）。
 *
 * ## 架构
 * - 顶层：`AdvancedSymbolInputView`（`ViewGroup`）
 * - 分组指示器：`GroupIndicatorBar`（`HorizontalScrollView > LinearLayout > TextView`）
 * - 分页容器：`SymbolPagerHost`（`ViewPager`）
 * - 页内网格：`SymbolPageGridView`（自定义网格测量与布局）
 *
 * ## 设计目标
 * 1. 避免 XML 多层嵌套，改为轻量级代码布局；
 * 2. 将“折叠/展开高度”与网格行高统一到同一计算模型；
 * 3. 通过设置项控制固定行数、每行符号数以及分页切换体验。
  *
 * @author android_zero
 * @github msmt2018/zero-Symbol-input-view
 */
class AdvancedSymbolInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private val indicatorBar = GroupIndicatorBar(context) { index -> pagerHost.setCurrentPage(index, true) }
    private val bottomIndicator = CompactPageIndicator(context) { index -> pagerHost.setCurrentPage(index, true) }
    private val pagerHost = SymbolPagerHost(context)
    private val pageAdapter = SymbolPagerAdapter()

    private var editor: CodeEditor? = null
    var onOpenManagerListener: (() -> Unit)? = null
    var followSystemIme: Boolean = false

    private val groups = mutableListOf<SymbolGroup>()
    private var uiSettings = SymbolUiSettings()

    private val gridCellHeightPx by lazy { dp(28) }
    private val gridHorizontalGapPx by lazy { dp(2) }
    private val gridVerticalGapPx by lazy { dp(2) }
    private val gridVerticalPaddingPx by lazy { dp(4) }
    private var collapsedHeightPx = calculateGridHeight(2)
    private var expandedHeightPx = calculateGridHeight(4)

    private val touchSlop by lazy { ViewConfiguration.get(context).scaledTouchSlop }
    private var initialY = 0f
    private var initialX = 0f
    private var lastY = 0f
    private var isDragging = false
    private var heightAnimator: ValueAnimator? = null
    private var lastSavedPageIndex = -1

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (SymbolDataManager.shouldTriggerUiRefresh(key)) refreshData()
    }

    init {
        addView(indicatorBar, LayoutParams(LayoutParams.MATCH_PARENT, dp(26)))
        addView(pagerHost, LayoutParams(LayoutParams.MATCH_PARENT, collapsedHeightPx))
        addView(bottomIndicator, LayoutParams(LayoutParams.MATCH_PARENT, dp(14)))
        pagerHost.bindAdapter(pageAdapter)
        pagerHost.onPageChanged = { page ->
            indicatorBar.setSelectedIndex(page)
            bottomIndicator.setSelectedIndex(page)
            if (uiSettings.rememberLastPage && lastSavedPageIndex != page) {
                lastSavedPageIndex = page
                SymbolDataManager.setLastPageIndex(context, page)
            }
            recalculateHeights()
        }
        refreshData()
        applyIndicatorReveal(0f)
    }

    /**
     * 与旧调用链兼容的占位方法。
     *
     * 当前版本由控件内部管理展开/折叠，不再依赖外部 `BottomSheetBehavior`。
     */
    fun setupWithBottomSheet(rootView: View, bottomSheet: View, followView: View? = null) = Unit

    /** 绑定编辑器实例，用于执行符号插入动作。 */
    fun bindEditor(editor: CodeEditor) {
        this.editor = editor
    }

    /** 页面恢复时按设置恢复抽屉展开状态。 */
    fun onHostResume() {
        val shouldExpand = uiSettings.rememberExpanded && SymbolDataManager.getLastExpanded(context)
        animateToHeight(if (shouldExpand) expandedHeightPx else collapsedHeightPx)
    }

    /**
     * 重新加载分组与样式配置，并刷新分页与指示器。
     */
    fun refreshData() {
        uiSettings = SymbolDataManager.getUiSettings(context)

        groups.clear()
        groups.addAll(SymbolDataManager.loadData(context).filter { it.items.isNotEmpty() })
        if (groups.isEmpty()) {
            val defaults = SymbolDefaults.createFallbackGroups()
            groups.addAll(defaults)
            SymbolDataManager.saveData(context, defaults)
        }

        indicatorBar.submitGroups(groups, uiSettings.indicatorStyle)
        bottomIndicator.submitCount(groups.size, uiSettings.indicatorStyle)
        pageAdapter.notifyDataSetChanged()

        if (groups.isNotEmpty()) {
            val target = if (uiSettings.rememberLastPage) {
                SymbolDataManager.getLastPageIndex(context).coerceIn(0, groups.lastIndex)
            } else 0
            pagerHost.setCurrentPage(target, false)
            indicatorBar.setSelectedIndex(target)
            bottomIndicator.setSelectedIndex(target)
            lastSavedPageIndex = target
        }

        recalculateHeights()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        context.getSharedPreferences("advanced_symbol_prefs", Context.MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(prefsListener)
    }

    override fun onDetachedFromWindow() {
        context.getSharedPreferences("advanced_symbol_prefs", Context.MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(prefsListener)
        super.onDetachedFromWindow()
    }

    /**
     * 计算网格尺寸。
     *
     * 关键点：
     * - `cols` 来自设置中的每行符号数量；
     * - 行高固定为 `itemCellHeightPx`；
     * - 总高度 = 上下内边距 + 行高总和 + 行间距总和。
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val indicatorHeight = indicatorBar.layoutParams.height.coerceAtLeast(0)
        measureChild(indicatorBar, widthMeasureSpec, MeasureSpec.makeMeasureSpec(indicatorHeight, MeasureSpec.EXACTLY))

        val pagerHeight = pagerHost.layoutParams.height.coerceAtLeast(collapsedHeightPx)
        pagerHost.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(pagerHeight, MeasureSpec.EXACTLY)
        )

        val bottomHeight = bottomIndicator.layoutParams.height.coerceAtLeast(0)
        measureChild(bottomIndicator, widthMeasureSpec, MeasureSpec.makeMeasureSpec(bottomHeight, MeasureSpec.EXACTLY))

        setMeasuredDimension(width, indicatorBar.measuredHeight + pagerHost.measuredHeight + bottomIndicator.measuredHeight)
    }

    /**
     * 根据行列索引将符号项定位到对应单元格。
     */
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val width = r - l
        val indicatorHeight = indicatorBar.measuredHeight
        indicatorBar.layout(0, 0, width, indicatorHeight)
        val pagerBottom = indicatorHeight + pagerHost.measuredHeight
        pagerHost.layout(0, indicatorHeight, width, pagerBottom)
        bottomIndicator.layout(0, pagerBottom, width, pagerBottom + bottomIndicator.measuredHeight)
    }

    override fun generateLayoutParams(attrs: AttributeSet): LayoutParams = MarginLayoutParams(context, attrs)
    override fun generateDefaultLayoutParams(): LayoutParams = MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    override fun generateLayoutParams(p: LayoutParams): LayoutParams = MarginLayoutParams(p)

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialY = ev.rawY
                lastY = ev.rawY
                initialX = ev.rawX
                isDragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dy = ev.rawY - initialY
                val dx = ev.rawX - initialX
                if (!isDragging && expandedHeightPx > collapsedHeightPx && kotlin.math.abs(dy) > touchSlop && kotlin.math.abs(dy) > kotlin.math.abs(dx)) {
                    isDragging = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> isDragging = false
        }
        return super.onInterceptTouchEvent(ev)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                heightAnimator?.cancel()
                initialY = event.rawY
                lastY = event.rawY
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isDragging) return super.onTouchEvent(event)
                val deltaY = event.rawY - lastY
                val nextHeight = (pagerHost.layoutParams.height - deltaY.toInt()).coerceIn(collapsedHeightPx, expandedHeightPx)
                updatePagerHeight(nextHeight)
                lastY = event.rawY
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    val midpoint = (collapsedHeightPx + expandedHeightPx) / 2
                    val target = if (pagerHost.layoutParams.height >= midpoint) expandedHeightPx else collapsedHeightPx
                    if (uiSettings.rememberExpanded) {
                        SymbolDataManager.setLastExpanded(context, target == expandedHeightPx)
                    }
                    animateToHeight(target)
                }
                isDragging = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun animateToHeight(targetHeight: Int) {
        val currentHeight = pagerHost.layoutParams.height.coerceAtLeast(collapsedHeightPx)
        if (currentHeight == targetHeight) return
        heightAnimator?.cancel()
        heightAnimator = ValueAnimator.ofInt(currentHeight, targetHeight).apply {
            duration = 200
            addUpdateListener { updatePagerHeight(it.animatedValue as Int) }
            start()
        }
    }

    private fun updatePagerHeight(height: Int) {
        val clamped = height.coerceIn(collapsedHeightPx, expandedHeightPx)
        if (pagerHost.layoutParams.height != clamped) {
            pagerHost.layoutParams = pagerHost.layoutParams.apply { this.height = clamped }
            requestLayout()
        }
        val fraction = (clamped - collapsedHeightPx).toFloat() / (expandedHeightPx - collapsedHeightPx).coerceAtLeast(1)
        applyIndicatorReveal(fraction)
    }

    private fun applyIndicatorReveal(fraction: Float) {
        val reveal = ((fraction - 0.08f) / 0.47f).coerceIn(0f, 1f)
        indicatorBar.applyReveal(reveal, dp(26), uiSettings.indicatorStyle)
        bottomIndicator.applyReveal(reveal, uiSettings.indicatorStyle)
        requestLayout()
    }

    /**
     * 依据当前配置与页面数据重新计算折叠/展开高度。
     *
     * - 折叠高度：严格按 `collapsedRows` 计算；
     * - 展开高度：按当前页的真实行数计算；
     * - 两者都使用同一网格高度公式，避免出现“设置 2 行却显示 3 行”的漂移问题。
     */
    private fun recalculateHeights() {
        val collapsedRows = uiSettings.collapsedRows.coerceAtLeast(1)
        collapsedHeightPx = calculateGridHeight(collapsedRows)

        val cols = uiSettings.symbolsPerRow.coerceIn(1, 20)
        val maxPageRows = groups.maxOfOrNull { group ->
            ((group.items.size + cols - 1) / cols).coerceAtLeast(1)
        } ?: collapsedRows
        expandedHeightPx = calculateGridHeight(maxPageRows.coerceAtLeast(collapsedRows))

        updatePagerHeight(pagerHost.layoutParams.height.coerceIn(collapsedHeightPx, expandedHeightPx))
    }

    private fun calculateGridHeight(rows: Int): Int {
        val safeRows = rows.coerceAtLeast(1)
        return (safeRows * gridCellHeightPx) + ((safeRows - 1) * gridVerticalGapPx) + (gridVerticalPaddingPx * 2)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).roundToInt()

    private inner class SymbolPagerAdapter : PagerAdapter() {
        override fun getCount(): Int = groups.size
        override fun isViewFromObject(view: View, `object`: Any): Boolean = view === `object`

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val group = groups[position]
            val pageView = SymbolPageGridView(context) { item, isLong ->
                if (isLong) {
                    val action = item.longAction
                    if (action != null) {
                        editor?.let {
                            SymbolActionExecutor.execute(it, action, item.longText, onOpenManagerListener)
                        }
                    }
                } else {
                    editor?.let {
                        SymbolActionExecutor.execute(it, item.shortAction, item.shortText, onOpenManagerListener)
                    }
                }
            }
            pageView.updateConfig(uiSettings)
            pageView.submit(group.items)
            container.addView(pageView)
            return pageView
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View)
        }

        override fun getItemPosition(`object`: Any): Int = POSITION_NONE
    }
}

/**
 * 分组指示器组件。
 *
 * 使用横向滚动容器承载多个分组项，负责：
 * - 渲染分组标题；
 * - 选中态高亮与自动滚动居中；
 * - 展开过程中的显隐与位移动画。
  *
 * @author android_zero
 * @github msmt2018/zero-Symbol-input-view
 */
private class GroupIndicatorBar(
    context: Context,
    private val onGroupClicked: (Int) -> Unit
) : ViewGroup(context) {

    private val scroll = HorizontalScrollView(context).apply {
        overScrollMode = OVER_SCROLL_NEVER
        isHorizontalScrollBarEnabled = false
    }
    private val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }

    init {
        scroll.addView(row, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun submitGroups(groups: List<SymbolGroup>, style: Int) {
        row.removeAllViews()
        groups.forEachIndexed { index, group ->
            val item = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setOnClickListener { onGroupClicked(index) }
            }
            if (style == 3) {
                item.setPadding(dp(6), dp(8), dp(6), dp(8))
                val line = View(context).apply {
                    alpha = 0.55f
                }
                item.addView(line, LinearLayout.LayoutParams(dp(22), dp(3)).apply { gravity = Gravity.CENTER })
                row.addView(item, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
            } else {
                item.setPadding(dp(8), dp(6), dp(8), dp(6))
                item.addView(TextView(context).apply {
                    text = group.name
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                })
                row.addView(item, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
            }
        }
    }

    fun setSelectedIndex(index: Int) {
        for (i in 0 until row.childCount) {
            val child = row.getChildAt(i)
            child.alpha = if (i == index) 1f else 0.6f
            if (child is LinearLayout && child.childCount > 0 && child.getChildAt(0) is View && child.getChildAt(0) !is TextView) {
                val line = child.getChildAt(0)
                val selected = i == index
                line.alpha = if (selected) 1f else 0.45f
                line.background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    cornerRadius = dp(2).toFloat()
                    setColor(if (selected) 0xFFFFFFFF.toInt() else 0x99FFFFFF.toInt())
                }
            }
        }
        row.getChildAt(index)?.let { child ->
            val center = child.left + child.width / 2
            scroll.smoothScrollTo((center - width / 2).coerceAtLeast(0), 0)
        }
    }

    fun applyReveal(fraction: Float, fullHeight: Int, style: Int) {
        if (style == 1 || style == 4 || style == 2) {
            visibility = View.GONE
            layoutParams = layoutParams.apply { height = 0 }
            return
        }
        alpha = fraction
        translationY = (1f - fraction) * -6f * resources.displayMetrics.density
        layoutParams = layoutParams.apply { height = (fullHeight * fraction).roundToInt() }
        visibility = if (fraction <= 0f) View.INVISIBLE else View.VISIBLE
    }

    /**
     * 计算网格尺寸。
     *
     * 关键点：
     * - `cols` 来自设置中的每行符号数量；
     * - 行高固定为 `itemCellHeightPx`；
     * - 总高度 = 上下内边距 + 行高总和 + 行间距总和。
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        scroll.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
        setMeasuredDimension(width, height)
    }

    /**
     * 根据行列索引将符号项定位到对应单元格。
     */
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        scroll.layout(0, 0, r - l, b - t)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()
}


private class CompactPageIndicator(
    context: Context,
    private val onItemClicked: (Int) -> Unit
) : ViewGroup(context) {

    private var style: Int = 2

    fun submitCount(count: Int, style: Int) {
        this.style = style
        removeAllViews()
        if (style != 1 && style != 4) return
        repeat(count) { index ->
            addView(View(context).apply {
                setOnClickListener { onItemClicked(index) }
            }, LayoutParams(dp(8), dp(8)))
        }
    }

    fun setSelectedIndex(index: Int) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val selected = i == index
            val size = if (selected) dp(8) else dp(6)
            child.layoutParams = child.layoutParams.apply { width = size; height = size }
            child.alpha = if (selected) 1f else 0.55f
            val color = if (selected) 0xFFFFFFFF.toInt() else 0x99FFFFFF.toInt()
            child.background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = if (style == 1) size / 2f else dp(1).toFloat()
                setColor(color)
            }
        }
    }

    fun applyReveal(fraction: Float, style: Int) {
        val showCompact = style == 1 || style == 4
        visibility = if (showCompact) View.VISIBLE else View.GONE
        layoutParams = layoutParams.apply { height = if (showCompact) dp(14) else 0 }
        // Compact indicators should remain visible at drawer bottom for these styles,
        // instead of being tied to top-tab reveal progress.
        alpha = if (showCompact) 1f else 0f
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        for (i in 0 until childCount) {
            val c = getChildAt(i)
            measureChild(c, MeasureSpec.makeMeasureSpec(dp(8), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(dp(8), MeasureSpec.EXACTLY))
        }
        setMeasuredDimension(width, h)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val total = childCount * dp(8) + (childCount - 1).coerceAtLeast(0) * dp(6)
        var left = ((r - l - total) / 2).coerceAtLeast(0)
        val top = ((b - t - dp(8)) / 2).coerceAtLeast(0)
        for (i in 0 until childCount) {
            val c = getChildAt(i)
            c.layout(left, top, left + c.measuredWidth, top + c.measuredHeight)
            left += c.measuredWidth + dp(6)
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()
}

/**
 * 分页承载组件，职责为包装 `ViewPager` 并向外暴露最小化 API。
  *
 * @author android_zero
 * @github msmt2018/zero-Symbol-input-view
 */
private class SymbolPagerHost(context: Context) : ViewGroup(context) {
    private val pager = ViewPager(context).apply { overScrollMode = OVER_SCROLL_NEVER }
    var onPageChanged: ((Int) -> Unit)? = null
    val currentPage: Int get() = pager.currentItem

    init {
        addView(pager, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        pager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                onPageChanged?.invoke(position)
            }
        })
    }

    fun bindAdapter(adapter: PagerAdapter) {
        pager.adapter = adapter
    }

    fun setCurrentPage(page: Int, smooth: Boolean) {
        pager.setCurrentItem(page, smooth)
    }

    /**
     * 计算网格尺寸。
     *
     * 关键点：
     * - `cols` 来自设置中的每行符号数量；
     * - 行高固定为 `itemCellHeightPx`；
     * - 总高度 = 上下内边距 + 行高总和 + 行间距总和。
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        pager.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
        setMeasuredDimension(width, height)
    }

    /**
     * 根据行列索引将符号项定位到对应单元格。
     */
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        pager.layout(0, 0, r - l, b - t)
    }
}

/**
 * 分页内符号网格组件。
 *
 * 采用自定义测量与布局逻辑，支持：
 * - 固定每行符号数；
 * - 紧凑的行高/间距控制；
 * - 点击与长按动作分发。
  *
 * @author android_zero
 * @github msmt2018/zero-Symbol-input-view
 */
private class SymbolPageGridView(
    context: Context,
    private val onItemTriggered: (SymbolItem, Boolean) -> Unit
) : ViewGroup(context) {

    private var settings = SymbolUiSettings()

    /** 更新页面渲染配置（行列、字体、交互策略等）。 */
    fun updateConfig(settings: SymbolUiSettings) {
        this.settings = settings
    }

    private val itemHorizontalGapPx: Int get() = dp(2)
    private val itemVerticalGapPx: Int get() = dp(2)
    private val itemCellHeightPx: Int get() = dp(28)

    /**
     * 提交当前页符号列表并重建子视图。
     *
     * @param items 当前分组下用于展示的符号项集合。
     */
    fun submit(items: List<SymbolItem>) {
        removeAllViews()
        setPadding(paddingLeft, dp(4), paddingRight, dp(4))
        items.forEach { item ->
            val tv = TextView(context).apply {
                text = item.display
                gravity = Gravity.CENTER
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                minHeight = dp(32)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, settings.symbolTextSizeSp.toFloat())
                TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                    this,
                    10,
                    settings.symbolTextSizeSp.coerceAtLeast(12),
                    1,
                    TypedValue.COMPLEX_UNIT_SP
                )
                val bg = TypedValue()
                context.theme.resolveAttribute(android.R.attr.selectableItemBackground, bg, true)
                setBackgroundResource(bg.resourceId)
                setOnClickListener { onItemTriggered(item, false) }
                setOnLongClickListener {
                    if (item.longAction != null) {
                        onItemTriggered(item, true)
                        true
                    } else {
                        false
                    }
                }
            }
            addView(tv)
        }
        requestLayout()
    }

    /**
     * 计算网格尺寸。
     *
     * 关键点：
     * - `cols` 来自设置中的每行符号数量；
     * - 行高固定为 `itemCellHeightPx`；
     * - 总高度 = 上下内边距 + 行高总和 + 行间距总和。
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val cols = settings.symbolsPerRow.coerceIn(1, 20)
        val totalContentWidth = (width - paddingLeft - paddingRight).coerceAtLeast(0)
        val gapX = itemHorizontalGapPx
        val gapY = itemVerticalGapPx
        val cellWidth = ((totalContentWidth - gapX * (cols - 1)).coerceAtLeast(0)) / cols
        val cellHeight = itemCellHeightPx
        val cw = MeasureSpec.makeMeasureSpec(cellWidth, MeasureSpec.EXACTLY)
        val ch = MeasureSpec.makeMeasureSpec(cellHeight, MeasureSpec.EXACTLY)

        repeat(childCount) { getChildAt(it).measure(cw, ch) }

        val rows = (childCount + cols - 1) / cols
        val totalHeight = paddingTop + paddingBottom + rows * cellHeight + (rows - 1).coerceAtLeast(0) * gapY
        setMeasuredDimension(width, totalHeight)
    }

    /**
     * 根据行列索引将符号项定位到对应单元格。
     */
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val cols = settings.symbolsPerRow.coerceIn(1, 20)
        val totalContentWidth = (width - paddingLeft - paddingRight).coerceAtLeast(0)
        val gapX = itemHorizontalGapPx
        val gapY = itemVerticalGapPx
        val cellWidth = ((totalContentWidth - gapX * (cols - 1)).coerceAtLeast(0)) / cols
        val cellHeight = itemCellHeightPx

        repeat(childCount) { index ->
            val row = index / cols
            val col = index % cols
            val left = paddingLeft + col * (cellWidth + gapX)
            val top = paddingTop + row * (cellHeight + gapY)
            getChildAt(index).layout(left, top, left + cellWidth, top + cellHeight)
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).roundToInt()
}
