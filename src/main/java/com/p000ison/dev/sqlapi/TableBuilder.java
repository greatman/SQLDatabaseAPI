package com.p000ison.dev.sqlapi;

import com.p000ison.dev.sqlapi.annotation.DatabaseColumn;
import com.p000ison.dev.sqlapi.annotation.DatabaseColumnGetter;
import com.p000ison.dev.sqlapi.annotation.DatabaseColumnSetter;
import com.p000ison.dev.sqlapi.exception.TableBuildingException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Represents a TableBuilder
 */
public abstract class TableBuilder {

    /**
     * A temporary query which is used to create a table or modify a table for example
     */
    protected StringBuilder query;
    /**
     * The class which represents the table
     */
    private Class<? extends TableObject> object;
    /**
     * The expected columns
     */
    private List<Column> buildingColumns;
    /**
     * Whether we have already found a primary key
     */
    protected boolean primaryColumn = false;
    /**
     * The columns which are already in the database
     */
    private List<String> databaseColumns;

    private Database database;

    private String tableName;

    private boolean existed;

    public TableBuilder(Class<? extends TableObject> object, Database database)
    {
        this.database = database;
        query = new StringBuilder();
        buildingColumns = new ArrayList<Column>();
        this.object = object;

        init();
    }

    public TableBuilder(TableObject object, Database database)
    {
        this(object.getClass(), database);
    }

    private void init()
    {
        tableName = Database.getTableName(object);

        existed = database.existsDatabaseTable(tableName);

        if (tableName == null) {
            throw new TableBuildingException("The name of the table is not given! Add the @DatabaseTable annotation!");
        }

        setupBuildingColumns();
        databaseColumns = database.getDatabaseColumns(tableName);

    }

    public TableBuilder createTable()
    {
        clearQuery();
        if (existed) {
            return this;
        }
        query.append("CREATE TABLE IF NOT EXISTS ").append(tableName).append('(');

        buildColumns();

        query.append(");");

        return this;
    }

    public TableBuilder createModifyQuery()
    {
        clearQuery();
        if (!existed) {
            return this;
        }
//        query.append("ALTER TABLE ").append(tableName);

        buildModifyColumns();

        return this;
    }

    private void clearQuery()
    {
        query.setLength(0);
    }

    public Column getColumn(String dbColumn)
    {
        for (Column column : buildingColumns) {
            if (column.getColumnName().equals(dbColumn)) {
                return column;
            }
        }
        return null;
    }

    public boolean existsColumn(String dbColumn)
    {
        return getColumn(dbColumn) != null;
    }

    public MethodColumn getMethodColumn(String dbColumn)
    {
        for (Column column : buildingColumns) {
            if ((column instanceof MethodColumn) && column.getColumnName().equals(dbColumn)) {
                return (MethodColumn) column;
            }
        }
        return null;
    }

    private void setupBuildingColumns()
    {
        buildingColumns.clear();

        Method[] methods = object.getDeclaredMethods();

        for (Method method : methods) {
            String columnName;

            DatabaseColumnSetter setter = method.getAnnotation(DatabaseColumnSetter.class);
            if (setter != null) {
                MethodColumn.validateSetterMethod(method);
                columnName = setter.databaseName();
            } else {
                DatabaseColumnGetter getter = method.getAnnotation(DatabaseColumnGetter.class);
                if (getter == null) {
                    continue;
                }

                MethodColumn.validateGetterMethod(method);

                columnName = getter.databaseName();
            }

            MethodColumn column = getMethodColumn(columnName);
            if (column == null) {
                column = new MethodColumn();
                buildingColumns.add(column);
            }

            if (setter == null) {
                column.setGetter(method);
            } else {
                column.setSetter(method);
                column.setAnnotation(setter);
            }
        }

        for (Iterator<Column> it = buildingColumns.iterator(); it.hasNext(); ) {
            Column column = it.next();
            if (column instanceof MethodColumn) {
                MethodColumn methodColumn = (MethodColumn) column;

                if (methodColumn.isNull()) {
                    it.remove();
                } else {
                    methodColumn.validate();
                }
            }
        }

        for (Field field : object.getDeclaredFields()) {
            DatabaseColumn column;
            if ((column = field.getAnnotation(DatabaseColumn.class)) != null) {
                if (existsColumn(column.databaseName())) {
                    throw new TableBuildingException("Duplicate column \"%s\"!", column.databaseName());
                }
                FieldColumn fieldColumn = new FieldColumn(field, column);
                buildingColumns.add(fieldColumn);
            }
        }

        //
        // Sort the columns by the given position, since getDeclaredFields and getDeclaredMethods do not have a specific order
        //
        Collections.sort(buildingColumns, new Comparator<Column>() {
            @Override
            public int compare(Column o1, Column o2)
            {
                int p1 = o1.getPosition();
                int p2 = o2.getPosition();
                return p1 < p2 ? -1 : p1 > p2 ? 1 : 0;
            }
        });
    }

    private void buildColumns()
    {
        if (buildingColumns.isEmpty()) {
            throw new TableBuildingException("The table must have at least one column!");
        }

        for (Column column : buildingColumns) {
            buildColumn(column);
            query.append(',');
        }

        query.deleteCharAt(query.length() - 1);
        primaryColumn = false;
    }

    private void buildModifyColumns()
    {
        if (buildingColumns.isEmpty()) {

            throw new TableBuildingException("The table must have at least one column!");
        }

        Set<Column> toAdd = new HashSet<Column>();


        for (Column column : buildingColumns) {
            if (!databaseColumns.contains(column.getColumnName())) {
                //missing in database
                toAdd.add(column);
            }
        }

        if (!toAdd.isEmpty() && isSupportAddColumns()) {
            query.append("ALTER TABLE ").append(Database.getTableName(object)).append(" ADD COLUMN (");

            for (Column column : toAdd) {
                buildColumn(column);
                query.append(',');
            }

            query.deleteCharAt(query.length() - 1);
            query.append(')');
        }


        if (database.isDropOldColumns() && isSupportRemoveColumns()) {
            List<String> toDrop = new ArrayList<String>();

            for (String column : databaseColumns) {
                if (!existsColumn(column)) {
                    toDrop.add(column);
                }
            }

            if (!toDrop.isEmpty()) {
                if (toAdd.isEmpty()) {
                    query.append("ALTER TABLE ").append(Database.getTableName(object));
                } else {
                    query.append(',');
                }

                for (String column : toDrop) {
                    query.append(" DROP COLUMN ").append(column);
                    query.append(',');
                }

                query.deleteCharAt(query.length() - 1);
            }
        }

        query.append(';');
        clearQuery();
    }

    public String getQuery()
    {
        if (query.length() == 0) {
            return null;
        }

        return query.toString();
    }

    List<Column> getColumns()
    {
        return Collections.unmodifiableList(buildingColumns);
    }

    protected abstract void buildColumn(Column column);

    protected abstract boolean isSupportAddColumns();

    protected abstract boolean isSupportRemoveColumns();

    protected abstract boolean isSupportModifyColumns();
}



