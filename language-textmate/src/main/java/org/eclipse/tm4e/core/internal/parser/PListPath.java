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
package org.eclipse.tm4e.core.internal.parser;

/**
 * Represents the hierarchical path of an PList value calculated based on the &lt;key&gt; tags. E.g.
 * <li><code>fileTypes</code>
 * <li><code>scopeName</code>
 * <li><code>repository/constants/patterns/patterns</code>
 * <li><code>repository/statements/patterns/include</code>
 * <li><code>repository/var-single-variable/beginCaptures</code>
 */
public interface PListPath extends Iterable<String> {

	String first();

	String get(int index);

	String last();

	int size();
}