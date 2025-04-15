package io.github.rosemoe.sora.lsp.utils;

import android.annotation.SuppressLint;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.lsp4j.CompletionItem;

import io.github.rosemoe.sora.widget.CodeEditor;

public class OtherUtils {
    @SuppressLint("NewApi")
    public static String toString(List<?> list) {
        return Arrays
                .toString(list
                .stream()
                .map(Object::toString)
                .collect(java.util.stream.Collectors.toList())
                .toArray(new String[0]));
    }
    @SuppressLint("NewApi")
    public static String compeltionItemListToString(List<CompletionItem> list) {
            return Arrays
                    .toString(list
                            .stream()
                            .map(completionItem -> {
                                StringBuilder str = new StringBuilder("detail:" + completionItem.getDetail() + "\n");
                                str.append("label:" +completionItem.getLabel() + '\n');
                                str.append("insertText:" + completionItem.getInsertText() + "\n");
                                str.append("data:" + completionItem.getData() + "\n");
                                return str.toString();
                            })
                            .collect(java.util.stream.Collectors.toList())
                            .toArray(new String[0]));
    }
    @SuppressLint("NewApi")
    public static <K,V> K getKeyClass(Map<K,V> map, V value) {
        for (K key: map.keySet()) {
            if (map.get(key).getClass() == value.getClass()) return key;
        }
        return null;
    }

    public static int handleOutOfBoundIndex(StringIndexOutOfBoundsException e, CodeEditor editor) {
        //example : e.getMessage() = "Column 15 out of bounds. line: 15 , column count (line separator included):14"
        List<Integer> numbers = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(e.getMessage());

        while (matcher.find()) {
            numbers.add(Integer.parseInt(matcher.group()));
        }
        Log.i("out",String.format("message:%s,result:%s",e.getMessage(),toString(numbers)));
        int size = numbers.get(0) - numbers.get(2);
        for (int i = 0; i <size ; i++) {
            Log.i("out","insert space");
            editor.getText().insert(editor.getCursor().getRange().getEnd().getLine(),
                    editor.getCursor().getRange().getEnd().getColumn(),
                    " ");
        }
        editor.getText().insert(editor.getCursor().getRange().getEnd().getLine(),
                editor.getCursor().getRange().getEnd().getColumn(),
                "\n");

        return numbers.get(0) //out bounds column count
                - numbers.get(2); //column count
    }
}
