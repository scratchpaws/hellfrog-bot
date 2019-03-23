package xyz.funforge.scratchypaws.hellfrog.reactions;

import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.MessageDeleteEvent;
import org.javacord.api.event.message.MessageEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.listener.message.MessageDeleteListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.funforge.scratchypaws.hellfrog.settings.SettingsController;
import xyz.funforge.scratchypaws.hellfrog.settings.old.MessageStatistic;
import xyz.funforge.scratchypaws.hellfrog.settings.old.ServerStatistic;

import java.time.Instant;
import java.util.Optional;

public class MessageStats
        implements MessageCreateListener, MessageDeleteListener {

    public static void collectStat(@NotNull Server s, @NotNull ServerTextChannel ch,
                                   @Nullable User author, @Nullable Instant messageDate,
                                   boolean isCreate) {
        ServerStatistic stat = SettingsController.getInstance().getServerStatistic(s.getId());
        if (!stat.isCollectNonDefaultSmileStats()) return;
        MessageStatistic textChatStatistic = stat.getTextChatStatistic(ch);
        react(textChatStatistic, ch.getName(), messageDate, isCreate);
        if (author != null) {
            String displayUserName = s.getDisplayName(author) + " (" + author.getDiscriminatedName() + ")";
            MessageStatistic userGlobalStat = stat.getUserMessageStatistic(author);
            MessageStatistic userTextChatStat = textChatStatistic.getChildItemStatistic(author.getId());
            react(userGlobalStat, displayUserName, messageDate, isCreate);
            react(userTextChatStat, displayUserName, messageDate, isCreate);
        }
    }

    private static void react(@NotNull MessageStatistic stats, @NotNull String lastKnownName,
                              @Nullable Instant messageDate, boolean isCreate) {
        if (isCreate) {
            if (messageDate != null) {
                stats.incrementWithLastDate(messageDate);
            } else {
                stats.increment();
            }
        } else {
            stats.decrement();
        }
        stats.setLastKnownName(lastKnownName);
    }

    @Override
    public void onMessageCreate(MessageCreateEvent event) {
        if (event.getMessageAuthor().isYourself()) return;
        if (!event.getMessageAuthor().isUser()) return;
        Optional<User> mayBeUser = event.getMessageAuthor().asUser();
        User author = null;
        if (mayBeUser.isPresent()) {
            author = mayBeUser.get();
        }
        Instant messageDate = event.getMessage().getCreationTimestamp();
        onMessage(event, author, messageDate, true);
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        User user = null;
        if (event.getMessageAuthor().isPresent()) {
            MessageAuthor author = event.getMessageAuthor().get();
            if (author.isYourself()) return;
            if (!author.isUser()) return;
            Optional<User> mayBeUser = author.asUser();
            if (mayBeUser.isPresent()) {
                user = mayBeUser.get();
            }
        }
        onMessage(event, user, null, false);
    }

    private void onMessage(@NotNull MessageEvent event, @Nullable User author, @Nullable Instant messageDate, boolean isCreate) {
        event.getServer().ifPresent(s ->
                event.getServerTextChannel().ifPresent(ch ->
                        collectStat(s, ch, author, messageDate, isCreate)
                )
        );
    }
}
