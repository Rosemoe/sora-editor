/**
 * Copyright (c) 2015-2017 Angelo ZERR.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 * <p>
 * Contributors:
 * Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package io.github.rosemoe.sora.textmate.core.theme;

/**
 * Font style definitions.
 *
 * @see https://github.com/Microsoft/vscode-textmate/blob/master/src/theme.ts
 *
 */
public class FontStyle {

    public static final int NotSet = -1;

    // This can are bit-flags, so it can be `Italic | Bold`
    public static final int None = 0;
    public static final int Italic = 1;
    public static final int Bold = 2;
    public static final int Underline = 4;

}
