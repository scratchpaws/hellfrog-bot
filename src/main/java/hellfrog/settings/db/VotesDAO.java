package hellfrog.settings.db;

import hellfrog.settings.db.entity.Vote;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

public interface VotesDAO {

    List<Vote> getAll(long serverId);

    List<Vote> getAllExpired(long serverId);

    List<Vote> getAllExpired(long serverId, @NotNull Instant dateTime);

    List<Long> getAllowedRoles(long messageId);

    Vote addVote(@NotNull Vote vote) throws VoteCreateException;

    Vote activateVote(@NotNull Vote vote) throws VoteCreateException;

    Vote getVoteById(long voteId) throws SQLException;

    boolean deleteVote(@NotNull Vote vote);

}
