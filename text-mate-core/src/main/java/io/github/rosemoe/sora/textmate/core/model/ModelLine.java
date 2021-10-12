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
package io.github.rosemoe.sora.textmate.core.model;

import java.util.List;

public class ModelLine {

    //String text;
    boolean isInvalid;
    TMState state;
    List<TMToken> tokens;

    public ModelLine(/*String text*/) {
        //this.text = text;
    }

    public void resetTokenizationState() {
        this.state = null;
        this.tokens = null;
    }

    public TMState getState() {
        return state;
    }

    public void setState(TMState state) {
        this.state = state;
    }

    public List<TMToken> getTokens() {
        return tokens;
    }

    public void setTokens(List<TMToken> tokens) {
        this.tokens = tokens;
    }
}
