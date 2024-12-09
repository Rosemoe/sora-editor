/*******************************************************************************
 * Copyright (c) 2015-2017 Angelo ZERR.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 * <p>
 * Contributors:
 * Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 * Sebastian Thomschke (Vegard IT GmbH) - add previousLineText support
 *
 ******************************************************************************/

package io.github.rosemoe.sora.langs.monarch.theme

class ColorMap {

    private var lastColorId: Int = -1
    private val id2color = mutableListOf<String>()
    private val color2id = mutableMapOf<String, Int>()

    fun getId(color: String?): Int {
        if (color == null) {
            return 0
        }
        return color2id.getOrPut(color) {
            lastColorId++
            id2color.add(lastColorId, color)
            lastColorId
        }
    }

    fun getColor(id: Int): String? {
        return id2color.getOrNull(id)
    }

    val colorMap: List<String>
        get() = id2color.toList()

    override fun toString(): String {
        return "ColorMap(lastColorId=$lastColorId, id2color=$id2color, color2id=$color2id)"
    }


}
