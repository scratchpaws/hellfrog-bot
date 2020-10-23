package hellfrog.reacts;

import hellfrog.settings.ServerStatistic;
import hellfrog.settings.SettingsController;
import hellfrog.settings.SmileStatistic;
import org.javacord.api.entity.emoji.CustomEmoji;
import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ReactReaction {

    public static void collectStat(final Server server,
                                   final CustomEmoji customEmoji,
                                   final boolean isAdd,
                                   final Instant reactDate) {

        SettingsController settingsController = SettingsController.getInstance();
        ServerStatistic serverStatistic = settingsController.getServerStatistic(server.getId());
        if (!serverStatistic.isCollectNonDefaultSmileStats()) return;

        long emojiId = customEmoji.getId();
        server.getCustomEmojiById(emojiId)
                .ifPresent(kce -> {
                    SmileStatistic stat = serverStatistic.getSmileStatistic(emojiId);
                    if (isAdd) {
                        if (reactDate != null) {
                            stat.incrementWithLastDate(reactDate);
                        } else {
                            stat.increment();
                        }
                    } else {
                        stat.decrement();
                    }
                });
    }

    public void parseReaction(@NotNull SingleReactionEvent event, final boolean isAdd) {
        Optional<Server> mayBeServer = event.getServer();
        if (mayBeServer.isEmpty()) return;
        Emoji received = event.getEmoji();
        if (!received.isCustomEmoji()) return;
        Optional<CustomEmoji> mayBeCustomEmoji = received.asCustomEmoji();
        if (mayBeCustomEmoji.isEmpty()) return;

        Optional<User> mayBeUser = event.getUser();
        if (mayBeUser.isEmpty())
            return;
        User user = mayBeUser.get();
        if (user.isYourself() || user.isBot())
            return;

        final Server server = mayBeServer.get();
        final CustomEmoji customEmoji = mayBeCustomEmoji.get();

        CompletableFuture.runAsync(() -> collectStat(server, customEmoji, isAdd, Instant.now()));
    }
}
