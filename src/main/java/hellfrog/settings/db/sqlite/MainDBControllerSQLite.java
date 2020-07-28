package hellfrog.settings.db.sqlite;

import hellfrog.common.CodeSourceUtils;
import hellfrog.common.CommonUtils;
import hellfrog.settings.db.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;

public class MainDBControllerSQLite
        extends MainDBController {

    private final Connection connection;
    private boolean closed = false;
    private final CommonPreferencesDAO commonPreferencesDAO;
    private final BotOwnersDAO botOwnersDAO;
    private final ServerPreferencesDAO serverPreferencesDAO;
    private final UserRightsDAO userRightsDAO;
    private final RoleRightsDAO roleRightsDAO;
    private final TextChannelRightsDAO textChannelRightsDAO;
    private final ChannelCategoryRightsDAO channelCategoryRightsDAO;
    private final VotesDAO votesDAO;
    private final WtfAssignDAO wtfAssignDAO;
    private final EmojiTotalStatisticDAO emojiTotalStatisticDAO;

    private static final String SETTINGS_DIR_NAME = "settings";
    private static final String PROD_DB_FILE_NAME = "hellfrog_main.sqlite3";
    private static final String TEST_DB_FILE_NAME = "hellfrog_test.sqlite3";
    private static final String BACKUP_DB_FILE_NAME = "hellfrog_backup.sqlite3";

    public MainDBControllerSQLite(@Nullable InstanceType type) throws IOException, SQLException {
        super(type);
        if (type == null) {
            type = InstanceType.PROD;
        }
        Path codeSourcePath = CodeSourceUtils.getCodeSourceParent();
        Path settingsPath = codeSourcePath.resolve(SETTINGS_DIR_NAME);
        Path pathToDb = switch (type) {
            case PROD -> settingsPath.resolve(PROD_DB_FILE_NAME);
            case TEST -> settingsPath.resolve(TEST_DB_FILE_NAME);
            case BACKUP -> settingsPath.resolve(BACKUP_DB_FILE_NAME);
        };
        String JDBC_PREFIX = "jdbc:sqlite:";
        String connectionURL = JDBC_PREFIX + pathToDb.toString();
        try {
            if (!Files.exists(settingsPath) || !Files.isDirectory(settingsPath)) {
                Files.createDirectory(settingsPath);
            }
        } catch (IOException err) {
            mainLog.fatal("Unable to create settings directory: " + err);
            throw err;
        }
        try {
            connection = DriverManager.getConnection(connectionURL);
            commonPreferencesDAO = new CommonPreferencesDAOImpl(connection);
            botOwnersDAO = new BotOwnersDAOImpl(connection);
            serverPreferencesDAO = new ServerPreferencesDAOImpl(connection);
            userRightsDAO = new UserRightsDAOImpl(connection);
            roleRightsDAO = new RoleRightsDAOImpl(connection);
            textChannelRightsDAO = new TextChannelRightsDAOImpl(connection);
            channelCategoryRightsDAO = new ChannelCategoryRightsDAOImpl(connection);
            votesDAO = new VotesDAOImpl(connection);
            wtfAssignDAO = new WtfAssignDAOImpl(connection);
            emojiTotalStatisticDAO = new EmojiTotalStatisticDAOImpl(connection);
            sqlLog.info("Main database opened");
        } catch (SQLException err) {
            sqlLog.fatal("Unable to open main database: " + err.getMessage(), err);
            throw err;
        }
        new SchemaVersionCheckerSQLite(this, type.equals(InstanceType.PROD)).checkSchemaVersion();
    }

    public static void destroyTestDatabase() throws IOException {
        Path codeSourcePath = CodeSourceUtils.getCodeSourceParent();
        Path settingsPath = codeSourcePath.resolve(SETTINGS_DIR_NAME);
        Path pathToDb = settingsPath.resolve(TEST_DB_FILE_NAME);
        Files.deleteIfExists(pathToDb);
    }

    Connection getConnection() {
        return this.connection;
    }

    @Override
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
        if (!closed) {
            try {
                connection.close();
                sqlLog.info("Main database closed");
            } catch (SQLException err) {
                sqlLog.warn("Unable to close main database: " + err.getMessage(), err);
            } finally {
                closed = true;
            }
        }
    }

    @Override
    public CommonPreferencesDAO getCommonPreferencesDAO() {
        return commonPreferencesDAO;
    }

    @Override
    public BotOwnersDAO getBotOwnersDAO() {
        return botOwnersDAO;
    }

    @Override
    public ServerPreferencesDAO getServerPreferencesDAO() {
        return serverPreferencesDAO;
    }

    @Override
    public UserRightsDAO getUserRightsDAO() {
        return userRightsDAO;
    }

    @Override
    public RoleRightsDAO getRoleRightsDAO() {
        return roleRightsDAO;
    }

    @Override
    public TextChannelRightsDAO getTextChannelRightsDAO() {
        return textChannelRightsDAO;
    }

    @Override
    public ChannelCategoryRightsDAO getChannelCategoryRightsDAO() {
        return channelCategoryRightsDAO;
    }

    @Override
    public VotesDAO getVotesDAO() {
        return votesDAO;
    }

    @Override
    public WtfAssignDAO getWtfAssignDAO() {
        return wtfAssignDAO;
    }

    @Override
    public EmojiTotalStatisticDAO getEmojiTotalStatisticDAO() {
        return emojiTotalStatisticDAO;
    }
}
