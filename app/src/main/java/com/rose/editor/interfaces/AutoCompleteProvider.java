package com.rose.editor.interfaces;
import java.util.List;

import com.rose.editor.android.ResultItem;
import com.rose.editor.common.TextColorProvider;

/**
 * Interface for auto completion analysis
 * @author Rose
 */
public interface AutoCompleteProvider
{

    List<ResultItem> getAutoCompleteItems(String prefix, boolean isInCodeBlock, TextColorProvider.TextColors colors, int line);

}

