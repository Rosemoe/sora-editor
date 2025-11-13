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
package io.github.rosemoe.sora.langs.textmate;

import android.graphics.Color;

import androidx.annotation.NonNull;

import org.eclipse.tm4e.core.internal.theme.Theme;
import org.eclipse.tm4e.core.internal.theme.raw.IRawTheme;
import org.eclipse.tm4e.core.internal.theme.raw.RawTheme;
import org.eclipse.tm4e.core.registry.IThemeSource;

import java.util.List;

import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel;
import io.github.rosemoe.sora.langs.textmate.utils.ColorUtils;
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;

public class TextMateColorScheme extends EditorColorScheme implements ThemeRegistry.ThemeChangeListener {

    private Theme theme;

    private IRawTheme rawTheme;

    @Deprecated
    private IThemeSource themeSource;

    private ThemeModel currentTheme;

    private final ThemeRegistry themeRegistry;

    public TextMateColorScheme(ThemeRegistry themeRegistry, ThemeModel themeModel) {
        this.themeRegistry = themeRegistry;
        currentTheme = themeModel;
        setTheme(themeModel);
    }

    @Deprecated
    public static TextMateColorScheme create(IThemeSource themeSource) {
        return create(new ThemeModel(themeSource));
    }

    public static TextMateColorScheme create(ThemeModel themeModel) {
        return create(ThemeRegistry.getInstance(), themeModel);
    }

    public static TextMateColorScheme create(ThemeRegistry themeRegistry) {
        return create(ThemeRegistry.getInstance(), themeRegistry.getCurrentThemeModel());
    }

    public static TextMateColorScheme create(ThemeRegistry themeRegistry, ThemeModel themeModel) {
        return new TextMateColorScheme(themeRegistry, themeModel);
    }

    public void setTheme(ThemeModel themeModel) {
        currentTheme = themeModel;
        super.colors.clear();
        this.rawTheme = themeModel.getRawTheme();
        this.theme = themeModel.getTheme();
        this.themeSource = themeModel.getThemeSource();
        applyDefault();
    }

    @Override
    public void onChangeTheme(ThemeModel newTheme) {
        setTheme(newTheme);
    }

    @Override
    public void applyDefault() {
        super.applyDefault();

        if (themeRegistry != null && !themeRegistry.hasListener(this)) {
            themeRegistry.addListener(this);
        }

        if (rawTheme == null) {
            return;
        }
        var settings = rawTheme.getSettings();

        RawTheme rawSubTheme;
        if (settings == null) {
            rawSubTheme = ((RawTheme) ((RawTheme) rawTheme).get("colors"));
            if (rawSubTheme != null)
                applyVSCTheme(rawSubTheme);
        } else {
            rawSubTheme = (RawTheme) ((List<?>) settings).get(0);
            if (rawSubTheme != null)
                rawSubTheme = (RawTheme) rawSubTheme.getSetting();
            if (rawSubTheme != null)
                applyTMTheme(rawSubTheme);
        }
    }


    private void applyVSCTheme(RawTheme RawTheme) {
        setColor(LINE_DIVIDER, Color.TRANSPARENT);

        String caret = (String) RawTheme.get("editorCursor.foreground");
        if (caret != null) {
            setColor(SELECTION_INSERT, ColorUtils.parseRGBAToARGB(caret));
        }

        String selection = (String) RawTheme.get("editor.selectionBackground");
        if (selection != null) {
            setColor(SELECTED_TEXT_BACKGROUND, ColorUtils.parseRGBAToARGB(selection));
        }

        String invisibles = (String) RawTheme.get("editorWhitespace.foreground");
        if (invisibles != null) {
            setColor(NON_PRINTABLE_CHAR, ColorUtils.parseRGBAToARGB(invisibles));
        }

        String lineHighlight = (String) RawTheme.get("editor.lineHighlightBackground");
        if (lineHighlight != null) {
            setColor(CURRENT_LINE, ColorUtils.parseRGBAToARGB(lineHighlight));
        }

        String background = (String) RawTheme.get("editor.background");
        if (background != null) {
            setColor(WHOLE_BACKGROUND, ColorUtils.parseRGBAToARGB(background));
            setColor(LINE_NUMBER_BACKGROUND, ColorUtils.parseRGBAToARGB(background));
        }

        String lineHighlightBackground = (String) RawTheme.get("editorLineNumber.foreground");
        if (lineHighlightBackground != null) {
            setColor(LINE_NUMBER, ColorUtils.parseRGBAToARGB(lineHighlightBackground));
        }

        String lineHighlightActiveForeground = (String) RawTheme.get("editorLineNumber.activeForeground");
        if (lineHighlightActiveForeground != null) {
            setColor(LINE_NUMBER_CURRENT, ColorUtils.parseRGBAToARGB(lineHighlightActiveForeground));
        }

        String foreground = (String) RawTheme.get("editor.foreground");
        if (foreground != null) {
            setColor(TEXT_NORMAL, ColorUtils.parseRGBAToARGB(foreground));
        }

        String highlightedDelimitersForeground =
                (String) RawTheme.get("highlightedDelimitersForeground");
        if (highlightedDelimitersForeground != null) {
            setColor(HIGHLIGHTED_DELIMITERS_FOREGROUND, ColorUtils.parseRGBAToARGB(highlightedDelimitersForeground));
        }

        String tooltipBackground = (String) RawTheme.get("tooltipBackground");
        if (tooltipBackground != null) {
            setColor(DIAGNOSTIC_TOOLTIP_BACKGROUND, ColorUtils.parseRGBAToARGB(tooltipBackground));
        }

        String tooltipBriefMessageColor = (String) RawTheme.get("tooltipBriefMessageColor");
        if (tooltipBriefMessageColor != null) {
            setColor(DIAGNOSTIC_TOOLTIP_BRIEF_MSG, ColorUtils.parseRGBAToARGB(tooltipBriefMessageColor));
        }

        String tooltipDetailedMessageColor = (String) RawTheme.get("tooltipDetailedMessageColor");
        if (tooltipDetailedMessageColor != null) {
            setColor(DIAGNOSTIC_TOOLTIP_DETAILED_MSG, ColorUtils.parseRGBAToARGB(tooltipDetailedMessageColor));
        }

        String tooltipActionColor = (String) RawTheme.get("tooltipActionColor");
        if (tooltipActionColor != null) {
            setColor(DIAGNOSTIC_TOOLTIP_ACTION, ColorUtils.parseRGBAToARGB(tooltipActionColor));
        }

        String suggestHighlightForeground = (String) RawTheme.get("editorSuggestWidget.highlightForeground");
        if (suggestHighlightForeground != null) {
            setColor(COMPLETION_WND_TEXT_MATCHED, ColorUtils.parseRGBAToARGB(suggestHighlightForeground));
        }

        String suggestBackground = (String) RawTheme.get("editorSuggestWidget.background");
        if (suggestBackground != null) {
            setColor(COMPLETION_WND_BACKGROUND, ColorUtils.parseRGBAToARGB(suggestBackground));
        }

        String suggestForeground = (String) RawTheme.get("editorSuggestWidget.foreground");
        if (suggestForeground != null) {
            setColor(COMPLETION_WND_TEXT_PRIMARY, ColorUtils.parseRGBAToARGB(suggestForeground));
        }

        String suggestSelectedBackground = (String) RawTheme.get("editorSuggestWidget.selectedBackground");
        if (suggestSelectedBackground != null) {
            setColor(COMPLETION_WND_ITEM_CURRENT, ColorUtils.parseRGBAToARGB(suggestSelectedBackground));
        }

        String editorIndentGuideBackground = (String) RawTheme.get("editorIndentGuide.background");
        int blockLineColor = ((getColor(WHOLE_BACKGROUND) + getColor(TEXT_NORMAL)) / 2) & 0x00FFFFFF | 0x88000000;
        int blockLineColorCur = (blockLineColor) | 0xFF000000;

        if (editorIndentGuideBackground != null) {
            setColor(BLOCK_LINE, ColorUtils.parseRGBAToARGB(editorIndentGuideBackground));
        } else {
            setColor(BLOCK_LINE, blockLineColor);
        }

        String editorIndentGuideActiveBackground = (String) RawTheme.get("editorIndentGuide.activeBackground");

        if (editorIndentGuideActiveBackground != null) {
            setColor(BLOCK_LINE_CURRENT, ColorUtils.parseRGBAToARGB(editorIndentGuideActiveBackground));
        } else {
            setColor(BLOCK_LINE_CURRENT, blockLineColorCur);
        }

        String wordHighlightStrongBackground = (String) RawTheme.get("editor.wordHighlightStrongBackground");
        if (wordHighlightStrongBackground != null) {
            setColor(TEXT_HIGHLIGHT_STRONG_BACKGROUND, ColorUtils.parseRGBAToARGB(wordHighlightStrongBackground));
        }

        String wordHighlightBackground = (String) RawTheme.get("editor.wordHighlightBackground");
        if (wordHighlightBackground != null) {
            setColor(TEXT_HIGHLIGHT_BACKGROUND, ColorUtils.parseRGBAToARGB(wordHighlightBackground));
        }

        String findMatchBackground = (String) RawTheme.get("editor.findMatchBackground");
        if (findMatchBackground != null) {
            setColor(MATCHED_TEXT_BACKGROUND, ColorUtils.parseRGBAToARGB(findMatchBackground));
        }
    }

    @Override
    public boolean isDark() {
        var superIsDark = super.isDark();
        if (superIsDark) {
            return true;
        }
        if (currentTheme != null) {
            return currentTheme.isDark();
        }
        return false;
    }

    private void applyTMTheme(RawTheme RawTheme) {
        setColor(LINE_DIVIDER, Color.TRANSPARENT);

        String caret = (String) RawTheme.get("caret");
        if (caret != null) {
            setColor(SELECTION_INSERT, ColorUtils.parseRGBAToARGB(caret));
        }

        String selection = (String) RawTheme.get("selection");
        if (selection != null) {
            setColor(SELECTED_TEXT_BACKGROUND, ColorUtils.parseRGBAToARGB(selection));
        }

        String invisibles = (String) RawTheme.get("invisibles");
        if (invisibles != null) {
            setColor(NON_PRINTABLE_CHAR, ColorUtils.parseRGBAToARGB(invisibles));
        }

        String lineHighlight = (String) RawTheme.get("lineHighlight");
        if (lineHighlight != null) {
            setColor(CURRENT_LINE, ColorUtils.parseRGBAToARGB(lineHighlight));
        }

        String background = (String) RawTheme.get("background");
        if (background != null) {
            setColor(WHOLE_BACKGROUND, ColorUtils.parseRGBAToARGB(background));
            setColor(LINE_NUMBER_BACKGROUND, ColorUtils.parseRGBAToARGB(background));
        }

        String foreground = (String) RawTheme.get("foreground");
        if (foreground != null) {
            setColor(TEXT_NORMAL, ColorUtils.parseRGBAToARGB(foreground));
        }

        String highlightedDelimitersForeground =
                (String) RawTheme.get("highlightedDelimitersForeground");
        if (highlightedDelimitersForeground != null) {
            setColor(HIGHLIGHTED_DELIMITERS_FOREGROUND, ColorUtils.parseRGBAToARGB(highlightedDelimitersForeground));
        }

        String completionWindowBackground = (String) RawTheme.get("completionWindowBackground");
        if (completionWindowBackground != null) {
            setColor(COMPLETION_WND_BACKGROUND, ColorUtils.parseRGBAToARGB(completionWindowBackground));
        }

        String completionWindowBackgroundCurrent = (String) RawTheme.get("completionWindowBackgroundCurrent");
        if (completionWindowBackgroundCurrent != null) {
            setColor(COMPLETION_WND_ITEM_CURRENT, ColorUtils.parseRGBAToARGB(completionWindowBackgroundCurrent));
        }

        //TMTheme seems to have no fields to control BLOCK_LINE colors
        int blockLineColor = ((getColor(WHOLE_BACKGROUND) + getColor(TEXT_NORMAL)) / 2) & 0x00FFFFFF | 0x88000000;
        setColor(BLOCK_LINE, blockLineColor);
        int blockLineColorCur = (blockLineColor) | 0xFF000000;
        setColor(BLOCK_LINE_CURRENT, blockLineColorCur);
    }

    @Override
    public int getColor(int type) {
        if (type >= 255) {
            // Cache colors in super class
            var superColor = super.getColor(type);
            if (superColor == 0) {
                if (theme != null) {
                    String color;
                    try {
                        color = theme.getColor(type - 255);
                    } catch (IndexOutOfBoundsException e) {
                        return super.getColor(TEXT_NORMAL);
                    }
                    var newColor = (color != null && !"@default".equalsIgnoreCase(color)) ?
                            ColorUtils.parseRGBAToARGB(color) : super.getColor(TEXT_NORMAL);
                    super.colors.put(type, newColor);
                    return newColor;
                }
                return super.getColor(TEXT_NORMAL);
            } else {
                return superColor;
            }
        }
        return super.getColor(type);
    }

    @Override
    public void detachEditor(@NonNull CodeEditor editor) {
        super.detachEditor(editor);
        themeRegistry.removeListener(this);
    }

    @Override
    public void attachEditor(@NonNull CodeEditor editor) {
        super.attachEditor(editor);
        try {
            themeRegistry.loadTheme(currentTheme);
        } catch (Exception e) {
            //throw new RuntimeException(e);
        }
        setTheme(currentTheme);
        editor.rerunAnalysis();
    }

    @Deprecated
    public IRawTheme getRawTheme() {
        return rawTheme;
    }


    @Deprecated
    public IThemeSource getThemeSource() {
        return themeSource;
    }
}
