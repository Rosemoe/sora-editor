package io.github.rosemoe.sora.langs.desc;

import io.github.rosemoe.sora.langs.universal.LanguageDescription;

public class PhpDescription implements LanguageDescription {
    public boolean isLineCommentStart(char c, char c2) {
        return c == '/' && c2 == '/';
    }

    public boolean isLongCommentEnd(char c, char c2) {
        return c == '*' && c2 == '/';
    }

    public boolean isLongCommentStart(char c, char c2) {
        return c == '/' && c2 == '*';
    }

    public boolean isSupportBlockLine() {
        return true;
    }

    public boolean useTab() {
        return false;
    }

    public boolean isOperator(char[] cArr, int i) {
        if (i != 1) {
            return false;
        }
        char c = cArr[0];
        return c == '+' || c == '-' || c == '{' || c == '}' || c == '[' || c == ']' || c == '(' || c == ')' || c == '|' || c == ':' || c == '.' || c == ',' || c == ';' || c == '*' || c == '/' || c == '&' || c == '^' || c == '%' || c == '!' || c == '~' || c == '<' || c == '>' || c == '=' || c == '\\' || c == '#';
    }

    public String[] getKeywords() {
        return new String[]{"abstract", "and", "array", "as", "break", "callable", "case", "catch", "class", "clone", "const", "continue", "declare", "default", "die", "do", "echo", "else", "elseif", "empty", "enddeclare", "endfor", "endforeach", "endif", "endswitch", "endwhile", "eval", "exit", "extends", "final", "finally", "for", "foreach", "function", "global", "goto", "if", "implements", "include", "include_once", "instanceof", "insteadof", "interface", "isset", "list", "namespace", "new", "or", "print","require", "require_once", "return", "self", "static", "switch", "parent", "throw", "trait", "try", "true", "TRUE", "false", "FALSE", "null", "NULL", "unset", "use", "var", "while", "xor", "yield", "__halt_compiler", "__CLASS__", "__DIR__", "__FILE__", "__FUNCTION__", "__LINE__", "__METHOD__", "__NAMESPACE__", "__TRAIT__"};
    }

    public int getOperatorAdvance(String str) {
        str.hashCode();
        if (!str.equals("{")) {
            return !str.equals("}") ? 0 : -4;
        }
        return 4;
    }

    public boolean isBlockStart(String str) {
        return str.equals("{");
    }

    public boolean isBlockEnd(String str) {
        return str.equals("}");
    }
}
///code by ninja coder
