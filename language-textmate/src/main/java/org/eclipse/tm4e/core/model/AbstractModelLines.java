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
package org.eclipse.tm4e.core.model;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tm4e.core.grammar.IStateStack;
import org.eclipse.tm4e.core.internal.grammar.StateStack;
import org.eclipse.tm4e.core.internal.utils.StringUtils;

/**
 * Abstract class for Model lines used by {@link TMModel}.
 * <p>
 * Implementation class must synchronize lines with the lines of the editor content when it changed.
 */
public abstract class AbstractModelLines {

	static final class ModelLine {
		/**
		 * specifies if the {@link ModelLine#tokens} and/or the {@link ModelLine#startState} are out-of-date and do not
		 * represent the current content of the related line in the editor
		 */
		volatile boolean isInvalid = true;
		IStateStack startState = StateStack.NULL;
		List<TMToken> tokens = Collections.emptyList();
	}

	private final List<ModelLine> list = new LinkedList<>();

	@Nullable
	private TMModel model;

	void setModel(@Nullable final TMModel model) {
		this.model = model;
		synchronized (list) {
			for (ModelLine line : list) {
				line.isInvalid = true;
			}
		}
	}

	protected void addLines(final int lineIndex, final int count) {
		if (count < 1)
			return;

		synchronized (list) {
			final var firstLine = getOrNull(lineIndex);
			for (int i = 0; i < count; i++) {
				list.add(lineIndex, new ModelLine());
			}
			if (firstLine != null) {
				list.get(lineIndex).startState = firstLine.startState;
			}
			updateLine(lineIndex);
		}
	}

	/**
	 * Removes one or more lines at the given index.
	 *
	 * @param lineIndex (0-based)
	 * @param count number of lines to remove
	 *
	 * @throws IndexOutOfBoundsException if <code>lineIndex < 0 || lineIndex >= {@link #getNumberOfLines()}</code>
	 */
	protected void removeLines(final int lineIndex, int count) {
		if (count < 1)
			return;

		synchronized (list) {
			count = Math.min(count, getNumberOfLines() - lineIndex);
			for (int i = 0; i < count; i++) {
				list.remove(lineIndex);
			}
			updateLine(lineIndex);
		}
	}

	/**
	 * Replaces lines at the given index
	 *
	 * @param lineIndex (0-based)
	 * @param linesRemoved number of lines that are replaced
	 * @param linesAdded number of lines that are replacing the replaced lines
	 *
	 * @throws IndexOutOfBoundsException if <code>lineIndex < 0 || lineIndex >= {@link #getNumberOfLines()}</code>
	 */
	protected void replaceLines(final int lineIndex, int linesRemoved, final int linesAdded) {
		if (linesRemoved == 0 && linesAdded == 0)
			return;

		// check if operation single line update
		if (linesRemoved == 1 && linesAdded == 1) {
			updateLine(lineIndex);
			return;
		}

		synchronized (list) {
			final var firstLine = getOrNull(lineIndex);
			linesRemoved = Math.min(linesRemoved, getNumberOfLines() - lineIndex);
			for (int i = 0; i < linesRemoved; i++) {
				list.remove(lineIndex);
			}
			for (int i = 0; i < linesAdded; i++) {
				list.add(lineIndex, new ModelLine());
			}

			if (firstLine != null) {
				list.get(lineIndex).startState = firstLine.startState;
			}
			updateLine(lineIndex);
		}
	}

	/**
	 * Marks a line as updated.
	 *
	 * @param lineIndex (0-based)
	 */
	protected void updateLine(final int lineIndex) {
		if (model != null) {
			model.invalidateLine(lineIndex);
		}
	}

	/**
	 * @throws IndexOutOfBoundsException if <code>lineIndex < 0 || lineIndex >= {@link #getNumberOfLines()}</code>
	 */
	ModelLine get(final int lineIndex) {
		synchronized (list) {
			return list.get(lineIndex);
		}
	}

	@Nullable
	ModelLine getOrNull(final int lineIndex) {
		synchronized (list) {
			if (lineIndex > -1 && lineIndex < list.size())
				return list.get(lineIndex);
		}
		return null;
	}

	public int getNumberOfLines() {
		synchronized (list) {
			return list.size();
		}
	}

	/**
	 * @param lineIndex (0-based)
	 */
	public abstract String getLineText(int lineIndex) throws Exception;

	public void dispose() {
	}

	@Override
	public String toString() {
		synchronized (list) {
			return StringUtils.toString(this, sb -> {
				if (!list.isEmpty()) {
					for (int i = 0; i < list.size(); i++) {
						sb.append(i)
							.append(": isInvalid=")
							.append(list.get(i).isInvalid)
							.append(", ");
					}
					sb.setLength(sb.length() - 2);
				}
			});
		}
	}
}