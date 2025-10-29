package io.github.rosemoe.sora.lsp.editor.diagnostics

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.ColorInt
import io.github.rosemoe.sora.lsp.R
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticDetail
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion
import io.github.rosemoe.sora.lsp.utils.blendARGB
import io.github.rosemoe.sora.widget.component.DiagnosticTooltipLayout
import io.github.rosemoe.sora.widget.component.EditorDiagnosticTooltipWindow
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

/**
 * Diagnostic tooltip layout tuned for LSP that only renders the detailed message.
 */
class LspDiagnosticTooltipLayout : DiagnosticTooltipLayout {

    private lateinit var window: EditorDiagnosticTooltipWindow
    private lateinit var root: View
    private lateinit var detailMessageText: TextView
    private lateinit var messagePanel: ViewGroup
    private lateinit var copyButton: ImageButton

    private val backgroundDrawable = GradientDrawable()
    private val severityColors = mutableMapOf<Short, Int>()
    private var appliedRegion: DiagnosticRegion? = null
    private var baseBackgroundColor: Int = Color.TRANSPARENT
    private var borderWidthPx: Int = 1
    private var cornerRadiusDp: Float = 8f
    private var borderWidthDp: Float = 1f
    private var severityBlendRatio = 0.25f
    private var pointerOverPopup = false
    private var detailTextSizePx: Float = -1f

    init {
        installDefaultSeverityPalette()
    }

    override fun attach(window: EditorDiagnosticTooltipWindow) {
        this.window = window
    }

    override fun createView(inflater: LayoutInflater): View {
        val view = inflater.inflate(R.layout.lsp_diagnostic_tooltip_window, null)
        root = view
        root.clipToOutline = true
        root.setOnGenericMotionListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_HOVER_ENTER -> pointerOverPopup = true
                MotionEvent.ACTION_HOVER_EXIT -> pointerOverPopup = false
            }
            false
        }

        detailMessageText = root.findViewById(R.id.diagnostic_tooltip_detailed_message)
        messagePanel = root.findViewById(R.id.diagnostic_container_message)
        copyButton = root.findViewById(R.id.diagnostic_copy_button)
        copyButton.setOnClickListener { copyDetailedMessageToClipboard() }
        copyButton.setOnHoverListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_HOVER_ENTER -> pointerOverPopup = true
                MotionEvent.ACTION_HOVER_EXIT -> pointerOverPopup = false
            }
            false
        }
        copyButton.isEnabled = false
        copyButton.visibility = View.GONE
        updateCopyButtonTint(null)
        updateDetailMessageTextSize(window.editor.textSizePx)
        return view
    }

    override fun applyColorScheme(colorScheme: EditorColorScheme) {
        val editor = window.editor
        detailMessageText.setTextColor(colorScheme.getColor(EditorColorScheme.DIAGNOSTIC_TOOLTIP_DETAILED_MSG))
        baseBackgroundColor = colorScheme.getColor(EditorColorScheme.DIAGNOSTIC_TOOLTIP_BACKGROUND)
        borderWidthPx = (editor.dpUnit * borderWidthDp).toInt().coerceAtLeast(1)
        backgroundDrawable.cornerRadius = editor.dpUnit * cornerRadiusDp
        backgroundDrawable.setStroke(borderWidthPx, resolveBorderColor(appliedRegion))
        backgroundDrawable.setColor(resolveFillColor(appliedRegion))
        root.background = backgroundDrawable
        updateCopyButtonTint(appliedRegion)
    }

    override fun onTextSizeChanged(oldSizePx: Float, newSizePx: Float) {
        updateDetailMessageTextSize(newSizePx)
    }

    override fun renderDiagnostic(diagnostic: DiagnosticDetail?) {
        renderDiagnostic(diagnostic, appliedRegion)
    }

    override fun renderDiagnostic(diagnostic: DiagnosticDetail?, region: DiagnosticRegion?) {
        appliedRegion = region
        if (diagnostic == null) {
            detailMessageText.text = ""
            detailMessageText.visibility = View.GONE
            copyButton.isEnabled = false
            copyButton.visibility = View.GONE
            backgroundDrawable.setStroke(borderWidthPx, resolveBorderColor(null))
            backgroundDrawable.setColor(resolveFillColor(null))
            updateCopyButtonTint(null)
            return
        }
        val detailedMessage = diagnostic.detailedMessage
        if (detailedMessage.isNullOrEmpty()) {
            detailMessageText.text = ""
            detailMessageText.visibility = View.GONE
            copyButton.isEnabled = false
            copyButton.visibility = View.GONE
            updateCopyButtonTint(null)
        } else {
            detailMessageText.visibility = View.VISIBLE
            detailMessageText.text = detailedMessage
            copyButton.isEnabled = true
            copyButton.visibility = View.VISIBLE
            updateCopyButtonTint(region)
        }
        backgroundDrawable.setStroke(borderWidthPx, resolveBorderColor(region))
        backgroundDrawable.setColor(resolveFillColor(region))
    }

    override fun measureContent(maxWidth: Int, maxHeight: Int): Pair<Int, Int> {
        val widthSpec = MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.AT_MOST)
        val heightSpec = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)
        val layoutParams = messagePanel.layoutParams
        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        messagePanel.layoutParams = layoutParams
        messagePanel.measure(widthSpec, heightSpec)
        val messageHeight = messagePanel.measuredHeight.coerceAtMost(maxHeight)
        layoutParams.height = messageHeight
        messagePanel.layoutParams = layoutParams
        val dialogWidth = messagePanel.measuredWidth.coerceAtMost(maxWidth)
        return dialogWidth to messageHeight
    }

    override fun isPointerOverPopup(): Boolean = pointerOverPopup

    override fun isMenuShowing(): Boolean = false

    override fun onWindowDismissed() {
        pointerOverPopup = false
    }

    fun setCornerRadiusDp(radius: Float) {
        cornerRadiusDp = radius
        if (::window.isInitialized) {
            backgroundDrawable.cornerRadius = window.editor.dpUnit * cornerRadiusDp
        }
    }

    fun setBorderWidthDp(width: Float) {
        borderWidthDp = width
        if (::window.isInitialized) {
            borderWidthPx = (window.editor.dpUnit * borderWidthDp).toInt().coerceAtLeast(1)
            backgroundDrawable.setStroke(borderWidthPx, resolveBorderColor(appliedRegion))
        }
    }

    fun setSeverityBlendRatio(ratio: Float) {
        severityBlendRatio = ratio.coerceIn(0f, 1f)
        backgroundDrawable.setColor(resolveFillColor(appliedRegion))
        updateCopyButtonTint(appliedRegion)
    }

    fun setSeverityColor(severity: Short, @ColorInt color: Int) {
        severityColors[severity] = color
        backgroundDrawable.setStroke(borderWidthPx, resolveBorderColor(appliedRegion))
        backgroundDrawable.setColor(resolveFillColor(appliedRegion))
        updateCopyButtonTint(appliedRegion)
    }

    fun setSeverityColors(
        @ColorInt none: Int? = null,
        @ColorInt typo: Int? = null,
        @ColorInt warning: Int? = null,
        @ColorInt error: Int? = null
    ) {
        none?.let { severityColors[DiagnosticRegion.SEVERITY_NONE] = it }
        typo?.let { severityColors[DiagnosticRegion.SEVERITY_TYPO] = it }
        warning?.let { severityColors[DiagnosticRegion.SEVERITY_WARNING] = it }
        error?.let { severityColors[DiagnosticRegion.SEVERITY_ERROR] = it }
        backgroundDrawable.setStroke(borderWidthPx, resolveBorderColor(appliedRegion))
        backgroundDrawable.setColor(resolveFillColor(appliedRegion))
        updateCopyButtonTint(appliedRegion)
    }

    private fun resolveFillColor(region: DiagnosticRegion?): Int {
        val severityColor = resolveSeverityColor(region)
        return if (severityColor == null) {
            baseBackgroundColor
        } else {
            blendARGB(baseBackgroundColor, severityColor, severityBlendRatio)
        }
    }

    private fun resolveBorderColor(region: DiagnosticRegion?): Int {
        return resolveSeverityColor(region) ?: Color.TRANSPARENT
    }

    private fun resolveSeverityColor(region: DiagnosticRegion?): Int? {
        val severity = region?.severity ?: DiagnosticRegion.SEVERITY_NONE
        return severityColors[severity]
    }

    private fun installDefaultSeverityPalette() {
        severityColors[DiagnosticRegion.SEVERITY_NONE] = Color.parseColor("#FF94A3B8")
        severityColors[DiagnosticRegion.SEVERITY_TYPO] = Color.parseColor("#FF38BDF8")
        severityColors[DiagnosticRegion.SEVERITY_WARNING] = Color.parseColor("#FFFACC15")
        severityColors[DiagnosticRegion.SEVERITY_ERROR] = Color.parseColor("#FFFB7185")
    }

    private fun updateDetailMessageTextSize(sizePx: Float) {
        if (!::detailMessageText.isInitialized || sizePx <= 0f) {
            return
        }
        if (detailTextSizePx == sizePx) {
            return
        }
        detailTextSizePx = sizePx
        detailMessageText.setTextSize(TypedValue.COMPLEX_UNIT_PX, sizePx)
    }

    private fun copyDetailedMessageToClipboard() {
        val text = detailMessageText.text?.toString()?.takeIf { it.isNotBlank() } ?: return
        val context = window.editor.context
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val label = context.getString(android.R.string.copy)
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    private fun updateCopyButtonTint(region: DiagnosticRegion?) {
        if (!::copyButton.isInitialized) {
            return
        }
        val tintColor = resolveBorderColor(region).takeUnless { it == Color.TRANSPARENT }
            ?: severityColors[DiagnosticRegion.SEVERITY_ERROR]
            ?: Color.parseColor("#FFFB7185")
        copyButton.imageTintList = ColorStateList.valueOf(tintColor)
    }
}
