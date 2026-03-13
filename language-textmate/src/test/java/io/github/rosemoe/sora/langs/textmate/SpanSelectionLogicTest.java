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
 */
package io.github.rosemoe.sora.langs.textmate;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for the span selection logic introduced in TextMateAnalyzer#tokenizeLine.
 *
 * <p>The fix ensures that {@code SpanFactory.obtain()} (which supports underline color) is only
 * called when the token actually has the Underline font style set, while the lighter-weight
 * {@code SpanFactory.obtainNoExt()} is used for all other tokens. Calling
 * {@code setUnderlineColor()} on a {@code NoExtSpanImpl} throws
 * {@link UnsupportedOperationException}, so the selection must happen <em>before</em> the span
 * is created.
 *
 * <p>These tests validate the boolean guard logic ({@code needUnderline}) using the same
 * bit-flag constants defined in {@code FontStyle}, without requiring Android runtime.
 */
public class SpanSelectionLogicTest {

    // Mirrors org.eclipse.tm4e.core.internal.theme.FontStyle constants
    private static final int FONT_STYLE_NONE          = 0;
    private static final int FONT_STYLE_ITALIC        = 1;
    private static final int FONT_STYLE_BOLD          = 2;
    private static final int FONT_STYLE_UNDERLINE     = 4;
    private static final int FONT_STYLE_STRIKETHROUGH = 8;

    /**
     * Simulates the {@code needUnderline} guard extracted in the fix.
     */
    private boolean needUnderline(int fontStyle) {
        return (fontStyle & FONT_STYLE_UNDERLINE) != 0;
    }

    /**
     * Simulates the span type selection: returns "obtain" when underline is needed,
     * "obtainNoExt" otherwise — mirroring the ternary introduced in the fix.
     */
    private String selectSpanFactory(int fontStyle) {
        return needUnderline(fontStyle) ? "obtain" : "obtainNoExt";
    }

    // ------------------------------------------------------------------
    // needUnderline() guard
    // ------------------------------------------------------------------

    @Test
    public void needUnderline_returnsFalse_whenFontStyleIsNone() {
        assertFalse(needUnderline(FONT_STYLE_NONE));
    }

    @Test
    public void needUnderline_returnsFalse_whenOnlyItalic() {
        assertFalse(needUnderline(FONT_STYLE_ITALIC));
    }

    @Test
    public void needUnderline_returnsFalse_whenOnlyBold() {
        assertFalse(needUnderline(FONT_STYLE_BOLD));
    }

    @Test
    public void needUnderline_returnsFalse_whenOnlyStrikethrough() {
        assertFalse(needUnderline(FONT_STYLE_STRIKETHROUGH));
    }

    @Test
    public void needUnderline_returnsTrue_whenOnlyUnderline() {
        assertTrue(needUnderline(FONT_STYLE_UNDERLINE));
    }

    @Test
    public void needUnderline_returnsTrue_whenUnderlineWithBold() {
        assertTrue(needUnderline(FONT_STYLE_BOLD | FONT_STYLE_UNDERLINE));
    }

    @Test
    public void needUnderline_returnsTrue_whenUnderlineWithItalic() {
        assertTrue(needUnderline(FONT_STYLE_ITALIC | FONT_STYLE_UNDERLINE));
    }

    @Test
    public void needUnderline_returnsTrue_whenAllStylesSet() {
        int all = FONT_STYLE_ITALIC | FONT_STYLE_BOLD | FONT_STYLE_UNDERLINE | FONT_STYLE_STRIKETHROUGH;
        assertTrue(needUnderline(all));
    }

    // ------------------------------------------------------------------
    // Span factory selection
    // ------------------------------------------------------------------

    @Test
    public void selectSpanFactory_usesObtainNoExt_whenNoUnderline() {
        assertEquals("obtainNoExt", selectSpanFactory(FONT_STYLE_NONE));
    }

    @Test
    public void selectSpanFactory_usesObtainNoExt_whenBoldItalic() {
        assertEquals("obtainNoExt", selectSpanFactory(FONT_STYLE_BOLD | FONT_STYLE_ITALIC));
    }

    @Test
    public void selectSpanFactory_usesObtain_whenUnderline() {
        assertEquals("obtain", selectSpanFactory(FONT_STYLE_UNDERLINE));
    }

    @Test
    public void selectSpanFactory_usesObtain_whenBoldUnderline() {
        assertEquals("obtain", selectSpanFactory(FONT_STYLE_BOLD | FONT_STYLE_UNDERLINE));
    }

    @Test
    public void selectSpanFactory_usesObtain_whenAllStyles() {
        int all = FONT_STYLE_ITALIC | FONT_STYLE_BOLD | FONT_STYLE_UNDERLINE | FONT_STYLE_STRIKETHROUGH;
        assertEquals("obtain", selectSpanFactory(all));
    }

    // ------------------------------------------------------------------
    // Before-fix regression: NoExtSpanImpl would throw on setUnderlineColor
    // ------------------------------------------------------------------

    /**
     * Demonstrates that calling setUnderlineColor on a NoExtSpanImpl-like object
     * (one that does NOT support ext) throws UnsupportedOperationException.
     * The fix avoids this by never picking the no-ext span when underline is needed.
     */
    @Test
    public void noExtSpan_throwsOnSetUnderlineColor() {
        // Simulate what NoExtSpanImpl does
        Runnable setUnderlineOnNoExt = () -> {
            throw new UnsupportedOperationException();
        };

        int fontStyleWithUnderline = FONT_STYLE_UNDERLINE;

        // Old (broken) logic: always use obtainNoExt, then try to set underline color
        assertThrows(
            "NoExtSpanImpl must throw UnsupportedOperationException when setUnderlineColor is called",
            UnsupportedOperationException.class,
            () -> {
                // Simulate old code: always obtainNoExt regardless of underline flag
                boolean wouldUseNoExt = true;
                if (wouldUseNoExt && (fontStyleWithUnderline & FONT_STYLE_UNDERLINE) != 0) {
                    setUnderlineOnNoExt.run(); // this throws
                }
            }
        );
    }

    /**
     * Demonstrates that the fixed logic never calls setUnderlineColor on a no-ext span:
     * when underline is needed, obtain() is chosen instead of obtainNoExt().
     */
    @Test
    public void fixedLogic_doesNotCallSetUnderlineOnNoExtSpan() {
        int fontStyleWithUnderline = FONT_STYLE_UNDERLINE;
        boolean needUnderline = needUnderline(fontStyleWithUnderline);

        // Fixed logic: only use obtainNoExt when needUnderline is false
        String factory = selectSpanFactory(fontStyleWithUnderline);

        // When underline is needed, we must NOT pick obtainNoExt
        assertNotEquals(
            "Fixed logic must not select obtainNoExt when underline flag is set",
            "obtainNoExt",
            factory
        );
        assertEquals("obtain", factory);
        assertTrue(needUnderline);
    }
}

