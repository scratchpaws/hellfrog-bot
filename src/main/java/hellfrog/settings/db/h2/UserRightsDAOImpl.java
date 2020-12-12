package hellfrog.settings.db.h2;

import hellfrog.common.TriFunction;
import hellfrog.settings.db.UserRightsDAO;
import hellfrog.settings.db.entity.UserRight;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.time.Instant;

class UserRightsDAOImpl
        extends EntityRightsDAOImpl<UserRight>
        implements UserRightsDAO {

    private static final String LOGGER_NAME = "User rights";
    private static final TriFunction<Long, String, Long, UserRight> USERS_BUILDER =
            (serverId, commandPrefix, userId) -> {
                UserRight userRight = new UserRight();
                userRight.setServerId(serverId);
                userRight.setCommandPrefix(commandPrefix);
                userRight.setUserId(userId);
                userRight.setCreateDate(Timestamp.from(Instant.now()));
                return userRight;
            };

    UserRightsDAOImpl(@NotNull final AutoSessionFactory sessionFactory) {
        super(sessionFactory, LOGGER_NAME, UserRight.class, USERS_BUILDER, "userId");
    }
}
