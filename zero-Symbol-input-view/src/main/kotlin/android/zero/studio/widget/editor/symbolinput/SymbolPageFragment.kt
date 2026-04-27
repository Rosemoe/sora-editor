package android.zero.studio.widget.editor.symbolinput

import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

/**
 * SymbolPageFragment 的核心实现。
 *
 * @author android_zero
 * @github msmt2018/zero-Symbol-input-view
 */
class SymbolPageFragment : Fragment() {

    companion object {
        private const val ARG_GROUP_INDEX = "arg_group_index"
        private const val ARG_VIEW_ID = "arg_view_id"

        /**
         * 执行 newInstance 方法。
         */
        fun newInstance(groupIndex: Int, viewId: Int): SymbolPageFragment {
            val fragment = SymbolPageFragment()
            val args = Bundle()
            args.putInt(ARG_GROUP_INDEX, groupIndex)
            args.putInt(ARG_VIEW_ID, viewId)
            fragment.arguments = args
            return fragment
        }
    }

    private var groupIndex: Int = 0
    private lateinit var viewModel: SymbolInputViewModel

    /**
     * 执行 onCreate 方法。
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        groupIndex = arguments?.getInt(ARG_GROUP_INDEX) ?: 0
    }

    /**
     * 执行 onCreateView 方法。
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = requireContext()
        val viewId = arguments?.getInt(ARG_VIEW_ID) ?: 0
        val key = "SymbolInputViewModel_$viewId"
        
        viewModel = ViewModelProvider(requireActivity()).get(key, SymbolInputViewModel::class.java)

        val rv = RecyclerView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setHasFixedSize(true)
            overScrollMode = View.OVER_SCROLL_NEVER
            layoutManager = GridLayoutManager(context, 8)
            clipToPadding = false
            val horizontal = (8 * resources.displayMetrics.density).roundToInt()
            val vertical = (2 * resources.displayMetrics.density).roundToInt()
            setPadding(horizontal, vertical, horizontal, vertical)
        }

        val group = viewModel.groups.getOrNull(groupIndex)
        if (group != null) {
            rv.adapter = SymbolAdapter(group.items)
        }

        return rv
    }

    private inner class SymbolAdapter(private val items: List<SymbolItem>) : RecyclerView.Adapter<SymbolAdapter.SymbolViewHolder>() {

        inner class SymbolViewHolder(val tv: TextView) : RecyclerView.ViewHolder(tv)

        /**
         * 执行 onCreateViewHolder 方法。
         */
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SymbolViewHolder {
            val tv = TextView(parent.context).apply {
                layoutParams = GridLayoutManager.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                minHeight = (34 * resources.displayMetrics.density).roundToInt()
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                isClickable = true
                isFocusable = true

                val tvColor = TypedValue()
                context.theme.resolveAttribute(android.R.attr.textColorPrimary, tvColor, true)
                setTextColor(if (tvColor.resourceId != 0) context.getColor(tvColor.resourceId) else tvColor.data)

                val tvBg = TypedValue()
                context.theme.resolveAttribute(android.R.attr.selectableItemBackground, tvBg, true)
                setBackgroundResource(tvBg.resourceId)
            }
            return SymbolViewHolder(tv)
        }

        /**
         * 执行 onBindViewHolder 方法。
         */
        override fun onBindViewHolder(holder: SymbolViewHolder, position: Int) {
            val item = items[position]
            holder.tv.text = item.display

            holder.tv.setOnClickListener {
                viewModel.editor?.let { ed ->
                    SymbolActionExecutor.execute(ed, item.shortAction, item.shortText, viewModel.onOpenManagerListener)
                }
            }

            holder.tv.setOnLongClickListener {
                if (item.longAction != null) {
                    viewModel.editor?.let { ed ->
                        SymbolActionExecutor.execute(ed, item.longAction!!, item.longText, viewModel.onOpenManagerListener)
                    }
                    true
                } else {
                    false
                }
            }
        }

        /**
         * 执行 getItemCount 方法。
         */
        override fun getItemCount(): Int = items.size
    }
}
