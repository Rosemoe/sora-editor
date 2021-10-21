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

public class EnterActionAndIndent {

	private final EnterAction enterAction;

	private final String indentation;

	/**
	 * @param enterAction
	 * @param indentation
	 */
	public EnterActionAndIndent(EnterAction enterAction, String indentation) {
		super();
		this.enterAction = enterAction;
		this.indentation = indentation;
	}

	public EnterAction getEnterAction() {
		return enterAction;
	}

	public String getIndentation() {
		return indentation;
	}
}
