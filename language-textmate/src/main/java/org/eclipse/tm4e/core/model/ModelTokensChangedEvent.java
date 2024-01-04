/**
 * Copyright (c) 2015-2017 Angelo ZERR.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Initial code from https://github.com/microsoft/vscode-textmate/
 * Initial copyright Copyright (C) Microsoft Corporation. All rights reserved.
 * Initial license: MIT
 *
 * Contributors:
 * - Microsoft Corporation: Initial code, written in TypeScript, licensed under MIT license
 * - Angelo Zerr <angelo.zerr@gmail.com> - translation and adaptation to Java
 */
package org.eclipse.tm4e.core.model;

import java.util.List;

import org.eclipse.tm4e.core.internal.utils.AbstractListeners;
import org.eclipse.tm4e.core.internal.utils.StringUtils;

/**
 * Model tokens changed event.
 *
 * @see <a href=
 *      "https://github.com/microsoft/vscode/blob/d81ca6dfcae29a9bf0f648b94dff145b3665fac1/src/vs/editor/common/textModelEvents.ts#L100">
 *      github.com/microsoft/vscode/main/src/vs/editor/common/textModelEvents.ts <code>#IModelTokensChangedEvent</code></a>
 */
public class ModelTokensChangedEvent {

	public interface Listenable {
		/**
		 * Add the given {@link ModelTokensChangedEvent} listener.
		 *
		 * @param listener to add
		 *
		 * @return <code>false</code> if the listener was registered already, otherwise <code>true</code>
		 */
		boolean addModelTokensChangedListener(Listener listener);

		/**
		 * Removes the given {@link ModelTokensChangedEvent} listener.
		 *
		 * @param listener to remove
		 *
		 * @return <code>false</code> if the listener was not registered, otherwise <code>true</code>
		 */
		boolean removeModelTokensChangedListener(Listener listener);
	}

	public interface Listener {
		void onModelTokensChanged(ModelTokensChangedEvent event);
	}

	public static class Listeners extends AbstractListeners<Listener, ModelTokensChangedEvent> {
		@Override
		public void dispatchEvent(final ModelTokensChangedEvent event, final Listener listener) {
			listener.onModelTokensChanged(event);
		}

		/**
		 * If ranges is not empty, constructs a {@link ModelTokensChangedEvent} from the given input
		 * and forwards it to all registered listeners.
		 */
		public void dispatchEvent(final List<Range> ranges, final ITMModel model) {
			if (ranges.isEmpty())
				return;
			dispatchEvent(new ModelTokensChangedEvent(ranges, model));
		}
	}

	public final List<Range> ranges;
	public final ITMModel model;

	public ModelTokensChangedEvent(final List<Range> ranges, final ITMModel model) {
		this.ranges = ranges;
		this.model = model;
	}

	public ModelTokensChangedEvent(final Range range, final ITMModel model) {
		this(List.of(range), model);
	}

	@Override
	public String toString() {
		return StringUtils.toString(this, sb -> {
			sb.append("ranges=").append(ranges).append(", ");
			sb.append("model=").append(model);
		});
	}
}
