package hellfrog.settings.db;

import hellfrog.settings.oldjson.JSONLegacySettings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;

public class MainDBController
    implements Closeable, AutoCloseable {

    private final Path SETTINGS_PATH = Paths.get("./settings/");
    private final Path MAIN_DB_PATH = SETTINGS_PATH.resolve("hellfrog_main.sqlite3");
    private final String MAIN_CONNECTION_URL = "jdbc:sqlite:" + MAIN_DB_PATH.toString();

    private final Path TEST_DB_PATH = SETTINGS_PATH.resolve("test.sqlite3");
    private final String TEST_CONNECTION_URL = "jdbc:sqlite:" + TEST_DB_PATH.toString();

    private final int CURRENT_SCHEMA_VERSION = 1;
    private final String SCHEMA_VERSION_SET_QUERY = "PRAGMA user_version = " + CURRENT_SCHEMA_VERSION;
    private final String SCHEMA_VERSION_GET_QUERY = "PRAGMA user_version";
    private final String SCHEMA_CREATE_QUERY_1 = "schema_create_query_v1.sql";

    private final Logger sqlLog = LogManager.getLogger("SQL logger");
    private final Logger mainLog = LogManager.getLogger("Main");
    private Connection connection;

    public MainDBController() {
        checkSettingsPath();
        testJdbc();
        createConnection();
        checkSchemaVersion();
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

    private void testJdbc() {
        try {
            Files.deleteIfExists(TEST_DB_PATH);
            try (Connection connection = DriverManager.getConnection(TEST_CONNECTION_URL)) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate(SCHEMA_VERSION_SET_QUERY);
                }
                try (Statement statement = connection.createStatement()) {
                    try (ResultSet resultSet = statement.executeQuery(SCHEMA_VERSION_GET_QUERY)) {
                        if (resultSet.next()) {
                            int version = resultSet.getInt(1);
                            if (version != CURRENT_SCHEMA_VERSION)
                                throw new SQLException("Incorrect version set");
                        } else {
                            throw new SQLException("Incorrect version set");
                        }
                    }
                }
            }
        } catch (Exception err) {
            sqlLog.fatal("SQLite test error: " + err.getMessage(), err);
            System.exit(2);
        }
    }

    private void createConnection() {
        try {
            connection = DriverManager.getConnection(MAIN_CONNECTION_URL);
            sqlLog.info("Main database opened");
        } catch (Exception err) {
            sqlLog.fatal("Unable to open main database: " + err.getMessage(), err);
            System.exit(2);
        }
    }

    private void checkSchemaVersion() {
        int currentSchemaVersion = 0;
        try (Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(SCHEMA_VERSION_GET_QUERY)) {
                if (resultSet.next()) {
                    currentSchemaVersion = resultSet.getInt(1);
                }
            }
        } catch (SQLException err) {
            sqlLog.fatal("Unable to check schema version: " + err.getMessage(), err);
            System.exit(2);
        }
        if (currentSchemaVersion == 0) {
            initSchema();
        }
    }

    private void initSchema() {
        sqlLog.info("Initial scheme");
        sqlLog.info("Searching legacy settings into JSON files");
        JSONLegacySettings jsonLegacySettings = new JSONLegacySettings();
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream queryStream = classLoader.getResourceAsStream(SCHEMA_CREATE_QUERY_1);
        if (queryStream == null) {
            queryStream = classLoader.getResourceAsStream("/" + SCHEMA_CREATE_QUERY_1);
        }
        if (queryStream != null) {
            StringBuilder stringBuilder = new StringBuilder();
            try (BufferedReader queryReader = new BufferedReader(new InputStreamReader(queryStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = queryReader.readLine()) != null)
                    stringBuilder.append(line).append('\n');
            } catch (IOException err) {
                sqlLog.fatal("Unable to read resource file " + SCHEMA_CREATE_QUERY_1 + " with initial schema query: "
                + err.getMessage(), err);
                System.exit(2);
            }
            String[] buildQueries = stringBuilder.toString().split(";");
            try (Statement statement = connection.createStatement()) {
                for (String query : buildQueries) {
                    try {
                        statement.executeUpdate(query);
                    } catch (Exception updateErr) {
                        sqlLog.fatal("Unable to execute init SQL query \"" + query + "\": " + updateErr.getMessage(),
                                updateErr);
                        System.exit(2);
                    }
                }
            } catch (SQLException err) {
                sqlLog.fatal("Unable to create statement: " + err.getMessage(), err);
                System.exit(2);
            }
        } else {
            sqlLog.fatal("Unable to found resource file " + SCHEMA_CREATE_QUERY_1);
            System.exit(2);
        }
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
}
