/**
 * Copyright (c) 2015-2017 Angelo ZERR.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package io.github.rosemoe.sora.textmate.languageconfiguration.internal.supports;

public class IndentationRule {
    private String increaseIndentPattern;

    public String getIncreaseIndentPattern() {
        return increaseIndentPattern;
    }

    public void setIncreaseIndentPattern(String increaseIndentPattern) {
        this.increaseIndentPattern = increaseIndentPattern;
    }

    public String getDecreaseIndentPattern() {
        return decreaseIndentPattern;
    }

    public void setDecreaseIndentPattern(String decreaseIndentPattern) {
        this.decreaseIndentPattern = decreaseIndentPattern;
    }

    private String decreaseIndentPattern;


    public IndentationRule(String increaseIndentPattern, String decreaseIndentPattern) {
        this.increaseIndentPattern = increaseIndentPattern;
        this.decreaseIndentPattern = decreaseIndentPattern;
    }
}
