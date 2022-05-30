package hellfrog.reacts;

import hellfrog.common.CoubGrabber;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoCoub extends MsgCreateReaction {

    private static final String PREFIX = "autocoub";
    private static final String DESCRIPTION = "automatically download coub for messages from links";
    private static final Pattern SEARCH_PATTERN = Pattern.compile("https://coub\\.com/(embed|view|v)/\\w+",
            Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public AutoCoub() {
        super.setCommandPrefix(PREFIX);
        super.setCommandDescription(DESCRIPTION);
        super.enableAccessControl();
        super.enableStrictByChannel();
    }

    @Override
    public boolean canReact(MessageCreateEvent event) {
        Optional<User> mayBeUser = event.getMessageAuthor().asUser();
        if (mayBeUser.isPresent() && !mayBeUser.get().isBot()) {
            String messageString = event.getMessageContent();
            Matcher matcher = SEARCH_PATTERN.matcher(messageString);
            return matcher.find();
        } else {
            return false;
        }
    }

    @Override
    void parallelExecuteReact(final String strMessage,
                              final @Nullable Server server,
                              final @Nullable User user,
                              final TextChannel textChannel,
                              final Instant messageCreateDate,
                              final Message sourceMessage) {

        final URI coubUrl = CoubGrabber.findFirst(strMessage);
        if (coubUrl != null) {
            try {
                CoubGrabber.grabCoub(coubUrl, textChannel, user, server);
            } catch (RuntimeException ignore) {
            }
        }
    }
}
