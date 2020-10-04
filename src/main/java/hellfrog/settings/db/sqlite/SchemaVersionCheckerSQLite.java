package hellfrog.settings.db.sqlite;

import hellfrog.common.CommonUtils;
import hellfrog.common.ResourcesLoader;
import hellfrog.settings.db.*;
import hellfrog.settings.db.entity.Vote;
import hellfrog.settings.db.entity.VotePoint;
import hellfrog.settings.db.entity.VoteRoleFilter;
import hellfrog.settings.db.entity.WtfEntry;
import hellfrog.settings.oldjson.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class SchemaVersionCheckerSQLite {

    private static final int CURRENT_SCHEMA_VERSION = 1;
    private static final String SCHEMA_VERSION_GET_QUERY = "PRAGMA user_version";

    private final boolean migrateOldSettings;
    private final Logger sqlLog = LogManager.getLogger("Schema version checker");
    private final Connection connection;
    private final MainDBControllerSQLite mainDBController;

    private final String schemaCreateQuery1 = ResourcesLoader.fromTextFile("sql/scheme_create_queries/schema_create_query_v1.sql");

    SchemaVersionCheckerSQLite(@NotNull MainDBControllerSQLite mainDBController,
                               boolean migrateOldSettings) {
        this.migrateOldSettings = migrateOldSettings;
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

                    List<JSONActiveVote> legacyVotes = jsonServerPreferences.getActiveVotes();
                    if (!legacyVotes.isEmpty()) {
                        sqlLog.info("Server: {}, found votes", serverId);
                        VotesDAO votesDAO = mainDBController.getVotesDAO();

                        for (JSONActiveVote legacyVote : legacyVotes) {
                            sqlLog.info("Vote {}: {}", legacyVote.getId(), legacyVote.getReadableVoteText());

                            Vote vote = new Vote();
                            vote.setServerId(serverId);
                            vote.setTextChatId(legacyVote.getTextChatId());
                            vote.setFinishTime(legacyVote.getEndDate() > 0L
                                    ? Timestamp.from(Instant.ofEpochSecond(legacyVote.getEndDate()))
                                    : null);
                            vote.setVoteText(legacyVote.getReadableVoteText());
                            vote.setHasTimer(legacyVote.isHasTimer());
                            vote.setExceptional(legacyVote.isExceptionalVote());
                            vote.setHasDefault(legacyVote.isWithDefaultPoint());
                            vote.setWinThreshold(legacyVote.getWinThreshold());

                            Set<VoteRoleFilter> roleFilters = legacyVote.getRolesFilter().stream()
                                    .map(old -> {
                                        VoteRoleFilter voteRoleFilter = new VoteRoleFilter();
                                        voteRoleFilter.setCreateDate(vote.getCreateDate());
                                        voteRoleFilter.setMessageId(vote.getMessageId());
                                        voteRoleFilter.setRoleId(old);
                                        voteRoleFilter.setUpdateDate(vote.getUpdateDate());
                                        voteRoleFilter.setVote(vote);
                                        return voteRoleFilter;
                                    }).collect(Collectors.toSet());
                            vote.setRolesFilter(roleFilters);

                            Set<VotePoint> votePoints = new HashSet<>();
                            for (JSONVotePoint legacyVotePoint : legacyVote.getVotePoints()) {
                                VotePoint votePoint = new VotePoint();
                                votePoint.setVote(vote);
                                votePoint.setPointText(legacyVotePoint.getPointText());
                                votePoint.setUnicodeEmoji(legacyVotePoint.getEmoji());
                                votePoint.setCustomEmojiId(legacyVotePoint.getCustomEmoji() != null
                                        ? legacyVotePoint.getCustomEmoji() : 0L);
                                votePoints.add(votePoint);
                            }
                            vote.setVotePoints(votePoints);

                            sqlLog.info("Original vote record: {}", legacyVote.toString());
                            try {
                                Vote added = votesDAO.addVote(vote);
                                added.setMessageId(legacyVote.getMessageId());
                                Vote activated = votesDAO.activateVote(added);
                                sqlLog.info("Converted vote record: {}", activated.toString());
                            } catch (VoteCreateException err) {
                                sqlLog.error("Unable to add converted vote to database");
                            }
                        }
                    }

                    WtfAssignDAO wtfAssignDAO = mainDBController.getWtfAssignDAO();
                    if (!jsonServerPreferences.getWtfMapper().isEmpty()) {
                        sqlLog.info("Server {}, found wtfmap", serverId);
                        for (Map.Entry<Long, JSONWtfMap> entry : jsonServerPreferences.getWtfMapper().entrySet()) {
                            long userId = entry.getKey();
                            if (userId == 0L) {
                                sqlLog.info("Found wtf assign for user with id 0, skipping");
                                continue;
                            }
                            sqlLog.info("Converting assign for user with id {}", userId);
                            JSONWtfMap jsonWtfMap = entry.getValue();
                            WtfEntry lastEntry = null;
                            for (Map.Entry<Long, String> nameValue : jsonWtfMap.getNameValues().entrySet()) {
                                long authorId = nameValue.getKey();
                                String description = nameValue.getValue();
                                if (CommonUtils.isTrStringNotEmpty(description)) {
                                    WtfEntry wtfEntry = new WtfEntry();
                                    wtfEntry.setServerId(serverId);
                                    wtfEntry.setAuthorId(authorId);
                                    wtfEntry.setDescription(description);
                                    wtfEntry.setTargetId(userId);
                                    if (jsonWtfMap.getLastName().get() == authorId) {
                                        lastEntry = wtfEntry;
                                    }
                                    sqlLog.info("Converted wtf entry: {}", wtfEntry.toString());
                                    AddUpdateState state = wtfAssignDAO.addOrUpdate(serverId, userId, wtfEntry);
                                    if (state.equals(AddUpdateState.ADDED)) {
                                        sqlLog.info("Saved OK");
                                    } else {
                                        sqlLog.warn("Saved not ok - {}", state);
                                    }
                                } else {
                                    sqlLog.info("Found empty description from author with id {}, skipping", authorId);
                                }
                            }
                            if (lastEntry != null) {
                                sqlLog.info("Updating last entry by date: {}", lastEntry);
                                AddUpdateState state = wtfAssignDAO.addOrUpdate(serverId, userId, lastEntry);
                                if (state.equals(AddUpdateState.UPDATED)) {
                                    sqlLog.info("Updated OK");
                                } else {
                                    sqlLog.warn("Updated not ok - {}", state);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
