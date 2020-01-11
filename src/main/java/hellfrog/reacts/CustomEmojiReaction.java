package hellfrog.reacts;

import hellfrog.common.CommonUtils;
import hellfrog.settings.ServerStatistic;
import hellfrog.settings.SettingsController;
import hellfrog.settings.SmileStatistic;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomEmojiReaction
        extends MsgCreateReaction {

    private static final Pattern CUSTOM_EMOJI_SEARCH = Pattern.compile("<a?:.+?:\\d+>", Pattern.MULTILINE);

    public CustomEmojiReaction() {
        super.disableRateLimit();
    }

    public static boolean messageContainCustomEmoji(@NotNull Message message) {
        String messageString = message.getContent();
        return messageContainCustomEmoji(messageString);
    }

    private static boolean messageContainCustomEmoji(@NotNull String messageString) {
        return CUSTOM_EMOJI_SEARCH.matcher(messageString).find();
    }

    public static void collectStat(@NotNull String strMessage, @Nullable User user, @NotNull Server server,
                                   @Nullable Instant messageCreationDate) {
        long serverId = server.getId();
        if (user != null && (user.isYourself() || user.isBot())) return;

        ServerStatistic serverStatistic = SettingsController.getInstance()
                .getServerStatistic(serverId);
        if (serverStatistic.isCollectNonDefaultSmileStats()) {
            Matcher smileMatcher = CUSTOM_EMOJI_SEARCH.matcher(strMessage);
            while (smileMatcher.find()) {
                String matched = smileMatcher.group();
                String[] sub = matched.split(":");
                if (sub.length == 3) {
                    long customSmileId = CommonUtils.onlyNumbersToLong(sub[2]);
                    server.getCustomEmojiById(customSmileId).ifPresent(e -> {
                        SmileStatistic stat = serverStatistic.getSmileStatistic(customSmileId);
                        if (messageCreationDate != null) {
                            stat.incrementWithLastDate(messageCreationDate);
                        } else {
                            stat.increment();
                        }
                    });
                }
            }
        }
    }

    @Override
    public boolean canReact(MessageCreateEvent event) {
        String messageString = event.getMessageContent();
        return messageContainCustomEmoji(messageString);
    }

    @Override
    void parallelExecuteReact(String strMessage, @Nullable Server server, @Nullable User user, TextChannel textChannel,
                              Instant messageCreationDate, Message sourceMessage) {
        if (server == null) return;

        collectStat(strMessage, user, server, messageCreationDate);
    }
}
