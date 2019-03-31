package xyz.funforge.scratchypaws.hellfrog.reactions;

import besus.utils.collection.Sequental;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.jetbrains.annotations.Nullable;
import xyz.funforge.scratchypaws.hellfrog.common.CommonUtils;
import xyz.funforge.scratchypaws.hellfrog.core.AccessControlCheck;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public abstract class MsgCreateReaction
        implements MessageCreateListener {

    private boolean accessControl = false;
    private boolean strictByChannel = false;
    private String commandPrefix = "";
    private String commandDescription = "";

    public static Sequental<MsgCreateReaction> all() {
        return Sequental
                .all(new DiceReaction(), new CustomEmojiReaction())
                .repeatable();
    }

    void enableAccessControl() {
        if (CommonUtils.isTrStringEmpty(commandPrefix) ||
                CommonUtils.isTrStringEmpty(commandDescription)) {
            throw new IllegalArgumentException("To enable access control you must set command prefix and description");
        }
        this.accessControl = true;
    }

    void enableStrictByChannel() {
        if (CommonUtils.isTrStringEmpty(commandPrefix) ||
                CommonUtils.isTrStringEmpty(commandDescription)) {
            throw new IllegalArgumentException("To enable strict by channel you must set command prefix and description");
        }
        this.strictByChannel = true;
    }

    public abstract boolean canReact(MessageCreateEvent event);

    @Override
    public void onMessageCreate(MessageCreateEvent event) {
        Optional<Server> server = event.getServer();
        if (accessControl && server.isPresent()) {
            boolean granted = AccessControlCheck.canExecuteOnServer(commandPrefix, event, server.get(),
                    strictByChannel, event.getChannel().getId());
            if (!granted) return;
        }

        executeReaction(event);
    }

    private void executeReaction(MessageCreateEvent event) {
        Optional<Server> mayBeServer = event.getServer();
        Optional<User> mayBeUser = event.getMessageAuthor().asUser();

        final String strMessage = event.getMessageContent();
        final Server server = mayBeServer.orElse(null);
        final User user = mayBeUser.orElse(null);
        final TextChannel textChannel = event.getChannel();
        final Instant messageCreateDate = event.getMessage().getCreationTimestamp();
        CompletableFuture.runAsync(() -> parallelExecuteReact(strMessage, server, user, textChannel, messageCreateDate));
    }

    abstract void parallelExecuteReact(String strMessage, @Nullable Server server,
                                       @Nullable User user, TextChannel textChannel,
                                       Instant messageCreateDate);

    public boolean isAccessControl() {
        return accessControl;
    }

    public String getCommandPrefix() {
        return commandPrefix;
    }

    void setCommandPrefix(@Nullable String commandPrefix) {
        if (CommonUtils.isTrStringEmpty(commandPrefix)) {
            throw new IllegalArgumentException("Command prefix cannot be empty");
        }
        this.commandPrefix = commandPrefix.toLowerCase().trim();
    }

    public String getCommandDescription() {
        return commandDescription;
    }

    void setCommandDescription(@Nullable String commandDescription) {
        if (CommonUtils.isTrStringEmpty(commandDescription)) {
            throw new IllegalArgumentException("Command description cannot be empty");
        }
        this.commandDescription = commandDescription;
    }
}
