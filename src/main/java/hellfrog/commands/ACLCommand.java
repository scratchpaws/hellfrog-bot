package hellfrog.commands;

import hellfrog.common.*;
import hellfrog.core.RateLimiter;
import hellfrog.core.ServerSideResolver;
import hellfrog.settings.SettingsController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageSet;
import org.javacord.api.entity.message.MessageType;
import org.javacord.api.entity.message.Messageable;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.MessageEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.javacord.api.exception.MissingPermissionsException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public abstract class ACLCommand {

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
    protected final Logger log = LogManager.getLogger(this.getClass().getSimpleName());
    private final String prefix;
    private final String description;
    private final List<Long> notStrictByChannelServers = new CopyOnWriteArrayList<>();
    private boolean strictByChannels = false;
    private boolean onlyServerCommand = false;
    private boolean updateLastUsage = true;
    private boolean expertCommand = false;
    private boolean adminCommand = false;
    private boolean visibleInHelp = true;

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

    protected final void setAdminCommand() {
        this.adminCommand = true;
    }

    protected final void disableVisibleInHelp() {
        this.visibleInHelp = false;
    }

    public boolean isVisibleInHelp() {
        return visibleInHelp;
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

    public boolean isAdminCommand() {
        return adminCommand;
    }

    public boolean isStrictByChannelsOnServer(@NotNull final Server server) {
        return strictByChannels && !notStrictByChannelServers.contains(server.getId());
    }

    public boolean canExecuteServerCommand(MessageCreateEvent event, Server server,
                                           long... anotherTargetChannel) {
        return SettingsController.getInstance()
                .getAccessControlService()
                .canExecuteOnServer(getPrefix(), event, server, isStrictByChannelsOnServer(server), anotherTargetChannel);
    }

    public boolean canExecuteServerCommand(SingleReactionEvent event, Server server,
                                           long... anotherTargetChannel) {
        return SettingsController.getInstance()
                .getAccessControlService()
                .canExecuteOnServer(getPrefix(), event, server, isStrictByChannelsOnServer(server), anotherTargetChannel);
    }

    protected boolean canExecuteGlobalCommand(@NotNull MessageCreateEvent event) {
        return SettingsController.getInstance()
                .getAccessControlService()
                .canExecuteGlobalCommand(event);
    }

    protected boolean canExecuteGlobalCommand(@NotNull SingleReactionEvent event) {
        return SettingsController.getInstance()
                .getAccessControlService()
                .canExecuteGlobalCommand(event);
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

    protected void showAccessDeniedGlobalMessage(MessageCreateEvent event) {
        showErrorMessage("Only bot owners can do it.", event);
    }

    protected void showAccessDeniedServerMessage(MessageCreateEvent event) {
        showErrorMessage("Only allowed users and roles can do it.", event);
    }

    protected void showAccessDeniedServerMessage(SingleReactionEvent event) {
        showErrorMessage("Only allowed users and roles can do it.", event);
    }

    protected void showErrorMessage(String textMessage, MessageCreateEvent event) {
        LongEmbedMessage message = new LongEmbedMessage()
                .setErrorStyle()
                .append(textMessage);
        showMessage(message, event);
    }

    public void showErrorMessage(String textMessage, SingleReactionEvent event) {
        LongEmbedMessage message = new LongEmbedMessage()
                .setErrorStyle()
                .append(textMessage);
        showMessage(message, event);
    }

    protected void showInfoMessage(String textMessage, MessageCreateEvent event) {
        LongEmbedMessage message = new LongEmbedMessage()
                .setInfoStyle()
                .append(textMessage);
        showMessage(message, event);
    }

    protected void showMessage(@NotNull LongEmbedMessage message, @NotNull MessageCreateEvent event) {
        resolveMessageEmbedByRights(message, event);
    }

    protected void showMessage(@NotNull LongEmbedMessage message, @NotNull SingleReactionEvent event) {
        resolveMessageEmbedByRights(message, event);
    }

    private void resolveMessageEmbedByRights(@NotNull final LongEmbedMessage message,
                                             @NotNull final MessageCreateEvent event) {
        event.getMessageAuthor().asUser().ifPresentOrElse(user ->
                        resolveMessageEmbedByRights(message, event, user),
                () -> log.warn("Receive message from {}", event.getMessageAuthor()));
    }

    private void resolveMessageEmbedByRights(@NotNull final LongEmbedMessage message,
                                             @NotNull final SingleReactionEvent event) {
        event.getUser().ifPresent(user -> resolveMessageEmbedByRights(message, event, user));
    }

    private void resolveMessageEmbedByRights(@NotNull final LongEmbedMessage message,
                                             @NotNull final MessageEvent event,
                                             @NotNull final User user) {

        event.getServer().ifPresentOrElse(server -> event.getServerTextChannel().ifPresentOrElse(serverTextChannel -> {

                    boolean hasRights = SettingsController.getInstance()
                            .getAccessControlService()
                            .canExecuteOnServer(prefix, user, server, serverTextChannel, isStrictByChannelsOnServer(server));
                    boolean canWriteToChannel = serverTextChannel.canYouWrite();
                    if (hasRights && canWriteToChannel) {
                        showEmbedMessage(message, serverTextChannel, user);
                    } else {
                        showEmbedMessage(message, user, null);
                    }

                }, () -> showEmbedMessage(message, user, null)),

                () -> showEmbedMessage(message, user, null));
    }

    @Contract("null -> false")
    private boolean canShowMessageByRights(@Nullable final MessageCreateEvent event) {
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
            boolean hasRights = SettingsController.getInstance()
                    .getAccessControlService()
                    .canExecuteOnServer(prefix, event, server, isStrictByChannelsOnServer(server));
            boolean canWriteToChannel = channel.canYouWrite();
            return hasRights && canWriteToChannel;
        } else {
            return false;
        }
    }

    private void showEmbedMessage(@NotNull final LongEmbedMessage message,
                                  @NotNull final Messageable target,
                                  @Nullable User alternatePrivateTarget) {

        boolean isTargetLimited = false;
        if (target instanceof User) {
            isTargetLimited = RateLimiter.notifyIsLimited(((User) target).getId());
        }
        if (isTargetLimited) {
            return;
        }

        try {
            message.send(target)
                    .get(10_000L, TimeUnit.SECONDS);
        } catch (Exception err) {
            if (target instanceof TextChannel && alternatePrivateTarget != null) {
                if (err.getCause() instanceof MissingPermissionsException) {
                    LongEmbedMessage errMessage = new LongEmbedMessage()
                            .setErrorStyle()
                            .append("Unable sent message to text channel! Missing permission!");
                    showEmbedMessage(errMessage, alternatePrivateTarget, null);
                } else {
                    log.error("Unable to send the message: " + err.getMessage(), err);
                }
                showEmbedMessage(message, alternatePrivateTarget, null);
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
    protected String getReadableMessageContentWithoutPrefix(@NotNull final Message message) {
        String messageWoBotPrefix = MessageUtils.getEventMessageWithoutBotPrefix(message.getContent(), message.getServer());
        messageWoBotPrefix = ServerSideResolver.getReadableContent(messageWoBotPrefix, message.getServer());
        return CommonUtils.cutLeftString(messageWoBotPrefix, prefix).trim();
    }

    protected String getFullMessagesContentFromAuthorMessage(@NotNull final Message message, final long toExcludeMessageId) {
        StringBuilder result = new StringBuilder();
        extractMessageTextWithEmbed(result, message);
        final long authorId = message.getAuthor().getId();
        final int maxCount = 9; // 10 total with start
        try {
            MessageSet nextMessages = message.getMessagesAfter(maxCount)
                    .get(CommonConstants.OP_WAITING_TIMEOUT, CommonConstants.OP_TIME_UNIT);
            if (nextMessages != null && !nextMessages.isEmpty()) {
                for (Message msg : nextMessages) {
                    if (msg.getAuthor().getId() != authorId) {
                        break;
                    }
                    if (msg.getId() == toExcludeMessageId) {
                        break;
                    }
                    result.append(" ");
                    extractMessageTextWithEmbed(result, msg);
                }
            }
        } catch (Exception extrErr) {
            BroadCast.getLogger()
                    .addErrorMessage("Unable to fetch next 10 messages after ID " + message.getId() + ": " + extrErr.getMessage())
                    .send();
        }
        return ServerSideResolver.getReadableContent(result.toString(), message.getServer());
    }

    @NotNull
    protected String getAllAvailableReadableContentWithoutPrefix(@NotNull final Message message) {
        StringBuilder result = new StringBuilder();
        extractMessageTextWithEmbed(result, message);
        return ServerSideResolver.getReadableContent(result.toString(), message.getServer());
    }

    private void extractMessageTextWithEmbed(@NotNull final StringBuilder text,
                                             @NotNull final Message message) {
        String messageContent = getReadableMessageContentWithoutPrefix(message);
        String embedContents = message.getEmbeds()
                .stream()
                .filter(embed -> embed.getProvider().isEmpty())
                .map(embed -> embed.getDescription().orElse(""))
                .reduce(CommonUtils::reduceNewLine)
                .orElse("");
        if (CommonUtils.isTrStringNotEmpty(messageContent)) {
            text.append(messageContent);
        }
        if (CommonUtils.isTrStringNotEmpty(messageContent) && CommonUtils.isTrStringNotEmpty(embedContents)) {
            text.append('\n');
        }
        if (CommonUtils.isTrStringNotEmpty(embedContents)) {
            text.append(embedContents);
        }
    }

    @NotNull
    protected Optional<String> getReplyAllAvailableReadableContentWithoutPrefix(@NotNull final MessageCreateEvent event) {

        final boolean isReply = event.getMessage().getType().equals(MessageType.REPLY);
        if (!isReply) {
            return Optional.empty();
        }

        Message referencedMessage = event.getMessage().getReferencedMessage().orElse(null);
        if (referencedMessage != null) {
            return Optional.of(getFullMessagesContentFromAuthorMessage(referencedMessage, event.getMessageId()));
        }

        CompletableFuture<Message> featuredReference = event.getMessage().requestReferencedMessage().orElse(null);
        if (featuredReference == null) {
            return Optional.empty();
        }
        try {
            referencedMessage = featuredReference.get(CommonConstants.OP_WAITING_TIMEOUT, CommonConstants.OP_TIME_UNIT);
            if (referencedMessage == null) {
                return Optional.empty();
            }
            return Optional.of(getFullMessagesContentFromAuthorMessage(referencedMessage, event.getMessageId()));
        } catch (Exception ignore) {
            return Optional.empty();
        }
    }

    @NotNull
    protected String getMessageContentWithoutPrefix(@NotNull final MessageCreateEvent event) {
        final String eventMessage = event.getMessageContent();
        String messageWoBotPrefix = MessageUtils.getEventMessageWithoutBotPrefix(eventMessage, event.getServer());
        return CommonUtils.cutLeftString(messageWoBotPrefix, prefix).trim();
    }
}
