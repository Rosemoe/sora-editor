package io.github.rosemoe.sora.lang.styling

/**
 * Provider for text highlights
 */
fun interface HighlightTextProvider {
    /**
     * Provide text highlights to the given container
     */
    fun provideHighlightTexts(container: HighlightTextContainer)
}
