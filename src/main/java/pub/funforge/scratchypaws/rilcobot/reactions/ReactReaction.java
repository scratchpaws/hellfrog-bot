package pub.funforge.scratchypaws.rilcobot.reactions;

import org.javacord.api.entity.emoji.CustomEmoji;
import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.jetbrains.annotations.NotNull;
import pub.funforge.scratchypaws.rilcobot.settings.SettingsController;
import pub.funforge.scratchypaws.rilcobot.settings.old.ServerStatistic;
import pub.funforge.scratchypaws.rilcobot.settings.old.SmileStatistic;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ReactReaction {

    public void parseReaction(@NotNull SingleReactionEvent event, final boolean isAdd) {
        Optional<Server> mayBeServer = event.getServer();
        if (mayBeServer.isEmpty()) return;
        Emoji received = event.getEmoji();
        if (!received.isCustomEmoji()) return;
        Optional<CustomEmoji> mayBeCustomEmoji = received.asCustomEmoji();
        if (mayBeCustomEmoji.isEmpty()) return;

        User user = event.getUser();
        if (user.isYourself() || user.isBot())
            return;

        final Server server = mayBeServer.get();
        final CustomEmoji customEmoji = mayBeCustomEmoji.get();

        CompletableFuture.runAsync(() -> parallelExecute(server, customEmoji, isAdd));
    }

    private void parallelExecute(final Server server,
                                 final CustomEmoji customEmoji,
                                 final boolean isAdd) {

        SettingsController settingsController = SettingsController.getInstance();
        ServerStatistic serverStatistic = settingsController.getServerStatistic(server.getId());
        if (!serverStatistic.isCollectNonDefaultSmileStats()) return;

        long emojiId = customEmoji.getId();
        server.getCustomEmojiById(emojiId)
                .ifPresent(kce -> {
                    SmileStatistic stat = serverStatistic.getSmileStatistic(emojiId);
                    if (isAdd) {
                        stat.increment();
                    } else {
                        stat.decrement();
                    }
                });
    }
}
