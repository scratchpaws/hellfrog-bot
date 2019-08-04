package bruva.settings.DAO;

import bruva.settings.AutoSession;
import bruva.settings.Entity.BotOwner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BotOwnersDAO {

    private static final Logger log = LogManager.getLogger(BotOwnersDAO.class.getSimpleName());

    public List<Long> getAll() throws Exception {
        try (AutoSession session = AutoSession.openSession()) {
            List<BotOwner> allOwners = session.createQuery("from BotOwner", BotOwner.class)
                    .list();
            session.success();
            if (allOwners == null)
                return Collections.emptyList();
            return allOwners.stream()
                    .map(BotOwner::getUserId)
                    .collect(Collectors.toList());
        }
    }

    public boolean isBotOwner(long userId) throws Exception {
        try (AutoSession session = AutoSession.openSession()) {
            List<BotOwner> found = session.createQuery("from BotOwner bo where bo.userId = :userId", BotOwner.class)
                    .setParameter("userId", userId)
                    .list();
            session.success();
            return found != null && !found.isEmpty();
        }
    }

    public boolean addBotOwner(long userId) throws Exception {
        try (AutoSession session = AutoSession.openSession()) {
            List<BotOwner> found = session.createQuery("from BotOwner bo where bo.userId = :userId", BotOwner.class)
                    .setParameter("userId", userId)
                    .list();
            if (found != null && !found.isEmpty()) {
                session.success();
                return false;
            }
            BotOwner botOwner = new BotOwner();
            botOwner.setUserId(userId);
            session.save(botOwner);
            return true;
        }
    }

    public boolean deleteBotOwner(long userId) throws Exception {
        try (AutoSession session = AutoSession.openSession()) {
            int deletedRows = session.createQuery("delete from BotOwner bo where bo.userId = :userId")
                    .setParameter("userId", userId)
                    .executeUpdate();
            session.success();
            return deletedRows > 0;
        }
    }
}
