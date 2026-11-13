package android.zero.studio.widget.editor.symbolinput

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

/**
 * 底部抽屉 + 分页容器的基础抽象组件。
 *
 * 该类用于兼容仍需要 `TabLayout + ViewPager2` 形态的场景，
 * 同时将布局改为纯代码创建，避免依赖外部 XML 结构。
  *
 * @author android_zero
 * @github msmt2018/zero-Symbol-input-view
 */
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
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        tabLayout = TabLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(44))
            tabMode = TabLayout.MODE_SCROLLABLE
        }
        tabRow = tabLayout
        viewPager = ViewPager2(context).apply {
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0)
            offscreenPageLimit = 1
            isSaveEnabled = false
        }
        root.addView(tabLayout)
        root.addView(viewPager)
        addView(root)
        setExpansionFraction(0f)
    }

    /** 根据抽屉滑动进度更新分页栏显隐效果。 */
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

    /** 绑定并监听底部抽屉状态变化。 */
    open fun setupWithBottomSheet(rootView: View, bottomSheet: View, followView: View? = null) {
        val behavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior?.let { pb -> registeredBottomSheetCallback?.let { pc -> pb.removeBottomSheetCallback(pc) } }

        bottomSheetBehavior = behavior
        behavior.saveFlags = BottomSheetBehavior.SAVE_NONE
        behavior.isHideable = false
        behavior.isDraggable = true
        behavior.skipCollapsed = false
        behavior.isFitToContents = true
        bottomSheet.post { behavior.state = BottomSheetBehavior.STATE_COLLAPSED }

        val sheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_COLLAPSED -> setExpansionFraction(0f)
                    BottomSheetBehavior.STATE_EXPANDED -> setExpansionFraction(1f)
                    BottomSheetBehavior.STATE_HIDDEN -> behavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                setExpansionFraction(slideOffset.coerceIn(0f, 1f))
            }
        }
        behavior.addBottomSheetCallback(sheetCallback)
        registeredBottomSheetCallback = sheetCallback
    }

    /** 页面恢复时将抽屉重置到折叠态。 */
    fun onHostResume() {
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    /** 将标题列表绑定到 `TabLayout` 与 `ViewPager2`。 */
    protected fun bindTabs(titles: List<String>) {
        detachTabMediatorSafely()
        if (titles.isEmpty()) {
            tabLayout.removeAllTabs(); return
        }
        tabMediator = TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = titles.getOrNull(position) ?: "Tab ${position + 1}"
        }.apply { attach() }
    }

    private fun detachTabMediatorSafely() {
        val mediator = tabMediator ?: return
        try { mediator.detach() } catch (_: IllegalStateException) {}
        tabMediator = null
    }

    override fun onDetachedFromWindow() {
        detachTabMediatorSafely()
        bottomSheetBehavior?.let { behavior ->
            registeredBottomSheetCallback?.let { callback -> behavior.removeBottomSheetCallback(callback) }
        }
        registeredBottomSheetCallback = null
        bottomSheetBehavior = null
        super.onDetachedFromWindow()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
