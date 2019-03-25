package xyz.funforge.scratchypaws.hellfrog.core;

import com.vdurmont.emoji.EmojiParser;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageSet;
import org.javacord.api.entity.message.Reaction;
import org.javacord.api.entity.message.embed.Embed;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.jetbrains.annotations.NotNull;
import xyz.funforge.scratchypaws.hellfrog.reactions.CustomEmojiReaction;
import xyz.funforge.scratchypaws.hellfrog.reactions.MessageStats;
import xyz.funforge.scratchypaws.hellfrog.reactions.ReactReaction;
import xyz.funforge.scratchypaws.hellfrog.settings.SettingsController;

import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class StatisticRebuilder {

    private static final String STAT_CMD_ALIAS = "stat";
    private static final StatisticRebuilder INSTANCE = new StatisticRebuilder();
    private static final String STOP_EMOJI = EmojiParser.parseToUnicode(":black_square_for_stop:");
    private static final String PLAY_EMOJI = EmojiParser.parseToUnicode(":arrow_forward:");
    private ConcurrentHashMap<Long, RebuildInfo> activeRebuilds = new ConcurrentHashMap<>();
    private ReentrantLock infoCreateLock = new ReentrantLock();
    private static final int MSG_HIST_LIMIT = 100; // объём сообщений, извлекаемых из истории
    private static final long HIST_TIMEOUT = 10L; // таймаут на ожидание извлечения истории из API
    private static final long HIST_SLEEP_MULTIPLY = 200L;

    private StatisticRebuilder() {
    }

    public static StatisticRebuilder getInstance() {
        return INSTANCE;
    }

    public void rebuild(MessageCreateEvent event, TextChannel channel) {
        event.getServer().ifPresent(s ->
                channel.asServerTextChannel().ifPresent(ch -> {
                    EmbedBuilder embed = new EmbedBuilder();
                    embed.setAuthor(event.getMessageAuthor());
                    embed.setColor(Color.RED);
                    embed.setTimestampToNow();
                    embed.setDescription("Statistics Recalculation can be performed for a **long time**!\n" +
                            "The Current statistics will be **completely deleted**.\nContinue?");
                    embed.setFooter("Press play or stop reaction...");
                    embed.setTitle("Rebuilding server statistic...");
                    new MessageBuilder()
                            .setEmbed(embed)
                            .send(channel)
                            .thenAccept(message -> {
                                message.addReaction(PLAY_EMOJI);
                                RebuildInfo info = getRebuildInfoForServer(s.getId());
                                if (info.active.get()) {
                                    s.getTextChannelById(info.textChatId).ifPresent(oldChan ->
                                            oldChan.getMessageById(info.messageId).thenAccept(oldMsg -> {
                                                if (oldMsg.canYouDelete())
                                                    oldMsg.delete();
                                            })
                                    );
                                }
                                info.active.set(true);
                                info.messageId = message.getId();
                                info.textChatId = ch.getId();
                                info.started.set(false);
                                info.interrupt.set(false);
                            });
                }));
    }

    private RebuildInfo getRebuildInfoForServer(long serverId) {
        if (!activeRebuilds.containsKey(serverId)) {
            infoCreateLock.lock();
            try {
                if (!activeRebuilds.containsKey(serverId)) {
                    RebuildInfo info = new RebuildInfo();
                    activeRebuilds.put(serverId, info);
                }
            } finally {
                infoCreateLock.unlock();
            }
        }
        return activeRebuilds.get(serverId);
    }

    public boolean isRebuildInProgress(long serverId) {
        return getRebuildInfoForServer(serverId).started.get();
    }

    void onReact(@NotNull ReactionAddEvent event) {
        parseReaction(event);
    }

    private void parseReaction(@NotNull ReactionAddEvent event) {
        event.getServer().ifPresent(server ->
                event.getServerTextChannel().ifPresent(channel -> {
                    RebuildInfo info = getRebuildInfoForServer(server.getId());
                    if (info.messageId != event.getMessageId()) return;
                    // это наше сообщение
                    if (event.getUser().isYourself()) return;

                    if (!AccessControlCheck.canExecuteOnServer(STAT_CMD_ALIAS, event.getUser(),
                            server, event.getChannel(), false)) {
                        event.removeReaction();
                        return;
                    }

                    event.getEmoji().asUnicodeEmoji().ifPresentOrElse(em -> {
                        if (em.equals(PLAY_EMOJI) && info.canStart()) {
                            server.getTextChannelById(info.textChatId).ifPresent(ch ->
                                    ch.getMessageById(info.messageId).thenAccept(m -> {
                                        m.removeAllReactions().thenAccept(v0 ->
                                                m.addReaction(STOP_EMOJI).thenAccept(v1 -> {
                                                    info.started.set(true);
                                                    parallelRebuildStatistic(server.getId());
                                                }));
                                    }));
                        } else if (em.equals(STOP_EMOJI) && info.canInterrupt()) {
                            info.interrupt.set(true);
                            server.getTextChannelById(info.textChatId).ifPresent(ch ->
                                    ch.getMessageById(info.messageId)
                                            .thenAccept(Message::removeAllReactions));
                        } else {
                            event.removeReaction();
                        }
                    }, event::removeReaction);
                })
        );
    }

    private void parallelRebuildStatistic(long serverId) {

        RebuildInfo info = getRebuildInfoForServer(serverId);

        try {
            SettingsController.getInstance().getDiscordApi().getServerById(serverId).ifPresent(server ->
                    server.getTextChannelById(info.textChatId).ifPresent(ch ->
                            ch.getMessageById(info.messageId).thenAccept(msg -> {
                                List<Embed> embeds = msg.getEmbeds();
                                if (embeds.size() > 0) {
                                    EmbedBuilder embed = embeds.get(0).toBuilder();

                                    SettingsController.getInstance()
                                            .getServerStatistic(serverId).clear();
                                    SettingsController.getInstance().saveServerSideParameters(serverId);

                                    server.getTextChannels().stream()
                                            .filter(TextChannel::canYouReadMessageHistory)
                                            .forEach(channel ->
                                                    processingChannel(info, embed, msg, ch, server));

                                    embed.setFooter("...");
                                    if (info.interrupt.get()) {
                                        embed.setDescription("Interrupted");
                                    } else {
                                        embed.setDescription("Done");
                                    }
                                    editSilently(msg, embed);
                                }
                            })
                    )
            );

        } finally {
            activeRebuilds.remove(serverId);
        }
    }

    private void processingChannel(RebuildInfo info, EmbedBuilder embed, Message msg,
                                   ServerTextChannel channel, Server server) {
            if (info.interrupt.get()) return;

            embed.setTimestampToNow()
                    .setDescription("Processing server text channel \""
                            + channel.getName() + "\"")
                    .setFooter("Begin read messages...");
            editSilently(msg, embed);

            long currentMsgNumber = 0L;
            Message currentHistMsg = null;
            for (int trying = 1; trying <= 3; trying++) {
                try {
                    embed.setFooter("Processed ~" + currentMsgNumber + " messages. "
                            + "Get last message of channel, trying "
                            + trying + " of 3...");
                    editSilently(msg, embed);

                    MessageSet firstProbe = channel.getMessages(1)
                            .get(HIST_TIMEOUT, TimeUnit.SECONDS);
                    Optional<Message> mayBeMsg = firstProbe.getOldestMessage();
                    if (mayBeMsg.isPresent()) {
                        currentHistMsg = mayBeMsg.get();
                        break;
                    } else {
                        embed.setFooter("Processed ~" + currentMsgNumber + " messages. "
                                + "Channel has not last message");
                        editSilently(msg, embed);
                        return;
                    }
                } catch (Exception err) {
                    embed.setFooter("Processed ~" + currentMsgNumber + " messages. "
                            + "Unable to get latest message: " + err);
                    if (doSleepExcepted(msg, embed)) return;
                }
            }

            if (currentHistMsg == null)
                return;
            MessageSet set = null;

            processingMessage(currentHistMsg, channel, server);

            do {
                for (int trying = 1; trying <= 3; trying++) {
                    try {
                        embed.setFooter("Processed ~" + currentMsgNumber + " messages. " +
                                "Get next, trying " + trying + " of 3...");
                        editSilently(msg, embed);

                        set = currentHistMsg.getMessagesBefore(MSG_HIST_LIMIT)
                                .get(HIST_TIMEOUT, TimeUnit.SECONDS);

                        embed.setFooter("Processed ~" + currentMsgNumber + " messages."
                                + " Ok, parsing...");
                        editSilently(msg, embed);
                        break;
                    } catch (Exception err) {
                        embed.setFooter("Processed ~" + currentMsgNumber + " messages. "
                                + "Unable to extract messages: " + err);
                        if (doSleepExcepted(msg, embed)) return;
                    }
                }

                try {
                    int activeThreads = activeRebuilds.size();
                    activeThreads = activeThreads == 0 ? 1 : activeThreads;
                    Thread.sleep(activeThreads * HIST_SLEEP_MULTIPLY);
                } catch (InterruptedException breakSig) {
                    info.interrupt.set(true);
                    return;
                }

                if (set != null && set.size() > 0) {
                    for (Message hist : set) {
                        if (!processingMessage(hist, channel, server)) continue;
                        currentMsgNumber++;
                    }
                    Optional<Message> oldest = set.getOldestMessage();
                    if (oldest.isPresent()) {
                        currentHistMsg = oldest.get();
                    } else {
                        break;
                    }
                } else {
                    embed.setFooter("Processed ~" + currentMsgNumber + " messages. "
                            + "Next portion message is empty");
                    editSilently(msg, embed);
                }
            } while ((set != null && set.size() > 0) || info.interrupt.get());

            embed.setFooter("Done");
            editSilently(msg, embed);
    }

    private boolean processingMessage(@NotNull Message hist, @NotNull ServerTextChannel channel, @NotNull Server server) {
        if (!hist.getAuthor().isUser())
            return false;
        if (hist.getAuthor().isYourself())
            return false;

        User author = null;
        Optional<User> mayBeUser = hist.getAuthor().asUser();
        if (mayBeUser.isPresent()) {
            author = mayBeUser.get();
        }
        MessageStats.collectStat(channel.getServer(),
                channel, author, hist.getCreationTimestamp(), true);
        if (CustomEmojiReaction.messageContainCustomEmoji(hist)) {
            CustomEmojiReaction.collectStat(hist.getContent(), author, server,
                    hist.getCreationTimestamp());
        }
        for (Reaction reaction : hist.getReactions()) {
            reaction.getEmoji().asCustomEmoji().ifPresent(ke ->
                    ReactReaction.collectStat(server, ke, true, hist.getCreationTimestamp())
            );
        }

        return true;
    }

    private boolean doSleepExcepted(Message msg, EmbedBuilder embed) {
        editSilently(msg, embed);
        try {
            Thread.sleep(500);
        } catch (InterruptedException brk) {
            return true;
        }
        return false;
    }

    private void editSilently(Message msg, EmbedBuilder embed) {
        try {
            embed.setTimestampToNow();
            msg.edit(embed).get(10, TimeUnit.SECONDS);
        } catch (Exception ignore) {
        }
    }

    private static class RebuildInfo {
        volatile long messageId = 0L;
        volatile long textChatId = 0L;
        AtomicBoolean active = new AtomicBoolean(false);
        AtomicBoolean started = new AtomicBoolean(false);
        AtomicBoolean interrupt = new AtomicBoolean(false);

        boolean canStart() {
            return !started.get() && !interrupt.get();
        }

        boolean canInterrupt() {
            return started.get() && !interrupt.get();
        }

        @Override
        public String toString() {
            return "RebuildInfo{" +
                    "messageId=" + messageId +
                    ", textChatId=" + textChatId +
                    ", active=" + active +
                    ", started=" + started +
                    ", interrupt=" + interrupt +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RebuildInfo that = (RebuildInfo) o;
            return messageId == that.messageId &&
                    textChatId == that.textChatId &&
                    Objects.equals(active, that.active) &&
                    Objects.equals(started, that.started) &&
                    Objects.equals(interrupt, that.interrupt);
        }

        @Override
        public int hashCode() {
            return Objects.hash(messageId, textChatId, active, started, interrupt);
        }
    }
}
