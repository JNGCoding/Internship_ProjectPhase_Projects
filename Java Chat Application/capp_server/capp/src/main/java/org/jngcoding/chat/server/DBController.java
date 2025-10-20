package org.jngcoding.chat.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicReference;

public class DBController {
    public static String DEFAULT_DBUSERNAME = "root";
    public static String DEFAULT_DBURL = "jdbc:mysql://localhost:3306/";

    private Connection DBConnection = null;
    public SQLException LatestException = null;
    
    public static enum QueryType {
        NORMAL,
        UPDATE,
        QUERY
    }

    public static class DBMetaData {
        public final String username, password, url, database;
        public DBMetaData(String user_name, String pass_word, String ur_l, String data_base) {
            username = user_name;
            password = pass_word;
            url = ur_l;
            database = data_base;
        }
    }

    public static class DBResult {
        public final Statement stmt;
        public final ResultSet rlst;
        public DBResult(Statement statement, ResultSet resultset) {
            this.stmt = statement;
            this.rlst = resultset;
        }
    }

    @FunctionalInterface
    private interface SQLRunnable {
        public void run() throws SQLException;
    }

    @SuppressWarnings("CallToPrintStackTrace")
    private boolean try_sql_operation(SQLRunnable runnable) {
        try {
            runnable.run();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            LatestException = e;
            return false;
        }
    }

    public DBController(final DBMetaData credentials) {
        try_sql_operation(() -> {
            DBConnection = DriverManager.getConnection(credentials.url + credentials.database, credentials.username, credentials.password);
        });
    }

    public DBResult ExecuteQuery(String statement, QueryType querytype) {
        final AtomicReference<DBResult> result = new AtomicReference<>();
        
        boolean s = try_sql_operation(() -> {
            Statement stmt = DBConnection.createStatement();
            ResultSet resultSet = null;

            switch (querytype) {
                case NORMAL: {
                    stmt.execute(statement);
                    break;
                }

                case UPDATE: {
                    stmt.executeUpdate(statement);
                    break;
                }

                case QUERY: {
                    resultSet = stmt.executeQuery(statement);
                    break;
                }

                default: {
                    break;
                }
            }

            result.set(new DBResult(stmt, resultSet));
        });

        return s ? result.get() : null;
    }

    public void closeDBResult(DBResult result) {
        try_sql_operation(() -> {
            if (result != null) {
                if (result.stmt != null && !result.stmt.isClosed()) {
                    result.stmt.close(); // Also closes any Result set associated with it.
                }
            }
        });
    }
}
