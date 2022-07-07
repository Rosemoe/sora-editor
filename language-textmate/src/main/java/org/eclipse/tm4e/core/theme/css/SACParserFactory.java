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
import org.w3c.css.sac.helpers.ParserFactory;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.tm4e.core.internal.css.SACParserFactoryImpl;

/**
 * SAC Parser Factory.
 */
public abstract class SACParserFactory extends ParserFactory implements ISACParserFactory {

    private static final Logger LOGGER = Logger.getLogger(SACParserFactory.class.getName());

    private String preferredParserName;

    /**
     * Return instance of SACParserFactory
     *
     * @return
     */
    public static ISACParserFactory newInstance() {
        // TODO : manage new instance of SAC Parser Factory like
        // SAXParserFactory.
        return new SACParserFactoryImpl();
    }

    /**
     * Return default instance of SAC Parser. If preferredParserName is filled,
     * it return the instance of SAC Parser registered with this name, otherwise
     * this method search the SAC Parser class name to instanciate into System
     * property with key org.w3c.css.sac.parser.
     */
    @Override
    public Parser makeParser() throws ClassNotFoundException, IllegalAccessException, InstantiationException,
            NullPointerException, ClassCastException {
        try {
            if (preferredParserName != null) {
                return makeParser(preferredParserName);
            }
        } catch (Throwable e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        return super.makeParser();
    }

    /**
     * Return instance of SAC Parser registered into the factory with name
     * <code>name</code>.
     */
    @Override
    public abstract Parser makeParser(String name) throws ClassNotFoundException, IllegalAccessException,
            InstantiationException, NullPointerException, ClassCastException;

    /**
     * Return preferred SAC parser name if it is filled and null otherwise.
     *
     * @return
     */
    public String getPreferredParserName() {
        return preferredParserName;
    }

    /**
     * Set the preferred SAC parser name to use when makeParser is called.
     *
     * @param preferredParserName
     */
    public void setPreferredParserName(String preferredParserName) {
        this.preferredParserName = preferredParserName;
    }
}
