package hellfrog.settings.db.h2;

import hellfrog.common.TriFunction;
import hellfrog.settings.db.RoleRightsDAO;
import hellfrog.settings.db.entity.RoleRight;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.time.Instant;

class RoleRightsDAOImpl
        extends EntityRightsDAOImpl<RoleRight>
        implements RoleRightsDAO {

    private static final String LOGGER_NAME = "Role rights";
    private static final TriFunction<Long, String, Long, RoleRight> ROLES_BUILDER =
            (serverId, commandPrefix, roleId) -> {
                RoleRight roleRight = new RoleRight();
                roleRight.setServerId(serverId);
                roleRight.setCommandPrefix(commandPrefix);
                roleRight.setRoleId(roleId);
                roleRight.setCreateDate(Timestamp.from(Instant.now()));
                return roleRight;
            };

    RoleRightsDAOImpl(@NotNull final AutoSessionFactory sessionFactory) {
        super(sessionFactory, LOGGER_NAME, RoleRight.class, ROLES_BUILDER, "roleId");
    }
}
