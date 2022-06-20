/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2022  Rosemoe
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 *
 *     Please contact Rosemoe by email 2073412493@qq.com if you need
 *     additional information or have any questions
 */
package org.eclipse.tm4e.core.model;

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
