package hellfrog.settings.db;

import java.util.List;

public interface BotOwnersDAO {

    List<Long> getAll();

    boolean isPresent(long userId);

    boolean addToOwners(long userId);

    boolean deleteFromOwners(long userId);
}
