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

package io.github.rosemoe.sora.langs.monarch.languageconfiguration.model

data class CompleteEnterAction(
    /**
     * Describe what to do with the indentation.
     */
    val indentAction: Int,
    /**
     * Describes text to be appended after the new line and after the indentation.
     */
    val appendText: String,
    /**
     * Describes the number of characters to remove from the new line's indentation.
     */
    val removeText: Int?,
    /**
     * The line's indentation minus removeText
     */
    val indentation: String
)