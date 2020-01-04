package hellfrog.settings.db;

import hellfrog.common.CommonUtils;
import hellfrog.common.FromTextFile;
import hellfrog.common.ResourcesLoader;
import hellfrog.settings.oldjson.JSONCommandRights;
import hellfrog.settings.oldjson.JSONCommonPreferences;
import hellfrog.settings.oldjson.JSONLegacySettings;
import hellfrog.settings.oldjson.JSONServerPreferences;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

class SchemaVersionChecker {

    private static final int CURRENT_SCHEMA_VERSION = 1;
    private static final String SCHEMA_VERSION_GET_QUERY = "PRAGMA user_version";

    private final boolean migrateOldSettings;
    private final Logger sqlLog = LogManager.getLogger("Schema version checker");
    private final Connection connection;
    private final MainDBController mainDBController;

    @FromTextFile(fileName = "sql/scheme_create_queries/schema_create_query_v1.sql")
    private String schemaCreateQuery1 = null;

    SchemaVersionChecker(@NotNull MainDBController mainDBController,
                         boolean migrateOldSettings) {
        this.migrateOldSettings = migrateOldSettings;
        this.mainDBController = mainDBController;
        this.connection = mainDBController.getConnection();
        ResourcesLoader.initFileResources(this, SchemaVersionChecker.class);
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
        writeSchema(schemaCreateQuery1, CURRENT_SCHEMA_VERSION);
        if (migrateOldSettings) {
            convertLegacy();
        }
    }

    private void writeSchema(@NotNull String schemaQuery, int schemaVersion) throws SQLException {
        sqlLog.info("Write schema from " + schemaVersion);
        String[] buildQueries = schemaQuery.split(";");
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
    }

    private void convertLegacy() {
        JSONLegacySettings jsonLegacySettings = new JSONLegacySettings();
        if (jsonLegacySettings.isHasCommonPreferences() || jsonLegacySettings.isHasServerPreferences()
                || jsonLegacySettings.isHasServerStatistics()) {
            sqlLog.info("Attempt to legacy JSON settings conversion");
            if (jsonLegacySettings.isHasCommonPreferences()) {
                JSONCommonPreferences oldCommonPreferences = jsonLegacySettings.getJsonCommonPreferences();
                CommonPreferencesDAO newCommonPreferences = mainDBController.getCommonPreferencesDAO();
                BotOwnersDAO botOwnersDAO = mainDBController.getBotOwnersDAO();
                if (CommonUtils.isTrStringNotEmpty(oldCommonPreferences.getApiKey())) {
                    sqlLog.info("Common preferences: found API key: {}", oldCommonPreferences.getApiKey());
                    newCommonPreferences.setApiKey(oldCommonPreferences.getApiKey());
                }
                if (CommonUtils.isTrStringNotEmpty(oldCommonPreferences.getCommonBotPrefix())) {
                    sqlLog.info("Common preferences: found global bot prefix: {}",
                            oldCommonPreferences.getCommonBotPrefix());
                    newCommonPreferences.setBotPrefix(oldCommonPreferences.getCommonBotPrefix());
                }
                if (CommonUtils.isTrStringNotEmpty(oldCommonPreferences.getBotName())) {
                    sqlLog.info("Common preferences: found bot name: {}", oldCommonPreferences.getBotName());
                    newCommonPreferences.setBotName(oldCommonPreferences.getBotName());
                }
                if (oldCommonPreferences.getGlobalBotOwners() != null
                        && !oldCommonPreferences.getGlobalBotOwners().isEmpty()) {
                    sqlLog.info("Common preferences: found global bot owners: {}",
                            oldCommonPreferences.getGlobalBotOwners().stream()
                                    .map(String::valueOf)
                                    .reduce(CommonUtils::reduceConcat)
                                    .orElse(""));
                    oldCommonPreferences.getGlobalBotOwners().forEach(botOwnersDAO::addToOwners);
                }
            }
            if (jsonLegacySettings.isHasServerPreferences()) {
                ServerPreferencesDAO serverPreferencesDAO = mainDBController.getServerPreferencesDAO();
                for (Map.Entry<Long, JSONServerPreferences> serverPref : jsonLegacySettings.getPrefByServer().entrySet()) {
                    long serverId = serverPref.getKey();
                    JSONServerPreferences jsonServerPreferences = serverPref.getValue();
                    sqlLog.info("Found preferences for server {}", serverId);

                    String botPrefix = jsonServerPreferences.getBotPrefix();
                    if (CommonUtils.isTrStringNotEmpty(botPrefix)) {
                        sqlLog.info("Server {}: found bot prefix: {}", serverId, botPrefix);
                        serverPreferencesDAO.setPrefix(serverId, botPrefix);
                    }

                    boolean isJoinLeaveDisplay = jsonServerPreferences.isJoinLeaveDisplay();
                    sqlLog.info("Server {}: found join/leave display state: {}", serverId, isJoinLeaveDisplay);
                    serverPreferencesDAO.setJoinLeaveDisplay(serverId, isJoinLeaveDisplay);

                    long joinLeaveChannel = jsonServerPreferences.getJoinLeaveChannel();
                    if (joinLeaveChannel > 0L) {
                        sqlLog.info("Server {}: found join/leave channel: {}", serverId, joinLeaveChannel);
                        serverPreferencesDAO.setJoinLeaveChannel(serverId, joinLeaveChannel);
                    }

                    boolean isNewAclMode = jsonServerPreferences.getNewAclMode();
                    sqlLog.info("Server {}: found new ACL state: {}", serverId, isNewAclMode);
                    serverPreferencesDAO.setNewAclMode(serverId, isNewAclMode);

                    Map<String, JSONCommandRights> legacyRights = jsonServerPreferences.getSrvCommandRights();
                    if (!legacyRights.isEmpty()) {
                        sqlLog.info("Server {}: found command rights settings", serverId);
                        UserRightsDAO userRightsDAO = mainDBController.getUserRightsDAO();
                        RoleRightsDAO roleRightsDAO = mainDBController.getRoleRightsDAO();
                        TextChannelRightsDAO textChannelRightsDAO = mainDBController.getTextChannelRightsDAO();
                        ChannelCategoryRightsDAO channelCategoryRightsDAO = mainDBController.getChannelCategoryRightsDAO();
                        for (Map.Entry<String, JSONCommandRights> rightsEntry : legacyRights.entrySet()) {
                            String commandPrefix = rightsEntry.getKey();
                            JSONCommandRights commandRights = rightsEntry.getValue();
                            if (!commandPrefix.equals(commandRights.getCommandPrefix())) {
                                sqlLog.warn("Server {}: command prefix of legacy map key {} " +
                                                "not equals with command prefix name in object: {}",
                                        serverId, commandPrefix, commandRights.getCommandPrefix());
                            }
                            if (commandRights.getAllowUsers().isEmpty()
                                    && commandRights.getAllowRoles().isEmpty()
                                    && commandRights.getAllowChannels().isEmpty()) {
                                sqlLog.info("Server {}: empty privileges for command {}, skipping",
                                        serverId, commandPrefix);
                            } else {
                                for (long userId : commandRights.getAllowUsers()) {
                                    sqlLog.info("Server {}: grant access to userId {} for command {}",
                                            serverId, userId, commandPrefix);
                                    boolean isAllowed = userRightsDAO.allow(serverId, userId, commandPrefix);
                                    if (isAllowed) {
                                        sqlLog.info("Grant is OK");
                                    } else {
                                        sqlLog.warn("Grant is not OK");
                                    }
                                }
                                for (long roleId : commandRights.getAllowRoles()) {
                                    sqlLog.info("Server {}: grant access to roleId {} for command {}",
                                            serverId, roleId, commandPrefix);
                                    boolean isAllowed = roleRightsDAO.allow(serverId, roleId, commandPrefix);
                                    if (isAllowed) {
                                        sqlLog.info("Grant is OK");
                                    } else {
                                        sqlLog.warn("Grant is not OK");
                                    }
                                }
                                for (long textChatId : commandRights.getAllowChannels()) {
                                    sqlLog.info("Server {}: grant access to textChatId/categoryId {} " +
                                            "for command {}", serverId, textChatId, commandPrefix);
                                    boolean isAllowedChat = textChannelRightsDAO.allow(serverId, textChatId,
                                            commandPrefix);
                                    boolean isAllowedCategory = channelCategoryRightsDAO.allow(serverId,
                                            textChatId, commandPrefix);
                                    if (isAllowedChat) {
                                        sqlLog.info("Grant text chat is OK");
                                    } else {
                                        sqlLog.warn("Grant text chat is not OK");
                                    }
                                    if (isAllowedCategory) {
                                        sqlLog.info("Grant category is OK");
                                    } else {
                                        sqlLog.warn("Grant category is not OK");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
