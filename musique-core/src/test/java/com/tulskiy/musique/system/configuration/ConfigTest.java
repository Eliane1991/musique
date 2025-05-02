/*
 * Copyright (c) 2008, 2009, 2010 Denis Tulskiy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * version 3 along with this work.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.tulskiy.musique.system.configuration;

import org.junit.Before;
import org.junit.Test;

import java.awt.*;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * Author: Denis Tulskiy
 * Date: Jun 16, 2010
 */
public class ConfigTest {
    private Configuration config;

    @Before
    public void setUp() {
        Logger.getLogger(getClass().getName()).setLevel(Level.OFF);
        config = new Configuration();

//        String configStr =
//                "font: Serif, 0, 14\n" +
//                        "color: #DECAFE\n" +
//                        "int: 12345\n" +
//                        "float: 1.2345\n" +
//                        "rectangle: 12 34 56 78\n" +
//                        "string: some string\n" +
//                        "list:\n" +
//                        "  item 1\n" +
//                        "  item 2\n" ;


        String configStr =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<data>\n" +
                        "  <font>Serif, 0, 14</font>\n" +
                        "  <color>#DECAFE</color>\n" +
                        "  <int>12345</int>\n" +
                        "  <float>1.2345</float>\n" +
                        "  <rectangle>12 34 56 78</rectangle>\n" +
                        "  <string>some string</string>\n" +
                        "  <list>\n" +
                        "    <item>item 1</item>\n" +
                        "    <item>item 2</item>\n" +
                        "  </list>\n" +
                        "</data>" ;

        System.out.println(configStr);

        config.load(new StringReader(configStr));
    }

    @Test
    public void testLoad() {
        Font font = config.getFont("font", null);
        assertNotNull(font);
        assertEquals(new Font("Serif", 0, 14), font);

        Color color = config.getColor("color", null);
        assertNotNull(color);
        assertEquals(new Color(0xDECAFE), color);

        Rectangle rectangle = config.getRectangle("rectangle", null);
        assertNotNull(rectangle);
        assertEquals(new Rectangle(12, 34, 56, 78), rectangle);

        int anInt = config.getInt("int", -1);
        assertEquals(12345, anInt);

        float aFloat = config.getFloat("float", -1);
        assertEquals(1.2345, aFloat, 0.00001);

        String string = config.getString("string", null);
        assertNotNull(string);
        assertEquals("some string", string);

//        List<String> list = config.getList("list", null);
//        ArrayList<String> expected = new ArrayList<String>();
//        expected.add("item 1");
//        expected.add("item 2");
//        assertEquals(expected, list);
    }

    @Test
    public void testDefaults() {
        Font font = config.getFont("doesNotExist1", null);
        assertNull(font);

        Color color = config.getColor("doesNotExist2", null);
        assertNull(color);

        Rectangle rectangle = config.getRectangle("doesNotExist3", null);
        assertNull(rectangle);

        int anInt = config.getInt("doesNotExist4", -1);
        assertEquals(-1, anInt);

        float aFloat = config.getFloat("doesNotExist5", -1);
        assertEquals(-1, aFloat, 0.00001);

        String string = config.getString("doesNotExist6", null);
        assertNull(string);
    }

    @Test
    public void testSave() {
        config.setInt("newInt", 123);
        config.setFloat("newFloat", 1.23f);
        config.setString("newString", "new string");
        config.setColor("newColor", new Color(0xD017AA));
        config.setFont("newFont", new Font("Serif", 0, 14));
        config.setRectangle("newRect", new Rectangle(98, 76, 54, 32));

        Font font = config.getFont("newFont", null);
        assertNotNull(font);
        assertEquals(new Font("Serif", 0, 14), font);

        Color color = config.getColor("newColor", null);
        assertNotNull(color);
        assertEquals(new Color(0xD017AA), color);

        Rectangle rectangle = config.getRectangle("newRect", null);
        assertNotNull(rectangle);
        assertEquals(new Rectangle(98, 76, 54, 32), rectangle);

        int anInt = config.getInt("newInt", -1);
        assertEquals(123, anInt);

        float aFloat = config.getFloat("newFloat", -1);
        assertEquals(1.23, aFloat, 0.00001);

        String string = config.getString("newString", null);
        assertNotNull(string);
        assertEquals("new string", string);
    }
}