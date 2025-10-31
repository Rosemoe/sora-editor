package io.github.rosemoe.sora.lsp.editor.text

import kotlin.math.pow

internal fun curvedTextScale(rawScale: Float): Float {
    if (!rawScale.isFinite() || rawScale <= 0f) {
        return 1f
    }
    if (rawScale == 1f) {
        return 1f
    }
    return if (rawScale > 1f) {
        val diff = rawScale - 1f
        val curved = diff.toDouble().pow(0.75).toFloat()
        1f + diff + curved * 0.2f
    } else {
        val diff = 1f - rawScale
        val curved = diff.toDouble().pow(1.5).toFloat()
        (1f - curved).coerceAtLeast(0f)
    }
}
