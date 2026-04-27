package android.zero.studio.widget.editor.symbolinput

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

abstract class BottomSheetTabPagerContainer @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    protected val viewPager: ViewPager2
    protected val tabLayout: TabLayout
    protected val tabRow: View

    private var tabMediator: TabLayoutMediator? = null
    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null
    private var registeredBottomSheetCallback: BottomSheetBehavior.BottomSheetCallback? = null

    init {
        val root = LayoutInflater.from(context).inflate(R.layout.view_advanced_symbol_input, this, true)
        viewPager = root.findViewById(R.id.symbol_view_pager)
        tabLayout = root.findViewById(R.id.symbol_tab_layout)
        tabRow = root.findViewById(R.id.tab_row)

        viewPager.offscreenPageLimit = 1
        viewPager.isSaveEnabled = false
        setExpansionFraction(0f)
    }

    /**
     * 使用 Alpha 与 TranslationY 实现极高帧率的抽屉伸缩切换
     */
    protected fun setExpansionFraction(fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        if (clamped <= 0f) {
            tabRow.visibility = View.GONE
        } else {
            tabRow.visibility = View.VISIBLE
            tabRow.alpha = clamped
            tabRow.translationY = (1f - clamped) * -8f * resources.displayMetrics.density
        }
    }

    /**
     * 接管并挂载到底部抽屉行为树中
     */
    open fun setupWithBottomSheet(rootView: View, bottomSheet: View, followView: View? = null) {
        val behavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior?.let { pb ->
            registeredBottomSheetCallback?.let { pc -> pb.removeBottomSheetCallback(pc) }
        }
        
        bottomSheetBehavior = behavior
        behavior.saveFlags = BottomSheetBehavior.SAVE_NONE
        behavior.isHideable = false
        behavior.isDraggable = true
        behavior.skipCollapsed = false
        behavior.isFitToContents = true

        bottomSheet.post {
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        val sheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_COLLAPSED -> setExpansionFraction(0f)
                    BottomSheetBehavior.STATE_EXPANDED -> setExpansionFraction(1f)
                    BottomSheetBehavior.STATE_HIDDEN -> {
                        behavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                setExpansionFraction(slideOffset.coerceIn(0f, 1f))
            }
        }
        behavior.addBottomSheetCallback(sheetCallback)
        registeredBottomSheetCallback = sheetCallback
    }

    fun onHostResume() {
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    /**
     * 绑定选项卡
     */
    protected fun bindTabs(titles: List<String>) {
        detachTabMediatorSafely()
        if (titles.isEmpty()) {
            tabLayout.removeAllTabs()
            return
        }
        tabMediator = TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = titles.getOrNull(position) ?: "Tab ${position + 1}"
        }.apply { attach() }
    }

    private fun detachTabMediatorSafely() {
        val mediator = tabMediator ?: return
        try {
            mediator.detach()
        } catch (_: IllegalStateException) {}
        tabMediator = null
    }

    override fun onDetachedFromWindow() {
        detachTabMediatorSafely()
        bottomSheetBehavior?.let { behavior ->
            registeredBottomSheetCallback?.let { callback ->
                behavior.removeBottomSheetCallback(callback)
            }
        }
        registeredBottomSheetCallback = null
        bottomSheetBehavior = null
        super.onDetachedFromWindow()
    }
}