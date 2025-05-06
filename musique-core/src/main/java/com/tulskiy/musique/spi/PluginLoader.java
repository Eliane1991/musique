/*
 * Copyright (c) 2008, 2009, 2010, 2011 Denis Tulskiy
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

package com.tulskiy.musique.spi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Level;

/**
 * Author: Denis Tulskiy
 * Date: 2/27/11
 */
public class PluginLoader {
    private final Logger logger = LoggerFactory.getLogger(getClass().getName());
    private ArrayList<Plugin> activePlugins = new ArrayList<Plugin>();

    public void load() {
        logger.info("Loading plugins");
//            URLClassLoader classLoader = new URLClassLoader(new URL[]{
//                    new File("musique.jar").toURI().toURL(),
//            });
        try {
            ServiceLoader<Plugin> loader = ServiceLoader.load(Plugin.class, getClass().getClassLoader());
            for (Plugin plugin : loader) {
            try {
                logger.info("Loading plugin: " + plugin);
                if (plugin.init()) {
                    activePlugins.add(plugin);
                }
            } catch (Throwable e) {
                logger.warn( "Error loading " + plugin.getDescription(), e);
            }
        }
        } catch (Throwable e) {
            e.printStackTrace();
            logger.warn("Error loading plugins", e);
        }
        logger.info("Finished loading plugins");
    }

    public void shutdown() {
        for (Plugin plugin : activePlugins) {
            plugin.shutdown();
        }
    }

    public List<Plugin> getActivePlugins() {
        return Collections.unmodifiableList(activePlugins);
    }
}
