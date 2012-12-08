package com.p000ison.dev.sqlapi.test;

import com.p000ison.dev.sqlapi.Database;
import com.p000ison.dev.sqlapi.MySQLimpl.MySQLConfiguration;
import com.p000ison.dev.sqlapi.MySQLimpl.MySQLDatabase;

/**
 * Represents a StartTest
 */
public class StartTest {
    private static final int PORT = 3306;

    public static void main(String[] args)
    {
        long start = System.currentTimeMillis();
        try {
            Person person = new Person();

            Database db = new MySQLDatabase(new MySQLConfiguration("root", "m1nt", "localhost", PORT, "test"));
//            Database db = new SQLiteDatabase(new SQLiteConfiguration(new File("/home/max/Arbeitsfläche/test.db")));
            db.setDropOldColumns(true);
            db.registerTable(person);
//            db.getConnection().prepareStatement("SELECT * FROM d").executeQuery();
            db.close();

        } catch (Exception e) {
            e.printStackTrace();
        }


        long finish = System.currentTimeMillis();
        System.out.printf("Check took %s!", finish - start);


    }
}