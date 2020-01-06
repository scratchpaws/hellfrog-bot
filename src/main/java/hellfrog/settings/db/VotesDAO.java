package hellfrog.settings.db;

import hellfrog.common.FromTextFile;
import hellfrog.common.ResourcesLoader;
import hellfrog.settings.entity.Vote;
import hellfrog.settings.entity.VotePoint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Работа с голосованиями чата.
 * <p>
 * CREATE TABLE "active_votes" (<br>
 *     "vote_id"           INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,<br>
 *     "server_id"         INTEGER NOT NULL,<br>
 *     "text_chat_id"      INTEGER NOT NULL,<br>
 *     "message_id"        INTEGER NOT NULL,<br>
 *     "is_active"         INTEGER NOT NULL,<br>
 *     "finish_date"       INTEGER NOT NULL,<br>
 *     "vote_text"         TEXT NOT NULL,<br>
 *     "has_timer"         INTEGER NOT NULL DEFAULT 0,<br>
 *     "is_exceptional"    INTEGER NOT NULL DEFAULT 0,<br>
 *     "has_default"       INTEGER NOT NULL DEFAULT 0,<br>
 *     "win_threshold"     INTEGER NOT NULL DEFAULT 0,<br>
 *     "create_date"       INTEGER NOT NULL DEFAULT 0,<br>
 *     "update_date"       INTEGER NOT NULL DEFAULT 0<br>
 * );
 * </p>
 * <p>
 * CREATE TABLE "vote_points" (<br>
 *     "id"                INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,<br>
 *     "vote_id"           INTEGER NOT NULL,<br>
 *     "point_text"        INTEGER NOT NULL,<br>
 *     "unicode_emoji"     TEXT,<br>
 *     "custom_emoji_id"   INTEGER,<br>
 *     "create_date"       INTEGER NOT NULL DEFAULT 0,<br>
 *     "update_date"       INTEGER NOT NULL DEFAULT 0,<br>
 *     FOREIGN KEY("vote_id") REFERENCES "active_votes"("vote_id")<br>
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

    @NotNull
    private List<Vote> extractVotes(@NotNull ResultSet resultSet) throws SQLException {
        List<Vote> result = new ArrayList<>();
        Vote currentVote = null;
        List<VotePoint> votePoints = null;
        while (resultSet.next()) {
            int column = 1; // Первый столбец active_votes в результате
            long voteId = resultSet.getLong(column++);
            if (currentVote == null || voteId != currentVote.getId()) {
                currentVote = new Vote();
                votePoints = new ArrayList<>();
                currentVote.setId(voteId)
                        .setServerId(resultSet.getLong(column++))
                        .setTextChatId(resultSet.getLong(column++))
                        .setMessageId(resultSet.getLong(column++))
                        .setActive(resultSet.getLong(column++) > 0)
                        .setFinishTime(Instant.ofEpochMilli(resultSet.getLong(column++)))
                        .setVoteText(resultSet.getString(column++))
                        .setHasTimer(resultSet.getLong(column++) > 0)
                        .setHasDefault(resultSet.getLong(column++) > 0)
                        .setWinThreshold(resultSet.getLong(column++))
                        .setCreateDate(Instant.ofEpochMilli(resultSet.getLong(column++)))
                        .setUpdateDate(Instant.ofEpochMilli(resultSet.getLong(column)))
                        .setVotePoints(votePoints);
                result.add(currentVote);
            }
            column = 14; // Первый столбец vote_points в результате
            if (resultSet.getString(column) == null) {
                continue;
            }
            VotePoint votePoint = new VotePoint()
                    .setId(resultSet.getLong(column++));
            column++; // пропускаем vote_id у vote_point
            votePoint.setPointText(resultSet.getString(column++))
                    .setUnicodeEmoji(resultSet.getString(column++))
                    .setCustomEmojiId(resultSet.getLong(column++))
                    .setCreateDate(Instant.ofEpochMilli(resultSet.getLong(column++)))
                    .setUpdateDate(Instant.ofEpochMilli(resultSet.getLong(column)));
            votePoints.add(votePoint);
        }
        return Collections.unmodifiableList(result);
    }
}
