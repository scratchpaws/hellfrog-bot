package hellfrog.settings.db;

import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import hellfrog.common.CodeSourceUtils;
import hellfrog.common.CommonUtils;
import hellfrog.common.FromTextFile;
import hellfrog.common.ResourcesLoader;
import hellfrog.settings.entity.*;
import hellfrog.settings.oldjson.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class SchemaVersionChecker {

    private static final int CURRENT_SCHEMA_VERSION = 1;
    private static final String SCHEMA_VERSION_GET_QUERY = "PRAGMA user_version";

    private final boolean migrateOldSettings;
    private final Logger sqlLog = LogManager.getLogger("Schema version checker");
    private final MainDBController mainDBController;
    private final ConnectionSource connectionSource;

    @FromTextFile(fileName = "sql/scheme_create_queries/schema_create_query_v1.sql")
    private String schemaCreateQuery1 = null;

    SchemaVersionChecker(@NotNull MainDBController mainDBController,
                         boolean migrateOldSettings) {
        this.migrateOldSettings = migrateOldSettings;
        this.mainDBController = mainDBController;
        this.connectionSource = mainDBController.getConnectionSource();
        ResourcesLoader.initFileResources(this, SchemaVersionChecker.class);
    }

    void checkSchemaVersion() throws SQLException {
        int currentSchemaVersion;
        try {
            GenericRawResults<String[]> result = mainDBController.getCommonPreferencesDAO()
                    .queryRaw(SCHEMA_VERSION_GET_QUERY);
            String rawValue = result.getFirstResult()[0];
            currentSchemaVersion = Integer.parseInt(rawValue);
        } catch (Exception err) {
            sqlLog.fatal("Unable to check schema version: " + err.getMessage(), err);
            throw new SQLException(err);
        }
        if (currentSchemaVersion == 0) {
            initSchemaVersion();
        }
    }

    private void initSchemaVersion() throws SQLException {
        sqlLog.info("Initial scheme " + CURRENT_SCHEMA_VERSION);
        List<Class<?>> dataClasses = CodeSourceUtils.findDataTableClass();
        // prior classes that has foreign ids for another tables
        TableUtils.createTableIfNotExists(connectionSource, ActiveVote.class);
        for (Class<?> dataClass : dataClasses) {
            TableUtils.createTableIfNotExists(connectionSource, dataClass);
        }
        writeSchema(schemaCreateQuery1, CURRENT_SCHEMA_VERSION);
        if (migrateOldSettings) {
            sqlLog.info("Searching legacy settings into JSON files");
            convertLegacy();
        }
    }

    private void writeSchema(@NotNull String schemaQuery, int schemaVersion) throws SQLException {
        sqlLog.info("Write schema from " + schemaVersion);
        String[] buildQueries = schemaQuery.split(";");
        for (String query : buildQueries) {
            try {
                mainDBController.executeRawQuery(query);
            } catch (Exception updateErr) {
                sqlLog.fatal("Unable to execute init SQL query \"" + query + "\": " + updateErr.getMessage(),
                        updateErr);
                throw new SQLException("Init error", updateErr);
            }
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

                    List<JSONActiveVote> legacyVotes = jsonServerPreferences.getActiveVotes();
                    if (!legacyVotes.isEmpty()) {
                        sqlLog.info("Server: {}, found votes", serverId);
                        VotesDAO votesDAO = mainDBController.getVotesDAO();

                        for (JSONActiveVote legacyVote : legacyVotes) {
                            sqlLog.info("Vote {}: {}", legacyVote.getId(), legacyVote.getReadableVoteText());

                            ActiveVote vote = new ActiveVote();
                            vote.setServerId(serverId);
                            vote.setTextChatId(legacyVote.getTextChatId());
                            vote.setFinishTime(legacyVote.getEndDate() > 0L
                                    ? Instant.ofEpochSecond(legacyVote.getEndDate())
                                    : null);
                            vote.setVoteText(legacyVote.getReadableVoteText());
                            vote.setHasTimer(legacyVote.isHasTimer());
                            vote.setExceptional(legacyVote.isExceptionalVote());
                            vote.setHasDefault(legacyVote.isWithDefaultPoint());
                            vote.setWinThreshold(legacyVote.getWinThreshold());
                            List<VoteRole> rolesFilter = new ArrayList<>();
                            for (long roleId : legacyVote.getRolesFilter()) {
                                VoteRole voteRole = new VoteRole();
                                voteRole.setActiveVote(vote);
                                voteRole.setMessageId(vote.getMessageId());
                                voteRole.setRoleId(roleId);
                                rolesFilter.add(voteRole);
                            }
                            vote.setRolesFilter(rolesFilter);

                            List<ActiveVotePoint> votePoints = new ArrayList<>();
                            for (JSONVotePoint legacyVotePoint : legacyVote.getVotePoints()) {
                                ActiveVotePoint votePoint = new ActiveVotePoint();
                                votePoint.setPointText(legacyVotePoint.getPointText());
                                votePoint.setUnicodeEmoji(legacyVotePoint.getEmoji());
                                votePoint.setCustomEmojiId(legacyVotePoint.getCustomEmoji() != null
                                        ? legacyVotePoint.getCustomEmoji() : 0L);
                                votePoints.add(votePoint);
                            }
                            vote.setVotePoints(votePoints);

                            sqlLog.info("Original vote record: {}", legacyVote.toString());
                            try {
                                ActiveVote added = votesDAO.addVote(vote);
                                added.setMessageId(legacyVote.getMessageId());
                                ActiveVote activated = votesDAO.activateVote(added);
                                sqlLog.info("Converted vote record: {}", activated.toString());
                            } catch (VoteCreateException err) {
                                sqlLog.error("Unable to add converted vote to database");
                            }
                        }
                    }
                }
            }
        }
    }
}
