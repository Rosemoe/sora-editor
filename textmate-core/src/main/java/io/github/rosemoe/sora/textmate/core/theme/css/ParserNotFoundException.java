/*******************************************************************************
 * Copyright (c) 2008, 2013 Angelo Zerr and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 *     IBM Corporation - ongoing development
 *******************************************************************************/
package io.github.rosemoe.sora.textmate.core.theme.css;

/**
 * Exception used when SAC parser is not retrieved.
 */
public class ParserNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 4339161134287845644L;

    public ParserNotFoundException(Throwable throwable) {
        super(throwable);
    }
}
