package hellfrog.commands;

import hellfrog.common.CommonUtils;
import hellfrog.core.AccessControlCheck;
import hellfrog.settings.SettingsController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.Messageable;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.exception.MissingPermissionsException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public abstract class ACLCommand {

    protected static final int ERROR_MESSAGE = 0;
    protected static final int INFO_MESSAGE = 1;
    private final String prefix;
    private final String description;
    private boolean strictByChannels = false;
    private boolean onlyServerCommand = false;
    private boolean updateLastUsage = true;
    protected final Logger log = LogManager.getLogger(this.getClass().getSimpleName());

    protected ACLCommand(@NotNull String prefix, @NotNull String description) {
        if (CommonUtils.isTrStringEmpty(prefix))
            throw new IllegalArgumentException("BUG: prefix cannot be null");
        if (CommonUtils.isTrStringEmpty(description))
            throw new IllegalArgumentException("BUG: description cannot be null");

        this.prefix = prefix;
        this.description = description;
    }

    protected final void enableStrictByChannels() {
        this.strictByChannels = true;
    }

    protected final void enableOnlyServerCommandStrict() {
        this.onlyServerCommand = true;
    }

    protected final void disableUpdateLastCommandUsage() {
        this.updateLastUsage = false;
    }

    /**
     * Получить префикс команды для её вызова
     *
     * @return префикс команды
     */
    public String getPrefix() {
        return prefix;
    }

    public boolean isStrictByChannels() {
        return strictByChannels;
    }

    public boolean isOnlyServerCommand() {
        return onlyServerCommand;
    }

    public boolean isUpdateLastUsage() {
        return updateLastUsage;
    }

    public boolean canExecuteServerCommand(MessageCreateEvent event, Server server,
                                    long... anotherTargetChannel) {
        return AccessControlCheck.canExecuteOnServer(getPrefix(), event, server, strictByChannels, anotherTargetChannel);
    }

    protected boolean canExecuteGlobalCommand(@NotNull MessageCreateEvent event) {
        return AccessControlCheck.canExecuteGlobalCommand(event);
    }

    /**
     * Показывает краткое описание выполняемой функции команды.
     *
     * @return краткое описание
     */
    public String getCommandDescription() {
        return description;
    }

    Messageable getMessageTargetByRights(MessageCreateEvent event) {
        return canShowMessageByRights(event) ? event.getChannel() : event.getMessageAuthor()
                .asUser().orElse(event.getApi().getOwner().join());
    }

    public void showAccessDeniedGlobalMessage(MessageCreateEvent event) {
        showErrorMessage("Only bot owners can do it.", event);
    }

    public void showAccessDeniedServerMessage(MessageCreateEvent event) {
        showErrorMessage("Only allowed users and roles can do it.", event);
    }

    public void showErrorMessage(String textMessage, MessageCreateEvent event) {
        resolveMessageEmbedByRights(textMessage, event, ERROR_MESSAGE);
    }

    protected void showInfoMessage(String textMessage, MessageCreateEvent event) {
        resolveMessageEmbedByRights(textMessage, event, INFO_MESSAGE);
    }

    private void resolveMessageEmbedByRights(@NotNull String textMessage, @NotNull MessageCreateEvent event, int type) {
        event.getMessageAuthor().asUser().ifPresentOrElse(user -> event.getServer().ifPresentOrElse(server ->
                        event.getServerTextChannel().ifPresentOrElse(serverTextChannel -> {
                            boolean hasRights = AccessControlCheck.canExecuteOnServer(prefix, event, server, strictByChannels);
                            boolean canWriteToChannel = serverTextChannel.canYouWrite();
                            if (hasRights && canWriteToChannel) {
                                showEmbedMessage(textMessage, serverTextChannel, user, type);
                            } else {
                                showEmbedMessage(textMessage, user, null, type);
                            }
                        }, () -> showEmbedMessage(textMessage, user, null, type)),
                () -> showEmbedMessage(textMessage, user, null, type)),
                () -> log.warn("Receive message from {}", event.getMessageAuthor()));
    }

    @Contract("null -> false")
    private boolean canShowMessageByRights(MessageCreateEvent event) {
        if (event == null)
            return false;
        if (event.isServerMessage())
            return false;

        Optional<User> mayBeUser = event.getMessageAuthor().asUser();
        Optional<Server> mayBeServer = event.getServer();
        Optional<ServerTextChannel> mayBeTextChannel = event.getServerTextChannel();
        if (mayBeUser.isPresent() && mayBeServer.isPresent() && mayBeTextChannel.isPresent()) {
            Server server = mayBeServer.get();
            ServerTextChannel channel = mayBeTextChannel.get();
            boolean hasRights = AccessControlCheck.canExecuteOnServer(prefix, event, server, strictByChannels);
            boolean canWriteToChannel = channel.canYouWrite();
            return hasRights && canWriteToChannel;
        } else {
            return false;
        }
    }

    private void showEmbedMessage(String textMessage, Messageable target, @Nullable User alternatePrivateTarget, int type) {
        Color messageColor = Color.BLACK;
        switch (type) {
            case ERROR_MESSAGE:
                messageColor = Color.RED;
                break;

            case INFO_MESSAGE:
                messageColor = Color.CYAN;
                break;
        }
        User yourself = SettingsController.getInstance().getDiscordApi().getYourself();
        try {
            new MessageBuilder()
                    .setEmbed(new EmbedBuilder()
                            .setColor(messageColor)
                            .setDescription(textMessage)
                            .setTimestampToNow()
                            //.setFooter(type == ERROR_MESSAGE ? "This message will automatically delete in 20 seconds." : null)
                            .setAuthor(yourself))
                    .send(target).get(10_000L, TimeUnit.SECONDS);
        } catch (Exception err) {
            if (target instanceof TextChannel && alternatePrivateTarget != null) {
                if (err.getCause() instanceof MissingPermissionsException) {
                    showEmbedMessage("Unable sent message to text channel! Missing permission!",
                            alternatePrivateTarget, null, ERROR_MESSAGE);
                } else {
                    log.error("Unable to send message: " + err.getMessage(), err);
                }
                showEmbedMessage(textMessage, alternatePrivateTarget, null, type);
            }
        }
    }
}
