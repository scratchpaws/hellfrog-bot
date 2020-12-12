package hellfrog.settings.db.h2;

import hellfrog.common.TriFunction;
import hellfrog.settings.db.TextChannelRightsDAO;
import hellfrog.settings.db.entity.ChannelRight;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.time.Instant;

class TextChannelRightsDAOImpl
        extends EntityRightsDAOImpl<ChannelRight>
        implements TextChannelRightsDAO {

    private static final String LOGGER_NAME = "Channel rights";
    private static final TriFunction<Long, String, Long, ChannelRight> CHANNELS_BUILDER =
            (serverId, commandPrefix, channelId) -> {
                ChannelRight channelRight = new ChannelRight();
                channelRight.setServerId(serverId);
                channelRight.setCommandPrefix(commandPrefix);
                channelRight.setChannelId(channelId);
                channelRight.setCreateDate(Timestamp.from(Instant.now()));
                return channelRight;
            };

    TextChannelRightsDAOImpl(@NotNull final AutoSessionFactory sessionFactory) {
        super(sessionFactory, LOGGER_NAME, ChannelRight.class, CHANNELS_BUILDER, "channelId");
    }
}
