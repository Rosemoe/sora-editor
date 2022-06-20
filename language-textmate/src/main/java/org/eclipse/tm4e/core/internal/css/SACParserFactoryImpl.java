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
package org.eclipse.tm4e.core.internal.css;

import org.eclipse.tm4e.core.theme.css.SACConstants;
import org.eclipse.tm4e.core.theme.css.SACParserFactory;
import org.w3c.css.sac.Parser;

import java.util.HashMap;
import java.util.Map;

/**
 * SAC Parser factory implementation. By default, this SAC FActory support
 * Flute, SteadyState and Batik SAC Parser.
 */
public class SACParserFactoryImpl extends SACParserFactory {

    private static Map<String, String> parsers = new HashMap<>();

    static {
        // Register Flute SAC Parser
        registerSACParser(SACConstants.SACPARSER_FLUTE);
        // Register Flute SAC CSS3Parser
        registerSACParser(SACConstants.SACPARSER_FLUTE_CSS3);
        // Register SteadyState SAC Parser
        registerSACParser(SACConstants.SACPARSER_STEADYSTATE);
        // Register Batik SAC Parser
        registerSACParser(SACConstants.SACPARSER_BATIK);
    }

    public SACParserFactoryImpl() {
        // Flute parser is the default SAC Parser to use.
        super.setPreferredParserName(SACConstants.SACPARSER_BATIK);
    }

    /**
     * Register SAC parser name.
     *
     * @param parser
     */
    public static void registerSACParser(String parser) {
        registerSACParser(parser, parser);
    }

    /**
     * register SAC parser with name <code>name</code> mapped with Class name
     * <code>classNameParser</code>.
     *
     * @param name
     * @param classNameParser
     */
    public static void registerSACParser(String name, String classNameParser) {
        parsers.put(name, classNameParser);
    }

    @Override
    public Parser makeParser(String name) throws ClassNotFoundException, IllegalAccessException, InstantiationException,
            NullPointerException, ClassCastException {
        String classNameParser = parsers.get(name);
        if (classNameParser != null) {
            Class<?> classParser = super.getClass().getClassLoader().loadClass(classNameParser);
            return (Parser) classParser.newInstance();
        }
        throw new IllegalAccessException("SAC parser with name=" + name
                + " was not registered into SAC parser factory.");
    }
}
