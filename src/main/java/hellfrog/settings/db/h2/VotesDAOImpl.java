package hellfrog.settings.db.h2;

import hellfrog.common.CommonUtils;
import hellfrog.settings.db.VoteCreateException;
import hellfrog.settings.db.VotesDAO;
import hellfrog.settings.db.entity.Vote;
import hellfrog.settings.db.entity.VotePoint;
import hellfrog.settings.db.entity.VoteRoleFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

class VotesDAOImpl
        implements VotesDAO {

    private final Logger log = LogManager.getLogger("Votes");
    private final AutoSessionFactory sessionFactory;

    VotesDAOImpl(@NotNull AutoSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public List<Vote> getAll(long serverId) {
        try (AutoSession session = sessionFactory.openSession()) {

            List<Vote> result = session.createQuery("from " + Vote.class.getSimpleName() + " v where v.serverId = :serverId",
                    Vote.class)
                    .setParameter("serverId", serverId)
                    .list();

            if (result == null || result.isEmpty()) {
                return Collections.emptyList();
            } else {
                return Collections.unmodifiableList(result);
            }
        } catch (Exception err) {
            String errMsg = String.format("Unable extract votes for serverId \"%d\": %s",
                    serverId, err.getMessage());
            log.error(errMsg, err);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Vote> getAllExpired(long serverId) {
        return getAllExpired(serverId, Instant.now());
    }

    @Override
    public List<Vote> getAllExpired(long serverId, @NotNull Instant dateTime) {
        try (AutoSession session = sessionFactory.openSession()) {

            List<Vote> result = session.createQuery("from " + Vote.class.getSimpleName() + " v " +
                    "where v.serverId = :serverId and v.finishTime >= :finishTime " +
                    "and v.hasTimer = true", Vote.class)
                    .setParameter("serverId", serverId)
                    .setParameter("finishTime", Timestamp.from(dateTime))
                    .list();

            if (result == null || result.isEmpty()) {
                return Collections.emptyList();
            } else {
                return Collections.unmodifiableList(result);
            }
        } catch (Exception err) {
            String errMsg = String.format("Unable extract expired votes for serverId \"%d\": %s",
                    serverId, err.getMessage());
            log.error(errMsg, err);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Long> getAllowedRoles(long messageId) {
        try (AutoSession session = sessionFactory.openSession()) {

            List<Long> result = session.createQuery("select vf.roleId from " + VoteRoleFilter.class.getSimpleName() + " vf " +
                    "where vf.messageId = :messageId", Long.class)
                    .setParameter("messageId", messageId)
                    .list();

            if (result == null || result.isEmpty()) {
                return Collections.emptyList();
            } else {
                return Collections.unmodifiableList(result);
            }
        } catch (Exception err) {
            String errMsg = String.format("Unable to fetch allowed roles for vote with message id \"%d\": %s",
                    messageId, err.getMessage());
            log.error(errMsg, err);
            return Collections.emptyList();
        }
    }

    @Override
    public Vote addVote(@NotNull Vote vote) throws VoteCreateException {

        if (vote.getVotePoints() == null || vote.getVotePoints().isEmpty()) {
            log.error("Vote points is empty: {}", vote.toString());
            throw new VoteCreateException("one or more vote points required");
        }

        if (vote.getServerId() <= 0L) {
            log.error("Server id is less or equal zero");
            throw new VoteCreateException("server required");
        }

        Timestamp now = Timestamp.from(Instant.now());

        if (vote.getFinishTime() != null && !vote.isHasTimer()) {
            vote.setHasTimer(true);
        }

        for (VotePoint votePoint : vote.getVotePoints()) {
            if (CommonUtils.isTrStringEmpty(vote.getVoteText())) {
                throw new VoteCreateException("there should be no vote points with empty descriptions");
            }
            if (CommonUtils.isTrStringEmpty(votePoint.getUnicodeEmoji()) && votePoint.getCustomEmojiId() <= 0L) {
                throw new VoteCreateException("there should be no vote points with empty emoji");
            }
            if (CommonUtils.isTrStringNotEmpty(votePoint.getUnicodeEmoji()) && votePoint.getCustomEmojiId() > 0L) {
                throw new VoteCreateException("application error (there should be no items with unicode and server emoji at the same time)");
            }
            votePoint.setVote(vote);
            votePoint.setCreateDate(now);
            votePoint.setUpdateDate(now);
        }

        if (vote.getRolesFilter() == null) {
            vote.setRolesFilter(Collections.emptySet());
        }

        for (VoteRoleFilter filter : vote.getRolesFilter()) {
            if (filter.getRoleId() <= 0L) {
                throw new VoteCreateException("application error (role id is zero)");
            }
            filter.setMessageId(vote.getMessageId());
            filter.setVote(vote);
            filter.setCreateDate(now);
            filter.setUpdateDate(now);
        }

        if (vote.getId() > 0L) {
            vote.setId(0L);
        }

        vote.setCreateDate(now);
        vote.setUpdateDate(now);

        try (AutoSession session = sessionFactory.openSession()) {
            session.save(vote);
            if (vote.getId() <= 0L) {
                log.error("Vote with empty/zero vote id is created: {}", vote);
                throw new VoteCreateException("database error");
            }
            for (VotePoint votePoint : vote.getVotePoints()) {
                session.save(votePoint);
                if (votePoint.getId() <= 0L) {
                    log.error("VotePoint with empty/zero vote id is created: {}", votePoint);
                    throw new VoteCreateException("database error");
                }
            }
            for (VoteRoleFilter voteRoleFilter : vote.getRolesFilter()) {
                session.save(voteRoleFilter);
                if (voteRoleFilter.getId() <= 0L) {
                    log.error("VoteRoleFilter with empty/zero vote id is created: {}", voteRoleFilter);
                    throw new VoteCreateException("database error");
                }
            }
        } catch (Exception err) {
            String errMsg = String.format("Unable to insert new vote \"%s\" value: %s",
                    vote, err.getMessage());
            log.error(errMsg, err);
            throw new VoteCreateException("database error", err);
        }

        return vote;
    }

    @Override
    public Vote activateVote(@NotNull Vote vote) throws VoteCreateException {

        long messageId = vote.getMessageId();
        long voteId = vote.getId();

        if (messageId <= 0L) {
            log.error("messageId required (must be great that zero): {}", vote.toString());
            throw new VoteCreateException("message not sent, voting message identifier missing");
        }

        if (voteId <= 0L) {
            log.error("voteId required (must be great that zero) (vote is not saved?): {}",
                    vote.toString());
            throw new VoteCreateException("this vote was not stored in the database");
        }

        Timestamp now = Timestamp.from(Instant.now());

        try (AutoSession session = sessionFactory.openSession()) {
            Vote persisted = session.get(Vote.class, voteId);
            if (persisted == null) {
                log.error("This vote is not present into database: {}", vote);
                throw new VoteCreateException("this vote was not stored in the database");
            }
            persisted.setMessageId(messageId);
            persisted.setUpdateDate(now);
            if (persisted.getRolesFilter() != null) {
                for (VoteRoleFilter filter : persisted.getRolesFilter()) {
                    filter.setMessageId(messageId);
                    filter.setUpdateDate(now);
                    session.save(filter);
                }
            }
            session.save(persisted);
            return persisted;
        } catch (Exception err) {
            String errMsg = String.format("Unable to update vote with vote_id \"%d\": %s",
                    vote.getId(), err.getMessage());
            log.error(errMsg, err);
            throw new VoteCreateException("database error");
        }
    }

    @Override
    public Vote getVoteById(long voteId) throws SQLException {
        try (AutoSession session = sessionFactory.openSession()) {
            return session.get(Vote.class, voteId);
        } catch (Exception err) {
            throw new SQLException(err.getMessage(), err);
        }
    }

    @Override
    public boolean deleteVote(@NotNull Vote vote) {
        long voteId = vote.getId();
        try (AutoSession session = sessionFactory.openSession()) {
            Vote persisted = session.get(Vote.class, voteId);
            if (persisted == null) {
                return false;
            }
            session.removeAll(persisted.getRolesFilter());
            session.removeAll(persisted.getVotePoints());
            session.remove(persisted);
            return true;
        } catch (Exception err) {
            String errMsg = String.format("Unable to delete vote points with vote_id \"%d\": %s",
                    vote.getId(), err.getMessage());
            log.error(errMsg, err);
            return false;
        }
    }
}
