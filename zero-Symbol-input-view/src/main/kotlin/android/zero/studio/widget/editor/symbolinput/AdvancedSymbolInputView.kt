package android.zero.studio.widget.editor.symbolinput

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.widget.TextViewCompat
import androidx.core.widget.NestedScrollView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import io.github.rosemoe.sora.widget.CodeEditor
import kotlin.math.roundToInt

/**
 * AdvancedSymbolInputView 的核心实现。
 *
 * @author android_zero
 * @github msmt2018/zero-Symbol-input-view
 */
class AdvancedSymbolInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val viewPager: ViewPager
    private val tabLayout: TabLayout
    private val tabRow: View

    private var editor: CodeEditor? = null
    var onOpenManagerListener: (() -> Unit)? = null

    private val groups = mutableListOf<SymbolGroup>()
    private val pagerAdapter = SymbolPagerAdapter()
    private var uiSettings = SymbolUiSettings()
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (SymbolDataManager.shouldTriggerUiRefresh(key)) {
            refreshData()
        }
    }

    private val rowHeightPx by lazy { (36 * resources.displayMetrics.density).roundToInt() }
    private val itemHeightPx by lazy { (44 * resources.displayMetrics.density).roundToInt() }
    private var fullTabHeightPx = (44 * resources.displayMetrics.density).roundToInt()
    private val collapsedExtraPaddingPx by lazy { (20 * resources.displayMetrics.density).roundToInt() }
    private val gridTopPaddingPx by lazy { (2 * resources.displayMetrics.density).roundToInt() }
    private val gridBottomPaddingPx by lazy { (8 * resources.displayMetrics.density).roundToInt() }
    private var collapsedHeightPx = rowHeightPx * 2 + collapsedExtraPaddingPx
    private var expandedHeightPx = (220 * resources.displayMetrics.density).roundToInt()
    private val touchSlop by lazy { ViewConfiguration.get(context).scaledTouchSlop }

    private var initialY = 0f
    private var initialX = 0f
    private var lastY = 0f
    private var isDragging = false
    private var heightAnimator: ValueAnimator? = null
    private var lastSavedPageIndex = -1
    private var dotTabListenerAttached = false
    private val expandedHeightCache = mutableMapOf<ExpandedHeightKey, Int>()

    /**
     * ExpandedHeightKey 的核心实现。
     *
     * @author android_zero
     * @github msmt2018/zero-Symbol-input-view
     */
    private data class ExpandedHeightKey(
        val pageIndex: Int,
        val itemCount: Int,
        val symbolsPerRow: Int
    )

    // 为兼容 MainActivity 旧代码提供空实现
    var followSystemIme: Boolean = false

    init {
        orientation = VERTICAL
        val root = LayoutInflater.from(context).inflate(R.layout.view_advanced_symbol_input, this, true)
        viewPager = root.findViewById(R.id.symbol_view_pager)
        tabLayout = root.findViewById(R.id.symbol_tab_layout)
        tabRow = root.findViewById(R.id.tab_row)
        fullTabHeightPx = tabRow.layoutParams.height
            .takeIf { it > 0 }
            ?: fullTabHeightPx

        viewPager.adapter = pagerAdapter
        tabLayout.setupWithViewPager(viewPager)
        ensureDotTabSelectionListener()
        viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            /**
             * 执行 onPageSelected 方法。
             */
            override fun onPageSelected(position: Int) {
                if (uiSettings.rememberLastPage && lastSavedPageIndex != position) {
                    lastSavedPageIndex = position
                    SymbolDataManager.setLastPageIndex(context, position)
                }
                recalculateHeights()
            }
        })

        updatePagerHeight(collapsedHeightPx)
        applyTabRowByFraction(0f)
        refreshData()
    }

    /**
     * 为兼容而保留的方法名，内部实际已不需要外部的 BottomSheet 支持
     */
    fun setupWithBottomSheet(rootView: View, bottomSheet: View, followView: View? = null) {
        // Do nothing. 我们现在依靠自己的手势和 RelativeLayout 机制。
    }

    /**
     * 执行 onHostResume 方法。
     */
    fun onHostResume() {
        val shouldExpand = uiSettings.rememberExpanded && SymbolDataManager.getLastExpanded(context)
        animateToHeight(if (shouldExpand) expandedHeightPx else collapsedHeightPx)
    }

    /**
     * 执行 bindEditor 方法。
     */
    fun bindEditor(editor: CodeEditor) {
        this.editor = editor
    }

    /**
     * 执行 refreshData 方法。
     */
    fun refreshData() {
        uiSettings = SymbolDataManager.getUiSettings(context)
        val newData = SymbolDataManager.loadData(context)
        groups.clear()
        groups.addAll(newData.filter { it.items.isNotEmpty() })
        if (groups.isEmpty()) {
            val defaults = SymbolDefaults.createFallbackGroups()
            groups.addAll(defaults)
            SymbolDataManager.saveData(context, defaults)
        }
        expandedHeightCache.clear()
        applyIndicatorStyle()
        recalculateHeights()
        pagerAdapter.notifyDataSetChanged()
        if (groups.isNotEmpty()) {
            val target = if (uiSettings.rememberLastPage) {
                SymbolDataManager.getLastPageIndex(context).coerceIn(0, groups.lastIndex)
            } else {
                0
            }
            viewPager.currentItem = target
            lastSavedPageIndex = target
        }
        tabLayout.post { applyIndicatorStyle() }
    }

    /**
     * 执行 onAttachedToWindow 方法。
     */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        context.getSharedPreferences("advanced_symbol_prefs", Context.MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(prefsListener)
        applyIndicatorStyle()
    }

    /**
     * 执行 onDetachedFromWindow 方法。
     */
    override fun onDetachedFromWindow() {
        context.getSharedPreferences("advanced_symbol_prefs", Context.MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(prefsListener)
        super.onDetachedFromWindow()
    }

    /**
     * 执行 onInterceptTouchEvent 方法。
     */
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialY = ev.rawY
                lastY = ev.rawY
                initialX = ev.rawX
                isDragging = false
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaY = ev.rawY - initialY
                val deltaX = ev.rawX - initialX
                if (!isDragging && kotlin.math.abs(deltaY) > touchSlop && kotlin.math.abs(deltaY) > kotlin.math.abs(deltaX)) {
                    isDragging = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    @SuppressLint("ClickableViewAccessibility")
    /**
     * 执行 onTouchEvent 方法。
     */
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
                val currentHeight = viewPager.layoutParams.height.coerceAtLeast(collapsedHeightPx)
                val nextHeight = (currentHeight - deltaY.toInt()).coerceIn(collapsedHeightPx, expandedHeightPx)
                updatePagerHeight(nextHeight)
                lastY = event.rawY
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    val currentHeight = viewPager.layoutParams.height.coerceAtLeast(collapsedHeightPx)
                    val midpoint = (collapsedHeightPx + expandedHeightPx) / 2
                    val targetHeight = if (currentHeight >= midpoint) expandedHeightPx else collapsedHeightPx
                    if (uiSettings.rememberExpanded) {
                        SymbolDataManager.setLastExpanded(context, targetHeight == expandedHeightPx)
                    }
                    animateToHeight(targetHeight)
                }
                isDragging = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    /**
     * 执行 animateToHeight 方法。
     */
    private fun animateToHeight(targetHeight: Int) {
        val currentHeight = viewPager.layoutParams.height.coerceAtLeast(collapsedHeightPx)
        if (currentHeight == targetHeight) return
        heightAnimator?.cancel()
        heightAnimator = ValueAnimator.ofInt(currentHeight, targetHeight).apply {
            duration = 200
            addUpdateListener { animation ->
                updatePagerHeight(animation.animatedValue as Int)
            }
            start()
        }
    }

    /**
     * 执行 updatePagerHeight 方法。
     */
    private fun updatePagerHeight(height: Int) {
        val clamped = height.coerceIn(collapsedHeightPx, expandedHeightPx)
        val params = viewPager.layoutParams
        if (params.height != clamped) {
            params.height = clamped
            viewPager.layoutParams = params
        }
        val range = (expandedHeightPx - collapsedHeightPx).coerceAtLeast(1)
        val fraction = (clamped - collapsedHeightPx).toFloat() / range.toFloat()
        applyTabRowByFraction(fraction)
    }

    /**
     * 执行 applyTabRowByFraction 方法。
     */
    private fun applyTabRowByFraction(fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        // Material 风格下采用“延迟开始、提前完成”的跟随曲线，避免 0%/100% 的突兀感
        // 例如抽屉到 10% 时，Tab 大约出现 4% 左右；到 55% 时基本完成显现
        val revealStart = 0.08f
        val revealEnd = 0.55f
        val revealProgress = ((clamped - revealStart) / (revealEnd - revealStart)).coerceIn(0f, 1f)

        val params = tabRow.layoutParams
        val rawHeight = (fullTabHeightPx * revealProgress).roundToInt()
        val quantizeStep = (2 * resources.displayMetrics.density).roundToInt().coerceAtLeast(1)
        val targetHeight = (rawHeight / quantizeStep) * quantizeStep
        if (params.height != targetHeight) {
            params.height = targetHeight
            tabRow.layoutParams = params
        }
        tabRow.alpha = revealProgress
        tabRow.translationY = (1f - revealProgress) * -6f * resources.displayMetrics.density
        tabRow.visibility = if (targetHeight == 0) View.INVISIBLE else View.VISIBLE
    }




    /**
     * 执行 recalculateHeights 方法。
     */
    private fun recalculateHeights() {
        collapsedHeightPx = rowHeightPx * uiSettings.collapsedRows.coerceAtLeast(1) + collapsedExtraPaddingPx
        val minExpanded = collapsedHeightPx + rowHeightPx
        val pageIndex = viewPager.currentItem
        expandedHeightPx = calculateExpandedHeightForPage(pageIndex).coerceAtLeast(minExpanded)
        val currentHeight = viewPager.layoutParams.height
        updatePagerHeight(currentHeight.coerceIn(collapsedHeightPx, expandedHeightPx))
    }

    /**
     * 执行 calculateExpandedHeightForPage 方法。
     */
    private fun calculateExpandedHeightForPage(pageIndex: Int): Int {
        val cols = uiSettings.symbolsPerRow.coerceIn(1, 20)
        val group = groups.getOrNull(pageIndex) ?: return collapsedHeightPx + rowHeightPx
        val key = ExpandedHeightKey(
            pageIndex = pageIndex,
            itemCount = group.items.size,
            symbolsPerRow = cols
        )
        expandedHeightCache[key]?.let { return it }
        val rows = (group.items.size + cols - 1) / cols
        return ((rows.coerceAtLeast(2) * itemHeightPx) + gridTopPaddingPx + gridBottomPaddingPx)
            .also { expandedHeightCache[key] = it }
    }

    /**
     * 执行 applyIndicatorStyle 方法。
     */
    private fun applyIndicatorStyle() {
        val accent = fetchColor(android.R.attr.colorAccent)
        tabLayout.isInlineLabel = false
        tabLayout.tabMode = TabLayout.MODE_SCROLLABLE
        tabLayout.setTabIndicatorFullWidth(false)
        tabLayout.setSelectedTabIndicator(ColorDrawable(accent))
        tabLayout.setSelectedTabIndicatorColor(accent)
        tabLayout.setSelectedTabIndicatorHeight((2 * resources.displayMetrics.density).roundToInt())
        tabLayout.setSelectedTabIndicatorGravity(TabLayout.INDICATOR_GRAVITY_BOTTOM)

        when (uiSettings.indicatorStyle) {
            0 -> Unit // 标准
            1 -> {
                // 简洁风格：采用页码点样式，不显示 Tab 文本与默认 TabLayout 指示条
                tabLayout.tabMode = TabLayout.MODE_FIXED
                tabLayout.setSelectedTabIndicator(ColorDrawable(0))
                tabLayout.setSelectedTabIndicatorHeight(0)
            }
            2 -> {
                tabLayout.setSelectedTabIndicator(ColorDrawable(0))
                tabLayout.setSelectedTabIndicatorHeight(0)
            }
            3 -> {
                tabLayout.setSelectedTabIndicator(ColorDrawable(accent))
                tabLayout.setSelectedTabIndicatorHeight((3 * resources.displayMetrics.density).roundToInt())
                tabLayout.setSelectedTabIndicatorGravity(TabLayout.INDICATOR_GRAVITY_TOP)
            }
            4 -> {
                tabLayout.setTabIndicatorFullWidth(true)
                tabLayout.setSelectedTabIndicator(R.drawable.bg_indicator_block)
                tabLayout.setSelectedTabIndicatorGravity(TabLayout.INDICATOR_GRAVITY_STRETCH)
            }
        }
        applyTabItemPresentation()
    }

    /**
     * 执行 ensureDotTabSelectionListener 方法。
     */
    private fun ensureDotTabSelectionListener() {
        if (dotTabListenerAttached) return
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            /**
             * 执行 onTabSelected 方法。
             */
            override fun onTabSelected(tab: TabLayout.Tab) = updateDotTabState(tab, true)
            /**
             * 执行 onTabUnselected 方法。
             */
            override fun onTabUnselected(tab: TabLayout.Tab) = updateDotTabState(tab, false)
            /**
             * 执行 onTabReselected 方法。
             */
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
        dotTabListenerAttached = true
    }

    /**
     * 执行 applyTabItemPresentation 方法。
     */
    private fun applyTabItemPresentation() {
        val isSimpleDots = uiSettings.indicatorStyle == 1
        for (index in 0 until tabLayout.tabCount) {
            val tab = tabLayout.getTabAt(index) ?: continue
            if (isSimpleDots) {
                if (tab.customView == null) {
                    tab.customView = createDotTabView()
                }
                updateDotTabState(tab, tab.isSelected)
                tab.contentDescription = groups.getOrNull(index)?.name
            } else {
                tab.customView = null
            }
        }
    }

    /**
     * 执行 createDotTabView 方法。
     */
    private fun createDotTabView(): View {
        val dot = View(context)
        dot.layoutParams = LinearLayout.LayoutParams(
            (8 * resources.displayMetrics.density).roundToInt(),
            (8 * resources.displayMetrics.density).roundToInt()
        ).apply {
            leftMargin = (4 * resources.displayMetrics.density).roundToInt()
            rightMargin = (4 * resources.displayMetrics.density).roundToInt()
            gravity = Gravity.CENTER
        }
        dot.setBackgroundResource(R.drawable.bg_page_indicator_dot)
        return dot
    }

    /**
     * 执行 updateDotTabState 方法。
     */
    private fun updateDotTabState(tab: TabLayout.Tab, selected: Boolean) {
        if (uiSettings.indicatorStyle != 1) return
        val dot = tab.customView ?: return
        val params = dot.layoutParams as? LinearLayout.LayoutParams ?: return
        val width = ((if (selected) 18 else 8) * resources.displayMetrics.density).roundToInt()
        if (params.width != width) {
            params.width = width
            dot.layoutParams = params
        }
        dot.alpha = if (selected) 1f else 0.65f
        dot.setBackgroundResource(
            if (selected) R.drawable.bg_page_indicator_capsule else R.drawable.bg_page_indicator_dot
        )
    }

    /**
     * 执行 fetchColor 方法。
     */
    private fun fetchColor(attr: Int): Int {
        val value = TypedValue()
        context.theme.resolveAttribute(attr, value, true)
        return if (value.resourceId != 0) context.getColor(value.resourceId) else value.data
    }

    private inner class SymbolPagerAdapter : PagerAdapter() {

        /**
         * 执行 getCount 方法。
         */
        override fun getCount(): Int = groups.size

        /**
         * 执行 isViewFromObject 方法。
         */
        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return view === `object`
        }

        /**
         * 执行 getPageTitle 方法。
         */
        override fun getPageTitle(position: Int): CharSequence {
            return groups[position].name
        }

        /**
         * 执行 instantiateItem 方法。
         */
        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val group = groups[position]

            val scrollView = NestedScrollView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                
                // 设置为false:避免内容被强制拉伸到整页高度，导致顶部出现空白
                isFillViewport = false
                overScrollMode = OVER_SCROLL_NEVER
            }

            val contentContainer = FrameLayout(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val gridLayout = GridLayout(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM
                )
                columnCount = group.items.size.coerceAtMost(uiSettings.symbolsPerRow.coerceIn(1, 20)).coerceAtLeast(1)
                val horizontalPadding = (6 * resources.displayMetrics.density).roundToInt()
                setPadding(horizontalPadding, gridTopPaddingPx, horizontalPadding, gridBottomPaddingPx)
            }

            for (item in group.items) {
                val tv = AppCompatTextView(context).apply {
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 0
                        height = ViewGroup.LayoutParams.WRAP_CONTENT
                        columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                        setMargins(4, 4, 4, 4)
                    }
                    minHeight = (36 * resources.displayMetrics.density).roundToInt()
                    gravity = Gravity.CENTER
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, uiSettings.symbolTextSizeSp.toFloat())
                    text = item.display
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                    isClickable = true
                    isFocusable = true
                    TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                        this,
                        10,
                        uiSettings.symbolTextSizeSp.coerceAtLeast(12),
                        1,
                        TypedValue.COMPLEX_UNIT_SP
                    )

                    val tvColor = TypedValue()
                    context.theme.resolveAttribute(android.R.attr.textColorPrimary, tvColor, true)
                    setTextColor(if (tvColor.resourceId != 0) context.getColor(tvColor.resourceId) else tvColor.data)

                    val tvBg = TypedValue()
                    context.theme.resolveAttribute(android.R.attr.selectableItemBackground, tvBg, true)
                    setBackgroundResource(tvBg.resourceId)

                    setOnClickListener {
                        editor?.let { ed ->
                            SymbolActionExecutor.execute(ed, item.shortAction, item.shortText, onOpenManagerListener)
                        }
                    }

                    setOnLongClickListener {
                        if (item.longAction != null) {
                            editor?.let { ed ->
                                SymbolActionExecutor.execute(ed, item.longAction!!, item.longText, onOpenManagerListener)
                            }
                            true
                        } else false
                    }
                }
                gridLayout.addView(tv)
            }

            contentContainer.addView(gridLayout)
            scrollView.addView(contentContainer)
            container.addView(scrollView)
            return scrollView
        }

        /**
         * 执行 destroyItem 方法。
         */
        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View)
        }
        
        /**
         * 执行 getItemPosition 方法。
         */
        override fun getItemPosition(`object`: Any): Int {
            return POSITION_NONE // 强制刷新
        }
    }
}
