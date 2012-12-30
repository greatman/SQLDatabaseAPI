/*
 * This file is part of SQLDatabaseAPI (2012).
 *
 * SQLDatabaseAPI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SQLDatabaseAPI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SQLDatabaseAPI.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Last modified: 27.12.12 23:09
 */

package com.p000ison.dev.sqlapi;

import com.p000ison.dev.sqlapi.jbdc.JBDCDatabase;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.WeakHashMap;

/**
 * Represents a DatabaseManager
 */
public class DatabaseManager {
    public static WeakHashMap<Database, Object> connections = new WeakHashMap<Database, Object>();
    private static Object obj = new Object();

    /**
     * If you use this, always use the reference which is returned to store the connection. This registration implements
     * a weak reference collection. The advantage of this registration is that there can not be 2 connections to the same
     * database.
     *
     * @param database The database to register
     * @return The reference to the database you should use!
     */
    public static Database registerConnection(Database database)
    {
        if (!connections.containsKey(database)) {
            connections.put(database, obj);
        }

        return getConnection(database.getConfiguration());
    }

    public static void unregisterConnection(Database database)
    {
        connections.remove(database);
    }

    public static boolean isConnected(DatabaseConfiguration config)
    {
        for (Database db : connections.keySet()) {
            if (db.getConfiguration().equals(config)) {
                return true;
            }
        }

        return false;
    }

    public static Database getConnection(DatabaseConfiguration config)
    {
        for (Database db : connections.keySet()) {
            if (db.getConfiguration().equals(config)) {
                return db;
            }
        }

        return null;
    }

    public static boolean isJBDCDatabase(Class<? extends Database> database)
    {
        return database.isAssignableFrom(JBDCDatabase.class);
    }

    public static boolean isJBDCDatabase(Database database)
    {
        return database instanceof JBDCDatabase;
    }

    public static void main(String[] args)
    {
        try {
            BufferedImage img = ImageIO.read(new File("/home/max/test.jpg"));
            int mean = 0, count = 0;
            for (int i = 0; i < img.getWidth(); i++) {
                for (int j = 0; j < img.getHeight(); j++) {
                    int rgb = img.getRGB(i, j);
                    Color color = new Color(rgb);

                    int newrgb = (color.getRed() + color.getGreen() + color.getBlue()) / 3;

                    img.setRGB(i, j, new Color(newrgb, newrgb, newrgb).getRGB());

                    mean += newrgb;
                    count++;
                }
            }

            mean /= count;

            for (int i = 0; i < img.getWidth(); i++) {
                for (int j = 0; j < img.getHeight(); j++) {
                    int rgb = img.getRGB(i, j);
                    Color color = new Color(rgb);

                    if (color.getRed() < mean) {
                        rgb = 0;
                    } else {
                        rgb = 255;
                    }

                    img.setRGB(i, j, new Color(rgb, rgb, rgb).getRGB());
                }
            }

            ImageIO.write(img, "jpg", new File("/home/max/test1.jpg"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}