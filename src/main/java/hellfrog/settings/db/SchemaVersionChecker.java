package hellfrog.settings.db;

import hellfrog.common.CommonUtils;
import hellfrog.settings.oldjson.JSONCommonPreferences;
import hellfrog.settings.oldjson.JSONLegacySettings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

class SchemaVersionChecker {

    private static final int CURRENT_SCHEMA_VERSION = 1;
    private static final String SCHEMA_VERSION_GET_QUERY = "PRAGMA user_version";
    private static final String SCHEMA_CREATE_QUERY_1 = "schema_create_query_v1.sql";

    private final Logger sqlLog = LogManager.getLogger("Schema version checker");
    private final Connection connection;
    private final MainDBController mainDBController;

    SchemaVersionChecker(@NotNull MainDBController mainDBController) {
        this.mainDBController = mainDBController;
        this.connection = mainDBController.getConnection();
    }

    void checkSchemaVersion() throws SQLException {
        int currentSchemaVersion = 0;
        try (Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(SCHEMA_VERSION_GET_QUERY)) {
                if (resultSet.next()) {
                    currentSchemaVersion = resultSet.getInt(1);
                }
            }
        } catch (SQLException err) {
            sqlLog.fatal("Unable to check schema version: " + err.getMessage(), err);
            throw err;
        }
        if (currentSchemaVersion == 0) {
            initSchemaVersion();
        }
    }

    private void initSchemaVersion() throws SQLException {
        sqlLog.info("Initial scheme " + CURRENT_SCHEMA_VERSION);
        sqlLog.info("Searching legacy settings into JSON files");
        writeSchema(SCHEMA_CREATE_QUERY_1);
        convertLegacy();
    }

    private void writeSchema(@NotNull String name) throws SQLException {
        sqlLog.info("Write schema from " + name);
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream queryStream = classLoader.getResourceAsStream(name);
        if (queryStream == null) {
            queryStream = classLoader.getResourceAsStream("/" + name);
        }
        if (queryStream != null) {
            StringBuilder stringBuilder = new StringBuilder();
            try (BufferedReader queryReader = new BufferedReader(new InputStreamReader(queryStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = queryReader.readLine()) != null)
                    stringBuilder.append(line).append('\n');
            } catch (IOException err) {
                sqlLog.fatal("Unable to read resource file " + name + " with initial schema query: "
                        + err.getMessage(), err);
                throw new SQLException("Init error", err);
            }
            String[] buildQueries = stringBuilder.toString().split(";");
            try (Statement statement = connection.createStatement()) {
                for (String query : buildQueries) {
                    try {
                        statement.executeUpdate(query);
                    } catch (Exception updateErr) {
                        sqlLog.fatal("Unable to execute init SQL query \"" + query + "\": " + updateErr.getMessage(),
                                updateErr);
                        throw new SQLException("Init error", updateErr);
                    }
                }
            } catch (SQLException err) {
                sqlLog.fatal("Unable to create statement: " + err.getMessage(), err);
                throw new SQLException("Init error", err);
            }
        } else {
            sqlLog.fatal("Unable to found resource file " + name);
            throw new SQLException("Init error");
        }
    }

    private void convertLegacy() {
        JSONLegacySettings jsonLegacySettings = new JSONLegacySettings();
        if (jsonLegacySettings.isHasCommonPreferences() || jsonLegacySettings.isHasServerPreferences()
                || jsonLegacySettings.isHasServerStatistics()) {
            sqlLog.info("Attempt to legacy JSON settings conversion");
            if (jsonLegacySettings.isHasCommonPreferences()) {
                JSONCommonPreferences oldCommonPreferences = jsonLegacySettings.getJsonCommonPreferences();
                DBCommonPreferences newCommonPreferences = mainDBController.getCommonPreferences();
                if (CommonUtils.isTrStringNotEmpty(oldCommonPreferences.getApiKey())) {
                    sqlLog.info("Common preferences: found API key");
                    newCommonPreferences.setApiKey(oldCommonPreferences.getApiKey());
                }
                if (CommonUtils.isTrStringNotEmpty(oldCommonPreferences.getCommonBotPrefix())) {
                    sqlLog.info("Common preferences: found global bot prefix");
                    newCommonPreferences.setBotPrefix(oldCommonPreferences.getCommonBotPrefix());
                }
                if (CommonUtils.isTrStringNotEmpty(oldCommonPreferences.getBotName())) {
                    sqlLog.info("Common preferences: found bot name");
                    newCommonPreferences.setBotName(oldCommonPreferences.getBotName());
                }
            }
        }
    }
}
