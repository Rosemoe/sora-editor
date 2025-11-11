/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2024  Rosemoe
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 *
 *     Please contact Rosemoe by email 2073412493@qq.com if you need
 *     additional information or have any questions
 */
package io.github.rosemoe.sora.langs.textmate.brackets.ast

interface IListNode {
    val childCount: Int
    fun getChild(idx: Int): ASTNode

    fun toMutable(): IListNode

    fun makeImmutable(): IListNode

    fun makeLastElementMutable(): ASTNode?

    fun makeFirstElementMutable(): ASTNode?

    fun appendChildOfSameHeight(node: ASTNode)

    fun unappendChild(): ASTNode

    fun prependChildOfSameHeight(node: ASTNode)

    fun unprependChild(): ASTNode

    fun handleChildrenChanged()
}

sealed class ASTNode {
    abstract val length: Length

    abstract val listHeight: Int

    internal open val unopenedBrackets: AnchorSet
        get() = AnchorSet.EMPTY

    internal open fun canBeReused(anchorSet: AnchorSet): Boolean {
        if (unopenedBrackets.intersects(anchorSet)) {
            return false
        }

        return true
    }

    open fun shallowClone(): ASTNode = this

    open fun toMutable(): ASTNode = this

    open fun makeImmutable(): ASTNode = this

    open val childCount: Int
        get() = 0

    open fun getChild(idx: Int): ASTNode {
        throw IndexOutOfBoundsException("Node ${this::class.simpleName} has no child at index $idx")
    }
}

class BracketAST(
    override val length: Length,
    val bracketId: Int,
    val isOpening: Boolean
) : ASTNode() {

    override val listHeight: Int = 0

    override val unopenedBrackets: AnchorSet
        get() = if (!isOpening) {
            // Closing brackets without opening are unopened
            AnchorSet.of(bracketId)
        } else {
            AnchorSet.EMPTY
        }

    override fun canBeReused(anchorSet: AnchorSet): Boolean {
        // Standalone brackets should not be reused; their parent pair handles reuse.
        return false
    }

    override fun toString(): String {
        return "Bracket(${if (isOpening) "open" else "close"}, id=$bracketId, len=$length)"
    }
}

class BracketPairAST(
    val openingBracket: BracketAST,
    val child: ASTNode?,  // Can be null for empty pairs like "{}"
    val closingBracket: BracketAST?  // Can be null for unclosed brackets
) : ASTNode() {

    override val length: Length by lazy {
        var len = openingBracket.length
        if (child != null) {
            len += child.length
        }
        if (closingBracket != null) {
            len += closingBracket.length
        }
        len
    }

    override val listHeight: Int = 0

    override val childCount: Int
        get() {
            var count = 1 // opening bracket
            if (child != null) {
                count++
            }
            if (closingBracket != null) {
                count++
            }
            return count
        }

    override fun getChild(idx: Int): ASTNode {
        return when (idx) {
            0 -> openingBracket
            1 -> child ?: closingBracket
                ?: throw IndexOutOfBoundsException("No child at index $idx")
            2 -> closingBracket
                ?: throw IndexOutOfBoundsException("No child at index $idx")
            else -> throw IndexOutOfBoundsException("Index: $idx, Size: $childCount")
        }
    }

    override val unopenedBrackets: AnchorSet by lazy {
        child?.unopenedBrackets ?: AnchorSet.EMPTY
    }

    val bracketId: Int
        get() = openingBracket.bracketId

    val isClosed: Boolean
        get() = closingBracket != null

    override fun canBeReused(anchorSet: AnchorSet): Boolean {
        if (!isClosed) {
            return false
        }
        return super.canBeReused(anchorSet)
    }

    override fun toString(): String {
        return "Pair(id=$bracketId, closed=$isClosed, child=$child)"
    }
}

class InvalidBracketAST(
    val bracketId: Int,
    override val length: Length
) : ASTNode() {

    override val listHeight: Int = 0

    override val unopenedBrackets: AnchorSet = AnchorSet.of(bracketId)

    override fun toString(): String {
        return "InvalidBracket(id=$bracketId, len=$length)"
    }
}

class ListAST private constructor(
    private var _children: MutableList<ASTNode>,
    private var _immutable: Boolean,
    // Pre-computed values for immutable nodes
    private var _cachedLength: Length?,
    private var _cachedHeight: Int?,
    private var _cachedUnopened: AnchorSet?
) : ASTNode(), IListNode {

        constructor(children: List<ASTNode>) : this(
        children.toMutableList(),
        false,  // Start as mutable
        null, null, null  // Will compute on first access
    ) {
        require(children.isNotEmpty()) { "List must have at least one child" }
    }

        val children: List<ASTNode>
        get() = _children

    override val length: Length
        get() {
            if (_cachedLength == null) {
                _cachedLength = _children.fold(Length.ZERO) { acc, node -> acc + node.length }
            }
            return _cachedLength!!
        }

    override val listHeight: Int
        get() {
            if (_cachedHeight == null) {
                _cachedHeight = if (_children.isEmpty()) 0
                else _children.first().listHeight + 1
            }
            return _cachedHeight!!
        }

    override val unopenedBrackets: AnchorSet
        get() {
            if (_cachedUnopened == null) {
                var result = AnchorSet.EMPTY
                for (child in _children) {
                    val childUnopened = child.unopenedBrackets
                    if (!childUnopened.isEmpty()) {
                        result = result.union(childUnopened)
                    }
                }
                _cachedUnopened = result
            }
            return _cachedUnopened!!
        }

    override val childCount: Int
        get() = _children.size

    override fun getChild(idx: Int): ASTNode = _children[idx]

    fun isBalanced(): Boolean {
        if (_children.isEmpty()) return false

        // Check child count (2-3 children except for root)
        // Note: We allow roots to violate this temporarily during construction

        // Check all children have same height
        val expectedHeight = _children.first().listHeight
        return _children.all { it.listHeight == expectedHeight }
    }

    override fun toMutable(): ListAST {
        return if (_immutable) {
            // Shallow copy: copy the list structure but share child references
            ListAST(
                _children.toMutableList(),  // New list, same children
                false,  // New copy is mutable
                _cachedLength,  // Share cached values
                _cachedHeight,
                _cachedUnopened
            )
        } else {
            this
        }
    }

    override fun makeImmutable(): ListAST {
        _immutable = true
        return this
    }

    override fun makeLastElementMutable(): ASTNode? {
        throwIfImmutable()
        if (_children.isEmpty()) return null

        val lastIndex = _children.size - 1
        val lastChild = _children[lastIndex]

        val mutable = if (lastChild is ListAST) lastChild.toMutable() else lastChild
        if (mutable !== lastChild) {
            _children[lastIndex] = mutable
            invalidateCache()
        }
        return mutable
    }

    override fun makeFirstElementMutable(): ASTNode? {
        throwIfImmutable()
        if (_children.isEmpty()) return null

        val firstChild = _children[0]
        val mutable = if (firstChild is ListAST) firstChild.toMutable() else firstChild
        if (mutable !== firstChild) {
            _children[0] = mutable
            invalidateCache()
        }
        return mutable
    }

    override fun appendChildOfSameHeight(node: ASTNode) {
        throwIfImmutable()
        require(_children.size < 3) { "Cannot append to a full (2,3) tree node" }
        require(node.listHeight == (_children.firstOrNull()?.listHeight ?: node.listHeight)) {
            "Child must have same height"
        }
        _children.add(node)
        invalidateCache()
    }

    override fun unappendChild(): ASTNode {
        throwIfImmutable()
        require(_children.size == 3) { "Cannot remove from non-full (2,3) tree node" }
        val removed = _children.removeAt(_children.size - 1)
        invalidateCache()
        return removed
    }

    override fun prependChildOfSameHeight(node: ASTNode) {
        throwIfImmutable()
        require(_children.size < 3) { "Cannot prepend to a full (2,3) tree node" }
        require(node.listHeight == (_children.firstOrNull()?.listHeight ?: node.listHeight)) {
            "Child must have same height"
        }
        _children.add(0, node)
        invalidateCache()
    }

    override fun unprependChild(): ASTNode {
        throwIfImmutable()
        require(_children.size == 3) { "Cannot remove from non-full (2,3) tree node" }
        val removed = _children.removeAt(0)
        invalidateCache()
        return removed
    }

    override fun handleChildrenChanged() {
        invalidateCache()
    }

    override fun canBeReused(anchorSet: AnchorSet): Boolean {
        // Empty lists should not be reused
        if (_children.isEmpty()) {
            return false
        }

        // First check the base conditions (anchor set)
        if (!super.canBeReused(anchorSet)) {
            return false
        }

        // Find the rightmost leaf node and check if it can be reused
        // This follows VSCode's logic where we traverse to the last child
        // IMPORTANT: Must check ALL list types (both ListAST and TwoThreeListAST)
        var lastChild: ASTNode = this
        while (lastChild is IListNode) {
            val childCount = lastChild.childCount
            if (childCount == 0) {
                // Empty child list should not happen, but be defensive
                return false
            }
            lastChild = lastChild.getChild(childCount - 1)
        }

        // Check if the last leaf can be reused
        // IMPORTANT: We don't pass maxLength here because it's relative to the
        // original node's position, not the leaf's position. The length check
        // must be done by the parser before calling canBeReused.
        return lastChild.canBeReused(anchorSet)
    }

    private fun throwIfImmutable() {
        if (_immutable) {
            throw IllegalStateException("Cannot modify immutable list")
        }
    }

    private fun invalidateCache() {
        _cachedLength = null
        _cachedHeight = null
        _cachedUnopened = null
    }

    override fun toString(): String {
        return "List(height=$listHeight, count=$childCount, immutable=$_immutable)"
    }
}

class TextAST(
    override val length: Length
) : ASTNode() {

    override val listHeight: Int = 0

    override fun toString(): String {
        return "Text(len=$length)"
    }

    companion object {
        // Cache for commonly used text lengths
        private val cache = HashMap<Length, TextAST>()

                fun of(length: Length): TextAST {
            if (length.isZero()) return EMPTY

            // Only cache small lengths to avoid memory bloat
            if (length.lineCount <= MAX_CACHE_LINE && length.columnCount <= MAX_CACHE_COLUMN) {
                return cache.getOrPut(length) { TextAST(length) }
            }

            return TextAST(length)
        }

        val EMPTY = TextAST(Length.ZERO)
        private const val MAX_CACHE_COLUMN = 256
        private const val MAX_CACHE_LINE = 8192
    }
}


class TwoThreeListAST private constructor(
    private var _item1: ASTNode,
    private var _item2: ASTNode,
    private var _item3: ASTNode?,
    private var _immutable: Boolean,
    // Pre-computed values for immutable nodes
    private var _cachedLength: Length?,
    private var _cachedHeight: Int?,
    private var _cachedUnopened: AnchorSet?
) : ASTNode(), IListNode {

        constructor(item1: ASTNode, item2: ASTNode) : this(
        item1, item2, null,
        false,
        null, null, null
    ) {
        require(item1.listHeight == item2.listHeight) { "Children must have same height" }
    }

        constructor(item1: ASTNode, item2: ASTNode, item3: ASTNode) : this(
        item1, item2, item3,
        false,
        null, null, null
    ) {
        require(item1.listHeight == item2.listHeight && item2.listHeight == item3.listHeight) {
            "Children must have same height"
        }
    }

    override val childCount: Int
        get() = if (_item3 != null) 3 else 2

    override fun getChild(idx: Int): ASTNode {
        return when (idx) {
            0 -> _item1
            1 -> _item2
            2 -> _item3 ?: throw IndexOutOfBoundsException("Index: $idx, Size: 2")
            else -> throw IndexOutOfBoundsException("Index: $idx, Size: $childCount")
        }
    }

        val children: List<ASTNode>
        get() = if (_item3 != null) {
            listOf(_item1, _item2, _item3!!)
        } else {
            listOf(_item1, _item2)
        }

    override val length: Length
        get() {
            if (_cachedLength == null) {
                _cachedLength = if (_item3 != null) {
                    _item1.length + _item2.length + _item3!!.length
                } else {
                    _item1.length + _item2.length
                }
            }
            return _cachedLength!!
        }

    override val listHeight: Int
        get() {
            if (_cachedHeight == null) {
                _cachedHeight = _item1.listHeight + 1
            }
            return _cachedHeight!!
        }

    override val unopenedBrackets: AnchorSet
        get() {
            if (_cachedUnopened == null) {
                var result = _item1.unopenedBrackets
                val item2Unopened = _item2.unopenedBrackets
                if (!item2Unopened.isEmpty()) {
                    result = result.union(item2Unopened)
                }
                if (_item3 != null) {
                    val item3Unopened = _item3!!.unopenedBrackets
                    if (!item3Unopened.isEmpty()) {
                        result = result.union(item3Unopened)
                    }
                }
                _cachedUnopened = result
            }
            return _cachedUnopened!!
        }

    override fun toMutable(): TwoThreeListAST {
        return if (_immutable) {
            // Shallow copy: copy the structure but share child references
            TwoThreeListAST(
                _item1, _item2, _item3,
                false,  // New copy is mutable
                _cachedLength,  // Share cached values
                _cachedHeight,
                _cachedUnopened
            )
        } else {
            this
        }
    }

    override fun makeImmutable(): TwoThreeListAST {
        _immutable = true
        return this
    }

    override fun makeLastElementMutable(): ASTNode {
        throwIfImmutable()
        val lastIdx = childCount - 1
        val lastChild = getChild(lastIdx)

        val mutable = when (lastChild) {
            is ListAST -> {
                lastChild.toMutable()
            }
            is TwoThreeListAST -> {
                lastChild.toMutable()
            }
            else -> {
                lastChild
            }
        }

        if (mutable !== lastChild) {
            setChild(lastIdx, mutable)
        }
        return mutable
    }

    override fun makeFirstElementMutable(): ASTNode {
        throwIfImmutable()
        val firstChild = _item1

        val mutable = if (firstChild is ListAST) {
            firstChild.toMutable()
        } else if (firstChild is TwoThreeListAST) {
            firstChild.toMutable()
        } else {
            firstChild
        }

        if (mutable !== firstChild) {
            _item1 = mutable
            invalidateCache()
        }
        return mutable
    }

    override fun appendChildOfSameHeight(node: ASTNode) {
        throwIfImmutable()
        require(_item3 == null) { "Cannot append to a full (2,3) tree node" }
        require(node.listHeight == _item1.listHeight) { "Child must have same height" }
        _item3 = node
        invalidateCache()
    }

    override fun unappendChild(): ASTNode {
        throwIfImmutable()
        require(_item3 != null) { "Cannot remove from a non-full (2,3) tree node" }
        val removed = _item3!!
        _item3 = null
        invalidateCache()
        return removed
    }

    override fun prependChildOfSameHeight(node: ASTNode) {
        throwIfImmutable()
        require(_item3 == null) { "Cannot prepend to a full (2,3) tree node" }
        require(node.listHeight == _item1.listHeight) { "Child must have same height" }
        _item3 = _item2
        _item2 = _item1
        _item1 = node
        invalidateCache()
    }

    override fun unprependChild(): ASTNode {
        throwIfImmutable()
        require(_item3 != null) { "Cannot remove from a non-full (2,3) tree node" }
        val removed = _item1
        _item1 = _item2
        _item2 = _item3!!
        _item3 = null
        invalidateCache()
        return removed
    }

    override fun handleChildrenChanged() {
        invalidateCache()
    }

    override fun canBeReused(anchorSet: AnchorSet): Boolean {
        // First check the base conditions (anchor set)
        if (!super.canBeReused(anchorSet)) {
            return false
        }

        // Find the rightmost leaf node and check if it can be reused
        // Use the same unified logic as ListAST - check all IListNode types
        var lastChild: ASTNode = getChild(childCount - 1)
        while (lastChild is IListNode) {
            val childCount = lastChild.childCount
            if (childCount == 0) {
                // Empty child list should not happen, but be defensive
                return false
            }
            lastChild = lastChild.getChild(childCount - 1)
        }

        return lastChild.canBeReused(anchorSet)
    }

    private fun setChild(idx: Int, node: ASTNode) {
        when (idx) {
            0 -> _item1 = node
            1 -> _item2 = node
            2 -> {
                require(_item3 != null) { "Cannot set third child when it doesn't exist" }
                _item3 = node
            }
            else -> throw IndexOutOfBoundsException("Index: $idx")
        }
        invalidateCache()
    }

    private fun throwIfImmutable() {
        if (_immutable) {
            throw IllegalStateException("Cannot modify immutable TwoThreeListAST")
        }
    }

    private fun invalidateCache() {
        _cachedLength = null
        _cachedHeight = null
        _cachedUnopened = null
    }

    override fun toString(): String {
        return "TwoThree(height=$listHeight, count=$childCount, immutable=$_immutable)"
    }

    companion object {
                fun createImmutable(item1: ASTNode, item2: ASTNode): TwoThreeListAST {
            require(item1.listHeight == item2.listHeight) { "Children must have same height" }

            // Pre-compute all values immediately
            val length = item1.length + item2.length
            val height = item1.listHeight + 1

            // Optimize unopened brackets merging
            val unopened1 = item1.unopenedBrackets
            val unopened2 = item2.unopenedBrackets
            val unopened = when {
                unopened1.isEmpty() -> unopened2
                unopened2.isEmpty() -> unopened1
                else -> unopened1.union(unopened2)
            }

            // Create immutable node with pre-computed values
            return TwoThreeListAST(
                item1, item2, null,
                true,  // immutable
                length,  // pre-computed, not null
                height,
                unopened
            )
        }

                fun createImmutable(item1: ASTNode, item2: ASTNode, item3: ASTNode): TwoThreeListAST {
            require(item1.listHeight == item2.listHeight && item2.listHeight == item3.listHeight) {
                "Children must have same height"
            }

            // Pre-compute all values immediately
            val length = item1.length + item2.length + item3.length
            val height = item1.listHeight + 1

            // Optimize unopened brackets merging
            var unopened = item1.unopenedBrackets
            val unopened2 = item2.unopenedBrackets
            val unopened3 = item3.unopenedBrackets

            if (!unopened2.isEmpty()) {
                unopened = unopened.union(unopened2)
            }
            if (!unopened3.isEmpty()) {
                unopened = unopened.union(unopened3)
            }

            // Create immutable node with pre-computed values
            return TwoThreeListAST(
                item1, item2, item3,
                true,  // immutable
                length,  // pre-computed, not null
                height,
                unopened
            )
        }
    }
}
