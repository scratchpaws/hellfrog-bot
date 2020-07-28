package hellfrog.settings.db.sqlite;

import hellfrog.common.CommonUtils;
import hellfrog.common.ResourcesLoader;
import hellfrog.settings.db.AddUpdateState;
import hellfrog.settings.db.WtfAssignDAO;
import hellfrog.settings.db.entity.WtfEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.net.URI;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * WTF assigns DAO
 * <p>
 * CREATE TABLE "wtf_assigns" (
 * "id"	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
 * "server_id"	INTEGER NOT NULL,
 * "author_id"	INTEGER NOT NULL,
 * "target_id"	INTEGER NOT NULL,
 * "description"	TEXT,
 * "image_url"	TEXT,
 * "create_date"	INTEGER NOT NULL DEFAULT 0,
 * "update_date"	INTEGER NOT NULL DEFAULT 0
 * );
 * </p>
 */
class WtfAssignDAOImpl
        implements WtfAssignDAO {

    private final Logger log = LogManager.getLogger("WTF assign");
    private final Connection connection;

    private final String getAllQuery = ResourcesLoader.fromTextFile("sql/wtf_assign/get_all_query.sql");
    private final String updateEntryQuery = ResourcesLoader.fromTextFile("sql/wtf_assign/update_entry_query.sql");
    private final String insertEntryQuery = ResourcesLoader.fromTextFile("sql/wtf_assign/insert_entry_query.sql");
    private final String deleteEntryQuery = ResourcesLoader.fromTextFile("sql/wtf_assign/delete_entry_query.sql");

    public WtfAssignDAOImpl(@NotNull Connection connection) {
        this.connection = connection;
    }

    @NotNull @UnmodifiableView
    private List<WtfEntry> getAssigns(long serverId, long userId, boolean onlyFirst) {
        List<WtfEntry> result = new ArrayList<>();
        if (log.isDebugEnabled()) {
            log.debug("Query:\n{}\nParam 1: {}\nParam 2: {}", getAllQuery, serverId, userId);
        }
        try (PreparedStatement statement = connection.prepareStatement(getAllQuery)) {
            statement.setLong(1, serverId);
            statement.setLong(2, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    WtfEntry entry = extractEntry(resultSet);
                    if (log.isDebugEnabled()) {
                        log.debug(entry);
                    }
                    result.add(entry);
                    if (onlyFirst) {
                        break;
                    }
                }
            }
        } catch (SQLException err) {
            String errMsg = String.format("Unable get all wtf for server id %d and user id %d: %s",
                    serverId, userId, err.getMessage());
            log.error(errMsg, err);
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public Optional<WtfEntry> getLatest(long serverId, long userId) {
        List<WtfEntry> wtfEntryList = getAssigns(serverId, userId, true);
        return CommonUtils.getFirstOrEmpty(wtfEntryList);
    }

    @Override
    public List<WtfEntry> getAll(long serverId, long userId) {
        return getAssigns(serverId, userId, false);
    }

    @NotNull
    private WtfEntry extractEntry(@NotNull ResultSet resultSet) throws SQLException {
        long authorId = resultSet.getLong(1);
        String description = resultSet.getString(2);
        String rawURI = resultSet.getString(3);
        URI uri = null;
        try {
            uri = URI.create(rawURI);
        } catch (Exception err) {
            String errMsg = String.format("Unable parse URI \"%s\": %s", rawURI, err.getMessage());
            log.error(errMsg, err);
        }
        long createDate = resultSet.getLong(4);
        long updateDate = resultSet.getLong(5);
        Instant date = Instant.ofEpochMilli(Math.max(createDate, updateDate));
        return WtfEntry.newBuilder()
                .authorId(authorId)
                .description(description)
                .uri(uri)
                .date(date)
                .build();
    }

    @Override
    public AddUpdateState addOrUpdate(long serverId, long userId, @NotNull WtfEntry wtfEntry) {
        String description = CommonUtils.isTrStringNotEmpty(wtfEntry.getDescription())
                ? wtfEntry.getDescription() : null;
        String url = wtfEntry.getUri() != null && CommonUtils.isTrStringNotEmpty(wtfEntry.getUri().toString())
                ? wtfEntry.getUri().toString() : null;
        Instant currentDate = Instant.now();
        long currentDateMillis = currentDate.toEpochMilli();
        long authorId = wtfEntry.getAuthorId();
        if (CommonUtils.isTrStringEmpty(description) && CommonUtils.isTrStringEmpty(url)) {
            return remove(serverId, authorId, userId);
        }
        if (log.isDebugEnabled()) {
            log.debug("Query:\n{}\nParam 1: {}\nParam 2: {}\nParam 3: {}\nParam 4: {}\nParam 5: {}\nParam 6: {}",
                    updateEntryQuery, description, url, currentDateMillis, serverId, userId, authorId);
        }
        try (PreparedStatement statement = connection.prepareStatement(updateEntryQuery)) {
            if (description == null) {
                statement.setNull(1, Types.VARCHAR);
            } else {
                statement.setString(1, description);
            }
            if (url == null) {
                statement.setNull(2, Types.VARCHAR);
            } else {
                statement.setString(2, wtfEntry.getUri().toString());
            }
            statement.setLong(3, currentDateMillis);
            statement.setLong(4, serverId);
            statement.setLong(5, userId);
            statement.setLong(6, authorId);
            int count = statement.executeUpdate();
            if (count == 1) {
                return AddUpdateState.UPDATED;
            } else if (count > 1) {
                throw new SQLException(String.format("Present non-unique values of server id \"%d\" and user id \"%d\" and " +
                        "author id \"%d\"", serverId, userId, authorId));
            }
        } catch (SQLException err) {
            String errMsg = String.format("Unable to update wtf entry for server id \"%d\" and user id \"%d\" with " +
                    "%s: %s", serverId, userId, wtfEntry, err.getMessage());
            log.error(errMsg, err);
            return AddUpdateState.ERROR;
        }
        if (log.isDebugEnabled()) {
            log.debug("Query:\n{}}\nParam 1: {}\nParam 2: {}\nParam 3: {}\nParam 4: {}\n" +
                            "Param 5: {}\nParam 6: {}\nParam 7: {}",
                    insertEntryQuery, serverId, authorId, userId, description, url, currentDateMillis, currentDateMillis);
        }
        try (PreparedStatement statement = connection.prepareStatement(insertEntryQuery)) {
            statement.setLong(1, serverId);
            statement.setLong(2, authorId);
            statement.setLong(3, userId);
            if (description == null) {
                statement.setNull(4, Types.VARCHAR);
            } else {
                statement.setString(4, description);
            }
            if (url == null) {
                statement.setNull(5, Types.VARCHAR);
            } else {
                statement.setString(5, url);
            }
            statement.setLong(6, currentDateMillis);
            statement.setLong(7, currentDateMillis);
            int count = statement.executeUpdate();
            return count == 1 ? AddUpdateState.ADDED : AddUpdateState.NO_CHANGES;
        } catch (SQLException err) {
            String errMsg = String.format("Unable to insert wtf entry for server id \"%d\" and user id \"%d\" with " +
                    "%s: %s", serverId, userId, wtfEntry, err.getMessage());
            log.error(errMsg, err);
            return AddUpdateState.ERROR;
        }
    }

    @Override
    public AddUpdateState remove(long serverId, long authorId, long userId) {
        if (log.isDebugEnabled()) {
            log.debug("Query:\n{}\nParam 1: {}\nParam 2: {}\nParam 3: {}",
                    deleteEntryQuery, serverId, authorId, userId);
        }
        try (PreparedStatement statement = connection.prepareStatement(deleteEntryQuery)) {
            statement.setLong(1, serverId);
            statement.setLong(2, authorId);
            statement.setLong(3, userId);
            int count = statement.executeUpdate();
            return count > 0 ? AddUpdateState.REMOVED : AddUpdateState.NO_CHANGES;
        } catch (SQLException err) {
            String errMsg = String.format("Unable delete wtf entry for server id \"%d\" and user id \"%d\" " +
                    "and author id \"%d\": %s", serverId, userId, authorId, err.getMessage());
            log.error(errMsg, err);
            return AddUpdateState.ERROR;
        }
    }
}
