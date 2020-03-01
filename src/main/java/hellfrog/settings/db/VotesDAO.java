package hellfrog.settings.db;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.j256.ormlite.support.ConnectionSource;
import hellfrog.settings.entity.ActiveVote;
import hellfrog.settings.entity.ActiveVotePoint;
import hellfrog.settings.entity.VoteRole;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DAO for working with voting in the database.
 */
public class VotesDAO {

    private static final Logger log = LogManager.getLogger("Votes");

    private final ActiveVotesDAO activeVotesDAO;
    private final ActiveVotePointsDAO activeVotePointsDAO;
    private final VoteRolesDAO voteRolesDAO;

    public VotesDAO(@NotNull ConnectionSource connectionSource) throws SQLException {
        this.activeVotesDAO = new ActiveVotesDAOImpl(connectionSource);
        this.activeVotePointsDAO = new ActiveVotePointsDAOImpl(connectionSource);
        this.voteRolesDAO = new VoteRolesDAOImpl(connectionSource);
    }

    public List<ActiveVote> getAll(long serverId) {
        try {
            List<ActiveVote> allActiveVotes = activeVotesDAO.queryBuilder().where()
                    .eq(ActiveVote.SERVER_ID_FIELD_NAME, serverId)
                    .and().gt(ActiveVote.MESSAGE_ID_FIELD_NAME, 0L)
                    .query();
            if (allActiveVotes == null) {
                allActiveVotes = Collections.emptyList();
            }
            fillVotesChildItems(allActiveVotes);
            if (log.isDebugEnabled()) {
                log.debug("Extracted votes for server id {}:", serverId);
                allActiveVotes.forEach(log::debug);
            }
            return Collections.unmodifiableList(allActiveVotes);
        } catch (SQLException err) {
            String errMsg = String.format("Unable extract votes for serverId \"%d\": %s",
                    serverId, err.getMessage());
            log.error(errMsg, err);

        }
        return Collections.emptyList();
    }

    private void fillVoteChildItems(@NotNull ActiveVote activeVote) throws SQLException {
        List<ActiveVotePoint> votePoints = activeVotePointsDAO.queryBuilder().where()
                .eq(ActiveVotePoint.ACTIVE_VOTE_FIELD_NAME, activeVote.getId())
                .query();
        if (votePoints == null) {
            votePoints = Collections.emptyList();
        }
        activeVote.setVotePoints(Collections.unmodifiableList(votePoints));
        List<VoteRole> voteRoles = voteRolesDAO.queryBuilder().where()
                .eq(VoteRole.VOTE_ID_FIELD_NAME, activeVote.getId())
                .query();
        if (voteRoles == null) {
            voteRoles = Collections.emptyList();
        }
        activeVote.setRolesFilter(Collections.unmodifiableList(voteRoles));
    }

    private void fillVotesChildItems(@NotNull List<ActiveVote> activeVotes) throws SQLException {
        for (ActiveVote activeVote : activeVotes) {
            fillVoteChildItems(activeVote);
        }
    }

    public List<ActiveVote> getAllExpired(long serverId) {
        return getAllExpired(serverId, Instant.now());
    }

    List<ActiveVote> getAllExpired(long serverId, @NotNull Instant dateTime) {
        try {
            List<ActiveVote> allExpired = activeVotesDAO.queryBuilder().where()
                    .eq(ActiveVote.SERVER_ID_FIELD_NAME, serverId)
                    .and().gt(ActiveVote.MESSAGE_ID_FIELD_NAME, 0L)
                    .and().eq(ActiveVote.HAS_TIMER_FIELD_NAME, true)
                    .and().ge(ActiveVote.FINISH_TIME_FIELD_NAME, dateTime)
                    .query();
            if (allExpired == null) {
                allExpired = Collections.emptyList();
            }
            fillVotesChildItems(allExpired);
            if (log.isDebugEnabled()) {
                log.debug("Extracted votes for server id {}:", serverId);
                allExpired.forEach(log::debug);
            }
            return Collections.unmodifiableList(allExpired);
        } catch (SQLException err) {
            String errMsg = String.format("Unable extract expired votes for serverId \"%d\": %s",
                    serverId, err.getMessage());
            log.error(errMsg, err);
        }
        return Collections.emptyList();
    }

    public List<Long> getAllowedRoles(long messageId) {
        List<Long> result = new ArrayList<>();
        try {
            List<VoteRole> allowedRoles = voteRolesDAO.queryBuilder().where()
                    .eq(VoteRole.MESSAGE_ID_FIELD_NAME, messageId)
                    .query();
            if (allowedRoles == null) {
                allowedRoles = Collections.emptyList();
            }
            for (VoteRole voteRole : allowedRoles) {
                result.add(voteRole.getRoleId());
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
     * the appropriate method {@link #activateVote(ActiveVote)}.</p>
     *
     * @param vote added vote. Fields such as identifier,
     *             creation date, and change are ignored during insertion.
     * @return created object of voting. This object has been retrieved from the database.
     * @throws VoteCreateException one or another error in the process of adding
     *                             voting to the database. Keep in mind that this method does not check
     *                             the correctness of filling in the voting fields. It only requires
     *                             that the vote have at least one point.
     */
    public synchronized ActiveVote addVote(@NotNull ActiveVote vote) throws VoteCreateException {
        if (vote.getVotePoints() == null || vote.getVotePoints().isEmpty()) {
            log.error("Vote points is empty: {}", vote.toString());
            throw new VoteCreateException("one or more vote points required");
        }
        Instant now = Instant.now();
        try {
            vote.setCreateDate(now);
            vote.setUpdateDate(now);
            if (vote.getRolesFilter() == null) {
                vote.setRolesFilter(Collections.emptyList());
            }
            Dao.CreateOrUpdateStatus status = activeVotesDAO.createOrUpdate(vote);
            if (log.isDebugEnabled()) {
                log.debug("Created - {}, updated - {}, lines changed - {}",
                        status.isCreated(), status.isUpdated(), status.getNumLinesChanged());
            }
        } catch (SQLException err) {
            String errMsg = String.format("Unable to insert new %s: %s",
                    vote, err.getMessage());
            log.error(errMsg, err);
            throw new VoteCreateException("database error", err);
        }
        for (ActiveVotePoint votePoint : vote.getVotePoints()) {
            votePoint.setActiveVote(vote);
            votePoint.setCreateDate(now);
            votePoint.setUpdateDate(now);
            try {
                Dao.CreateOrUpdateStatus status = activeVotePointsDAO.createOrUpdate(votePoint);
                if (log.isDebugEnabled()) {
                    log.debug("Created - {}, updated - {}, lines changed - {}",
                            status.isCreated(), status.isUpdated(), status.getNumLinesChanged());
                }
            } catch (SQLException err) {
                String errMsg = String.format("Unable to insert new %s: %s", votePoint, err.getMessage());
                log.error(errMsg, err);
                throw new VoteCreateException("database error", err);
            }
        }
        for (VoteRole voteRole : vote.getRolesFilter()) {
            voteRole.setActiveVote(vote);
            voteRole.setCreateDate(now);
            try {
                Dao.CreateOrUpdateStatus status = voteRolesDAO.createOrUpdate(voteRole);
                if (log.isDebugEnabled()) {
                    log.debug("Created - {}, updated - {}, lines changed - {}",
                            status.isCreated(), status.isUpdated(), status.getNumLinesChanged());
                }
            } catch (SQLException err) {
                String errMsg = String.format("Unable to insert %s: %s", voteRole, err.getMessage());
                log.error(errMsg, err);
                throw new VoteCreateException("database error", err);
            }
        }
        return vote;
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
    public ActiveVote activateVote(@NotNull ActiveVote vote) throws VoteCreateException {
        if (vote.getMessageId() <= 0L) {
            log.error("messageId required (must be great that zero): {}", vote.toString());
            throw new VoteCreateException("message not sent, voting message identifier missing");
        }
        if (vote.getId() <= 0) {
            log.error("voteId required (must be great that zero) (vote is not saved?): {}",
                    vote.toString());
            throw new VoteCreateException("this vote was not stored in the database");
        }
        Instant now = Instant.now();
        vote.setUpdateDate(now);
        try {
            int count = activeVotesDAO.update(vote);
            if (log.isDebugEnabled()) {
                log.debug("Updated {} votes", count);
            }
            if (count < 1) {
                log.error("Unable update vote {} - {} values updated (missing vote?)",
                        vote, count);
                throw new VoteCreateException("this vote was now stored in the database");
            } else if (count > 1) {
                log.error("Updated more that one - {}: {}", count, vote);
                throw new VoteCreateException("database error");
            }
        } catch (SQLException err) {
            String errMsg = String.format("Unable to update vote %s: %s",
                    vote, err.getMessage());
            log.error(errMsg, err);
            throw new VoteCreateException("database error");
        }
        try {
            UpdateBuilder<VoteRole, Long> updateBuilder = voteRolesDAO.updateBuilder();
            updateBuilder.updateColumnValue(VoteRole.MESSAGE_ID_FIELD_NAME, vote.getMessageId());
            updateBuilder.where().eq(VoteRole.VOTE_ID_FIELD_NAME, vote.getId());
            int count = updateBuilder.update();
            if (log.isDebugEnabled()) {
                log.debug("Updated {} vote role filters", count);
            }
        } catch (SQLException err) {
            String errMsg = String.format("Unable to update role filters for vote %s: %s",
                    vote, err.getMessage());
            log.error(errMsg, err);
            throw new VoteCreateException("database error");
        }
        return vote;
    }

    @Nullable
    public ActiveVote getVoteById(long voteId) throws SQLException {
        ActiveVote vote = activeVotesDAO.queryForId(voteId);
        if (vote == null) {
            return null;
        }
        fillVoteChildItems(vote);
        if (log.isDebugEnabled()) {
            log.debug("Extracted vote for vote id {}: {}", voteId, vote);
        }
        return vote;
    }

    public boolean deleteVote(@NotNull ActiveVote vote) {
        try {
            DeleteBuilder<VoteRole, Long> deleteBuilder = voteRolesDAO.deleteBuilder();
            deleteBuilder.where()
                    .eq(VoteRole.VOTE_ID_FIELD_NAME, vote.getId());
            int count = deleteBuilder.delete();
            if (log.isDebugEnabled()) {
                log.debug("Deleted {} role filters for vote {}", count, vote);
            }
        } catch (SQLException err) {
            String errMsg = String.format("Unable to delete role filters for vote %s: %s", vote, err.getMessage());
            log.error(errMsg, err);
            return false;
        }
        try {
            DeleteBuilder<ActiveVotePoint, Long> deleteBuilder = activeVotePointsDAO.deleteBuilder();
            deleteBuilder.where()
                    .eq(ActiveVotePoint.ACTIVE_VOTE_FIELD_NAME, vote.getId());
            int count = deleteBuilder.delete();
            if (log.isDebugEnabled()) {
                log.debug("Deleted {} vote points for vote {}", count, vote);
            }
        } catch (SQLException err) {
            String errMsg = String.format("Unable to delete vote points for vote %s: %s", vote, err.getMessage());
            log.error(errMsg, err);
            return false;
        }
        try {
            int count = activeVotesDAO.delete(vote);
            if (log.isDebugEnabled()) {
                log.debug("Deleted {} vote points for vote {}", count, vote);
            }
            return count == 1;
        } catch (SQLException err) {
            String errMsg = String.format("Unable to delete vote %s: %s", vote, err.getMessage());
            log.error(errMsg);
            return false;
        }
    }
}
