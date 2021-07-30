/*
 *    CodeEditor - the awesome code editor for Android
 *    Copyright (C) 2020-2021  Rosemoe
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 *     Please contact Rosemoe by email roses2020@qq.com if you need
 *     additional information or have any questions
 */
package io.github.rosemoe.editor.langs.html;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;

import java.io.IOException;
import java.io.StringReader;

import io.github.rosemoe.editor.interfaces.AutoCompleteProvider;
import io.github.rosemoe.editor.interfaces.CodeAnalyzer;
import io.github.rosemoe.editor.interfaces.EditorLanguage;
import io.github.rosemoe.editor.interfaces.NewlineHandler;
import io.github.rosemoe.editor.widget.SymbolPairMatch;

public class HTMLLanguage implements EditorLanguage {
    public static final String[] TAGS = {"a", "abbr", "acronym", "address", "applet", "area", "article", "aside", "audio", "b", "base", "basefont", "bdi", "bdo", "bgsound", "big", "blink", "blockquote", "body", "br", "button", "canvas", "caption", "center", "circle", "cite", "clipPath", "code", "col", "colgroup", "command", "content", "data", "datalist", "dd", "defs", "del", "details", "dfn", "dialog", "dir", "div", "dl", "dt", "element", "ellipse", "em", "embed", "fieldset", "figcaption", "figure", "font", "footer", "foreignObject", "form", "frame", "frameset", "g", "h1", "h2", "h3", "h4", "h5", "h6", "head", "header", "hgroup", "hr", "html", "i", "iframe", "image", "img", "input", "ins", "isindex", "kbd", "keygen", "label", "legend", "li", "line", "linearGradient", "link", "listing", "main", "map", "mark", "marquee", "mask", "math", "menu", "menuitem", "meta", "meter", "multicol", "nav", "nextid", "nobr", "noembed", "noframes", "noscript", "object", "ol", "optgroup", "option", "output", "p", "param", "path", "pattern", "picture", "plaintext", "polygon", "polyline", "pre", "progress", "q", "radialGradient", "rb", "rbc", "rect", "rp", "rt", "rtc", "ruby", "s", "samp", "script", "section", "select", "shadow", "slot", "small", "source", "spacer", "span", "stop", "strike", "strong", "style", "sub", "summary", "sup", "svg", "table", "tbody", "td", "template", "text", "textarea", "tfoot", "th", "thead", "time", "title", "tr", "track", "tspan", "tt", "u", "ul", "var", "video", "wbr", "xmp"};
    public static final String[] ATTRIBUTES = {"accept", "accept-charset", "accesskey", "action", "align", "alt", "async", "autocomplete", "autofocus", "autoplay", "border", "bgcolor", "charset", "checked", "cite", "class", "color", "cols", "colspan", "content", "contenteditable", "controls", "coords", "data", "data-*", "datetime", "default", "defer", "dir", "dirname", "disabled", "draggable", "dropzone", "enctype", "face", "for", "form", "formaction", "headers", "height", "hidden", "high", "href", "hreflang", "http-equiv", "id", "ismap", "kind", "label", "lang", "list", "loop", "low", "max", "maxlength", "media", "method", "min", "multiple", "muted", "name", "novalidate", "onabort", "onafterprint", "onbeforeprint", "onbeforeunload", "onblur", "oncanplay", "oncanplaythrough", "oncanplaythrough", "onchange", "onclick", "oncontextmenu", "oncopy", "oncuechange", "oncut", "ondblclick", "ondrag", "ondragend", "ondragenter", "ondragleave", "ondragover", "ondragstart", "ondrop", "ondurationchange", "onemptied", "onended", "onerror", "onfocus", "onhashchange", "oninput", "oninvalid", "onkeydown", "onkeypress", "onkeyup", "onload", "onloadeddata", "onloadedmetadata", "onloadstart", "onmousedown", "onmousemove", "onmouseout", "onmouseover", "onmouseup", "onmousewheel", "onoffline", "ononline", "onpagehide", "onpageshow", "onpaste", "onpaste", "onpause", "onplay", "onplaying", "onpopstate", "onprogress", "onratechange", "onreset", "onresize", "onscroll", "onsearch", "onseeked", "onseeking", "onselect", "onstalled", "onstorage", "onsubmit", "onsuspend", "ontimeupdate", "ontoggle", "onunload", "onvolumechange", "onwaiting", "onwheel", "open", "optimum", "pattern", "placeholder", "poster", "preload", "readonly", "rel", "required", "reversed", "rows", "rows", "rowspan", "sandbox", "scope", "selected", "shape", "size", "sizes", "span", "spellcheck", "src", "srcdoc", "srclang", "srcset", "start", "step", "style", "tabindex", "target", "title", "translate", "type", "usemap", "value", "width", "wrap"};

    @Override
    public CodeAnalyzer getAnalyzer() {
        return new HTMLAnalyzer();
    }

    @Override
    public AutoCompleteProvider getAutoCompleteProvider() {
        return new HTMLAutoComplete();
    }

    /**
     * Checks if the given character is an auto complete character in HTML
     */
    @Override
    public boolean isAutoCompleteChar(char ch) {
		// isDigit -> required to complete tags which may contain a number
		// For example -> h1, h2, h3, h4...
		// Better solution?
        return Character.isLetter(ch) || Character.isDigit(ch);
    }

    @Override
    public int getIndentAdvance(String content) {
        try {
            HTMLLexer lexer = new HTMLLexer(CharStreams.fromReader(new StringReader(content)));
            Token token = null;
            int advance = 0;
            while (((token = lexer.nextToken()) != null && token.getType() != token.EOF)) {
                switch (token.getType()) {
                    case HTMLLexer.TAG_OPEN:
                        advance++;
                        break;
                    case HTMLLexer.TAG_SLASH:
                    case HTMLLexer.TAG_SLASH_CLOSE:
                        advance--;
                        break;
                }
            }
            advance = Math.max(0, advance);
            return advance * 4;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public boolean useTab() {
        return true;
    }

    @Override
    public CharSequence format(CharSequence text) {
        return text;
    }

    @Override
    public SymbolPairMatch getSymbolPairs() {
        return new SymbolPairMatch.DefaultSymbolPairs();
    }

    @Override
    public NewlineHandler[] getNewlineHandlers() {
        return new NewlineHandler[0];
    }
}
