package hellfrog.reacts;

import hellfrog.settings.MessageStatistic;
import hellfrog.settings.ServerStatistic;
import hellfrog.settings.SettingsController;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.MessageAttachment;
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

import java.time.Instant;
import java.util.Optional;

public class MessageStats
        implements MessageCreateListener, MessageDeleteListener {

    public static void collectStat(@NotNull Server s, @NotNull ServerTextChannel ch,
                                   @Nullable User author, @Nullable Instant messageDate,
                                   boolean isCreate, int messageLength, long bytesCount) {
        ServerStatistic stat = SettingsController.getInstance().getServerStatistic(s.getId());
        if (!stat.isCollectNonDefaultSmileStats()) return;
        MessageStatistic textChatStatistic = stat.getTextChatStatistic(ch);
        react(textChatStatistic, ch.getName(), messageDate, isCreate, messageLength, bytesCount);
        if (author != null) {
            String displayUserName = s.getDisplayName(author) + " (" + author.getDiscriminatedName() + ")";
            MessageStatistic userGlobalStat = stat.getUserMessageStatistic(author);
            MessageStatistic userTextChatStat = textChatStatistic.getChildItemStatistic(author.getId());
            react(userGlobalStat, displayUserName, messageDate, isCreate, messageLength, bytesCount);
            react(userTextChatStat, displayUserName, messageDate, isCreate, messageLength, bytesCount);
        }
    }

    private static void react(@NotNull MessageStatistic stats, @NotNull String lastKnownName,
                              @Nullable Instant messageDate, boolean isCreate,
                              int messageLength, long bytesCount) {
        if (isCreate) {
            if (messageDate != null) {
                stats.incrementWithLastDate(messageDate, messageLength, bytesCount);
            } else {
                stats.increment(messageLength, bytesCount);
            }
        } else {
            stats.decrement(messageLength, bytesCount);
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
        int messageLength = event.getMessage().getContent().length();
        long bytesCount = event.getMessage().getAttachments().stream()
                .mapToLong(MessageAttachment::getSize)
                .sum();
        onMessage(event, author, messageDate, true, messageLength, bytesCount);
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
        int messageLength = 0;
        long bytesCount = 0L;
        if (event.getMessage().isPresent()) {
            messageLength = event.getMessage().get().getContent().length();
            bytesCount = event.getMessage().get().getAttachments().stream()
                    .mapToLong(MessageAttachment::getSize)
                    .sum();
        }
        onMessage(event, user, null, false, messageLength, bytesCount);
    }

    private void onMessage(@NotNull MessageEvent event, @Nullable User author, @Nullable Instant messageDate, boolean isCreate,
                           int messageLength, long bytesCount) {
        event.getServer().ifPresent(s ->
                event.getServerTextChannel().ifPresent(ch ->
                        collectStat(s, ch, author, messageDate, isCreate, messageLength, bytesCount)
                )
        );
    }
}
