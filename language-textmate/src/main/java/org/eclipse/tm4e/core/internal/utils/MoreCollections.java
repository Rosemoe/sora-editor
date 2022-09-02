/**
 * Copyright (c) 2022 Sebastian Thomschke and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Sebastian Thomschke - initial implementation
 */
package org.eclipse.tm4e.core.internal.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import org.eclipse.jdt.annotation.Nullable;

public final class MoreCollections {

	@SafeVarargs
	public static <T> List<T> asArrayList(final T... items) {
		final var list = new ArrayList<T>();
		Collections.addAll(list, items);
		return list;
	}

	public static <T> List<T> asArrayList(final T firstItem, final List<T> moreItems) {
		final var list = new ArrayList<T>();
		list.add(firstItem);
		list.addAll(moreItems);
		return list;
	}

	@Nullable
	public static <T> T findFirstMatching(final List<T> list, final Predicate<T> filter) {
		for (final T e : list) {
			if (filter.test(e))
				return e;
		}
		return null;
	}

	@Nullable
	public static <T> T findLastElement(@Nullable final List<T> list) {
		if (list == null || list.isEmpty())
			return null;
		return getLastElement(list);
	}

	/**
	 * @param list a non-empty list with non-nullable elements
	 */
	public static <T> T getLastElement(final List<T> list) {
		return list.get(list.size() - 1);
	}

	/**
	 * Removes the last element in this list.
	 *
	 * @return the element previously at the specified position
	 *
	 * @throws UnsupportedOperationException if the {@code remove} operation is not supported by this list
	 * @throws IndexOutOfBoundsException if the list is empty
	 */
	public static <T> T removeLastElement(final List<T> list) {
		return list.remove(list.size() - 1);
	}

	public static <T> List<T> nullToEmpty(@Nullable final List<T> list) {
		return list == null ? Collections.emptyList() : list;
	}

	private MoreCollections() {
	}
}
