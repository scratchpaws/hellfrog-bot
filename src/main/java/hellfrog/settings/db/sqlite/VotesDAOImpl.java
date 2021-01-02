package hellfrog.settings.db.sqlite;

import hellfrog.common.CommonUtils;
import hellfrog.common.ResourcesLoader;
import hellfrog.settings.db.VoteCreateException;
import hellfrog.settings.db.VotesDAO;
import hellfrog.settings.db.entity.Vote;
import hellfrog.settings.db.entity.VotePoint;
import hellfrog.settings.db.entity.VoteRoleFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.sql.*;
import java.time.Instant;
import java.util.*;

class VotesDAOImpl
        implements VotesDAO {

    private static final Logger log = LogManager.getLogger("Votes");

    private final Connection connection;

    private final String getAllVotesQuery = ResourcesLoader.fromTextFile("sql/votes/get_all_votes_query.sql");
    private final String getAllExpiredVotesQuery = ResourcesLoader.fromTextFile("sql/votes/get_all_expired_votes_query.sql");
    private final String insertVoteQuery = ResourcesLoader.fromTextFile("sql/votes/insert_vote_query.sql");
    private final String insertVotePointQuery = ResourcesLoader.fromTextFile("sql/votes/insert_vote_point_query.sql");
    private final String getLastInsertVoteIdQuery = ResourcesLoader.fromTextFile("sql/votes/get_last_insert_vote_id_query.sql");
    private final String getVoteByIdQuery = ResourcesLoader.fromTextFile("sql/votes/get_vote_by_id_query.sql");
    private final String activateVoteQuery = ResourcesLoader.fromTextFile("sql/votes/activate_vote_query.sql");
    private final String deleteVotePointsQuery = ResourcesLoader.fromTextFile("sql/votes/delete_vote_points_query.sql");
    private final String deleteVoteQuery = ResourcesLoader.fromTextFile("sql/votes/delete_vote_query.sql");
    private final String insertVoteRolesQuery = ResourcesLoader.fromTextFile("sql/votes/insert_vote_roles_query.sql");
    private final String getVoteRolesByVoteIdQuery = ResourcesLoader.fromTextFile("sql/votes/get_vote_roles_by_vote.sql");
    private final String getVoteRolesByMessageIdQuery = ResourcesLoader.fromTextFile("sql/votes/get_vote_roles_by_message.sql");
    private final String deleteVoteRolesQuery = ResourcesLoader.fromTextFile("sql/votes/delete_vote_roles_query.sql");
    private final String setVoteRolesMessageIdsQuery = ResourcesLoader.fromTextFile("sql/votes/set_vote_roles_message_id_query.sql");

    public VotesDAOImpl(@NotNull Connection connection) {
        this.connection = connection;
    }

    @Override
    public List<Vote> getAll(long serverId) {
        if (log.isDebugEnabled()) {
            log.debug("Query:\n{}\nParam 1: {}", getAllVotesQuery, serverId);
        }

        try (PreparedStatement statement = connection.prepareStatement(getAllVotesQuery)) {
            statement.setLong(1, serverId);
            return getVotesFromStatement(serverId, statement);
        } catch (SQLException err) {
            String errMsg = String.format("Unable extract votes for serverId \"%d\": %s",
                    serverId, err.getMessage());
            log.error(errMsg, err);
            return Collections.emptyList();
        }
    }

    @NotNull
    @UnmodifiableView
    private List<Vote> getVotesFromStatement(long serverId, PreparedStatement statement) throws SQLException {
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
    }

    @Override
    public List<Vote> getAllExpired(long serverId) {
        return getAllExpired(serverId, Instant.now());
    }

    @Override
    public List<Vote> getAllExpired(long serverId, @NotNull Instant dateTime) {
        long currentDateTime = dateTime.getEpochSecond();
        if (log.isDebugEnabled()) {
            log.debug("Query:\n{}\nParam 1: {}\nParam 2: {}", getAllExpiredVotesQuery, serverId,
                    currentDateTime);
        }
        try (PreparedStatement statement = connection.prepareStatement(getAllExpiredVotesQuery)) {
            statement.setLong(1, serverId);
            statement.setLong(2, currentDateTime);
            return getVotesFromStatement(serverId, statement);
        } catch (SQLException err) {
            String errMsg = String.format("Unable extract expired votes for serverId \"%d\": %s",
                    serverId, err.getMessage());
            log.error(errMsg, err);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Long> getAllowedRoles(long messageId) {
        List<Long> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(getVoteRolesByMessageIdQuery)) {
            if (log.isDebugEnabled()) {
                log.debug("Query:\n{}\nParam 1: {}", getVoteRolesByMessageIdQuery, messageId);
            }
            statement.setLong(1, messageId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(resultSet.getLong(1));
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("Allowed roles for vote with message_id {}: {}", messageId,
                        result.toString());
            }
        } catch (SQLException err) {
            String errMsg = String.format("Unable to fetch allowed roles for vote with message id \"%d\": %s",
                    messageId, err.getMessage());
            log.error(errMsg, err);
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public synchronized Vote addVote(@NotNull Vote vote) throws VoteCreateException {
        if (vote.getVotePoints().isEmpty()) {
            log.error("Vote points is empty: {}", vote.toString());
            throw new VoteCreateException("one or more vote points required");
        }
        long serverId = vote.getServerId();
        long textChatId = vote.getTextChatId();
        long messageId = 0L;
        long finishTime = vote.getFinishTime() != null ? vote.getFinishTime().toInstant().getEpochSecond() : 0L;
        String voteText = vote.getVoteText() != null ? vote.getVoteText() : " ";
        long hasTimer = vote.isHasTimer() ? 1L : 0L;
        long exceptionalVote = vote.isExceptional() ? 1L : 0L;
        long hasDefault = vote.isHasDefault() ? 1L : 0L;
        long winThreshold = vote.getWinThreshold();
        Set<VoteRoleFilter> rolesFilter = vote.getRolesFilter() == null ? Collections.emptySet() : vote.getRolesFilter();
        long currentDateTime = Instant.now().getEpochSecond();
        if (log.isDebugEnabled()) {
            log.debug("""
                            Query:
                            {}
                            Param 1: {}
                            Param 2: {}
                            Param 3: {}
                            Param 4: {}
                            Param 5: {}
                            Param 6: {}
                            Param 7: {}
                            Param 8: {}
                            Param 9: {}
                            Param 10: {}
                            Param 11: {}""", insertVoteQuery, serverId,
                    textChatId, messageId, finishTime, voteText, hasTimer, exceptionalVote, hasDefault,
                    winThreshold, currentDateTime, currentDateTime);
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
            String pointText = votePoint.getPointText() != null ? votePoint.getPointText() : " ";
            String emojiText = votePoint.getUnicodeEmoji() != null ? votePoint.getUnicodeEmoji() : " ";
            long customEmojiId = votePoint.getCustomEmojiId();
            currentDateTime = Instant.now().getEpochSecond();
            if (log.isDebugEnabled()) {
                log.debug("""
                                Query:
                                {}
                                Param 1: {}
                                Param 2: {}
                                Param 3: {}
                                Param 4: {}
                                Param 5: {}
                                Param 6: {}""", insertVotePointQuery, voteId, pointText,
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
        for (VoteRoleFilter voteRoleFilter : rolesFilter) {
            currentDateTime = Instant.now().getEpochSecond();
            try (PreparedStatement statement = connection.prepareStatement(insertVoteRolesQuery)) {
                if (log.isDebugEnabled()) {
                    log.debug("""
                                    Query:
                                    {}
                                    Param 1: {}
                                    Param 2: {}
                                    Param 3: {}
                                    Param 4: {}
                                    Param 5: {}""",
                            insertVoteRolesQuery, voteId, messageId, voteRoleFilter.getRoleId(), currentDateTime, currentDateTime);
                }
                statement.setLong(1, voteId);
                statement.setLong(2, messageId);
                statement.setLong(3, voteRoleFilter.getRoleId());
                statement.setLong(4, currentDateTime);
                statement.setLong(5, currentDateTime);
                int count = statement.executeUpdate();
                if (log.isDebugEnabled()) {
                    log.debug("Inserted {} values", count);
                }
            } catch (SQLException err) {
                String errMsg = String.format("Unable insert vote role: %s", err.getMessage());
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

    @Override
    public Vote activateVote(@NotNull Vote vote) throws VoteCreateException {
        if (vote.getMessageId() <= 0L) {
            log.error("messageId required (must be great that zero): {}", vote.toString());
            throw new VoteCreateException("message not sent, voting message identifier missing");
        }
        if (vote.getId() <= 0L) {
            log.error("voteId required (must be great that zero) (vote is not saved?): {}",
                    vote.toString());
            throw new VoteCreateException("this vote was not stored in the database");
        }
        long currentDateTime = Instant.now().getEpochSecond();
        if (log.isDebugEnabled()) {
            log.debug("""
                            Query:
                            {}
                            Param 1: {}
                            Param 2: {}
                            Param 3: {}""", activateVoteQuery,
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
        if (log.isDebugEnabled()) {
            log.debug("""
                            Query:
                            {}
                            Param 1: {}
                            Param 2: {}
                            Param 3: {}""",
                    setVoteRolesMessageIdsQuery, vote.getMessageId(), currentDateTime, vote.getId());
        }
        try (PreparedStatement statement = connection.prepareStatement(setVoteRolesMessageIdsQuery)) {
            statement.setLong(1, vote.getMessageId());
            statement.setLong(2, currentDateTime);
            statement.setLong(3, vote.getId());
            int count = statement.executeUpdate();
            if (log.isDebugEnabled()) {
                log.debug("Updated {} values", count);
            }
        } catch (SQLException err) {
            String errMsg = String.format("Unable update vote roles with vote id \"%d\" and " +
                    "set message id \"%d\": %s", vote.getId(), vote.getMessageId(), err.getMessage());
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
    @Override
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
    @UnmodifiableView
    private List<Vote> extractVotes(@NotNull ResultSet resultSet) throws SQLException {
        List<Vote> result = new ArrayList<>();
        Vote currentVote = null;
        while (resultSet.next()) {
            long voteId = resultSet.getLong(Vote.Columns.ID.selectColumn);
            if (currentVote == null || voteId != currentVote.getId()) {
                currentVote = new Vote();
                currentVote.setId(voteId);
                currentVote.setServerId(resultSet.getLong(Vote.Columns.SERVER_ID.selectColumn));
                currentVote.setTextChatId(resultSet.getLong(Vote.Columns.TEXT_CHAT_ID.selectColumn));
                currentVote.setMessageId(resultSet.getLong(Vote.Columns.MESSAGE_ID.selectColumn));
                currentVote.setFinishTime(resultSet.getLong(Vote.Columns.FINISH_DATE.selectColumn) > 0
                        ? Timestamp.from(Instant.ofEpochSecond(resultSet.getLong(Vote.Columns.FINISH_DATE.selectColumn)))
                        : null);
                currentVote.setVoteText(CommonUtils.isTrStringNotEmpty(resultSet.getString(Vote.Columns.VOTE_TEXT.selectColumn))
                        ? resultSet.getString(Vote.Columns.VOTE_TEXT.selectColumn)
                        : " ");
                currentVote.setHasTimer(resultSet.getLong(Vote.Columns.HAS_TIMER.selectColumn) > 0);
                currentVote.setExceptional(resultSet.getLong(Vote.Columns.IS_EXCEPTIONAL.selectColumn) > 0);
                currentVote.setHasDefault(resultSet.getLong(Vote.Columns.HAS_DEFAULT.selectColumn) > 0);
                currentVote.setWinThreshold(resultSet.getLong(Vote.Columns.WIN_THRESHOLD.selectColumn));
                currentVote.setCreateDate(resultSet.getLong(Vote.Columns.CREATE_DATE.selectColumn) > 0
                        ? Timestamp.from(Instant.ofEpochSecond(resultSet.getLong(Vote.Columns.CREATE_DATE.selectColumn)))
                        : null);
                currentVote.setUpdateDate(resultSet.getLong(Vote.Columns.UPDATE_DATE.selectColumn) > 0
                        ? Timestamp.from(Instant.ofEpochSecond(resultSet.getLong(Vote.Columns.UPDATE_DATE.selectColumn)))
                        : null);
                result.add(currentVote);
            }
            if (resultSet.getString(VotePoint.Columns.ID.selectColumn) == null) {
                continue;
            }
            VotePoint votePoint = new VotePoint();
            votePoint.setVote(currentVote);
            votePoint.setId(resultSet.getLong(VotePoint.Columns.ID.selectColumn));
            votePoint.setPointText(resultSet.getString(VotePoint.Columns.POINT_TEXT.selectColumn));
            votePoint.setUnicodeEmoji(CommonUtils.isTrStringNotEmpty(resultSet.getString(VotePoint.Columns.UNICODE_EMOJI.selectColumn))
                    ? resultSet.getString(VotePoint.Columns.UNICODE_EMOJI.selectColumn)
                    : null);
            votePoint.setCustomEmojiId(resultSet.getLong(VotePoint.Columns.CUSTOM_EMOJI_ID.selectColumn));
            votePoint.setCreateDate(resultSet.getLong(VotePoint.Columns.CREATE_DATE.selectColumn) > 0
                    ? Timestamp.from(Instant.ofEpochSecond(resultSet.getLong(VotePoint.Columns.CREATE_DATE.selectColumn)))
                    : null);
            votePoint.setUpdateDate(resultSet.getLong(VotePoint.Columns.UPDATE_DATE.selectColumn) > 0
                    ? Timestamp.from(Instant.ofEpochSecond(resultSet.getLong(VotePoint.Columns.UPDATE_DATE.selectColumn)))
                    : null);
            if (currentVote.getVotePoints() == null) {
                currentVote.setVotePoints(new HashSet<>());
            }
            currentVote.getVotePoints().add(votePoint);
        }
        for (Vote vote : result) {
            long voteId = vote.getId();
            try (PreparedStatement statement = connection.prepareStatement(getVoteRolesByVoteIdQuery)) {
                if (log.isDebugEnabled()) {
                    log.debug("Query:\n{}\nParam 1: {}", getVoteRolesByVoteIdQuery, voteId);
                }
                statement.setLong(1, voteId);
                Set<VoteRoleFilter> allowedRoles = new HashSet<>();
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        VoteRoleFilter voteRoleFilter = new VoteRoleFilter();
                        voteRoleFilter.setVote(vote);
                        long roleId = rs.getLong(1);
                        voteRoleFilter.setRoleId(roleId);
                        voteRoleFilter.setMessageId(vote.getMessageId());
                        voteRoleFilter.setCreateDate(vote.getCreateDate());
                        allowedRoles.add(voteRoleFilter);
                    }
                }
                vote.setRolesFilter(allowedRoles);
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
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
        try (PreparedStatement statement = connection.prepareStatement(deleteVoteRolesQuery)) {
            statement.setLong(1, vote.getId());
            int count = statement.executeUpdate();
            if (log.isDebugEnabled()) {
                log.debug("Deleted {} vote roles for vote_id {}", count, vote.getId());
            }
        } catch (SQLException err) {
            String errMsg = String.format("Unable to deleve vote roles filter with vote_id \"%d\": %s",
                    vote.getId(), err.getMessage());
            log.error(errMsg, err);
            return false;
        }
        try (PreparedStatement statement = connection.prepareStatement(deleteVoteQuery)) {
            statement.setLong(1, vote.getId());
            int count = statement.executeUpdate();
            if (count == 0) {
                log.error("Deleted {} votes with vote_id {}", count, vote.getId());
                return false;
            }
        } catch (SQLException err) {
            String errMsg = String.format("Unable to delete vote with vote_id \"%d\": %s",
                    vote.getId(), err.getMessage());
            log.error(errMsg, err);
            return false;
        }
        if (log.isDebugEnabled()) {
            log.debug("Query:\n{}\nParam 1: {}", deleteVoteRolesQuery, vote.getId());
        }
        return true;
    }
}
