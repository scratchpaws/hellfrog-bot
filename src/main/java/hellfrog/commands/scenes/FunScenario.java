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

import java.awt.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public abstract class FunScenario
        extends Scenario {

    protected static final List<String> BLUSH_URLS = new CopyOnWriteArrayList<>();
    protected static final List<String> HUG_URLS = new CopyOnWriteArrayList<>();
    protected static final List<String> KISS_URLS = new CopyOnWriteArrayList<>();
    protected static final List<String> PAT_URLS = new CopyOnWriteArrayList<>();
    protected static final List<String> SHOCK_URLS = new CopyOnWriteArrayList<>();
    protected static final List<String> SLAP_URLS = new CopyOnWriteArrayList<>();
    protected static final List<String> CUDDLE_URLS = new CopyOnWriteArrayList<>();
    protected static final List<String> DANCE_URLS = new CopyOnWriteArrayList<>();
    protected static final List<String> LICK_URLS = new CopyOnWriteArrayList<>();
    protected static final List<String> BITE_URLS = new CopyOnWriteArrayList<>();
    protected static final List<String> BONK_URLS = new CopyOnWriteArrayList<>();
    protected static final List<String> SPANK_URLS = new CopyOnWriteArrayList<>();

    private String lonelyResultMessage = "";
    private String withSomeoneResultMessage = "";
    private List<String> urlPicturesSet = Collections.emptyList();
    private final ConcurrentHashMap<Long, Queue<Integer>> prebuildPool = new ConcurrentHashMap<>();
    private final AtomicInteger previousSetSize = new AtomicInteger(0);
    private final Object lock = new Object();

    public FunScenario(String prefix, String description) {
        super(prefix, description);
        super.enableOnlyServerCommandStrict();
        super.enableStrictByChannels();
    }

    public static BroadCast.MessagesLogger InitUrlIndexes() {

        final BroadCast.MessagesLogger messagesLogger = BroadCast.getLogger();
        final SettingsController settingsController = SettingsController.getInstance();
        final CommonPreferencesDAO commonPreferencesDAO = settingsController.getMainDBController().getCommonPreferencesDAO();

        final long blushChannelId = commonPreferencesDAO.getFunBlushChannel();
        final long hugChannelId = commonPreferencesDAO.getFunHugChannel();
        final long kissChannelId = commonPreferencesDAO.getFunKissChannel();
        final long patChannelId = commonPreferencesDAO.getFunPatChannel();
        final long shockChannelId = commonPreferencesDAO.getFunShockChannel();
        final long slapChannelId = commonPreferencesDAO.getFunSlapChannel();
        final long cuddleChannelId = commonPreferencesDAO.getFunCuddleChannel();
        final long danceChannelId = commonPreferencesDAO.getFunDanceChannel();
        final long lickChannelId = commonPreferencesDAO.getFunLickChannel();
        final long biteChannelId = commonPreferencesDAO.getFunBiteChannel();
        final long bonkChannelId = commonPreferencesDAO.getFunBonkChannel();
        final long spankChannelId = commonPreferencesDAO.getFunSpankChannel();

        messagesLogger.add(rebuildUrlsList(BLUSH_URLS, blushChannelId, "pictures for \"blush\" command", false));
        messagesLogger.add(rebuildUrlsList(HUG_URLS, hugChannelId, "pictures for \"hug\" command", false));
        messagesLogger.add(rebuildUrlsList(KISS_URLS, kissChannelId, "pictures for \"kiss\" command", false));
        messagesLogger.add(rebuildUrlsList(PAT_URLS, patChannelId, "pictures for \"pat\" command", false));
        messagesLogger.add(rebuildUrlsList(SHOCK_URLS, shockChannelId, "pictures for \"shock\" command", false));
        messagesLogger.add(rebuildUrlsList(SLAP_URLS, slapChannelId, "pictures for \"slap\" command", false));
        messagesLogger.add(rebuildUrlsList(CUDDLE_URLS, cuddleChannelId, "pictures for \"cuddle\" command", false));
        messagesLogger.add(rebuildUrlsList(DANCE_URLS, danceChannelId, "pictures for \"dance\" command", false));
        messagesLogger.add(rebuildUrlsList(LICK_URLS, lickChannelId, "pictures for \"lick\" command", false));
        messagesLogger.add(rebuildUrlsList(BITE_URLS, biteChannelId, "pictures for \"bite\" command", false));
        messagesLogger.add(rebuildUrlsList(BONK_URLS, bonkChannelId, "pictures for \"bonk\" command", false));
        messagesLogger.add(rebuildUrlsList(SPANK_URLS, spankChannelId, "pictures for \"spank\" command", false));

        return messagesLogger;
    }

    private static BroadCast.MessagesLogger rebuildUrlsList(@NotNull final List<String> targetList,
                                                            final long channelId,
                                                            @NotNull final String channelDescription,
                                                            final boolean sendBroadcastSeparately) {

        final BroadCast.MessagesLogger messagesLogger = BroadCast.getLogger();
        final DiscordApi api = SettingsController.getInstance().getDiscordApi();
        if (api != null) {
            Optional<ServerTextChannel> mayBeChannel = api.getServerTextChannelById(channelId);
            if (mayBeChannel.isPresent()) {
                ServerTextChannel channel = mayBeChannel.get();
                if (channel.canYouSee() && channel.canYouReadMessageHistory()) {
                    final List<String> result = new ArrayList<>();
                    channel.getMessagesAsStream().forEach(message -> {
                        result.addAll(message.getAttachments().stream()
                                .map(MessageAttachment::getUrl)
                                .map(URL::toString)
                                .collect(Collectors.toList()));
                        result.addAll(CommonUtils.detectAllUrls(message.getReadableContent()));
                    });
                    targetList.clear();
                    targetList.addAll(result.stream().distinct().collect(Collectors.toList()));
                    messagesLogger.addInfoMessage(String.format("Found %d URLs for \"%s\"", result.size(), channelDescription));
                } else {
                    messagesLogger.addErrorMessage(String.format("(Unable to load URLS from channel \"%s\" by id %d: bot cannot read messages history)",
                            channelDescription, channelId));
                }

                List<MessageCreateListener> createListeners = channel.getMessageCreateListeners();
                List<MessageEditListener> editListeners = channel.getMessageEditListeners();
                List<MessageDeleteListener> deleteListeners = channel.getMessageDeleteListeners();

                if (createListeners.isEmpty()) {
                    channel.addMessageCreateListener(event -> rebuildUrlsList(targetList, channelId, channelDescription, true));
                }
                if (editListeners.isEmpty()) {
                    channel.addMessageEditListener(event -> rebuildUrlsList(targetList, channelId, channelDescription, true));
                }
                if (deleteListeners.isEmpty()) {
                    channel.addMessageDeleteListener(event -> rebuildUrlsList(targetList, channelId, channelDescription, true));
                }
            } else {
                messagesLogger.addErrorMessage(String.format("(Unable to load URLs from channel \"%s\" by id %d)", channelDescription, channelId));
            }
        } else {
            messagesLogger.addErrorMessage(String.format("(Unable to load/reload URLS from channel \"%s\" by id %d: Discord API is null)",
                    channelDescription, channelId));
        }

        if (sendBroadcastSeparately) {
            messagesLogger.send();
        }

        return messagesLogger;
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
                .orElse(ServerSideResolver.resolveRole(server, messageContent)
                        .map(target -> user.getMentionTag() + " " + withSomething + " " + target.getMentionTag())
                        .orElse(user.getMentionTag() + " " + lonelyMessage));

        final String imageUrl = getShuffleUrl(server.getId());

        new MessageBuilder()
                .setEmbed(new EmbedBuilder()
                        .setDescription(description)
                        .setImage(imageUrl)
                        .setColor(Color.GREEN))
                .send(serverTextChannel);
    }

    private String getShuffleUrl(final long serverId) {
        final ThreadLocalRandom tlr = ThreadLocalRandom.current();
        int previousSize = previousSetSize.get();
        boolean resetShuffle = previousSize != urlPicturesSet.size();
        Queue<Integer> currentQueue = prebuildPool.get(serverId);
        if (resetShuffle || currentQueue == null || currentQueue.isEmpty()) {
            synchronized (lock) {
                previousSize = previousSetSize.get();
                resetShuffle = previousSize != urlPicturesSet.size();
                currentQueue = prebuildPool.get(serverId);
                if (resetShuffle || currentQueue == null || currentQueue.isEmpty()) {
                    if (resetShuffle) {
                        prebuildPool.clear();
                        previousSetSize.set(urlPicturesSet.size());
                    }
                    List<Integer> values = new ArrayList<>();
                    for (int i = 0; i < urlPicturesSet.size(); i++) {
                        values.add(i);
                    }
                    Collections.shuffle(values, tlr);
                    currentQueue = new ConcurrentLinkedQueue<>(values);
                    prebuildPool.put(serverId, currentQueue);
                }
            }
        }
        Integer urlIndex = currentQueue.poll();
        if (urlIndex == null || urlPicturesSet.size() < urlIndex) {
            urlIndex = tlr.nextInt(0, urlPicturesSet.size());
        }
        try {
            return urlPicturesSet.size() > 0 ? urlPicturesSet.get(urlIndex) : "";
        } catch (IndexOutOfBoundsException err) {
            return "";
        }
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
