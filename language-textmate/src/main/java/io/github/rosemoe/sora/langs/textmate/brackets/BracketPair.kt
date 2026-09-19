/*******************************************************************************
 * ---------------------------------------------------------------------------------------------
 *  *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  *  Licensed under the MIT License.
 *  *--------------------------------------------------------------------------------------------
 ******************************************************************************/
package io.github.rosemoe.sora.langs.textmate.brackets

/**
 * One bracket pair found by a range query, in line/column offsets.
 *
 * @param start start of the opening delimiter
 * @param openLength length of the opening delimiter
 * @param closeStart start of the closing delimiter
 * @param closeLength length of the closing delimiter, `0` when the opener is unclosed
 * @param level nesting depth counted from the document root
 * @param levelOfEqualBracketType how many colorized pairs of the same bracket type enclose it
 * @param invalid whether the pair is malformed, currently only a closer without an opener
 * @param colorized whether the pair takes part in colorization
 */
internal data class BracketPair(
    val start: TextOffset,
    val openLength: Int,
    val closeStart: TextOffset,
    val closeLength: Int,
    val level: Int,
    val levelOfEqualBracketType: Int,
    val invalid: Boolean,
    val colorized: Boolean
)
