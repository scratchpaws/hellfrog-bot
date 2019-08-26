package hellfrog.settings.db;

import hellfrog.common.CommonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;

public class MainDBController
    implements Closeable, AutoCloseable {

    private final Path SETTINGS_PATH = Paths.get("./settings/");

    private final Logger sqlLog = LogManager.getLogger("DB controller");
    private final Logger mainLog = LogManager.getLogger("Main");
    private Connection connection;
    private DBCommonPreferences commonPreferences = null;

    public MainDBController() {
        String MAIN_DB_FILE_NAME = "hellfrog_main.sqlite3";
        init(MAIN_DB_FILE_NAME);
    }

    public MainDBController(@NotNull String anotherName) {
        init(anotherName);
    }

    private void init(@NotNull String dbFileName) {
        Path pathToDb = SETTINGS_PATH.resolve(dbFileName);
        String JDBC_PREFIX = "jdbc:sqlite:";
        String connectionURL = JDBC_PREFIX + pathToDb.toString();
        checkSettingsPath();
        createConnection(connectionURL);
        try {
            new SchemaVersionChecker(this).checkSchemaVersion();
        } catch (SQLException err) {
            mainLog.fatal("Terminated. See sql log.");
            System.exit(2);
        }
    }

    Connection getConnection() {
        return this.connection;
    }

    private void checkSettingsPath() {
        try {
            if (!Files.exists(SETTINGS_PATH) || !Files.isDirectory(SETTINGS_PATH)) {
                Files.createDirectory(SETTINGS_PATH);
            }
        } catch (IOException err) {
            mainLog.fatal("Unable to create settings directory: " + err);
            System.exit(2);
        }
    }

    private void createConnection(@NotNull String connectionURL) {
        try {
            connection = DriverManager.getConnection(connectionURL);
            commonPreferences = new DBCommonPreferences(connection);
            sqlLog.info("Main database opened");
        } catch (Exception err) {
            sqlLog.fatal("Unable to open main database: " + err.getMessage(), err);
            mainLog.fatal("Terminated. See sql log.");
            System.exit(2);
        }
    }

    public String executeRawQuery(@Nullable String rawQuery) {
        if (CommonUtils.isTrStringEmpty(rawQuery)) {
            return "(Query is empty or null)";
        }
        try {
            if (connection == null || connection.isClosed()) {
                return "(Connection is closed)";
            }
        } catch (SQLException err) {
            return err.getMessage();
        }
        StringBuilder result = new StringBuilder();
        try (Statement statement = connection.createStatement()) {
            boolean hasResult = statement.execute(rawQuery);
            if (!hasResult) {
                int updateCount = statement.getUpdateCount();
                return "Successful. Updated " + updateCount + " rows";
            } else {
                try (ResultSet resultSet = statement.getResultSet()) {
                    ResultSetMetaData metaData = resultSet.getMetaData();
                    for (int i = 1; i <= metaData.getColumnCount(); i++) {
                        String label = metaData.getColumnLabel(i);
                        result.append(label);
                        if (i < metaData.getColumnCount()) {
                            result.append("|");
                        }
                    }
                    result.append('\n');
                    result.append("-".repeat(Math.max(0, result.length())));
                    result.append('\n');
                    while (resultSet.next()) {
                        for (int i = 1; i <= metaData.getColumnCount(); i++) {
                            String value = resultSet.getString(i);
                            result.append(value);
                            if (i < metaData.getColumnCount()) {
                                result.append("|");
                            }
                        }
                        result.append('\n');
                    }
                }
            }
        } catch (SQLException err) {
            return err.getMessage();
        }
        return result.toString();
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                sqlLog.info("Main database closed");
            } catch (SQLException err) {
                sqlLog.warn("Unable to close main database: " + err.getMessage(), err);
            } finally {
                connection = null;
            }
        }
    }

    public DBCommonPreferences getCommonPreferences() {
        return commonPreferences;
    }
}
