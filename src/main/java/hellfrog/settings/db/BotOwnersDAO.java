package hellfrog.settings.db;

import com.j256.ormlite.dao.Dao;
import hellfrog.settings.entity.BotOwner;

import java.util.List;

public interface BotOwnersDAO extends Dao<BotOwner, Long> {

    List<Long> getAll();

    boolean isPresent(long userId);

    boolean addToOwners(long userId);

    boolean deleteFromOwners(long userId);
}
