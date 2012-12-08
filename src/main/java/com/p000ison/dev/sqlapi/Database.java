package com.p000ison.dev.sqlapi;

import com.p000ison.dev.sqlapi.annotation.DatabaseTable;
import com.p000ison.dev.sqlapi.exception.QueryException;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * Represents a Database
 */
public abstract class Database {

    protected DataSource dataSource;
    protected Connection connection;
    protected DatabaseConfiguration configuration;
    private boolean dropOldColumns = false;
    private TreeMap<Integer, PreparedStatement> preparedStatements = new TreeMap<Integer, PreparedStatement>();
    private Map<Class<? extends TableObject>, List<Column>> columns = new HashMap<Class<? extends TableObject>, List<Column>>();

    /**
     * Creates a new database connection based on the configuration
     *
     * @param configuration The database configuration
     * @throws SQLException
     */
    protected Database(DatabaseConfiguration configuration) throws SQLException
    {
        this.configuration = configuration;
        init(configuration);
        connection = dataSource.getConnection();
    }

    /**
     * Gets the name of a table
     *
     * @param clazz The class of the {@link TableObject}.
     * @return The name
     */
    public static String getTableName(Class<? extends TableObject> clazz)
    {
        DatabaseTable annotation = clazz.getAnnotation(DatabaseTable.class);
        return annotation == null ? null : annotation.name();
    }

    public PreparedStatement createPreparedStatement(int id, String query) throws SQLException
    {
        PreparedStatement statement = getConnection().prepareStatement(query);
        preparedStatements.put(id, statement);
        return statement;
    }

    public int createPreparedStatement(String query) throws SQLException
    {
        PreparedStatement statement = getConnection().prepareStatement(query);
        int statementId;
        if (preparedStatements.isEmpty()) {
            statementId = 0;
        } else {
            statementId = preparedStatements.lastKey() + 1;
        }
        preparedStatements.put(statementId, statement);
        return statementId;
    }

    protected abstract void init(DatabaseConfiguration configuration);

    /**
     * Closes the connection to the database
     *
     * @throws SQLException
     */
    public abstract void close() throws SQLException;

    TableBuilder createTableBuilder(TableObject table)
    {
        return createTableBuilder(table.getClass());
    }

    protected abstract TableBuilder createTableBuilder(Class<? extends TableObject> table);

    public final Database registerTable(Class<? extends TableObject> table)
    {
        TableBuilder builder = createTableBuilder(table);

        columns.put(table, builder.getColumns());

        String tableQuery = builder.createTable().getQuery();
        System.out.println(tableQuery);

        String modifyQuery = builder.createModifyQuery().getQuery();
        System.out.println(modifyQuery);

        executeQuery(tableQuery);
        executeQuery(modifyQuery);
        return this;
    }

    public boolean existsDatabaseTable(String table)
    {
        ResultSet set = null;
        try {
            set = getMetadata().getTables(null, null, table, null);
            return set.next();
        } catch (SQLException e) {
            throw new QueryException(e);
        }
    }


    public final Database registerTable(TableObject table)
    {
        return registerTable(table.getClass());
    }

    public final List<String> getDatabaseColumns(Class<? extends TableObject> table)
    {
        return getDatabaseColumns(getTableName(table));
    }

    public final List<String> getDatabaseColumns(TableObject table)
    {
        return getDatabaseColumns(getTableName(table.getClass()));
    }

    protected final List<String> getDatabaseColumns(String table)
    {
        List<String> columns = new ArrayList<String>();

        try {
            ResultSet columnResult = getMetadata().getColumns(null, null, table, null);


            while (columnResult.next()) {
                columns.add(columnResult.getString("COLUMN_NAME"));
            }
        } catch (SQLException e) {
            throw new QueryException(e);
        }

        return columns;
    }

    private DatabaseMetaData getMetadata()
    {
        try {
            return getConnection().getMetaData();
        } catch (SQLException e) {
            throw new QueryException(e);
        }
    }

    public final Set<String> getDatabaseTables() throws SQLException
    {
        Set<String> columns = new HashSet<String>();

        ResultSet columnResult = this.getConnection().getMetaData().getTables(null, null, null, null);

        while (columnResult.next()) {
            columns.add(columnResult.getString("TABLE_NAME"));
        }

        return columns;
    }

    void executeQuery(String query) {
        try {
            getConnection().createStatement().executeUpdate(query);
        } catch (SQLException e) {
            throw new QueryException(e);
        }
    }

    public final Connection getConnection()
    {
        return connection;
    }

    public DatabaseConfiguration getConfiguration()
    {
        return configuration;
    }

    public final boolean isDropOldColumns()
    {
        return dropOldColumns;
    }

    public final void setDropOldColumns(boolean dropOldColumns)
    {
        this.dropOldColumns = dropOldColumns;
    }

    public List<Column> getColumns(Class<? extends TableObject> clazz)
    {
        return columns.get(clazz);
    }
}
