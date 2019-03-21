package xyz.funforge.scratchypaws.hellfrog.reactions;

import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.MessageDeleteEvent;
import org.javacord.api.event.message.MessageEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.listener.message.MessageDeleteListener;
import org.jetbrains.annotations.Nullable;
import xyz.funforge.scratchypaws.hellfrog.settings.SettingsController;
import xyz.funforge.scratchypaws.hellfrog.settings.old.MessageStatistic;
import xyz.funforge.scratchypaws.hellfrog.settings.old.ServerStatistic;

import java.util.Optional;

public class MessageStats
        implements MessageCreateListener, MessageDeleteListener {

    @Override
    public void onMessageCreate(MessageCreateEvent event) {
        if (event.getMessageAuthor().isYourself()) return;
        if (!event.getMessageAuthor().isUser()) return;
        Optional<User> mayBeUser = event.getMessageAuthor().asUser();
        User author = null;
        if (mayBeUser.isPresent()) {
            author = mayBeUser.get();
        }
        onMessage(event, author, true);
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
        onMessage(event, user, false);
    }

    private void onMessage(MessageEvent event, @Nullable User author, boolean isCreate) {
        event.getServer().ifPresent(s ->
                event.getServerTextChannel().ifPresent(ch -> {
                    ServerStatistic stat = SettingsController.getInstance().getServerStatistic(s.getId());
                    if (!stat.isCollectNonDefaultSmileStats()) return;
                    MessageStatistic textChatStatistic = stat.getTextChatStatistic(ch);
                    react(textChatStatistic, ch.getName(), isCreate);
                    if (author != null) {
                        String displayUserName = s.getDisplayName(author) + " (" + author.getDiscriminatedName() + ")";
                        MessageStatistic userGlobalStat = stat.getUserMessageStatistic(author);
                        MessageStatistic userTextChatStat = textChatStatistic.getChildItemStatistic(author.getId());
                        react(userGlobalStat, displayUserName, isCreate);
                        react(userTextChatStat, displayUserName, isCreate);
                    }
                })
        );
    }

    private void react(MessageStatistic stats, String lastKnownName, boolean isCreate) {
        if (isCreate) {
            stats.increment();
        } else {
            stats.decrement();
        }
        stats.setLastKnownName(lastKnownName);
    }
}
