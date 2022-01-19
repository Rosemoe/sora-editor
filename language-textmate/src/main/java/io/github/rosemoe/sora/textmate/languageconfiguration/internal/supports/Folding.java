/**
 *  Copyright (c) 2018 Red Hat Inc. and others.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  Lucas Bullen (Red Hat Inc.) - initial API and implementation
 */
package io.github.rosemoe.sora.textmate.languageconfiguration.internal.supports;

public class Folding {

	private final Boolean offSide;

	private final String markersStart;

	private final String markersEnd;

	public Folding(Boolean offSide, String markersStart, String markersEnd) {
		this.offSide = offSide;
		this.markersStart = markersStart;
		this.markersEnd = markersEnd;
	}

	public Boolean getOffSide() {
		return offSide;
	}

	public String getMarkersStart() {
		return markersStart;
	}

	public String getMarkersEnd() {
		return markersEnd;
	}
}