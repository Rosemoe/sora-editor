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
package org.eclipse.tm4e.core.theme.css;

import org.w3c.css.sac.Parser;

/**
 * SAC parser factory interface to get instance of SAC {@link Parser}.
 */
public interface ISACParserFactory {

    /**
     * Return default instance of SAC Parser. If preferredParserName is filled,
     * it return the instance of SAC Parser registered with this name, otherwise
     * this method search teh SAC Parser class name to instanciate into System
     * property with key org.w3c.css.sac.parser.
     */
    public Parser makeParser() throws ClassNotFoundException,
            IllegalAccessException, InstantiationException,
            NullPointerException, ClassCastException;

    /**
     * Return instance of SAC Parser registered into the factory with name
     * <code>name</code>.
     *
     * @param name
     * @return
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws NullPointerException
     * @throws ClassCastException
     */
    public abstract Parser makeParser(String name)
            throws ClassNotFoundException, IllegalAccessException,
            InstantiationException, NullPointerException, ClassCastException;

    ;
}
