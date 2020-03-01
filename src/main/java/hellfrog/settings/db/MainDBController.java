package hellfrog.settings.db;

import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.field.DataPersisterManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.logger.LoggerFactory;
import com.j256.ormlite.support.ConnectionSource;
import hellfrog.common.CommonUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;

public class MainDBController
        implements Closeable, AutoCloseable {

    private final Path SETTINGS_PATH = Paths.get("./settings/");

    private final Logger sqlLog = LogManager.getLogger("DB controller");
    private final Logger mainLog = LogManager.getLogger("Main");
    private ConnectionSource connectionSource;
    private CommonPreferencesDAO commonPreferencesDAO = null;
    private BotOwnersDAO botOwnersDAO = null;
    private ServerPreferencesDAO serverPreferencesDAO = null;
    private UserRightsDAO userRightsDAO = null;
    private RoleRightsDAO roleRightsDAO = null;
    private TextChannelRightsDAO textChannelRightsDAO = null;
    private ChannelCategoryRightsDAO channelCategoryRightsDAO = null;
    private VotesDAO votesDAO = null;

    public MainDBController() throws IOException, SQLException {
        String MAIN_DB_FILE_NAME = "hellfrog_main.sqlite3";
        init(MAIN_DB_FILE_NAME, true);
    }

    public MainDBController(@NotNull String anotherName,
                            boolean migrateOldSettings) throws IOException, SQLException {
        init(anotherName, migrateOldSettings);
    }

    private void init(@NotNull String dbFileName,
                      boolean migrateOldSettings) throws IOException, SQLException {

        DataPersisterManager.registerDataPersisters(InstantPersister.getSingleton());
        Path pathToDb = SETTINGS_PATH.resolve(dbFileName);
        String JDBC_PREFIX = "jdbc:sqlite:";
        String connectionURL = JDBC_PREFIX + pathToDb.toString();
        try {
            if (!Files.exists(SETTINGS_PATH) || !Files.isDirectory(SETTINGS_PATH)) {
                Files.createDirectory(SETTINGS_PATH);
            }
        } catch (IOException err) {
            mainLog.fatal("Unable to create settings directory: " + err);
            throw err;
        }
        try {
            System.setProperty("com.j256.ormlite.logger.type", "LOG4J2");
            connectionSource = new JdbcConnectionSource(connectionURL);
            commonPreferencesDAO = new CommonPreferencesDAOImpl(connectionSource);
            botOwnersDAO = new BotOwnersDAOImpl(connectionSource);
            serverPreferencesDAO = new ServerPreferencesDAOImpl(connectionSource);
            userRightsDAO = new UserRightsDAOImpl(connectionSource);
            roleRightsDAO = new RoleRightsDAOImpl(connectionSource);
            textChannelRightsDAO = new TextChannelRightsDAOImpl(connectionSource);
            channelCategoryRightsDAO = new ChannelCategoryRightsDAOImpl(connectionSource);
            votesDAO = new VotesDAO(connectionSource);
            sqlLog.info("Main database opened");
        } catch (SQLException err) {
            sqlLog.fatal("Unable to open main database: " + err.getMessage(), err);
            throw err;
        }
        new SchemaVersionChecker(this, migrateOldSettings).checkSchemaVersion();
    }

    public String executeRawQuery(@Nullable String rawQuery) {
        if (CommonUtils.isTrStringEmpty(rawQuery)) {
            return "(Query is empty or null)";
        }
        StringBuilder result = new StringBuilder();
        try {
            if (StringUtils.startsWithAny(rawQuery.toLowerCase(),
                    "update", "delete", "alter", "create", "drop", "truncate")) {
                int updateCount = commonPreferencesDAO.updateRaw(rawQuery);
                return "Successful. Updated " + updateCount + " rows";
            } else {
                GenericRawResults<String[]> results = commonPreferencesDAO.queryRaw(rawQuery);
                String[] columnNames = results.getColumnNames();
                for (int i = 0; i < columnNames.length; i++) {
                    result.append(columnNames[i]);
                    if (i < (columnNames.length - 1)) {
                        result.append('|');
                    }
                }
                result.append('\n');
                result.append("-".repeat(Math.max(0, result.length())));
                result.append('\n');
                for (String[] line : results.getResults()) {
                    for (int i = 0; i < line.length; i++) {
                        result.append(line[i]);
                        if (i < (line.length - 1)) {
                            result.append('|');
                        }
                    }
                    result.append('\n');
                }
            }
        } catch (SQLException err) {
            return err.getMessage();
        }
        return result.toString();
    }

    @Override
    public void close() {
        if (connectionSource != null) {
            try {
                connectionSource.close();
            } catch (IOException err) {
                sqlLog.warn("Unable to close main database: " + err.getMessage(), err);
            } finally {
                connectionSource = null;
            }
        }
    }

    ConnectionSource getConnectionSource() {
        return connectionSource;
    }

    public CommonPreferencesDAO getCommonPreferencesDAO() {
        return commonPreferencesDAO;
    }

    public BotOwnersDAO getBotOwnersDAO() {
        return botOwnersDAO;
    }

    public ServerPreferencesDAO getServerPreferencesDAO() {
        return serverPreferencesDAO;
    }

    public UserRightsDAO getUserRightsDAO() {
        return userRightsDAO;
    }

    public RoleRightsDAO getRoleRightsDAO() {
        return roleRightsDAO;
    }

    public TextChannelRightsDAO getTextChannelRightsDAO() {
        return textChannelRightsDAO;
    }

    public ChannelCategoryRightsDAO getChannelCategoryRightsDAO() {
        return channelCategoryRightsDAO;
    }

    public VotesDAO getVotesDAO() {
        return votesDAO;
    }
}
