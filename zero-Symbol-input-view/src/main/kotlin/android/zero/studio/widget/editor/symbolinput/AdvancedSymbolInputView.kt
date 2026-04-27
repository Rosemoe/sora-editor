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
    private val fullTabHeightPx by lazy { (44 * resources.displayMetrics.density).roundToInt() }
    private var collapsedHeightPx = rowHeightPx * 2 + (20 * resources.displayMetrics.density).roundToInt()
    private var expandedHeightPx = (220 * resources.displayMetrics.density).roundToInt()
    private val touchSlop by lazy { ViewConfiguration.get(context).scaledTouchSlop }

    private var initialY = 0f
    private var initialX = 0f
    private var lastY = 0f
    private var isDragging = false
    private var heightAnimator: ValueAnimator? = null
    private var lastSavedPageIndex = -1
    private var dotTabListenerAttached = false

    // 为兼容 MainActivity 旧代码提供空实现
    var followSystemIme: Boolean = false

    init {
        orientation = VERTICAL
        val root = LayoutInflater.from(context).inflate(R.layout.view_advanced_symbol_input, this, true)
        viewPager = root.findViewById(R.id.symbol_view_pager)
        tabLayout = root.findViewById(R.id.symbol_tab_layout)
        tabRow = root.findViewById(R.id.tab_row)

        viewPager.adapter = pagerAdapter
        tabLayout.setupWithViewPager(viewPager)
        ensureDotTabSelectionListener()
        viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                if (uiSettings.rememberLastPage && lastSavedPageIndex != position) {
                    lastSavedPageIndex = position
                    SymbolDataManager.setLastPageIndex(context, position)
                }
                if (!uiSettings.uniformGroupHeight) {
                    recalculateHeights()
                }
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

    fun onHostResume() {
        val shouldExpand = uiSettings.rememberExpanded && SymbolDataManager.getLastExpanded(context)
        animateToHeight(if (shouldExpand) expandedHeightPx else collapsedHeightPx)
    }

    fun bindEditor(editor: CodeEditor) {
        this.editor = editor
    }

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

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        context.getSharedPreferences("advanced_symbol_prefs", Context.MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(prefsListener)
        applyIndicatorStyle()
    }

    override fun onDetachedFromWindow() {
        context.getSharedPreferences("advanced_symbol_prefs", Context.MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(prefsListener)
        super.onDetachedFromWindow()
    }

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




    private fun recalculateHeights() {
        collapsedHeightPx = rowHeightPx * uiSettings.collapsedRows.coerceAtLeast(1) + (20 * resources.displayMetrics.density).roundToInt()
        val baseExpanded = (220 * resources.displayMetrics.density).roundToInt()
        expandedHeightPx = if (uiSettings.uniformGroupHeight) {
            groups.maxOfOrNull { calculateExpandedHeightForGroup(it) }?.coerceAtLeast(baseExpanded) ?: baseExpanded
        } else {
            val current = groups.getOrNull(viewPager.currentItem)
            (current?.let(::calculateExpandedHeightForGroup) ?: baseExpanded).coerceAtLeast(baseExpanded)
        }
        val currentHeight = viewPager.layoutParams.height
        updatePagerHeight(currentHeight.coerceIn(collapsedHeightPx, expandedHeightPx))
    }

    private fun calculateExpandedHeightForGroup(group: SymbolGroup): Int {
        val cols = uiSettings.symbolsPerRow.coerceIn(1, 20)
        val rows = (group.items.size + cols - 1) / cols
        val itemHeight = (44 * resources.displayMetrics.density).roundToInt()
        val verticalPadding = (20 * resources.displayMetrics.density).roundToInt()
        return (rows.coerceAtLeast(2) * itemHeight) + verticalPadding + fullTabHeightPx
    }

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

    private fun ensureDotTabSelectionListener() {
        if (dotTabListenerAttached) return
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) = updateDotTabState(tab, true)
            override fun onTabUnselected(tab: TabLayout.Tab) = updateDotTabState(tab, false)
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
        dotTabListenerAttached = true
    }

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

    private fun fetchColor(attr: Int): Int {
        val value = TypedValue()
        context.theme.resolveAttribute(attr, value, true)
        return if (value.resourceId != 0) context.getColor(value.resourceId) else value.data
    }

    private inner class SymbolPagerAdapter : PagerAdapter() {

        override fun getCount(): Int = groups.size

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return view === `object`
        }

        override fun getPageTitle(position: Int): CharSequence {
            return groups[position].name
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val group = groups[position]

            val scrollView = NestedScrollView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                isFillViewport = true
                overScrollMode = OVER_SCROLL_NEVER
            }

            val gridLayout = GridLayout(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
                columnCount = group.items.size.coerceAtMost(uiSettings.symbolsPerRow.coerceIn(1, 20)).coerceAtLeast(1)
                val padding = (6 * resources.displayMetrics.density).roundToInt()
                setPadding(padding, padding, padding, padding)
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

            scrollView.addView(gridLayout)
            container.addView(scrollView)
            return scrollView
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View)
        }
        
        override fun getItemPosition(`object`: Any): Int {
            return POSITION_NONE // 强制刷新
        }
    }
}
