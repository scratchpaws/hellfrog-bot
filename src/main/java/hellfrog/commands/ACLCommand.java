package hellfrog.commands;

import hellfrog.common.BroadCast;
import hellfrog.common.CommonUtils;
import hellfrog.common.MessageUtils;
import hellfrog.core.AccessControlCheck;
import hellfrog.core.RateLimiter;
import hellfrog.core.ServerSideResolver;
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
import org.javacord.api.event.message.MessageEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.javacord.api.exception.MissingPermissionsException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public abstract class ACLCommand {

    protected static final int ERROR_MESSAGE = 0;
    protected static final int INFO_MESSAGE = 1;
    private final String prefix;
    private final String description;
    private boolean strictByChannels = false;
    private boolean onlyServerCommand = false;
    private boolean updateLastUsage = true;
    private boolean expertCommand = false;
    protected final Logger log = LogManager.getLogger(this.getClass().getSimpleName());
    public static final List<Long> CURRENT_SERVER_WITH_ACL_BUG = List.of(
            780560871100252171L,
            381461592300322821L,
            522748524857917450L,
            750800591189704714L,
            723289657789513828L,
            594617083631894624L,
            630837805362184240L,
            575113435524890646L,
            427210944247234564L,
            550256321149141002L
    );
    private final List<Long> notStrictByChannelServers = new CopyOnWriteArrayList<>();

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

    protected final void skipStrictByChannelWithAclBUg() {
        notStrictByChannelServers.addAll(CURRENT_SERVER_WITH_ACL_BUG);
    }

    protected final void enableOnlyServerCommandStrict() {
        this.onlyServerCommand = true;
    }

    protected final void disableUpdateLastCommandUsage() {
        this.updateLastUsage = false;
    }

    protected final void setCommandAsExpert() {
        this.expertCommand = true;
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

    public boolean isExpertCommand() {
        return expertCommand;
    }

    public boolean isNotExpertCommand() {
        return !expertCommand;
    }

    private boolean isStrictOnServer(@NotNull final Server server) {
        return strictByChannels && !notStrictByChannelServers.contains(server.getId());
    }

    public boolean canExecuteServerCommand(MessageCreateEvent event, Server server,
                                           long... anotherTargetChannel) {
        return AccessControlCheck.canExecuteOnServer(getPrefix(), event, server, isStrictOnServer(server), anotherTargetChannel);
    }

    public boolean canExecuteServerCommand(SingleReactionEvent event, Server server,
                                           long... anotherTargetChannel) {
        return AccessControlCheck.canExecuteOnServer(getPrefix(), event, server, isStrictOnServer(server), anotherTargetChannel);
    }

    protected boolean canExecuteGlobalCommand(@NotNull MessageCreateEvent event) {
        return AccessControlCheck.canExecuteGlobalCommand(event);
    }

    protected boolean canExecuteGlobalCommand(@NotNull SingleReactionEvent event) {
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

    protected Messageable getMessageTargetByRights(MessageCreateEvent event) {
        return canShowMessageByRights(event) ? event.getChannel() : event.getMessageAuthor()
                .asUser().orElse(event.getApi().getOwner().join());
    }

    public void showAccessDeniedGlobalMessage(MessageCreateEvent event) {
        showErrorMessage("Only bot owners can do it.", event);
    }

    public void showAccessDeniedServerMessage(MessageCreateEvent event) {
        showErrorMessage("Only allowed users and roles can do it.", event);
    }

    public void showAccessDeniedServerMessage(SingleReactionEvent event) {
        showErrorMessage("Only allowed users and roles can do it.", event);
    }

    public void showErrorMessage(String textMessage, MessageCreateEvent event) {
        resolveMessageEmbedByRights(textMessage, event, ERROR_MESSAGE);
    }

    public void showErrorMessage(String textMessage, SingleReactionEvent event) {
        resolveMessageEmbedByRights(textMessage, event, ERROR_MESSAGE);
    }

    protected void showInfoMessage(String textMessage, MessageCreateEvent event) {
        resolveMessageEmbedByRights(textMessage, event, INFO_MESSAGE);
    }

    private void resolveMessageEmbedByRights(@NotNull String textMessage,
                                             @NotNull MessageCreateEvent event,
                                             int type) {
        event.getMessageAuthor().asUser().ifPresentOrElse(user ->
                        resolveMessageEmbedByRights(textMessage, event, user, type),
                () -> log.warn("Receive message from {}", event.getMessageAuthor()));
    }

    private void resolveMessageEmbedByRights(@NotNull String textMessage,
                                             @NotNull SingleReactionEvent event,
                                             int type) {
        event.getUser().ifPresent(user -> resolveMessageEmbedByRights(textMessage, event, user, type));
    }

    private void resolveMessageEmbedByRights(@NotNull String textMessage,
                                             @NotNull MessageEvent event,
                                             @NotNull User user,
                                             int type) {
        event.getServer().ifPresentOrElse(server ->
                        event.getServerTextChannel().ifPresentOrElse(serverTextChannel -> {
                            boolean hasRights = AccessControlCheck.canExecuteOnServer(prefix, user, server, serverTextChannel, isStrictOnServer(server));
                            boolean canWriteToChannel = serverTextChannel.canYouWrite();
                            if (hasRights && canWriteToChannel) {
                                showEmbedMessage(textMessage, serverTextChannel, user, type);
                            } else {
                                showEmbedMessage(textMessage, user, null, type);
                            }
                        }, () -> showEmbedMessage(textMessage, user, null, type)),
                () -> showEmbedMessage(textMessage, user, null, type));
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
            boolean hasRights = AccessControlCheck.canExecuteOnServer(prefix, event, server, isStrictOnServer(server));
            boolean canWriteToChannel = channel.canYouWrite();
            return hasRights && canWriteToChannel;
        } else {
            return false;
        }
    }

    private void showEmbedMessage(String textMessage, Messageable target, @Nullable User alternatePrivateTarget, int type) {
        boolean isTargetLimited = false;
        if (target instanceof User) {
            isTargetLimited = RateLimiter.notifyIsLimited(((User) target).getId());
        }
        if (isTargetLimited) {
            return;
        }
        Color messageColor = switch (type) {
            case ERROR_MESSAGE -> Color.RED;
            case INFO_MESSAGE -> Color.CYAN;
            default -> Color.BLACK;
        };
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
                    log.error("Unable to send the message: " + err.getMessage(), err);
                }
                showEmbedMessage(textMessage, alternatePrivateTarget, null, type);
            }
        }
    }

    protected void updateLastUsage() {
        if (updateLastUsage) {
            SettingsController.getInstance().updateLastCommandUsage();
        }
    }

    protected boolean hasRateLimits(@NotNull MessageCreateEvent event) {
        boolean userIsLimited = RateLimiter.userIsLimited(event);
        boolean serverIsLimited = RateLimiter.serverIsLimited(event);
        if (userIsLimited) {
            showErrorMessage("You have exceeded the number of requests", event);
            String errorMessage = "This user exceeded the number of requests: " + ServerSideResolver.getFullUserDescriptionByEvent(event);
            BroadCast.getLogger().addWarnMessage(errorMessage).send();
            return true;
        } else if (serverIsLimited) {
            showErrorMessage("The number of requests exceeded from this server", event);
            String errorMessage = "This user exceeded the number of request for the server: " + ServerSideResolver.getFullUserDescriptionByEvent(event);
            BroadCast.getLogger().addWarnMessage(errorMessage).send();
            return true;
        } else {
            return false;
        }
    }

    @NotNull
    protected String getReadableMessageContentWithoutPrefix(@NotNull final MessageCreateEvent event) {
        final String eventMessage = event.getMessageContent();
        String messageWoBotPrefix = MessageUtils.getEventMessageWithoutBotPrefix(eventMessage, event.getServer());
        messageWoBotPrefix = ServerSideResolver.getReadableContent(messageWoBotPrefix, event.getServer());
        return CommonUtils.cutLeftString(messageWoBotPrefix, prefix).trim();
    }

    @NotNull
    protected String getMessageContentWithoutPrefix(@NotNull final MessageCreateEvent event) {
        final String eventMessage = event.getMessageContent();
        String messageWoBotPrefix = MessageUtils.getEventMessageWithoutBotPrefix(eventMessage, event.getServer());
        return CommonUtils.cutLeftString(messageWoBotPrefix, prefix).trim();
    }
}
