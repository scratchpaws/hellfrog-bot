package hellfrog.settings.db.h2;

import hellfrog.common.TriFunction;
import hellfrog.settings.db.ChannelCategoryRightsDAO;
import hellfrog.settings.db.entity.CategoryRight;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.time.Instant;

public class ChannelCategoryRightsDAOImpl
        extends EntityRightsDAOImpl<CategoryRight>
        implements ChannelCategoryRightsDAO {

    private static final String LOGGER_NAME = "Category rights";
    private static final TriFunction<Long, String, Long, CategoryRight> CATEGORY_BUILDER =
            (serverId, commandPrefix, categoryId) -> {
                CategoryRight categoryRight = new CategoryRight();
                categoryRight.setServerId(serverId);
                categoryRight.setCommandPrefix(commandPrefix);
                categoryRight.setCategoryId(categoryId);
                categoryRight.setCreateDate(Timestamp.from(Instant.now()));
                return categoryRight;
            };

    ChannelCategoryRightsDAOImpl(@NotNull final AutoSessionFactory sessionFactory) {
        super(sessionFactory, LOGGER_NAME, CategoryRight.class, CATEGORY_BUILDER, "categoryId");
    }
}
