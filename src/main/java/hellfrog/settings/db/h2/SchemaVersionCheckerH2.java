package hellfrog.settings.db.h2;

import hellfrog.common.CommonUtils;
import hellfrog.common.ResourcesLoader;
import hellfrog.settings.ApiKeyStorage;
import hellfrog.settings.db.*;
import hellfrog.settings.db.entity.*;
import hellfrog.settings.oldjson.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class SchemaVersionCheckerH2 {

    public static final String GET_SCHEMA_TABLE_RECORDS_COUNT = """
            SELECT
            	count(1)
            FROM
            	SCHEMA_VERSIONS sv""";
    private static final String RESOURCES_DIRECTORY = "sql/h2/schema";
    private static final String CHECK_SCHEMA_TABLE_QUERY = """
            SELECT
            	count(1)
            FROM
            	information_schema.tables t
            WHERE
            	lower(t.table_schema) = 'public'
            	AND lower(t.table_name) = 'schema_versions'""";
    private static final String GET_LATEST_SCHEMA_QUERY = """
            SELECT
            	max(sv.VERSION)
            FROM
            	SCHEMA_VERSIONS sv""";
    private final String connectionURL;
    private final String connectionUser;
    private final String connectionPassword;
    private final Logger log = LogManager.getLogger("Schema version checker");

    private final Pattern OLD_LAST_KNOWN_DISCRIMINATE_DETECTOR = Pattern.compile("\\(.{2,32}#\\d{4}\\)", Pattern.UNICODE_CHARACTER_CLASS);

    SchemaVersionCheckerH2(@NotNull final String connectionURL,
                           @NotNull final String connectionUser,
                           @NotNull final String connectionPassword) {
        this.connectionURL = connectionURL;
        this.connectionUser = connectionUser;
        this.connectionPassword = connectionPassword;
    }

    boolean checkSchema() throws SQLException {
        boolean migrationRequired;
        try (Connection connection = DriverManager.getConnection(connectionURL, connectionUser, connectionPassword)) {
            long latestSchemaQuery = getLatestSchemaQueryNumber(connection);
            migrationRequired = latestSchemaQuery == 0L;
            log.info("Current DB schema version is: {}", latestSchemaQuery);
            Map<Long, String> migrationFiles = decodeNames(getMigrationFileNames());
            for (Map.Entry<Long, String> migrationFile : migrationFiles.entrySet()) {
                long schemaVersion = migrationFile.getKey();
                String schemaQueriesFileName = migrationFile.getValue();
                if (schemaVersion > latestSchemaQuery) {
                    log.info("Execute migration script \"{}\"", schemaQueriesFileName);
                    String schemaQueries = loadMigrationFile(schemaQueriesFileName);
                    int count = writeSchema(connection, schemaQueries);
                    log.info("Done. Executed {} queries", count);
                }
            }
        }
        return migrationRequired;
    }

    private int writeSchema(@NotNull final Connection connection,
                            @NotNull final String schemaQuery) throws SQLException {

        String[] buildQueries = schemaQuery.split(";");
        try (Statement statement = connection.createStatement()) {
            for (String query : buildQueries) {
                try {
                    statement.executeUpdate(query);
                } catch (Exception updateErr) {
                    String errMessage = String.format("Unable to execute migration SQL query:\n%s\nError: %s",
                            query, updateErr.getMessage());
                    log.fatal(errMessage);
                    throw new SQLException("Init error", updateErr);
                }
            }
        } catch (SQLException err) {
            log.fatal("Unable to create statement: {}", err.getMessage());
            throw new SQLException("Init error", err);
        }
        return buildQueries.length;
    }

    private String loadMigrationFile(@NotNull final String schemaQueriesFileName) throws SQLException {
        try {
            return ResourcesLoader.fromTextFile(schemaQueriesFileName);
        } catch (RuntimeException err) {
            String errMsg = String.format("Unable to load migration script from resource file \"%s\": %s",
                    schemaQueriesFileName, err.getMessage());
            log.error(errMsg, err);
            throw new SQLException(errMsg, err);
        }
    }

    private Map<Long, String> decodeNames(@NotNull final List<String> migrationFileNames) {
        final Map<Long, String> result = new TreeMap<>(Comparator.naturalOrder());
        for (String item : migrationFileNames) {
            if (CommonUtils.isTrStringEmpty(item)) {
                continue;
            }
            String[] parts = item.split("_", 2);
            if (parts.length != 2 || !CommonUtils.isLong(parts[0])) {
                continue;
            }
            long index = CommonUtils.onlyNumbersToLong(parts[0]);
            String resourceFileName = RESOURCES_DIRECTORY + '/' + item;
            result.put(index, resourceFileName);
        }
        return Collections.unmodifiableMap(result);
    }

    private List<String> getMigrationFileNames() throws SQLException {
        try {
            return ResourcesLoader.getFilenamesInResourceDir(RESOURCES_DIRECTORY);
        } catch (RuntimeException err) {
            String errMsg = String.format("Unable to load migration scripts directory \"%s\": %s",
                    RESOURCES_DIRECTORY, err.getMessage());
            log.error(errMsg, err);
            throw new SQLException(errMsg, err);
        }
    }

    private long getLatestSchemaQueryNumber(@NotNull final Connection connection) throws SQLException {
        long tablesCount = executeAndGetFirstRowValue(connection, CHECK_SCHEMA_TABLE_QUERY);
        if (tablesCount == 0L) {
            log.info("Schema versions table does not present");
            return 0;
        }
        long recordsInSchemaTable = executeAndGetFirstRowValue(connection, GET_SCHEMA_TABLE_RECORDS_COUNT);
        if (recordsInSchemaTable == 0L) {
            log.info("Schema versions table is empty");
            return 0L;
        }
        return executeAndGetFirstRowValue(connection, GET_LATEST_SCHEMA_QUERY);
    }

    private long executeAndGetFirstRowValue(@NotNull final Connection connection,
                                            @NotNull final String query) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(query)) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                if (metaData.getColumnCount() == 0) {
                    return 0L;
                }
                if (resultSet.next()) {
                    return resultSet.getLong(1);
                } else {
                    return 0L;
                }
            }
        }
    }

    void convertLegacy(@NotNull final MainDBController mainDBController) throws IOException {
        JSONLegacySettings jsonLegacySettings = new JSONLegacySettings();
        if (jsonLegacySettings.isHasCommonPreferences() || jsonLegacySettings.isHasServerPreferences()
                || jsonLegacySettings.isHasServerStatistics()) {
            log.info("Attempt to legacy JSON settings conversion");
            if (jsonLegacySettings.isHasCommonPreferences()) {

                JSONCommonPreferences oldCommonPreferences = jsonLegacySettings.getJsonCommonPreferences();
                CommonPreferencesDAO newCommonPreferences = mainDBController.getCommonPreferencesDAO();
                BotOwnersDAO botOwnersDAO = mainDBController.getBotOwnersDAO();

                if (CommonUtils.isTrStringNotEmpty(oldCommonPreferences.getApiKey())) {
                    log.info("Common preferences: found API key: {}", oldCommonPreferences.getApiKey());
                    newCommonPreferences.setApiKey(oldCommonPreferences.getApiKey());
                    ApiKeyStorage.writeApiKey(oldCommonPreferences.getApiKey());
                }
                if (CommonUtils.isTrStringNotEmpty(oldCommonPreferences.getCommonBotPrefix())) {
                    log.info("Common preferences: found global bot prefix: {}",
                            oldCommonPreferences.getCommonBotPrefix());
                    newCommonPreferences.setBotPrefix(oldCommonPreferences.getCommonBotPrefix());
                }
                if (CommonUtils.isTrStringNotEmpty(oldCommonPreferences.getBotName())) {
                    log.info("Common preferences: found bot name: {}", oldCommonPreferences.getBotName());
                    newCommonPreferences.setBotName(oldCommonPreferences.getBotName());
                }
                if (oldCommonPreferences.getGlobalBotOwners() != null
                        && !oldCommonPreferences.getGlobalBotOwners().isEmpty()) {
                    log.info("Common preferences: found global bot owners: {}",
                            oldCommonPreferences.getGlobalBotOwners().stream()
                                    .map(String::valueOf)
                                    .reduce(CommonUtils::reduceConcat)
                                    .orElse(""));
                    oldCommonPreferences.getGlobalBotOwners().forEach(botOwnersDAO::addToOwners);
                }
            }
            if (jsonLegacySettings.isHasServerPreferences()) {

                ServerPreferencesDAO serverPreferencesDAO = mainDBController.getServerPreferencesDAO();
                AutoPromoteRolesDAO autoPromoteRolesDAO = mainDBController.getAutoPromoteRolesDAO();
                CommunityControlDAO communityControlDAO = mainDBController.getCommunityControlDAO();

                for (Map.Entry<Long, JSONServerPreferences> serverPref : jsonLegacySettings.getPrefByServer().entrySet()) {
                    long serverId = serverPref.getKey();
                    JSONServerPreferences jsonServerPreferences = serverPref.getValue();
                    log.info("Found preferences for server {}", serverId);

                    String botPrefix = jsonServerPreferences.getBotPrefix();
                    if (CommonUtils.isTrStringNotEmpty(botPrefix)) {
                        log.info("Server {}: found bot prefix: {}", serverId, botPrefix);
                        serverPreferencesDAO.setPrefix(serverId, botPrefix);
                    }

                    boolean isJoinLeaveDisplay = jsonServerPreferences.isJoinLeaveDisplay();
                    log.info("Server {}: found join/leave display state: {}", serverId, isJoinLeaveDisplay);
                    serverPreferencesDAO.setJoinLeaveDisplay(serverId, isJoinLeaveDisplay);

                    long joinLeaveChannel = jsonServerPreferences.getJoinLeaveChannel();
                    if (joinLeaveChannel > 0L) {
                        log.info("Server {}: found join/leave channel: {}", serverId, joinLeaveChannel);
                        serverPreferencesDAO.setJoinLeaveChannel(serverId, joinLeaveChannel);
                    }

                    boolean isNewAclMode = jsonServerPreferences.getNewAclMode();
                    log.info("Server {}: found new ACL state: {}", serverId, isNewAclMode);
                    serverPreferencesDAO.setNewAclMode(serverId, isNewAclMode);

                    boolean autoPromoteEnabled = jsonServerPreferences.getAutoPromoteEnabled()
                            && jsonServerPreferences.getAutoPromoteRoleId() != null
                            && jsonServerPreferences.getAutoPromoteRoleId() > 0L;
                    if (autoPromoteEnabled) {
                        long roleId = jsonServerPreferences.getAutoPromoteRoleId();
                        long timeout = jsonServerPreferences.getAutoPromoteTimeout() != null
                                ? jsonServerPreferences.getAutoPromoteTimeout() : 0L;
                        log.info("Server {}: found enabled auto promote role with id {} and timeout {} sec.",
                                serverId, roleId, timeout);
                        autoPromoteRolesDAO.addUpdateConfig(serverId, roleId, timeout);
                    }

                    String unicodeCommunityEmoji = jsonServerPreferences.getCommunityControlEmoji();
                    long communityControlEmojiId = jsonServerPreferences.getCommunityControlCustomEmojiId() != null
                            ? jsonServerPreferences.getCommunityControlCustomEmojiId()
                            : 0L;
                    long communityControlRoleId = jsonServerPreferences.getCommunityControlRoleId() != null
                            ? jsonServerPreferences.getCommunityControlRoleId()
                            : 0L;
                    long communityControlThreshold = jsonServerPreferences.getCommunityControlThreshold() != null
                            ? jsonServerPreferences.getCommunityControlThreshold()
                            : 0L;
                    List<Long> communityControlUsers = jsonServerPreferences.getCommunityControlUsers();
                    boolean hasCommunityControl = CommonUtils.isTrStringNotEmpty(unicodeCommunityEmoji)
                            || communityControlEmojiId > 0L
                            || communityControlRoleId > 0L
                            || communityControlThreshold > 0L
                            || (communityControlUsers != null && !communityControlUsers.isEmpty());

                    if (hasCommunityControl) {
                        CommunityControlSettings controlSettings = new CommunityControlSettings();
                        controlSettings.setServerId(serverId);
                        controlSettings.setRoleId(communityControlRoleId);
                        controlSettings.setUnicodeEmoji(unicodeCommunityEmoji);
                        controlSettings.setCustomEmojiId(communityControlEmojiId);
                        controlSettings.setThreshold(communityControlThreshold);
                        log.info("Found community control settings for server {}, converted to: {}",
                                serverId, controlSettings.toString());
                        communityControlDAO.setSettings(controlSettings);

                        if (communityControlUsers != null && !communityControlUsers.isEmpty()) {
                            String list = communityControlUsers.stream()
                                    .map(String::valueOf)
                                    .reduce(CommonUtils::reduceConcat)
                                    .orElse("(empty)");
                            log.info("Found community control users for server {}: {}", serverId, list);
                            communityControlUsers.forEach(userId -> communityControlDAO.addUser(serverId, userId));
                        }
                    }

                    boolean congratulationsEnabled = jsonServerPreferences.getCongratulationChannel() != null
                            && jsonServerPreferences.getCongratulationChannel() > 0L;
                    long congratulationsChannelId = jsonServerPreferences.getCongratulationChannel() != null
                            ? jsonServerPreferences.getCongratulationChannel() : 0L;
                    String congratulationsTimeZoneId = jsonServerPreferences.getTimezone() != null
                            ? jsonServerPreferences.getTimezone() : TimeZone.getDefault().getID();
                    log.info("Server {}: congratulations enabled: {} at channel {} with timezone {}",
                            serverId, congratulationsEnabled, congratulationsChannelId, congratulationsTimeZoneId);
                    serverPreferencesDAO.setCongratulationEnabled(serverId, congratulationsEnabled);
                    serverPreferencesDAO.setCongratulationChannel(serverId, congratulationsChannelId);
                    serverPreferencesDAO.setCongratulationTimeZone(serverId, congratulationsTimeZoneId);

                    Map<String, JSONCommandRights> legacyRights = jsonServerPreferences.getSrvCommandRights();
                    if (!legacyRights.isEmpty()) {
                        log.info("Server {}: found command rights settings", serverId);
                        UserRightsDAO userRightsDAO = mainDBController.getUserRightsDAO();
                        RoleRightsDAO roleRightsDAO = mainDBController.getRoleRightsDAO();
                        ChannelRightsDAO channelRightsDAO = mainDBController.getTextChannelRightsDAO();
                        ChannelCategoryRightsDAO channelCategoryRightsDAO = mainDBController.getChannelCategoryRightsDAO();
                        for (Map.Entry<String, JSONCommandRights> rightsEntry : legacyRights.entrySet()) {
                            String commandPrefix = rightsEntry.getKey();
                            JSONCommandRights commandRights = rightsEntry.getValue();
                            if (!commandPrefix.equals(commandRights.getCommandPrefix())) {
                                log.warn("Server {}: command prefix of legacy map key {} " +
                                                "not equals with command prefix name in object: {}",
                                        serverId, commandPrefix, commandRights.getCommandPrefix());
                            }
                            if (commandRights.getAllowUsers().isEmpty()
                                    && commandRights.getAllowRoles().isEmpty()
                                    && commandRights.getAllowChannels().isEmpty()) {
                                log.info("Server {}: empty privileges for command {}, skipping",
                                        serverId, commandPrefix);
                            } else {
                                for (long userId : commandRights.getAllowUsers()) {
                                    log.info("Server {}: grant access to userId {} for command {}",
                                            serverId, userId, commandPrefix);
                                    boolean isAllowed = userRightsDAO.allow(serverId, userId, commandPrefix);
                                    if (isAllowed) {
                                        log.info("Grant is OK");
                                    } else {
                                        log.warn("Grant is not OK");
                                    }
                                }
                                for (long roleId : commandRights.getAllowRoles()) {
                                    log.info("Server {}: grant access to roleId {} for command {}",
                                            serverId, roleId, commandPrefix);
                                    boolean isAllowed = roleRightsDAO.allow(serverId, roleId, commandPrefix);
                                    if (isAllowed) {
                                        log.info("Grant is OK");
                                    } else {
                                        log.warn("Grant is not OK");
                                    }
                                }
                                for (long textChatId : commandRights.getAllowChannels()) {
                                    log.info("Server {}: grant access to textChatId/categoryId {} " +
                                            "for command {}", serverId, textChatId, commandPrefix);
                                    boolean isAllowedChat = channelRightsDAO.allow(serverId, textChatId,
                                            commandPrefix);
                                    boolean isAllowedCategory = channelCategoryRightsDAO.allow(serverId,
                                            textChatId, commandPrefix);
                                    if (isAllowedChat) {
                                        log.info("Grant text chat is OK");
                                    } else {
                                        log.warn("Grant text chat is not OK");
                                    }
                                    if (isAllowedCategory) {
                                        log.info("Grant category is OK");
                                    } else {
                                        log.warn("Grant category is not OK");
                                    }
                                }
                            }
                        }
                    }

                    List<JSONActiveVote> legacyVotes = jsonServerPreferences.getActiveVotes();
                    if (!legacyVotes.isEmpty()) {
                        log.info("Server: {}, found votes", serverId);
                        VotesDAO votesDAO = mainDBController.getVotesDAO();

                        for (JSONActiveVote legacyVote : legacyVotes) {
                            log.info("Vote {}: {}", legacyVote.getId(), legacyVote.getReadableVoteText());

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

                            log.info("Original vote record: {}", legacyVote.toString());
                            try {
                                Vote added = votesDAO.addVote(vote);
                                added.setMessageId(legacyVote.getMessageId());
                                Vote activated = votesDAO.activateVote(added);
                                log.info("Converted vote record: {}", activated.toString());
                            } catch (VoteCreateException err) {
                                log.error("Unable to add converted vote to database");
                            }
                        }
                    }

                    WtfAssignDAO wtfAssignDAO = mainDBController.getWtfAssignDAO();
                    if (!jsonServerPreferences.getWtfMapper().isEmpty()) {
                        log.info("Server {}, found wtfmap", serverId);
                        for (Map.Entry<Long, JSONWtfMap> entry : jsonServerPreferences.getWtfMapper().entrySet()) {
                            long userId = entry.getKey();
                            if (userId == 0L) {
                                log.info("Found wtf assign for user with id 0, skipping");
                                continue;
                            }
                            log.info("Converting assign for user with id {}", userId);
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
                                    log.info("Converted wtf entry: {}", wtfEntry.toString());
                                    AddUpdateState state = wtfAssignDAO.addOrUpdate(wtfEntry);
                                    if (state.equals(AddUpdateState.ADDED)) {
                                        log.info("Saved OK");
                                    } else {
                                        log.warn("Saved not ok - {}", state);
                                    }
                                } else {
                                    log.info("Found empty description from author with id {}, skipping", authorId);
                                }
                            }
                            if (lastEntry != null) {
                                log.info("Updating last entry by date: {}", lastEntry);
                                AddUpdateState state = wtfAssignDAO.addOrUpdate(lastEntry);
                                if (state.equals(AddUpdateState.UPDATED)) {
                                    log.info("Updated OK");
                                } else {
                                    log.warn("Updated not ok - {}", state);
                                }
                            }
                        }
                    }
                }

                TotalStatisticDAO totalStatisticDAO = mainDBController.getTotalStatisticDAO();
                EntityNameCacheDAO nameCacheDAO = mainDBController.getEntityNameCacheDAO();
                for (Map.Entry<Long, JSONServerStatistic> serverStats : jsonLegacySettings.getStatByServer().entrySet()) {

                    long serverId = serverStats.getKey();
                    JSONServerStatistic jsonServerStatistic = serverStats.getValue();

                    if (jsonServerStatistic != null) {

                        boolean enableStatistic = jsonServerStatistic.isCollectNonDefaultSmileStats();
                        Long startDateMs = jsonServerStatistic.getStartDate();
                        Instant startDate;
                        if (startDateMs != null && startDateMs > 0L) {
                            Calendar since = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                            since.setTimeInMillis(startDateMs);
                            startDate = since.toInstant();
                        } else {
                            startDate = ServerPreferencesDAO.STATISTIC_START_DATE_DEFAULT;
                        }

                        log.info("Found server statistics for id: {}. Statistic is enabled: {}, since: {}",
                                serverId, enableStatistic, startDate);

                        serverPreferencesDAO.setStatisticEnabled(serverId, enableStatistic);
                        serverPreferencesDAO.setStatisticStartDate(serverId, startDate);

                        Map<Long, JSONSmileStatistic> smileStats = jsonServerStatistic.getNonDefaultSmileStats();
                        if (smileStats != null && !smileStats.isEmpty()) {
                            log.info("Found emoji total statistic for server {}", serverId);

                            for (Map.Entry<Long, JSONSmileStatistic> smileStat : smileStats.entrySet()) {
                                long emojiId = smileStat.getKey();
                                JSONSmileStatistic smileStatistic = smileStat.getValue();
                                long usagesCount = smileStatistic.getUsagesCount() != null
                                        ? smileStatistic.getUsagesCount().get()
                                        : 0L;
                                if (usagesCount < 0L) {
                                    usagesCount = 0L;
                                }
                                Instant lastUpdate = Instant.now();
                                if (smileStatistic.getLastUsage() != null) {
                                    long rawLastUpdate = smileStatistic.getLastUsage().get();
                                    if (rawLastUpdate > 0L) {
                                        Calendar current = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                                        current.setTimeInMillis(rawLastUpdate);
                                        lastUpdate = current.toInstant();
                                    }
                                }
                                log.info("Found emoji stat: server {}, emoji id {}, usages count {}, last update {}",
                                        serverId, emojiId, usagesCount, lastUpdate);
                                totalStatisticDAO.insertEmojiStats(serverId, emojiId, usagesCount, lastUpdate);
                            }
                        }

                        Map<Long, JSONMessageStatistic> textChatStats = jsonServerStatistic.getTextChatStats();
                        if (textChatStats != null && !textChatStats.isEmpty()) {
                            for (Map.Entry<Long, JSONMessageStatistic> channelEntry : textChatStats.entrySet()) {

                                long textChannelId = channelEntry.getKey();
                                JSONMessageStatistic oldChannelStat = channelEntry.getValue();

                                Map<Long, JSONMessageStatistic> byUserStats = oldChannelStat.getChildStatistic();
                                if (byUserStats != null && !byUserStats.isEmpty()) {
                                    log.info("Found text channels message statistic for server {}", serverId);

                                    if (CommonUtils.isTrStringNotEmpty(oldChannelStat.getLastKnownName())) {
                                        log.info("Found last known name for text chat entry {}: \"{}\"",
                                                textChannelId, oldChannelStat.getLastKnownName());
                                        nameCacheDAO.update(textChannelId, oldChannelStat.getLastKnownName(), NameType.CHANNEL);
                                        nameCacheDAO.update(serverId, textChannelId, oldChannelStat.getLastKnownName());
                                    }

                                    for (Map.Entry<Long, JSONMessageStatistic> byUserEntry : byUserStats.entrySet()) {

                                        long userId = byUserEntry.getKey();
                                        JSONMessageStatistic userStats = byUserEntry.getValue();

                                        if (userStats != null) {
                                            log.info("Found text channel message statistic for server: {}, " +
                                                            "text chat: {}, user: {}",
                                                    serverId, textChannelId, userId);

                                            if (CommonUtils.isTrStringNotEmpty(userStats.getLastKnownName())
                                                    && nameCacheDAO.find(userId).isEmpty()) {

                                                Matcher discriminateNameFinder = OLD_LAST_KNOWN_DISCRIMINATE_DETECTOR
                                                        .matcher(userStats.getLastKnownName());
                                                if (discriminateNameFinder.find()) {
                                                    String discriminationName = discriminateNameFinder.group();
                                                    discriminationName = CommonUtils.cutLeftString(discriminationName, "(");
                                                    discriminationName = CommonUtils.cutRightString(discriminationName, ")");
                                                    discriminationName = discriminationName.strip();
                                                    String displayName = CommonUtils.cutRightString(userStats.getLastKnownName(),
                                                            discriminateNameFinder.group()).strip();
                                                    log.info("Found last known name for user {}: {}. Convert to " +
                                                                    " display name: {}, discriminate name: {}", userId,
                                                            userStats.getLastKnownName(), displayName, discriminationName);
                                                    nameCacheDAO.update(userId, discriminationName, NameType.USER);
                                                    nameCacheDAO.update(serverId, userId, displayName);
                                                }
                                            }

                                            long messagesCount = userStats.getCountOfMessages();
                                            long bytesCount = userStats.getCountOfBytes();
                                            long symbolsCount = userStats.getCountOfSymbols();
                                            AtomicLong lastMessageDateMs = userStats.getLastMessageDate();
                                            Instant lastMessageDate;
                                            if (lastMessageDateMs != null) {
                                                long value = lastMessageDateMs.get();
                                                Calendar result = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                                                result.setTimeInMillis(value);
                                                lastMessageDate = result.toInstant();
                                            } else {
                                                lastMessageDate = Instant.EPOCH;
                                            }

                                            log.info("Found statistic for server: {}, text chat: {}, user: {}; " +
                                                            "messages count: {}, last message: {}, symbols count: {}, bytes count: {}",
                                                    serverId, textChannelId, userId, messagesCount, lastMessageDate, symbolsCount, bytesCount);

                                            totalStatisticDAO.insertChannelStats(serverId, textChannelId, userId,
                                                    messagesCount, lastMessageDate, symbolsCount, bytesCount);
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
}
