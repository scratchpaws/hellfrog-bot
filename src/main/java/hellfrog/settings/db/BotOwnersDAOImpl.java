package hellfrog.settings.db;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.support.ConnectionSource;
import hellfrog.common.CommonUtils;
import hellfrog.settings.entity.BotOwner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bot owners DAO (exclude API key owner)
 */
public class BotOwnersDAOImpl extends BaseDaoImpl<BotOwner, Long>
        implements BotOwnersDAO {

    private final Logger log = LogManager.getLogger("Bot owners");

    BotOwnersDAOImpl(@NotNull ConnectionSource connectionSource) throws SQLException {
        super(connectionSource, BotOwner.class);
    }

    @Override
    public List<Long> getAll() {
        List<Long> result = new ArrayList<>();
        try (CloseableIterator<BotOwner> iterator = super.iterator()) {
            while (iterator.hasNext()) {
                result.add(iterator.next().getUserId());
            }
        } catch (IOException err) {
            String errMsg = String.format("Unable to fetch all bot owners: %s", err.getMessage());
            log.error(errMsg, err);
        }
        if (log.isDebugEnabled()) {
            String allIdsAsString = result.stream()
                    .map(String::valueOf)
                    .reduce(CommonUtils::reduceConcat)
                    .orElse("[empty]");
            log.debug("Fetched bot owners ids: {}", allIdsAsString);
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public boolean isPresent(long userId) {
        boolean present = false;
        try {
            present = super.queryForId(userId) != null;
        } catch (SQLException err) {
            String errMsg = String.format("Unable to check what user with id %d is bot owner: %s",
                    userId, err.getMessage());
            log.error(errMsg, err);
        }
        if (log.isDebugEnabled()) {
            log.debug("User with id {} is present into bot owner table", userId);
        }
        return present;
    }

    @Override
    public boolean addToOwners(long userId) {
        if (!isPresent(userId)) {
            try {
                BotOwner botOwner = new BotOwner();
                botOwner.setUserId(userId);
                botOwner.setCreateDate(Instant.now());
                super.createOrUpdate(botOwner);
            } catch (SQLException err) {
                String errMsg = String.format("Unable to add %d to global bot owners: %s", userId, err.getMessage());
                log.error(errMsg, err);
            }
        }
        return false;
    }

    @Override
    public boolean deleteFromOwners(long userId) {
        if (isPresent(userId)) {
            try {
                BotOwner botOwner = super.queryForId(userId);
                super.delete(botOwner);
            } catch (SQLException err) {
                String errMsg = String.format("Unable to delete %d from global bot owners: %s", userId, err.getMessage());
                log.error(errMsg, err);
            }
        }
        return false;
    }
}
