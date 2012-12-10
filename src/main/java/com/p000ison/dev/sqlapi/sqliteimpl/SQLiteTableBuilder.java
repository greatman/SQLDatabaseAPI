package com.p000ison.dev.sqlapi.sqliteimpl;

import com.p000ison.dev.sqlapi.Column;
import com.p000ison.dev.sqlapi.Database;
import com.p000ison.dev.sqlapi.TableBuilder;
import com.p000ison.dev.sqlapi.TableObject;
import com.p000ison.dev.sqlapi.exception.TableBuildingException;

/**
 * Represents a SQLiteTableBuilder
 */
public final class SQLiteTableBuilder extends TableBuilder {

    public SQLiteTableBuilder(TableObject object, Database database)
    {
        super(object, database);
    }

    public SQLiteTableBuilder(Class<? extends TableObject> object, Database database)
    {
        super(object, database);
    }

    @Override
    protected void buildColumn(Column column)
    {
        Class<?> type = column.getType();
        query.append(column.getColumnName()).append(' ');

        if (type.equals(int.class)) {
            query.append("INTEGER");
        } else if (type.equals(String.class)) {
            query.append("TEXT");
        }

        if (column.getLength().length != 0) {
            query.append('(');
            for (int singleLength : column.getLength()) {
                query.append(singleLength).append(',');
            }

            query.deleteCharAt(query.length() - 1);
            query.append(')');
        }

        if (column.isPrimary() || column.isAutoIncrementing()) {
            if (primaryColumn) {
                throw new TableBuildingException("Duplicate primary/autoincrementing column %s!", column.getColumnName());
            }
            primaryColumn = true;
            query.append(" PRIMARY KEY");
        }

        if (column.isUnique()) {
            query.append(" UNIQUE");
        }

        if (column.isNotNull()) {
            query.append(" NOT NULL");
        }

        if (!column.getDefaultValue().isEmpty()) {
            query.append(" DEFAULT ").append(column.getDefaultValue());
        }
    }

    @Override
    protected boolean isSupportAddColumns()
    {
        return true;
    }

    @Override
    protected boolean isSupportRemoveColumns()
    {
        return false;
    }

    @Override
    protected boolean isSupportModifyColumns()
    {
        return false;
    }
}
