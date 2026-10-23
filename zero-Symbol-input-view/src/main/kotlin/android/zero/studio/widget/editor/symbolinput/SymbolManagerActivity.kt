package android.zero.studio.widget.editor.symbolinput

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * SymbolManagerActivity 的核心实现。
 *
 * @author android_zero
 * @github msmt2018/zero-Symbol-input-view
 */
class SymbolManagerActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var multiActionBar: View
    private var symbolGroups = mutableListOf<SymbolGroup>()
    private lateinit var pagerAdapter: GroupPagerAdapter

    private lateinit var actionValues: IntArray
    private lateinit var actionNames: Array<String>

    private var isBatchMode = false
    private var batchGroupIndex = -1
    private val selectedItems = linkedSetOf<SymbolItem>()
    private var dotTabListenerAttached = false
    private val settingsTabTitle by lazy { getString(R.string.settings_tab_title) }
    private val importFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(::importFromUri)
    }
    private val exportFolderLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let(::showExportNameDialog)
    }

    /**
     * 执行 onCreate 方法。
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContentView(R.layout.activity_symbol_manager)
        setupStatusBar()

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        appBarLayout = findViewById(R.id.app_bar_layout)
        multiActionBar = findViewById(R.id.multi_action_bar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            if (isBatchMode) {
                exitBatchMode()
            } else {
                finish()
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(appBarLayout) { view, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.setPadding(view.paddingLeft, topInset, view.paddingRight, view.paddingBottom)
            insets
        }

        actionValues = resources.getIntArray(R.array.symbol_action_values)
        actionNames = resources.getStringArray(R.array.symbol_action_names)

        tabLayout = findViewById(R.id.tab_layout)
        viewPager = findViewById(R.id.view_pager)

        symbolGroups = SymbolDataManager.loadData(this)

        setupBatchActionBar()

        pagerAdapter = GroupPagerAdapter()
        viewPager.adapter = pagerAdapter
        tabLayout.setupWithViewPager(viewPager)
        ensureDotTabSelectionListener()
        applyIndicatorStyle()
        bindGroupTabLongPressMenus()
        viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            /**
             * 执行 onPageSelected 方法。
             */
            override fun onPageSelected(position: Int) {
                if (isBatchMode && (position != batchGroupIndex || isSettingsPosition(position))) {
                    exitBatchMode()
                }
            }
        })
    }

    /**
     * 执行 bindGroupTabLongPressMenus 方法。
     */
    private fun bindGroupTabLongPressMenus() {
        tabLayout.post {
            for (i in 0 until tabLayout.tabCount) {
                val tab = tabLayout.getTabAt(i) ?: continue
                tab.view.setOnLongClickListener {
                    if (!isSettingsPosition(i) && i in symbolGroups.indices) {
                        showGroupTabMenu(tab.view, i)
                        true
                    } else {
                        false
                    }
                }
            }
        }
    }

    /**
     * 执行 showGroupTabMenu 方法。
     */
    private fun showGroupTabMenu(anchor: View, groupIndex: Int) {
        PopupMenu(this, anchor).apply {
            menuInflater.inflate(R.menu.menu_symbol_group_tab_actions, menu)
            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_tab_move_left -> moveGroup(groupIndex, -1)
                    R.id.action_tab_move_right -> moveGroup(groupIndex, 1)
                    R.id.action_tab_add -> showAddGroupDialog()
                    R.id.action_tab_copy_to -> copyGroupToAnother(groupIndex)
                    R.id.action_tab_rename -> renameGroup(groupIndex)
                    R.id.action_tab_delete -> deleteGroup(groupIndex)
                }
                true
            }
            show()
        }
    }

    /**
     * 执行 moveGroup 方法。
     */
    private fun moveGroup(groupIndex: Int, delta: Int) {
        val target = (groupIndex + delta).coerceIn(0, symbolGroups.lastIndex)
        if (target == groupIndex) return
        val item = symbolGroups.removeAt(groupIndex)
        symbolGroups.add(target, item)
        SymbolDataManager.saveData(this, symbolGroups)
        onGroupsChanged(targetGroupIndex = target)
    }

    /**
     * 执行 copyGroupToAnother 方法。
     */
    private fun copyGroupToAnother(sourceIndex: Int) {
        val source = symbolGroups.getOrNull(sourceIndex) ?: return
        showTargetGroupDialog(getString(R.string.tab_action_copy_to)) { targetIndex ->
            if (targetIndex !in symbolGroups.indices) return@showTargetGroupDialog
            val copied = source.items.map {
                SymbolItem(it.shortAction, it.display, it.shortText, it.longAction, it.longText)
            }
            symbolGroups[targetIndex].items.addAll(copied)
            SymbolDataManager.saveData(this, symbolGroups)
            onGroupsChanged(targetGroupIndex = targetIndex)
        }
    }

    /**
     * 执行 renameGroup 方法。
     */
    private fun renameGroup(groupIndex: Int) {
        val group = symbolGroups.getOrNull(groupIndex) ?: return
        val editText = EditText(this).apply {
            setText(group.name)
            setSelection(text.length)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_title_rename_group)
            .setView(editText)
            .setPositiveButton(R.string.dialog_save) { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    group.name = newName
                    SymbolDataManager.saveData(this, symbolGroups)
                    onGroupsChanged(targetGroupIndex = groupIndex)
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    /**
     * 执行 deleteGroup 方法。
     */
    private fun deleteGroup(groupIndex: Int) {
        if (groupIndex !in symbolGroups.indices) return
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.dialog_confirm_delete_group)
            .setPositiveButton(R.string.dialog_delete) { _, _ ->
                symbolGroups.removeAt(groupIndex)
                SymbolDataManager.saveData(this, symbolGroups)
                onGroupsChanged(targetGroupIndex = (groupIndex - 1).coerceAtLeast(0))
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    /**
     * 执行 setupBatchActionBar 方法。
     */
    private fun setupBatchActionBar() {
        findViewById<View>(R.id.action_batch_copy).setOnClickListener { performBatchCopyOrCut(isCut = false) }
        findViewById<View>(R.id.action_batch_cut).setOnClickListener { performBatchCopyOrCut(isCut = true) }
        findViewById<View>(R.id.action_batch_invert).setOnClickListener { invertBatchSelection() }
        findViewById<View>(R.id.action_batch_delete).setOnClickListener { confirmDeleteSelected() }
        findViewById<View>(R.id.action_batch_close).setOnClickListener { exitBatchMode() }
    }

    /**
     * 执行 onCreateOptionsMenu 方法。
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_symbol_manager, menu)
        return true
    }

    /**
     * 执行 onOptionsItemSelected 方法。
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_add -> {
                if (symbolGroups.isEmpty()) {
                    Toast.makeText(this, R.string.toast_need_group_first, Toast.LENGTH_SHORT).show()
                } else {
                    val currentGroup = viewPager.currentItem
                    if (isSettingsPosition(currentGroup)) {
                        Toast.makeText(this, R.string.toast_fail, Toast.LENGTH_SHORT).show()
                        return true
                    }
                    showEditDialog(symbolGroups[currentGroup], null)
                }
                return true
            }

            R.id.action_add_group -> {
                showAddGroupDialog()
                return true
            }

            R.id.action_import_clipboard -> importFromClipboard()
            R.id.action_export_clipboard -> exportToClipboard()
            R.id.action_import_file -> {
                importFileLauncher.launch(arrayOf("application/json"))
                return true
            }

            R.id.action_export_file -> {
                exportFolderLauncher.launch(null)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * 执行 showAddGroupDialog 方法。
     */
    private fun showAddGroupDialog(onCreated: ((Int) -> Unit)? = null) {
        val editText = EditText(this).apply {
            hint = getString(R.string.group_name)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.action_add_group)
            .setView(editText)
            .setPositiveButton(R.string.dialog_save) { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, R.string.toast_fail, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                symbolGroups.add(SymbolGroup(name = name, items = mutableListOf()))
                SymbolDataManager.saveData(this, symbolGroups)
                val newIndex = symbolGroups.lastIndex
                onGroupsChanged(targetGroupIndex = newIndex)
                onCreated?.invoke(newIndex)
                Toast.makeText(this, R.string.toast_success, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    /**
     * 执行 exportToClipboard 方法。
     */
    private fun exportToClipboard() {
        val jsonStr = SymbolDataManager.gson.toJson(symbolGroups)
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("SymbolData", jsonStr))
        Toast.makeText(this, R.string.toast_success, Toast.LENGTH_SHORT).show()
    }

    /**
     * 执行 importFromClipboard 方法。
     */
    private fun importFromClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
        if (text != null) {
            try {
                val listType = object : com.google.gson.reflect.TypeToken<MutableList<SymbolGroup>>() {}.type
                val importedData: MutableList<SymbolGroup> = SymbolDataManager.gson.fromJson(text, listType)
                symbolGroups.clear()
                symbolGroups.addAll(importedData)
                SymbolDataManager.saveData(this, symbolGroups)
                onGroupsChanged()
                Toast.makeText(this, R.string.toast_success, Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(this, R.string.toast_fail, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 执行 importFromUri 方法。
     */
    private fun importFromUri(uri: Uri) {
        try {
            val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (text.isNullOrBlank()) {
                Toast.makeText(this, R.string.toast_fail, Toast.LENGTH_SHORT).show()
                return
            }
            val listType = object : com.google.gson.reflect.TypeToken<MutableList<SymbolGroup>>() {}.type
            val importedData: MutableList<SymbolGroup> = SymbolDataManager.gson.fromJson(text, listType)
            symbolGroups.clear()
            symbolGroups.addAll(importedData)
            SymbolDataManager.saveData(this, symbolGroups)
            onGroupsChanged(targetGroupIndex = 0)
            Toast.makeText(this, R.string.toast_success, Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(this, R.string.toast_fail, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 执行 showExportNameDialog 方法。
     */
    private fun showExportNameDialog(treeUri: Uri) {
        val defaultName = "symbol-config-${
            SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())
        }.json"
        val editText = EditText(this).apply {
            setText(defaultName)
            setSelection(text.length)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.menu_export_file)
            .setView(editText)
            .setPositiveButton(R.string.dialog_save) { _, _ ->
                val rawName = editText.text.toString().trim()
                val fileName = when {
                    rawName.isEmpty() -> defaultName
                    rawName.endsWith(".json", true) -> rawName
                    else -> "$rawName.json"
                }
                exportToDirectoryUri(treeUri, fileName)
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    /**
     * 执行 exportToDirectoryUri 方法。
     */
    private fun exportToDirectoryUri(treeUri: Uri, fileName: String) {
        try {
            val treeDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
            val treeDocumentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocumentId)
            val targetUri = DocumentsContract.createDocument(
                contentResolver,
                treeDocumentUri,
                "application/json",
                fileName
            )
            if (targetUri == null) {
                Toast.makeText(this, R.string.toast_fail, Toast.LENGTH_SHORT).show()
                return
            }

            val jsonStr = SymbolDataManager.gson.toJson(symbolGroups)
            contentResolver.openOutputStream(targetUri)?.bufferedWriter()?.use { it.write(jsonStr) }
            Toast.makeText(this, R.string.toast_success, Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(this, R.string.toast_fail, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 执行 showEditDialog 方法。
     */
    private fun showEditDialog(group: SymbolGroup, itemToEdit: SymbolItem?) {
        showSymbolDialog(
            title = if (itemToEdit == null) getString(R.string.dialog_title_add_symbol) else getString(R.string.dialog_title_edit_symbol),
            initialItem = itemToEdit,
            showDeleteButton = itemToEdit != null,
            onSave = { newItem ->
                if (itemToEdit == null) {
                    group.items.add(newItem)
                } else {
                    val index = group.items.indexOf(itemToEdit)
                    if (index >= 0) {
                        group.items[index] = newItem
                    }
                }
                SymbolDataManager.saveData(this, symbolGroups)
                onGroupsChanged()
            },
            onDelete = {
                if (itemToEdit != null) {
                    group.items.remove(itemToEdit)
                    selectedItems.remove(itemToEdit)
                    SymbolDataManager.saveData(this, symbolGroups)
                    onGroupsChanged()
                }
            }
        )
    }

    /**
     * 执行 showCopyDialog 方法。
     */
    private fun showCopyDialog(group: SymbolGroup, sourceItem: SymbolItem) {
        showSymbolDialog(
            title = getString(R.string.menu_item_copy),
            initialItem = sourceItem,
            showDeleteButton = false,
            onSave = { newItem ->
                val index = group.items.indexOf(sourceItem)
                if (index >= 0) {
                    group.items.add(index + 1, newItem)
                } else {
                    group.items.add(newItem)
                }
                SymbolDataManager.saveData(this, symbolGroups)
                onGroupsChanged()
            },
            onDelete = null
        )
    }

    /**
     * 执行 showSymbolDialog 方法。
     */
    private fun showSymbolDialog(
        title: String,
        initialItem: SymbolItem?,
        showDeleteButton: Boolean,
        onSave: (SymbolItem) -> Unit,
        onDelete: (() -> Unit)?
    ) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_symbol_edit, null)
        val etDisplay = view.findViewById<EditText>(R.id.et_display)
        val acShortAction = view.findViewById<MaterialAutoCompleteTextView>(R.id.ac_short_action)
        val etShortText = view.findViewById<EditText>(R.id.et_short_text)
        val acLongAction = view.findViewById<MaterialAutoCompleteTextView>(R.id.ac_long_action)
        val etLongText = view.findViewById<EditText>(R.id.et_long_text)

        val shortActionItems = actionNames.toList()
        val longActionItems = listOf(getString(R.string.action_no_long_press)) + actionNames.toList()
        acShortAction.setSimpleItems(shortActionItems.toTypedArray())
        acLongAction.setSimpleItems(longActionItems.toTypedArray())

        var shortIndex = 0
        var longIndex = 0

        if (initialItem != null) {
            etDisplay.setText(initialItem.display)
            etShortText.setText(initialItem.shortText)
            etLongText.setText(initialItem.longText)
            shortIndex = actionValues.indexOf(initialItem.shortAction).coerceAtLeast(0)
            longIndex = initialItem.longAction?.let { actionValues.indexOf(it).coerceAtLeast(0) + 1 } ?: 0
        }

        acShortAction.setText(shortActionItems[shortIndex], false)
        acLongAction.setText(longActionItems[longIndex], false)

        acShortAction.setOnItemClickListener { _, _, position, _ -> shortIndex = position }
        acLongAction.setOnItemClickListener { _, _, position, _ -> longIndex = position }

        val builder = MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(view)
            .setPositiveButton(R.string.dialog_save) { _, _ ->
                val shortAct = actionValues.getOrElse(shortIndex) { actionValues.firstOrNull() ?: 0 }
                val longAct = if (longIndex > 0) actionValues.getOrNull(longIndex - 1) else null

                val newItem = SymbolItem(
                    shortAction = shortAct,
                    display = etDisplay.text.toString(),
                    shortText = etShortText.text.toString().takeIf { shortAct == 0 },
                    longAction = longAct,
                    longText = etLongText.text.toString().takeIf { longAct == 0 }
                )
                onSave(newItem)
            }
            .setNegativeButton(R.string.dialog_cancel, null)

        if (showDeleteButton && onDelete != null) {
            builder.setNeutralButton(R.string.dialog_delete) { _, _ -> onDelete() }
        }

        builder.show()
    }

    /**
     * 执行 showItemMenu 方法。
     */
    private fun showItemMenu(anchor: View, group: SymbolGroup, item: SymbolItem) {
        PopupMenu(this, anchor).apply {
            menuInflater.inflate(R.menu.menu_symbol_item_actions, menu)
            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_item_edit -> showEditDialog(group, item)
                    R.id.action_item_copy -> showCopyDialog(group, item)
                    R.id.action_item_delete -> confirmDeleteSingle(group, item)
                    R.id.action_item_batch -> enterBatchMode(viewPager.currentItem, item)
                }
                true
            }
            show()
        }
    }

    /**
     * 执行 confirmDeleteSingle 方法。
     */
    private fun confirmDeleteSingle(group: SymbolGroup, item: SymbolItem) {
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.dialog_confirm_delete_symbol)
            .setPositiveButton(R.string.dialog_delete) { _, _ ->
                group.items.remove(item)
                selectedItems.remove(item)
                SymbolDataManager.saveData(this, symbolGroups)
                if (isBatchMode && selectedItems.isEmpty()) {
                    exitBatchMode()
                }
                onGroupsChanged()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    /**
     * 执行 enterBatchMode 方法。
     */
    private fun enterBatchMode(groupIndex: Int, seedItem: SymbolItem) {
        isBatchMode = true
        batchGroupIndex = groupIndex
        selectedItems.clear()
        selectedItems.add(seedItem)
        tabLayout.visibility = View.GONE
        multiActionBar.visibility = View.VISIBLE
        pagerAdapter.notifyDataSetChanged()
    }

    /**
     * 执行 exitBatchMode 方法。
     */
    private fun exitBatchMode() {
        isBatchMode = false
        batchGroupIndex = -1
        selectedItems.clear()
        multiActionBar.visibility = View.GONE
        tabLayout.visibility = View.VISIBLE
        pagerAdapter.notifyDataSetChanged()
    }

    /**
     * 执行 toggleSelected 方法。
     */
    private fun toggleSelected(item: SymbolItem) {
        if (!selectedItems.add(item)) {
            selectedItems.remove(item)
        }
        if (selectedItems.isEmpty()) {
            exitBatchMode()
            return
        }
        pagerAdapter.notifyDataSetChanged()
    }

    /**
     * 执行 invertBatchSelection 方法。
     */
    private fun invertBatchSelection() {
        if (!isBatchMode || batchGroupIndex !in symbolGroups.indices) return
        val group = symbolGroups[batchGroupIndex]
        val newSelected = linkedSetOf<SymbolItem>()
        group.items.forEach { if (!selectedItems.contains(it)) newSelected.add(it) }
        selectedItems.clear()
        selectedItems.addAll(newSelected)
        if (selectedItems.isEmpty()) {
            exitBatchMode()
            return
        }
        pagerAdapter.notifyDataSetChanged()
    }

    /**
     * 执行 confirmDeleteSelected 方法。
     */
    private fun confirmDeleteSelected() {
        if (selectedItems.isEmpty()) {
            Toast.makeText(this, R.string.toast_no_selection, Toast.LENGTH_SHORT).show()
            return
        }
        if (batchGroupIndex !in symbolGroups.indices) return

        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.dialog_confirm_delete_selected)
            .setPositiveButton(R.string.dialog_delete) { _, _ ->
                val group = symbolGroups[batchGroupIndex]
                group.items.removeAll(selectedItems.toSet())
                SymbolDataManager.saveData(this, symbolGroups)
                exitBatchMode()
                onGroupsChanged(targetGroupIndex = batchGroupIndex)
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    /**
     * 执行 performBatchCopyOrCut 方法。
     */
    private fun performBatchCopyOrCut(isCut: Boolean) {
        if (selectedItems.isEmpty()) {
            Toast.makeText(this, R.string.toast_no_selection, Toast.LENGTH_SHORT).show()
            return
        }
        if (batchGroupIndex !in symbolGroups.indices) return

        val titleRes = if (isCut) R.string.dialog_move_to else R.string.dialog_copy_to
        showTargetGroupDialog(getString(titleRes)) { targetGroupIndex ->
            val sourceGroup = symbolGroups[batchGroupIndex]
            if (isCut && targetGroupIndex == batchGroupIndex) {
                Toast.makeText(this, R.string.toast_same_group_move, Toast.LENGTH_SHORT).show()
                return@showTargetGroupDialog
            }

            val orderedSelected = sourceGroup.items.filter { selectedItems.contains(it) }
            val copiedItems = orderedSelected.map {
                SymbolItem(
                    shortAction = it.shortAction,
                    display = it.display,
                    shortText = it.shortText,
                    longAction = it.longAction,
                    longText = it.longText
                )
            }

            symbolGroups[targetGroupIndex].items.addAll(copiedItems)
            if (isCut) {
                sourceGroup.items.removeAll(selectedItems.toSet())
            }

            SymbolDataManager.saveData(this, symbolGroups)
            exitBatchMode()
            onGroupsChanged(targetGroupIndex = targetGroupIndex)
        }
    }

    /**
     * 执行 showTargetGroupDialog 方法。
     */
    private fun showTargetGroupDialog(title: String, onTargetSelected: (Int) -> Unit) {
        if (symbolGroups.isEmpty()) return

        var selectedIndex = viewPager.currentItem.coerceIn(0, symbolGroups.lastIndex)

        /**
         * 执行 showChooser 方法。
         */
        fun showChooser() {
            val names = symbolGroups.map { it.name }.toTypedArray()
            MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setSingleChoiceItems(names, selectedIndex) { _, which ->
                    selectedIndex = which
                }
                .setPositiveButton(R.string.dialog_save) { _, _ ->
                    onTargetSelected(selectedIndex)
                }
                .setNeutralButton(R.string.dialog_new_group) { _, _ ->
                    showAddGroupDialog { newIndex ->
                        selectedIndex = newIndex
                        showChooser()
                    }
                }
                .setNegativeButton(R.string.dialog_cancel, null)
                .show()
        }

        showChooser()
    }

    /**
     * 执行 isSettingsPosition 方法。
     */
    private fun isSettingsPosition(position: Int): Boolean = position == symbolGroups.size

    /**
     * 执行 applyIndicatorStyle 方法。
     */
    private fun applyIndicatorStyle() {
        val accent = resolveThemeColor(android.R.attr.colorAccent, Color.GRAY)
        tabLayout.tabMode = TabLayout.MODE_SCROLLABLE
        tabLayout.setTabIndicatorFullWidth(false)
        tabLayout.setSelectedTabIndicator(android.graphics.drawable.ColorDrawable(accent))
        tabLayout.setSelectedTabIndicatorColor(accent)
        tabLayout.setSelectedTabIndicatorHeight((2 * resources.displayMetrics.density).toInt())
        tabLayout.setSelectedTabIndicatorGravity(TabLayout.INDICATOR_GRAVITY_BOTTOM)
        applyTabItemPresentation(false)
        // 分组栏增加阴影，和内容区分层
        tabLayout.elevation = 6f * resources.displayMetrics.density
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
    private fun applyTabItemPresentation(simpleDots: Boolean) {
        for (index in 0 until tabLayout.tabCount) {
            val tab = tabLayout.getTabAt(index) ?: continue
            if (simpleDots) {
                if (tab.customView == null) {
                    tab.customView = createDotTabView()
                }
                updateDotTabState(tab, tab.isSelected)
                tab.contentDescription = tab.text
            } else {
                tab.customView = null
            }
        }
    }

    /**
     * 执行 createDotTabView 方法。
     */
    private fun createDotTabView(): View {
        val dot = View(this)
        dot.layoutParams = LinearLayout.LayoutParams(dp(8), dp(8)).apply {
            leftMargin = dp(4)
            rightMargin = dp(4)
            gravity = Gravity.CENTER
        }
        dot.setBackgroundResource(R.drawable.bg_page_indicator_dot)
        return dot
    }

    /**
     * 执行 updateDotTabState 方法。
     */
    private fun updateDotTabState(tab: TabLayout.Tab, selected: Boolean) {
        if (SymbolDataManager.getUiSettings(this).indicatorStyle != 1) return
        val dot = tab.customView ?: return
        val params = dot.layoutParams as? LinearLayout.LayoutParams ?: return
        val width = if (selected) dp(18) else dp(8)
        if (params.width != width) {
            params.width = width
            dot.layoutParams = params
        }
        dot.alpha = if (selected) 1f else 0.65f
        dot.setBackgroundResource(if (selected) R.drawable.bg_page_indicator_capsule else R.drawable.bg_page_indicator_dot)
    }

    /**
     * 执行 dp 方法。
     */
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    /**
     * 执行 createSettingsPage 方法。
     */
    private fun createSettingsPage(container: ViewGroup): View {
        val scrollView = NestedScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val p = (16 * resources.displayMetrics.density).toInt()
            setPadding(p, p, p, p)
        }
        scrollView.addView(content)

        /**
         * 执行 createEntry 方法。
         */
        fun createEntry(title: String, subtitle: String): View {
            val item = LayoutInflater.from(this).inflate(R.layout.item_symbol_manage, content, false)
            val tvTitle = item.findViewById<TextView>(R.id.tv_title)
            val tvSubtitle = item.findViewById<TextView>(R.id.tv_subtitle)
            item.findViewById<View>(R.id.iv_drag_handle).visibility = View.GONE
            tvTitle.text = title
            tvSubtitle.text = subtitle
            return item
        }

        val settings = SymbolDataManager.getUiSettings(this)
        val lineItem = createEntry(getString(R.string.settings_lines_title), "${settings.collapsedRows} - ${settings.symbolsPerRow}")
        lineItem.setOnClickListener { showLineSettingDialog() }
        content.addView(lineItem)

        val rememberItem = createEntry(getString(R.string.settings_remember_title), getString(R.string.settings_remember_desc))
        val rememberSwitch = SwitchCompat(this).apply { isChecked = settings.rememberExpanded }
        (rememberItem as ViewGroup).addView(rememberSwitch)
        rememberSwitch.setOnCheckedChangeListener { _, isChecked ->
            val old = SymbolDataManager.getUiSettings(this)
            SymbolDataManager.saveUiSettings(this, old.copy(rememberExpanded = isChecked))
            if (!isChecked) {
                SymbolDataManager.setLastExpanded(this, false)
            }
        }
        content.addView(rememberItem)

        val uniformItem = createEntry(getString(R.string.settings_uniform_title), getString(R.string.settings_uniform_desc))
        val uniformSwitch = SwitchCompat(this).apply { isChecked = settings.uniformGroupHeight }
        (uniformItem as ViewGroup).addView(uniformSwitch)
        uniformSwitch.setOnCheckedChangeListener { _, isChecked ->
            val old = SymbolDataManager.getUiSettings(this)
            SymbolDataManager.saveUiSettings(this, old.copy(uniformGroupHeight = isChecked))
        }
        content.addView(uniformItem)

        val textSizeItem = createEntry(getString(R.string.settings_symbol_text_size), "${settings.symbolTextSizeSp}sp")
        textSizeItem.setOnClickListener { showTextSizeDialog() }
        content.addView(textSizeItem)

        val handleItem = createEntry(getString(R.string.settings_show_drag_handle), getString(R.string.settings_show_drag_handle_desc))
        val handleSwitch = SwitchCompat(this).apply { isChecked = settings.showDragHandle }
        (handleItem as ViewGroup).addView(handleSwitch)
        handleSwitch.setOnCheckedChangeListener { _, isChecked ->
            val old = SymbolDataManager.getUiSettings(this)
            SymbolDataManager.saveUiSettings(this, old.copy(showDragHandle = isChecked))
            pagerAdapter.notifyDataSetChanged()
        }
        content.addView(handleItem)

        val advancedItem = createEntry(getString(R.string.settings_enable_advanced_actions), getString(R.string.settings_enable_advanced_actions_desc))
        val advancedSwitch = SwitchCompat(this).apply { isChecked = settings.enableAdvancedActions }
        (advancedItem as ViewGroup).addView(advancedSwitch)
        advancedSwitch.setOnCheckedChangeListener { _, isChecked ->
            val old = SymbolDataManager.getUiSettings(this)
            SymbolDataManager.saveUiSettings(this, old.copy(enableAdvancedActions = isChecked))
        }
        content.addView(advancedItem)

        val rememberPageItem = createEntry(getString(R.string.settings_remember_page_title), getString(R.string.settings_remember_page_desc))
        val rememberPageSwitch = SwitchCompat(this).apply { isChecked = settings.rememberLastPage }
        (rememberPageItem as ViewGroup).addView(rememberPageSwitch)
        rememberPageSwitch.setOnCheckedChangeListener { _, isChecked ->
            val old = SymbolDataManager.getUiSettings(this)
            SymbolDataManager.saveUiSettings(this, old.copy(rememberLastPage = isChecked))
        }
        content.addView(rememberPageItem)

        return scrollView
    }

    /**
     * 执行 showLineSettingDialog 方法。
     */
    private fun showLineSettingDialog() {
        val settings = SymbolDataManager.getUiSettings(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_line_settings, null)
        val minEdit = view.findViewById<EditText>(R.id.et_min_rows)
        val maxEdit = view.findViewById<EditText>(R.id.et_max_cols)
        minEdit.setText(settings.collapsedRows.toString())
        maxEdit.setText(settings.symbolsPerRow.toString())

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings_lines_title)
            .setView(view)
            .setPositiveButton(R.string.dialog_save) { _, _ ->
                val minRows = minEdit.text.toString().toIntOrNull()?.coerceIn(1, 10) ?: settings.collapsedRows
                val maxCols = maxEdit.text.toString().toIntOrNull()?.coerceIn(1, 20) ?: settings.symbolsPerRow
                SymbolDataManager.saveUiSettings(
                    this,
                    settings.copy(collapsedRows = minRows, symbolsPerRow = maxCols)
                )
                pagerAdapter.notifyDataSetChanged()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    /**
     * 执行 setupStatusBar 方法。
     */
    private fun setupStatusBar() {
        val surface = resolveThemeColor(android.R.attr.colorBackground, Color.WHITE)
        window.statusBarColor = surface
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        val isLightSurface = androidx.core.graphics.ColorUtils.calculateLuminance(surface) > 0.5
        controller.isAppearanceLightStatusBars = isLightSurface
    }

    /**
     * 执行 resolveThemeColor 方法。
     */
    private fun resolveThemeColor(attr: Int, fallback: Int): Int {
        val value = TypedValue()
        if (!theme.resolveAttribute(attr, value, true)) return fallback
        return if (value.resourceId != 0) getColor(value.resourceId) else value.data
    }

    /**
     * 执行 showTextSizeDialog 方法。
     */
    private fun showTextSizeDialog() {
        val settings = SymbolDataManager.getUiSettings(this)
        val editText = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(settings.symbolTextSizeSp.toString())
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings_symbol_text_size)
            .setView(editText)
            .setPositiveButton(R.string.dialog_save) { _, _ ->
                val textSize = editText.text.toString().toIntOrNull()?.coerceIn(12, 28) ?: settings.symbolTextSizeSp
                SymbolDataManager.saveUiSettings(this, settings.copy(symbolTextSizeSp = textSize))
                pagerAdapter.notifyDataSetChanged()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    /**
     * 执行 onGroupsChanged 方法。
     */
    private fun onGroupsChanged(targetGroupIndex: Int? = null) {
        pagerAdapter.notifyDataSetChanged()
        tabLayout.post {
            val maxIndex = pagerAdapter.count - 1
            val target = targetGroupIndex ?: viewPager.currentItem.coerceAtMost(maxIndex)
            if (maxIndex >= 0 && target >= 0) {
                viewPager.currentItem = target
            }
            bindGroupTabLongPressMenus()
        }
    }

    private inner class GroupPagerAdapter : PagerAdapter() {
        /**
         * 执行 getCount 方法。
         */
        override fun getCount(): Int = symbolGroups.size + 1

        /**
         * 执行 isViewFromObject 方法。
         */
        override fun isViewFromObject(view: View, `object`: Any): Boolean = view === `object`

        /**
         * 执行 getPageTitle 方法。
         */
        override fun getPageTitle(position: Int): CharSequence {
            return if (isSettingsPosition(position)) settingsTabTitle else symbolGroups[position].name
        }

        /**
         * 执行 instantiateItem 方法。
         */
        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            if (isSettingsPosition(position)) {
                return createSettingsPage(container).also { container.addView(it) }
            }
            val group = symbolGroups[position]
            val itemsAdapter = ItemsAdapter(position, group)
            val rv = RecyclerView(this@SymbolManagerActivity).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                layoutManager = LinearLayoutManager(this@SymbolManagerActivity)
                adapter = itemsAdapter
            }

            val callback = object : ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                0
            ) {
                /**
                 * 执行 onMove 方法。
                 */
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    if (isBatchMode) return false
                    val fromPosition = viewHolder.bindingAdapterPosition
                    val toPosition = target.bindingAdapterPosition
                    if (fromPosition == RecyclerView.NO_POSITION || toPosition == RecyclerView.NO_POSITION) {
                        return false
                    }

                    val moved = group.items.removeAt(fromPosition)
                    group.items.add(toPosition, moved)
                    itemsAdapter.notifyItemMoved(fromPosition, toPosition)
                    SymbolDataManager.saveData(this@SymbolManagerActivity, symbolGroups)
                    return true
                }

                /**
                 * 执行 onSwiped 方法。
                 */
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    // no-op
                }

                /**
                 * 执行 isLongPressDragEnabled 方法。
                 */
                override fun isLongPressDragEnabled(): Boolean = false
            }
            val touchHelper = ItemTouchHelper(callback)
            touchHelper.attachToRecyclerView(rv)
            itemsAdapter.attachTouchHelper(touchHelper)

            container.addView(rv)
            return rv
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
        override fun getItemPosition(`object`: Any): Int = POSITION_NONE
    }

    private inner class ItemsAdapter(
        private val groupIndex: Int,
        private val group: SymbolGroup
    ) : RecyclerView.Adapter<ItemsAdapter.ItemViewHolder>() {

        private var touchHelper: ItemTouchHelper? = null

        /**
         * 执行 attachTouchHelper 方法。
         */
        fun attachTouchHelper(helper: ItemTouchHelper) {
            touchHelper = helper
        }

        inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTitle: TextView = view.findViewById(R.id.tv_title)
            val tvSubtitle: TextView = view.findViewById(R.id.tv_subtitle)
            val dragHandle: View = view.findViewById(R.id.iv_drag_handle)
        }

        /**
         * 执行 onCreateViewHolder 方法。
         */
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            val view = LayoutInflater.from(this@SymbolManagerActivity).inflate(R.layout.item_symbol_manage, parent, false)
            return ItemViewHolder(view)
        }

        /**
         * 执行 onBindViewHolder 方法。
         */
        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            val item = group.items[position]
            holder.tvTitle.text = item.display

            val shortDesc = getString(
                R.string.symbol_desc_short,
                SymbolDataManager.getActionDesc(this@SymbolManagerActivity, item.shortAction, item.shortText)
            )
            val longDesc = item.longAction?.let {
                getString(
                    R.string.symbol_desc_long,
                    SymbolDataManager.getActionDesc(this@SymbolManagerActivity, it, item.longText)
                )
            } ?: ""
            holder.tvSubtitle.text = shortDesc + longDesc

            val selected = isBatchMode && groupIndex == batchGroupIndex && selectedItems.contains(item)
            holder.itemView.setBackgroundColor(if (selected) Color.parseColor("#66BEEB") else Color.TRANSPARENT)
            val uiSettings = SymbolDataManager.getUiSettings(this@SymbolManagerActivity)
            holder.dragHandle.visibility = if (uiSettings.showDragHandle) View.VISIBLE else View.GONE

            holder.itemView.setOnClickListener {
                if (isBatchMode && groupIndex == batchGroupIndex) {
                    toggleSelected(item)
                } else {
                    showEditDialog(group, item)
                }
            }

            holder.itemView.setOnLongClickListener {
                if (isBatchMode && groupIndex == batchGroupIndex) {
                    toggleSelected(item)
                } else {
                    showItemMenu(holder.itemView, group, item)
                }
                true
            }

            holder.dragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN && !isBatchMode && uiSettings.showDragHandle) {
                    touchHelper?.startDrag(holder)
                }
                false
            }
        }

        /**
         * 执行 getItemCount 方法。
         */
        override fun getItemCount() = group.items.size
    }
}
