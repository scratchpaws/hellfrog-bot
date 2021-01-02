package hellfrog.settings.db;

import hellfrog.settings.db.entity.Vote;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.time.Instant;
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
 * <p>
 * CREATE TABLE "vote_roles" (
 * "id"                INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
 * "vote_id"           INTEGER NOT NULL,
 * "message_id"        INTEGER NOT NULL,
 * "role_id"           INTEGER NOT NULL,
 * "create_date"       INTEGER NOT NULL DEFAULT 0,
 * "update_date"       INTEGER NOT NULL DEFAULT 0,
 * FOREIGN KEY("vote_id") REFERENCES "active_votes"("vote_id")
 * );
 * </p>
 */
public interface VotesDAO {

    List<Vote> getAll(long serverId);

    List<Vote> getAllExpired(long serverId);

    List<Vote> getAllExpired(long serverId, @NotNull Instant dateTime);

    List<Long> getAllowedRoles(long messageId);

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
    Vote addVote(@NotNull Vote vote) throws VoteCreateException;

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
    Vote activateVote(@NotNull Vote vote) throws VoteCreateException;

    Vote getVoteById(long voteId) throws SQLException;

    boolean deleteVote(@NotNull Vote vote);

}
