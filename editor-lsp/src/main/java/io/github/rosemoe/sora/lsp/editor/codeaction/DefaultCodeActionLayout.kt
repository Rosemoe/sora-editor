package io.github.rosemoe.sora.lsp.editor.codeaction

import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.AbsListView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import io.github.rosemoe.sora.lsp.editor.curvedTextScale
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

class DefaultCodeActionLayout : CodeActionLayout {

    private lateinit var window: CodeActionWindow
    private lateinit var root: LinearLayout
    private lateinit var listView: ListView
    private val adapter = CodeActionAdapter()

    private var textColor: Int = 0
    private var highlightColor: Int = 0
    private var latestTextSizePx: Float? = null
    private var horizontalPaddingPx: Int = 0
    private var verticalPaddingPx: Int = 0

    override fun attach(window: CodeActionWindow) {
        this.window = window
    }

    override fun createView(inflater: LayoutInflater): View {
        val context = inflater.context
        val editor = window.editor
        val dpUnit = editor.dpUnit
        horizontalPaddingPx = (dpUnit * 12).toInt()
        verticalPaddingPx = (dpUnit * 8).toInt()

        root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            clipToOutline = true
        }

        listView = ListView(context).apply {
            dividerHeight = 0
            overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
            choiceMode = ListView.CHOICE_MODE_SINGLE
            adapter = this@DefaultCodeActionLayout.adapter
            setOnItemClickListener { _, _, position, _ ->
                adapter.getItem(position)?.let { window.onActionSelected(it as CodeActionItem) }
            }
        }

        root.addView(
            listView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val initialSize = latestTextSizePx ?: editor.textSizePx
        adapter.updateTextSize(initialSize)

        return root
    }

    override fun applyColorScheme(colorScheme: EditorColorScheme, typeface: Typeface) {
        if (!::root.isInitialized) {
            return
        }
        val editor = window.editor
        val cornerRadius = editor.dpUnit * 8
        val strokeWidth = editor.dpUnit.toInt().coerceAtLeast(1)
        textColor = colorScheme.getColor(EditorColorScheme.HOVER_TEXT_NORMAL)
        highlightColor = colorScheme.getColor(EditorColorScheme.COMPLETION_WND_ITEM_CURRENT)

        val background = GradientDrawable().apply {
            this.cornerRadius = cornerRadius
            setColor(colorScheme.getColor(EditorColorScheme.HOVER_BACKGROUND))
            setStroke(strokeWidth, colorScheme.getColor(EditorColorScheme.HOVER_BORDER))
        }
        root.background = background

        setRootViewOutlineProvider(root, cornerRadius)

        val selectorDrawable = GradientDrawable().apply {
            this.cornerRadius = cornerRadius * 0.6f
            setColor(highlightColor)
        }
        listView.selector = selectorDrawable
        listView.setBackgroundColor(0)
        adapter.updateTypeface(typeface)
        adapter.updateTextColor(textColor)
    }

    private fun setRootViewOutlineProvider(rootView: View, cornerRadius: Float) {
        rootView.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, cornerRadius)
            }
        }
        rootView.clipToOutline = true
    }

    override fun renderActions(actions: List<CodeActionItem>) {
        adapter.submit(actions)
    }

    override fun onTextSizeChanged(oldSize: Float, newSize: Float) {
        if (newSize <= 0f) {
            return
        }
        latestTextSizePx = newSize
        if (::listView.isInitialized) {
            adapter.updateTextSize(newSize)
        }
    }

    private inner class CodeActionAdapter : android.widget.BaseAdapter() {
        private var items: List<CodeActionItem> = emptyList()
        private var targetEditorTextSizePx: Float? = null
        private var editorBaselineTextSizePx: Float? = null
        private var itemBaselineTextSizePx: Float? = null
        private var textColorInternal: Int = 0
        private var typefaceInternal: Typeface? = null

        fun submit(newItems: List<CodeActionItem>) {
            items = newItems
            notifyDataSetChanged()
        }

        fun updateTextSize(sizePx: Float) {
            if (sizePx <= 0f) {
                return
            }
            if (editorBaselineTextSizePx == null) {
                editorBaselineTextSizePx = sizePx
            }
            targetEditorTextSizePx = sizePx
            notifyDataSetChanged()
        }

        fun updateTextColor(color: Int) {
            textColorInternal = color
            notifyDataSetChanged()
        }

        fun updateTypeface(typeface: Typeface) {
            typefaceInternal = typeface
            notifyDataSetChanged()
        }

        override fun getCount(): Int = items.size

        override fun getItem(position: Int): CodeActionItem? = items.getOrNull(position)

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = (convertView as? TextView) ?: createTextView(parent)
            val item = getItem(position)
            view.text = item?.title?.takeIf { it.isNotBlank() } ?: "<unnamed action>"
            if (textColorInternal != 0) {
                view.setTextColor(textColorInternal)
            }
            applyCurvedTextSize(view)
            typefaceInternal?.let { view.typeface = it }
            return view
        }

        private fun createTextView(parent: ViewGroup): TextView {
            val context = parent.context
            return TextView(context).apply {
                layoutParams = AbsListView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPadding(horizontalPaddingPx, verticalPaddingPx, horizontalPaddingPx, verticalPaddingPx)
                textAlignment = View.TEXT_ALIGNMENT_VIEW_START

                // Add ripple effect
                val outValue = android.util.TypedValue()
                context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                setBackgroundResource(outValue.resourceId)
            }
        }

        private fun applyCurvedTextSize(view: TextView) {
            val editorBaseline = editorBaselineTextSizePx
            val targetEditorSize = targetEditorTextSizePx
            if (editorBaseline == null || targetEditorSize == null || editorBaseline <= 0f || targetEditorSize <= 0f) {
                return
            }
            val itemBaseline = itemBaselineTextSizePx ?: view.textSize.also { itemBaselineTextSizePx = it }
            if (itemBaseline <= 0f) {
                return
            }
            val rawScale = targetEditorSize / editorBaseline
            if (rawScale <= 0f) {
                return
            }
            val curvedScale = curvedTextScale(rawScale)
            view.setTextSize(TypedValue.COMPLEX_UNIT_PX, itemBaseline * curvedScale)
        }
    }
}
