package io.github.rosemoe.sora.langs.desc;

import io.github.rosemoe.sora.langs.universal.LanguageDescription;

public class ShellDescription implements LanguageDescription {
    public boolean isLineCommentStart(char c, char c2) {
        return c == '#' && c2 == '#';
    }

    public boolean isLongCommentEnd(char c, char c2) {
        return c == '#' && c2 == '#';
    }

    public boolean isLongCommentStart(char c, char c2) {
        return c == '#' && c2 == '#';
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
    return new String[]{"alias","case","hash","wait","bg","bind","break","builtin","debugger","cd","command","compgen","declare","cat","dirs","disown","do","done","fc","fg","fi","for","function","getopts","jobs","kill","break","umask","let","local","logout","alias","else","elif","ulimit","complete","until","echo","continue","source","while","wh","times","exec","exit","export","zi","eval","pwd","popd","printf","pushd","if","del"};
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
