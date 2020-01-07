package hellfrog.settings.db;

import hellfrog.common.CommonUtils;
import hellfrog.common.FromTextFile;
import hellfrog.common.ResourcesLoader;
import hellfrog.settings.entity.Vote;
import hellfrog.settings.entity.VotePoint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DAO for working with voting in the database.
 * <p>
 * CREATE TABLE "active_votes" (<br>
 * "vote_id"           INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,<br>
 * "server_id"         INTEGER NOT NULL,<br>
 * "text_chat_id"      INTEGER NOT NULL,<br>
 * "message_id"        INTEGER NOT NULL,<br>
 * "finish_date"       INTEGER NOT NULL,<br>
 * "vote_text"         TEXT NOT NULL,<br>
 * "has_timer"         INTEGER NOT NULL DEFAULT 0,<br>
 * "is_exceptional"    INTEGER NOT NULL DEFAULT 0,<br>
 * "has_default"       INTEGER NOT NULL DEFAULT 0,<br>
 * "win_threshold"     INTEGER NOT NULL DEFAULT 0,<br>
 * "roles_filter"      TEXT NOT NULL,<br>
 * "create_date"       INTEGER NOT NULL DEFAULT 0,<br>
 * "update_date"       INTEGER NOT NULL DEFAULT 0<br>
 * );
 * </p>
 * <p>
 * CREATE TABLE "vote_points" (<br>
 * "id"                INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,<br>
 * "vote_id"           INTEGER NOT NULL,<br>
 * "point_text"        INTEGER NOT NULL,<br>
 * "unicode_emoji"     TEXT,<br>
 * "custom_emoji_id"   INTEGER,<br>
 * "create_date"       INTEGER NOT NULL DEFAULT 0,<br>
 * "update_date"       INTEGER NOT NULL DEFAULT 0,<br>
 * FOREIGN KEY("vote_id") REFERENCES "active_votes"("vote_id")<br>
 * );
 * </p>
 */
public class VotesDAO {

    private static final Logger log = LogManager.getLogger("Votes");

    private final Connection connection;

    @FromTextFile(fileName = "sql/votes/get_all_votes_query.sql")
    private String getAllVotesQuery = null;
    @FromTextFile(fileName = "sql/votes/get_all_expired_votes_query.sql")
    private String getAllExpiredVotesQuery = null;
    @FromTextFile(fileName = "sql/votes/insert_vote_query.sql")
    private String insertVoteQuery = null;
    @FromTextFile(fileName = "sql/votes/insert_vote_point_query.sql")
    private String insertVotePointQuery = null;
    @FromTextFile(fileName = "sql/votes/get_last_insert_vote_id_query.sql")
    private String getLastInsertVoteIdQuery = null;
    @FromTextFile(fileName = "sql/votes/get_vote_by_id_query.sql")
    private String getVoteByIdQuery = null;
    @FromTextFile(fileName = "sql/votes/activate_vote_query.sql")
    private String activateVoteQuery = null;
    @FromTextFile(fileName = "sql/votes/delete_vote_points_query.sql")
    private String deleteVotePointsQuery = null;
    @FromTextFile(fileName = "sql/votes/delete_vote_query.sql")
    private String deleteVoteQuery = null;

    public VotesDAO(@NotNull Connection connection) {
        this.connection = connection;
        ResourcesLoader.initFileResources(this, VotesDAO.class);
    }

    public List<Vote> getAll(long serverId) {
        if (log.isDebugEnabled()) {
            log.debug("Query:\n{}\nParam 1: {}", getAllVotesQuery, serverId);
        }

        try (PreparedStatement statement = connection.prepareStatement(getAllVotesQuery)) {
            statement.setLong(1, serverId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Vote> extracted = extractVotes(resultSet);
                if (log.isDebugEnabled()) {
                    log.debug("Extracted votes for server id {}:", serverId);
                    for (Vote vote : extracted) {
                        log.debug(vote.toString());
                    }
                }
                return extracted;
            }
        } catch (SQLException err) {
            String errMsg = String.format("Unable extract votes for serverId \"%d\": %s",
                    serverId, err.getMessage());
            log.error(errMsg, err);
            return Collections.emptyList();
        }
    }

    public List<Vote> getAllExpired(long serverId) {
        return getAllExpired(serverId, Instant.now());
    }

    List<Vote> getAllExpired(long serverId, @NotNull Instant dateTime) {
        long currentDateTime = dateTime.getEpochSecond();
        if (log.isDebugEnabled()) {
            log.debug("Query:\n{}\nParam 1: {}\nParam 2: {}", getAllExpiredVotesQuery, serverId,
                    currentDateTime);
        }
        try (PreparedStatement statement = connection.prepareStatement(getAllExpiredVotesQuery)) {
            statement.setLong(1, serverId);
            statement.setLong(2, currentDateTime);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Vote> extracted = extractVotes(resultSet);
                if (log.isDebugEnabled()) {
                    log.debug("Extracted votes for server id {}:", serverId);
                    for (Vote vote : extracted) {
                        log.debug(vote.toString());
                    }
                }
                return extracted;
            }
        } catch (SQLException err) {
            String errMsg = String.format("Unable extract expired votes for serverId \"%d\": %s",
                    serverId, err.getMessage());
            log.error(errMsg, err);
            return Collections.emptyList();
        }
    }

    /**
     * Adding voting to the database.<br>
     * <p>First, checks are carried out on the correctness of filling
     * in the fields and voting points on the side of the script
     * (or the old type of team). Next, the object is passed to this method.
     * If this method did not return an exception when adding a vote
     * to the database, then the vote is published in a text chat
     * (since there may be a situation that there will be insufficient
     * rights to send a message in the chat, or a network error).</p>
     * <p>After successful publication, voting is activated by
     * the appropriate method {@link #activateVote(Vote)}.</p>
     *
     * @param vote added vote. Fields such as identifier,
     *             creation date, and change are ignored during insertion.
     * @return created object of voting. This object has been retrieved from the database.
     * @throws VoteCreateException one or another error in the process of adding
     *                             voting to the database. Keep in mind that this method does not check
     *                             the correctness of filling in the voting fields. It only requires
     *                             that the vote have at least one point.
     */
    public synchronized Vote addVote(@NotNull Vote vote) throws VoteCreateException {
        if (vote.getVotePoints().isEmpty()) {
            log.error("Vote points is empty: {}", vote.toString());
            throw new VoteCreateException("one or more vote points required");
        }
        long serverId = vote.getServerId();
        long textChatId = vote.getTextChatId();
        long messageId = 0L;
        long finishTime = vote.getFinishTime().map(Instant::getEpochSecond).orElse(0L);
        String voteText = vote.getVoteText().orElse(" ");
        long hasTimer = vote.isHasTimer() ? 1L : 0L;
        long exceptionalVote = vote.isExceptional() ? 1L : 0L;
        long hasDefault = vote.isHasDefault() ? 1L : 0L;
        long winThreshold = vote.getWinThreshold();
        String rolesFilter = vote.getRolesFilter() == null ? " " : vote.getRolesFilter()
                .stream()
                .map(String::valueOf)
                .reduce(CommonUtils::reduceConcat)
                .orElse(" ");
        long currentDateTime = Instant.now().getEpochSecond();
        if (log.isDebugEnabled()) {
            log.debug("Query:\n{}\nParam 1: {}\nParam 2: {}\nParam 3: {}\nParam 4: {}\n" +
                            "Param 5: {}\nParam 6: {}\nParam 7: {}\nParam 8: {}\nParam 9: {}\n" +
                            "Param 10: {}\nParam 11: {}\nParam 12: {}", insertVoteQuery, serverId,
                    textChatId, messageId, finishTime, voteText, hasTimer, exceptionalVote, hasDefault,
                    winThreshold, rolesFilter, currentDateTime, currentDateTime);
        }
        try (PreparedStatement statement = connection.prepareStatement(insertVoteQuery)) {
            statement.setLong(Vote.Columns.SERVER_ID.insertColumn, serverId);
            statement.setLong(Vote.Columns.TEXT_CHAT_ID.insertColumn, textChatId);
            statement.setLong(Vote.Columns.MESSAGE_ID.insertColumn, messageId);
            statement.setLong(Vote.Columns.FINISH_DATE.insertColumn, finishTime);
            statement.setString(Vote.Columns.VOTE_TEXT.insertColumn, voteText);
            statement.setLong(Vote.Columns.HAS_TIMER.insertColumn, hasTimer);
            statement.setLong(Vote.Columns.IS_EXCEPTIONAL.insertColumn, exceptionalVote);
            statement.setLong(Vote.Columns.HAS_DEFAULT.insertColumn, hasDefault);
            statement.setLong(Vote.Columns.WIN_THRESHOLD.insertColumn, winThreshold);
            statement.setString(Vote.Columns.ROLES_FILTER.insertColumn, rolesFilter);
            statement.setLong(Vote.Columns.CREATE_DATE.insertColumn, currentDateTime);
            statement.setLong(Vote.Columns.UPDATE_DATE.insertColumn, currentDateTime);
            int insertCount = statement.executeUpdate();
            if (log.isDebugEnabled()) {
                log.debug("Inserted {} values", insertCount);
            }
        } catch (SQLException err) {
            String errMsg = String.format("Unable to insert new active_vote value: %s",
                    err.getMessage());
            log.error(errMsg, err);
            throw new VoteCreateException("database error", err);
        }
        if (log.isDebugEnabled()) {
            log.debug("Query:\n{}", getLastInsertVoteIdQuery);
        }
        long voteId = -1L;
        try (Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(getLastInsertVoteIdQuery)) {
                if (resultSet.next()) {
                    voteId = resultSet.getLong(1);
                }
            }
        } catch (SQLException err) {
            String errMsg = String.format("Unable to extract latest inserted vote id from table: %s",
                    err.getMessage());
            log.error(errMsg, err);
            throw new VoteCreateException("database error", err);
        }
        if (log.isDebugEnabled()) {
            log.debug("Latest inserted vote id: {}", voteId);
        }
        if (voteId <= 0) {
            log.error("Latest inserted vote id is {} (may be NULL)", voteId);
            throw new VoteCreateException("database error");
        }
        for (VotePoint votePoint : vote.getVotePoints()) {
            String pointText = votePoint.getPointText().orElse(" ");
            String emojiText = votePoint.getUnicodeEmoji().orElse(" ");
            long customEmojiId = votePoint.getCustomEmojiId();
            currentDateTime = Instant.now().getEpochSecond();
            if (log.isDebugEnabled()) {
                log.debug("Query:\n{}\nParam 1: {}\nParam 2: {}\nParam 3: {}\nParam 4: {}" +
                                "\nParam 5: {}\nParam 6: {}", insertVotePointQuery, voteId, pointText,
                        emojiText, customEmojiId, currentDateTime, currentDateTime);
            }
            try (PreparedStatement statement = connection.prepareStatement(insertVotePointQuery)) {
                statement.setLong(VotePoint.Columns.VOTE_ID.insertColumn, voteId);
                statement.setString(VotePoint.Columns.POINT_TEXT.insertColumn, pointText);
                statement.setString(VotePoint.Columns.UNICODE_EMOJI.insertColumn, emojiText);
                statement.setLong(VotePoint.Columns.CUSTOM_EMOJI_ID.insertColumn, customEmojiId);
                statement.setLong(VotePoint.Columns.CREATE_DATE.insertColumn, currentDateTime);
                statement.setLong(VotePoint.Columns.UPDATE_DATE.insertColumn, currentDateTime);
                int count = statement.executeUpdate();
                if (log.isDebugEnabled()) {
                    log.debug("Inserted {} values", count);
                }
            } catch (SQLException err) {
                String errMsg = String.format("Unable insert vote point: %s", err.getMessage());
                log.error(errMsg, err);
                throw new VoteCreateException("database error", err);
            }
        }
        try {
            Vote result = getVoteById(voteId);
            if (result == null) {
                log.error("Unable to extract inserted vote from database, getting vote by voteId {} " +
                        "is empty", voteId);
                throw new VoteCreateException("database error");
            }
            return result;
        } catch (SQLException err) {
            String errMsg = String.format("Unable to extract inserted vote from database with voteId " +
                    "\"%d\": %s", voteId, err.getMessage());
            log.error(errMsg, err);
            throw new VoteCreateException("database error", err);
        }
    }

    /**
     * Activation of voting records in the database.
     * Writes the value of the message identifier from the received object.
     *
     * @param vote received object of voting. The identifier of the sent message
     *             that was sent to the chat is extracted from the voting object.
     *             All other fields of the object are ignored.
     * @return updated object of voting. This object has been retrieved from the database.
     * @throws VoteCreateException one or another error in the process of updating
     *                             voting to the database.
     */
    public Vote activateVote(@NotNull Vote vote) throws VoteCreateException {
        if (vote.getMessageId() <= 0L) {
            log.error("messageId required (must be great that zero): {}", vote.toString());
            throw new VoteCreateException("message not sent, voting message identifier missing");
        }
        if (vote.getId() <= 0) {
            log.error("voteId required (must be great that zero) (vote is not saved?): {}",
                    vote.toString());
            throw new VoteCreateException("this vote was not stored in the database");
        }
        long currentDateTime = Instant.now().getEpochSecond();
        if (log.isDebugEnabled()) {
            log.debug("Query:\n{}\nParam 1: {}\nParam 2: {}\nParam 3: {}", activateVoteQuery,
                    vote.getMessageId(), currentDateTime, vote.getId());
        }
        try (PreparedStatement statement = connection.prepareStatement(activateVoteQuery)) {
            statement.setLong(1, vote.getMessageId());
            statement.setLong(2, currentDateTime);
            statement.setLong(3, vote.getId());
            int count = statement.executeUpdate();
            if (log.isDebugEnabled()) {
                log.debug("Updated {} values", count);
            }
            if (count != 1) {
                log.error("Unable update vote with id {} by message_id {} - {} values updated " +
                        "(missing vote?)", vote.getId(), vote.getMessageId(), count);
                throw new VoteCreateException("database error");
            }
        } catch (SQLException err) {
            String errMsg = String.format("Unable to update vote with vote_id \"%d\": %s",
                    vote.getId(), err.getMessage());
            log.error(errMsg, err);
            throw new VoteCreateException("database error");
        }
        try {
            Vote result = getVoteById(vote.getId());
            if (result == null) {
                log.error("Unable to extract updated vote from database, getting vote by voteId {} " +
                        "is empty", vote.getId());
                throw new VoteCreateException("database error");
            }
            return result;
        } catch (SQLException err) {
            String errMsg = String.format("Unable to extract updated vote from database with voteId " +
                    "\"%d\": %s", vote.getId(), err.getMessage());
            log.error(errMsg, err);
            throw new VoteCreateException("database error", err);
        }
    }

    @Nullable
    public Vote getVoteById(long voteId) throws SQLException {
        if (log.isDebugEnabled()) {
            log.debug("Query:\n{}\nParam 1: {}", getVoteByIdQuery, voteId);
        }
        try (PreparedStatement statement = connection.prepareStatement(getVoteByIdQuery)) {
            statement.setLong(1, voteId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Vote> result = extractVotes(resultSet);
                if (log.isDebugEnabled()) {
                    log.debug("Extracted votes for vote id {}:", voteId);
                    for (Vote vote : result) {
                        log.debug(vote.toString());
                    }
                }
                return !result.isEmpty() ? result.get(0) : null;
            }
        }
    }

    @NotNull
    private List<Vote> extractVotes(@NotNull ResultSet resultSet) throws SQLException {
        List<Vote> result = new ArrayList<>();
        Vote currentVote = null;
        while (resultSet.next()) {
            long voteId = resultSet.getLong(Vote.Columns.ID.selectColumn);
            if (currentVote == null || voteId != currentVote.getId()) {
                currentVote = new Vote();
                String rawRolesFilter = resultSet.getString(Vote.Columns.ROLES_FILTER.selectColumn);
                if (CommonUtils.isTrStringNotEmpty(rawRolesFilter)) {
                    List<Long> rolesFilter = currentVote.getRolesFilter();
                    for (String item : rawRolesFilter.split(",")) {
                        long roleId = CommonUtils.onlyNumbersToLong(item);
                        if (roleId > 0L)
                            rolesFilter.add(roleId);
                    }
                }
                currentVote.setId(voteId)
                        .setServerId(resultSet.getLong(Vote.Columns.SERVER_ID.selectColumn))
                        .setTextChatId(resultSet.getLong(Vote.Columns.TEXT_CHAT_ID.selectColumn))
                        .setMessageId(resultSet.getLong(Vote.Columns.MESSAGE_ID.selectColumn))
                        .setFinishTime(resultSet.getLong(Vote.Columns.FINISH_DATE.selectColumn) > 0
                                ? Instant.ofEpochSecond(resultSet.getLong(Vote.Columns.FINISH_DATE.selectColumn))
                                : null)
                        .setVoteText(CommonUtils.isTrStringNotEmpty(resultSet.getString(Vote.Columns.VOTE_TEXT.selectColumn))
                                ? resultSet.getString(Vote.Columns.VOTE_TEXT.selectColumn)
                                : null)
                        .setHasTimer(resultSet.getLong(Vote.Columns.HAS_TIMER.selectColumn) > 0)
                        .setExceptional(resultSet.getLong(Vote.Columns.IS_EXCEPTIONAL.selectColumn) > 0)
                        .setHasDefault(resultSet.getLong(Vote.Columns.HAS_DEFAULT.selectColumn) > 0)
                        .setWinThreshold(resultSet.getLong(Vote.Columns.WIN_THRESHOLD.selectColumn))
                        .setCreateDate(resultSet.getLong(Vote.Columns.CREATE_DATE.selectColumn) > 0
                                ? Instant.ofEpochSecond(resultSet.getLong(Vote.Columns.CREATE_DATE.selectColumn))
                                : null)
                        .setUpdateDate(resultSet.getLong(Vote.Columns.UPDATE_DATE.selectColumn) > 0
                                ? Instant.ofEpochSecond(resultSet.getLong(Vote.Columns.UPDATE_DATE.selectColumn))
                                : null);
                result.add(currentVote);
            }
            if (resultSet.getString(VotePoint.Columns.ID.selectColumn) == null) {
                continue;
            }
            VotePoint votePoint = new VotePoint()
                    .setId(resultSet.getLong(VotePoint.Columns.ID.selectColumn));
            votePoint.setPointText(resultSet.getString(VotePoint.Columns.POINT_TEXT.selectColumn))
                    .setUnicodeEmoji(CommonUtils.isTrStringNotEmpty(resultSet.getString(VotePoint.Columns.UNICODE_EMOJI.selectColumn))
                            ? resultSet.getString(VotePoint.Columns.UNICODE_EMOJI.selectColumn)
                            : null)
                    .setCustomEmojiId(resultSet.getLong(VotePoint.Columns.CUSTOM_EMOJI_ID.selectColumn))
                    .setCreateDate(resultSet.getLong(VotePoint.Columns.CREATE_DATE.selectColumn) > 0
                            ? Instant.ofEpochSecond(resultSet.getLong(VotePoint.Columns.CREATE_DATE.selectColumn))
                            : null)
                    .setUpdateDate(resultSet.getLong(VotePoint.Columns.UPDATE_DATE.selectColumn) > 0
                            ? Instant.ofEpochSecond(resultSet.getLong(VotePoint.Columns.UPDATE_DATE.selectColumn))
                            : null);
            currentVote.getVotePoints().add(votePoint);
        }
        return Collections.unmodifiableList(result);
    }

    public boolean deleteVote(@NotNull Vote vote) {
        if (log.isDebugEnabled()) {
            log.debug("Query:\n{}\nParam 1: {}", deleteVotePointsQuery, vote.getId());
        }
        try (PreparedStatement statement = connection.prepareStatement(deleteVotePointsQuery)) {
            statement.setLong(1, vote.getId());
            int count = statement.executeUpdate();
            if (log.isDebugEnabled()) {
                log.debug("Deleted {} values", count);
            }
            if (count == 0) {
                log.error("Deleted {} vote points with vote_id {}", count, vote.getId());
            }
        } catch (SQLException err) {
            String errMsg = String.format("Unable to delete vote points with vote_id \"%d\": %s",
                    vote.getId(), err.getMessage());
            log.error(errMsg, err);
            return false;
        }
        if (log.isDebugEnabled()) {
            log.debug("Query:\n{}\nParam 1: {}", deleteVoteQuery, vote.getId());
        }
        try (PreparedStatement statement = connection.prepareStatement(deleteVoteQuery)) {
            statement.setLong(1, vote.getId());
            int count = statement.executeUpdate();
            if (count == 0) {
                log.error("Deleted {} votes with vote_id {}", count, vote.getId());
                return false;
            }
            return true;
        } catch (SQLException err) {
            String errMsg = String.format("Unable to delete vote with vote_id \"%d\": %s",
                    vote.getId(), err.getMessage());
            log.error(errMsg, err);
            return false;
        }
    }
}
