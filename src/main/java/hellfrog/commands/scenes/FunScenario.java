package hellfrog.commands.scenes;

import hellfrog.common.BroadCast;
import hellfrog.common.CommonUtils;
import hellfrog.core.ServerSideResolver;
import hellfrog.core.SessionState;
import hellfrog.settings.SettingsController;
import hellfrog.settings.db.CommonPreferencesDAO;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.PrivateChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.MessageAttachment;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.listener.message.MessageDeleteListener;
import org.javacord.api.listener.message.MessageEditListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.awt.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public abstract class FunScenario
        extends Scenario {

    protected static final List<String> BLUSH_URLS = new CopyOnWriteArrayList<>();
    protected static final List<String> HUG_URLS = new CopyOnWriteArrayList<>();
    protected static final List<String> KISS_URLS = new CopyOnWriteArrayList<>();
    protected static final List<String> LOVE_URLS = new CopyOnWriteArrayList<>();
    protected static final List<String> PAT_URLS = new CopyOnWriteArrayList<>();
    protected static final List<String> SHOCK_URLS = new CopyOnWriteArrayList<>();
    protected static final List<String> SLAP_URLS = new CopyOnWriteArrayList<>();
    protected static final List<String> CUDDLE_URLS = new CopyOnWriteArrayList<>();
    protected static final List<String> DANCE_URLS = new CopyOnWriteArrayList<>();
    protected static final List<String> LICK_URLS = new CopyOnWriteArrayList<>();
    protected static final List<String> BITE_URLS = new CopyOnWriteArrayList<>();

    private String lonelyResultMessage = "";
    private String withSomeoneResultMessage = "";
    private List<String> urlPicturesSet = Collections.emptyList();

    public FunScenario(String prefix, String description) {
        super(prefix, description);
        super.enableOnlyServerCommandStrict();
        super.enableStrictByChannels();
    }

    public static BroadCast.MessagesLogger rebuildUrlIndexes(final boolean sendBroadcastSeparately) {
        final BroadCast.MessagesLogger messagesLogger = BroadCast.getLogger();
        final SettingsController settingsController = SettingsController.getInstance();
        final CommonPreferencesDAO commonPreferencesDAO = settingsController.getMainDBController().getCommonPreferencesDAO();

        final long blushChannelId = commonPreferencesDAO.getFunBlushChannel();
        final long hugChannelId = commonPreferencesDAO.getFunHugChannel();
        final long kissChannelId = commonPreferencesDAO.getFunKissChannel();
        final long loveChannelId = commonPreferencesDAO.getFunLoveChannel();
        final long patChannelId = commonPreferencesDAO.getFunPatChannel();
        final long shockChannelId = commonPreferencesDAO.getFunShockChannel();
        final long slapChannelId = commonPreferencesDAO.getFunSlapChannel();
        final long cuddleChannelId = commonPreferencesDAO.getFunCuddleChannel();
        final long danceChannelId = commonPreferencesDAO.getFunDanceChannel();
        final long lickChannelId = commonPreferencesDAO.getFunLickChannel();
        final long biteChannelId = commonPreferencesDAO.getFunBiteChannel();

        Optional.ofNullable(settingsController.getDiscordApi()).ifPresentOrElse(api -> {
            rebuildUrlsList(api, BLUSH_URLS, messagesLogger, blushChannelId, "pictures for \"blush\" command");
            rebuildUrlsList(api, HUG_URLS, messagesLogger, hugChannelId, "pictures for \"hug\" command");
            rebuildUrlsList(api, KISS_URLS, messagesLogger, kissChannelId, "pictures for \"kiss\" command");
            rebuildUrlsList(api, LOVE_URLS, messagesLogger, loveChannelId, "pictures for \"love\" command");
            rebuildUrlsList(api, PAT_URLS, messagesLogger, patChannelId, "pictures for \"pat\" command");
            rebuildUrlsList(api, SHOCK_URLS, messagesLogger, shockChannelId, "pictures for \"shock\" command");
            rebuildUrlsList(api, SLAP_URLS, messagesLogger, slapChannelId, "pictures for \"slap\" command");
            rebuildUrlsList(api, CUDDLE_URLS, messagesLogger, cuddleChannelId, "pictures for \"cuddle\" command");
            rebuildUrlsList(api, DANCE_URLS, messagesLogger, danceChannelId, "pictures for \"dance\" command");
            rebuildUrlsList(api, LICK_URLS, messagesLogger, lickChannelId, "pictures for \"lick\" command");
            rebuildUrlsList(api, BITE_URLS, messagesLogger, biteChannelId, "pictures for \"bite\" command");
        }, () -> {
            messagesLogger.addErrorMessage("(Unable to get Discord API for search fun command pictures)");
        });

        if (sendBroadcastSeparately) {
            messagesLogger.send();
        }
        return messagesLogger;
    }

    private static void rebuildUrlsList(@NotNull final DiscordApi api,
                                        @NotNull final List<String> targetList,
                                        @NotNull final BroadCast.MessagesLogger messagesLogger,
                                        final long channelId,
                                        @NotNull final String channelDescription) {

        try {
            List<String> urls = loadUrlsFromChannel(api, channelId, channelDescription);
            targetList.clear();
            targetList.addAll(urls);
            messagesLogger.addInfoMessage(String.format("Found %d URLs for \"%s\"", urls.size(), channelDescription));
        } catch (Exception err) {
            messagesLogger.addErrorMessage(err.getMessage());
        }
    }

    @NotNull
    @UnmodifiableView
    private static List<String> loadUrlsFromChannel(@NotNull final DiscordApi api,
                                                    final long channelId,
                                                    @NotNull final String channelDescription) throws Exception {

        Optional<ServerTextChannel> mayBeChannel = api.getServerTextChannelById(channelId);
        if (mayBeChannel.isEmpty()) {
            throw new Exception(String.format("(Unable to load URLs from channel \"%s\" by id %d)", channelDescription, channelId));
        }
        ServerTextChannel channel = mayBeChannel.get();
        checkChannelListeners(channel);
        if (channel.canYouSee() && channel.canYouReadMessageHistory()) {
            List<String> result = new ArrayList<>();
            channel.getMessagesAsStream().forEach(message -> {
                result.addAll(message.getAttachments().stream()
                        .map(MessageAttachment::getUrl)
                        .map(URL::toString)
                        .collect(Collectors.toList()));
            });
            return Collections.unmodifiableList(result);
        } else {
            throw new Exception(String.format("(Unable to load URLS from channel \"%s\" by id %d: bot cannot read messages history)",
                    channelDescription, channelId));
        }
    }

    private static void checkChannelListeners(@NotNull final ServerTextChannel channel) {

        List<MessageCreateListener> createListeners = channel.getMessageCreateListeners();
        List<MessageEditListener> editListeners = channel.getMessageEditListeners();
        List<MessageDeleteListener> deleteListeners = channel.getMessageDeleteListeners();

        if (createListeners.isEmpty()) {
            channel.addMessageCreateListener(event -> rebuildUrlIndexes(true));
        }
        if (editListeners.isEmpty()) {
            channel.addMessageEditListener(event -> rebuildUrlIndexes(true));
        }
        if (deleteListeners.isEmpty()) {
            channel.addMessageDeleteListener(event -> rebuildUrlIndexes(true));
        }
    }

    protected void setLonelyResultMessage(@NotNull String message) {
        this.lonelyResultMessage = message;
    }

    protected void setWithSomeoneResultMessage(@NotNull String message) {
        this.withSomeoneResultMessage = message;
    }

    protected void setUrlPicturesSet(@NotNull List<String> picturesSet) {
        this.urlPicturesSet = picturesSet;
    }

    @Override
    protected void executeServerFirstRun(@NotNull MessageCreateEvent event,
                                         @NotNull Server server,
                                         @NotNull ServerTextChannel serverTextChannel,
                                         @NotNull User user,
                                         boolean isBotOwner) {

        if (CommonUtils.isTrStringEmpty(lonelyResultMessage) || CommonUtils.isTrStringEmpty(withSomeoneResultMessage)) {
            BroadCast.MessagesLogger messagesLogger = BroadCast.getLogger();
            messagesLogger.addErrorMessage(String.format("Command with prefix \"%s\" has not defined result messages", getPrefix()));
            messagesLogger.send();
            showErrorMessage("Internal bot error", event);
            return;
        }

        if (urlPicturesSet.isEmpty()) {
            BroadCast.MessagesLogger messagesLogger = BroadCast.getLogger();
            messagesLogger.addErrorMessage(String.format("Command with prefix \"%s\" has not pictures URLs", getPrefix()));
            messagesLogger.send();
            showErrorMessage("There are no pictures for this command to work :-(", event);
            return;
        }

        final String lonelyMessage = this.lonelyResultMessage;
        final String withSomething = this.withSomeoneResultMessage;

        final String messageContent = getMessageContentWithoutPrefix(event);
        final String description = ServerSideResolver.resolveUser(server, messageContent)
                .map(target -> user.getMentionTag() + " " + withSomething + " " + target.getMentionTag())
                .orElse(user.getMentionTag() + " " + lonelyMessage);

        final ThreadLocalRandom tlr = ThreadLocalRandom.current();
        final int index = tlr.nextInt(0, urlPicturesSet.size());
        final String imageUrl = urlPicturesSet.get(index);

        new MessageBuilder()
                .setEmbed(new EmbedBuilder()
                        .setDescription(description)
                        .setImage(imageUrl)
                        .setColor(Color.GREEN))
                .send(serverTextChannel);
    }

    @Override
    protected void executePrivateFirstRun(@NotNull MessageCreateEvent event,
                                          @NotNull PrivateChannel privateChannel,
                                          @NotNull User user,
                                          boolean isBotOwner) {

        showErrorMessage("This command can't be run into private channel", event);
    }

    @Override
    protected boolean privateMessageStep(@NotNull MessageCreateEvent event,
                                         @NotNull PrivateChannel privateChannel,
                                         @NotNull User user,
                                         @NotNull SessionState sessionState,
                                         boolean isBotOwner) {
        return false;
    }

    @Override
    protected boolean serverMessageStep(@NotNull MessageCreateEvent event,
                                        @NotNull Server server,
                                        @NotNull ServerTextChannel serverTextChannel,
                                        @NotNull User user,
                                        @NotNull SessionState sessionState,
                                        boolean isBotOwner) {
        return false;
    }

    @Override
    protected boolean privateReactionStep(boolean isAddReaction,
                                          @NotNull SingleReactionEvent event,
                                          @NotNull PrivateChannel privateChannel,
                                          @NotNull User user,
                                          @NotNull SessionState sessionState,
                                          boolean isBotOwner) {
        return false;
    }

    @Override
    protected boolean serverReactionStep(boolean isAddReaction,
                                         @NotNull SingleReactionEvent event,
                                         @NotNull Server server,
                                         @NotNull ServerTextChannel serverTextChannel,
                                         @NotNull User user,
                                         @NotNull SessionState sessionState, boolean isBotOwner) {
        return false;
    }
}
