package com.p000ison.dev.sqlapi.test;

import com.p000ison.dev.sqlapi.TableObject;
import com.p000ison.dev.sqlapi.annotation.DatabaseColumn;
import com.p000ison.dev.sqlapi.annotation.DatabaseColumnGetter;
import com.p000ison.dev.sqlapi.annotation.DatabaseColumnSetter;
import com.p000ison.dev.sqlapi.annotation.DatabaseTable;

/**
 * Represents a Person
 */
@DatabaseTable(name = "asdf")
public class Person implements TableObject {

    @DatabaseColumn(position = 2, databaseName = "prim", primary = true, unique = true)
    private int test = 5;

    @DatabaseColumn(position = 1, databaseName = "id", primary = true, unique = true, autoIncrement = true)
    private int id;

    private String formattedName;


    @DatabaseColumnSetter(position = 2, databaseName = "fname")
    public void setFormattedName(String formattedName)
    {
        this.formattedName = formattedName.replace(' ', '!');
    }

    @DatabaseColumnGetter(databaseName = "fname")
    public String getFormattedName()
    {
        return formattedName;
    }
}