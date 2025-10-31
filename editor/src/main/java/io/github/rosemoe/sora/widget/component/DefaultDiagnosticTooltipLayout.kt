package io.github.rosemoe.sora.widget.component

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import io.github.rosemoe.sora.I18nConfig
import io.github.rosemoe.sora.R
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticDetail
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import kotlin.math.max

class DefaultDiagnosticTooltipLayout : DiagnosticTooltipLayout {

    private lateinit var window: EditorDiagnosticTooltipWindow
    private lateinit var root: View
    private lateinit var briefMessageText: TextView
    private lateinit var detailMessageText: TextView
    private lateinit var quickfixText: TextView
    private lateinit var moreActionText: TextView
    private lateinit var messagePanel: ViewGroup
    private lateinit var quickfixPanel: ViewGroup

    private var popupMenu: PopupMenu? = null
    private var currentDiagnostic: DiagnosticDetail? = null
    private var pointerOverPopup = false
    private var menuShowing = false

    override fun attach(window: EditorDiagnosticTooltipWindow) {
        this.window = window
    }

    override fun createView(inflater: LayoutInflater): View {
        val context = window.editor.context
        root = inflater.inflate(R.layout.diagnostic_tooltip_window, null)
        root.clipToOutline = true
        root.setOnGenericMotionListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_HOVER_ENTER -> pointerOverPopup = true
                MotionEvent.ACTION_HOVER_EXIT -> pointerOverPopup = false
            }
            false
        }

        briefMessageText = root.findViewById(R.id.diagnostic_tooltip_brief_message)
        detailMessageText = root.findViewById(R.id.diagnostic_tooltip_detailed_message)
        quickfixText = root.findViewById(R.id.diagnostic_tooltip_preferred_action)
        moreActionText = root.findViewById(R.id.diagnostic_tooltip_more_actions)
        messagePanel = root.findViewById(R.id.diagnostic_container_message)
        quickfixPanel = root.findViewById(R.id.diagnostic_container_quickfix)

        quickfixText.setOnClickListener {
            val quickfix = currentDiagnostic?.quickfixes?.firstOrNull()
            if (quickfix != null) {
                quickfix.executeQuickfix()
                window.dismiss()
            }
        }

        moreActionText.setText(I18nConfig.getResourceId(R.string.sora_editor_diagnostics_more_actions))
        moreActionText.setOnClickListener {
            val diagnostic = currentDiagnostic ?: return@setOnClickListener
            val quickfixes = diagnostic.quickfixes
            if (quickfixes.isNullOrEmpty() || quickfixes.size <= 1) {
                return@setOnClickListener
            }
            val menu = popupMenu ?: PopupMenu(context, moreActionText).also { popupMenu = it }
            menu.menu.apply {
                clear()
                for (i in 1 until quickfixes.size) {
                    add(0, i, 0, quickfixes[i].resolveTitle(context))
                }
            }
            menu.setOnMenuItemClickListener { item ->
                quickfixes[item.itemId].executeQuickfix()
                window.dismiss()
                true
            }
            menu.setOnDismissListener { menuShowing = false }
            menuShowing = true
            menu.show()
        }

        return root
    }

    override fun applyColorScheme(colorScheme: EditorColorScheme) {
        val editor = window.editor
        briefMessageText.setTextColor(colorScheme.getColor(EditorColorScheme.DIAGNOSTIC_TOOLTIP_BRIEF_MSG))
        detailMessageText.setTextColor(colorScheme.getColor(EditorColorScheme.DIAGNOSTIC_TOOLTIP_DETAILED_MSG))
        quickfixText.setTextColor(colorScheme.getColor(EditorColorScheme.DIAGNOSTIC_TOOLTIP_ACTION))
        moreActionText.setTextColor(colorScheme.getColor(EditorColorScheme.DIAGNOSTIC_TOOLTIP_ACTION))

        val background = GradientDrawable().apply {
            cornerRadius = editor.dpUnit * 5
            setColor(colorScheme.getColor(EditorColorScheme.DIAGNOSTIC_TOOLTIP_BACKGROUND))
        }
        root.background = background
    }

    override fun renderDiagnostic(diagnostic: DiagnosticDetail?) {
        currentDiagnostic = diagnostic
        if (diagnostic == null) {
            detailMessageText.visibility = View.GONE
            briefMessageText.text = ""
            quickfixPanel.visibility = View.GONE
            moreActionText.visibility = View.GONE
            popupMenu?.dismiss()
            return
        }
        briefMessageText.text = diagnostic.briefMessage.ifBlank { "<NULL>" }
        val detailedMessage = diagnostic.detailedMessage
        if (detailedMessage.isNullOrEmpty()) {
            detailMessageText.visibility = View.GONE
        } else {
            detailMessageText.visibility = View.VISIBLE
            detailMessageText.text = detailedMessage
        }
        val quickfixes = diagnostic.quickfixes
        if (quickfixes.isNullOrEmpty()) {
            quickfixPanel.visibility = View.GONE
            popupMenu?.dismiss()
        } else {
            quickfixPanel.visibility = View.VISIBLE
            quickfixText.text = quickfixes[0].resolveTitle(window.editor.context)
            moreActionText.visibility = if (quickfixes.size > 1) View.VISIBLE else View.GONE
        }
    }

    override fun measureContent(maxWidth: Int, maxHeight: Int): Pair<Int, Int> {
        val widthSpec = MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.AT_MOST)
        var bottomBarHeight = 0
        var bottomBarWidth = 0
        if (quickfixPanel.visibility == View.VISIBLE) {
            quickfixPanel.measure(widthSpec, MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST))
            bottomBarHeight = quickfixPanel.measuredHeight
            bottomBarWidth = quickfixPanel.measuredWidth.coerceAtMost(maxWidth)
        }
        val restHeight = (maxHeight - bottomBarHeight).coerceAtLeast(1)
        val layoutParams = messagePanel.layoutParams
        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        messagePanel.layoutParams = layoutParams
        messagePanel.measure(widthSpec, MeasureSpec.makeMeasureSpec(restHeight, MeasureSpec.AT_MOST))
        val messageHeight = messagePanel.measuredHeight.coerceAtMost(restHeight)
        layoutParams.height = messageHeight
        messagePanel.layoutParams = layoutParams
        val dialogWidth = max(bottomBarWidth, messagePanel.measuredWidth.coerceAtMost(maxWidth))
        val dialogHeight = bottomBarHeight + messageHeight
        return dialogWidth to dialogHeight
    }

    override fun isPointerOverPopup(): Boolean = pointerOverPopup

    override fun isMenuShowing(): Boolean = menuShowing

    override fun onWindowDismissed() {
        pointerOverPopup = false
        menuShowing = false
        popupMenu?.dismiss()
    }
}
