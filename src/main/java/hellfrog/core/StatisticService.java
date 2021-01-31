package hellfrog.core;

import hellfrog.common.CommonUtils;
import hellfrog.common.LongEmbedMessage;
import hellfrog.core.statistic.summary.SummaryChannelStatistic;
import hellfrog.core.statistic.summary.SummaryEmojiStatistic;
import hellfrog.core.statistic.summary.SummaryReport;
import hellfrog.core.statistic.summary.SummaryUserStatistic;
import hellfrog.settings.db.ServerPreferencesDAO;
import hellfrog.settings.db.TotalStatisticDAO;
import hellfrog.settings.db.entity.EmojiTotalStatistic;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.emoji.KnownCustomEmoji;
import org.javacord.api.entity.message.MessageAttachment;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.MessageDecoration;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.MessageDeleteEvent;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.event.message.reaction.ReactionRemoveEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.listener.message.MessageDeleteListener;
import org.javacord.api.listener.message.reaction.ReactionAddListener;
import org.javacord.api.listener.message.reaction.ReactionRemoveListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import org.ocpsoft.prettytime.PrettyTime;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StatisticService
        implements ReactionAddListener, ReactionRemoveListener, MessageCreateListener, MessageDeleteListener {

    private static final Pattern CUSTOM_EMOJI_SEARCH = Pattern.compile("<a?:.+?:\\d+>", Pattern.MULTILINE);
    private static final String TREE_MIDDLE = "├";
    private static final String TREE_END = "└";
    private static final String TREE_VERTICAL = "│";

    private final TotalStatisticDAO totalStatisticDAO;
    private final ServerPreferencesDAO serverPreferencesDAO;
    private final NameCacheService nameCacheService;

    public StatisticService(@NotNull final TotalStatisticDAO totalStatisticDAO,
                            @NotNull final ServerPreferencesDAO serverPreferencesDAO,
                            @NotNull final NameCacheService nameCacheService) {
        this.totalStatisticDAO = totalStatisticDAO;
        this.serverPreferencesDAO = serverPreferencesDAO;
        this.nameCacheService = nameCacheService;
    }

    @Override
    public void onReactionAdd(ReactionAddEvent event) {
        parseReaction(event, true);
    }

    @Override
    public void onReactionRemove(ReactionRemoveEvent event) {
        parseReaction(event, false);
    }

    private void parseReaction(@NotNull SingleReactionEvent event, final boolean isAdd) {
        if (event.getServer().isPresent()) {
            Server server = event.getServer().get();
            if (isStatisticEnabled(server)) {
                if (event.getUser().isPresent()) {
                    User user = event.getUser().get();
                    if (user.isBot()) {
                        return;
                    }
                    event.getEmoji()
                            .asCustomEmoji()
                            .flatMap(e -> server.getCustomEmojiById(e.getId()))
                            .map(KnownCustomEmoji::getId)
                            .ifPresent(emojiId -> {
                                if (isAdd) {
                                    totalStatisticDAO.incrementEmoji(server.getId(), emojiId);
                                } else {
                                    totalStatisticDAO.decrementEmoji(server.getId(), emojiId);
                                }
                            });
                }
            }
        }
    }

    @Override
    public void onMessageCreate(MessageCreateEvent event) {
        event.getServerTextChannel().ifPresent(serverTextChannel -> {
            if (isStatisticEnabled(serverTextChannel.getServer())) {
                event.getMessageAuthor().asUser().ifPresent(author -> {
                    if (author.isBot()) {
                        return;
                    }
                    String messageContent = event.getMessageContent();
                    Instant messageDate = event.getMessage().getCreationTimestamp();
                    int messageLength = event.getMessage().getReadableContent().length();
                    long bytesCount = event.getMessage().getAttachments().stream()
                            .mapToLong(MessageAttachment::getSize)
                            .sum();
                    parseMessage(serverTextChannel, author, messageContent, messageDate, messageLength, bytesCount, true);
                });
            }
        });
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        event.getServerTextChannel()
                .ifPresent(serverTextChannel -> {
                    if (isStatisticEnabled(serverTextChannel.getServer())) {
                        event.getMessageAuthor()
                                .flatMap(MessageAuthor::asUser)
                                .ifPresent(author ->
                                        event.getMessage().ifPresent(message -> {
                                            if (author.isBot()) {
                                                return;
                                            }
                                            String messageContent = message.getContent();
                                            Instant messageDate = message.getCreationTimestamp();
                                            int messageLength = message.getReadableContent().length();
                                            long bytesCount = message.getAttachments().stream()
                                                    .mapToLong(MessageAttachment::getSize)
                                                    .sum();
                                            parseMessage(serverTextChannel, author, messageContent,
                                                    messageDate, messageLength, bytesCount, false);
                                        }));
                    }
                });
    }

    private void parseMessage(@NotNull final ServerTextChannel serverTextChannel,
                              @NotNull final User author,
                              @NotNull final String messageContent,
                              @NotNull final Instant messageDate,
                              final int messageLength,
                              final long bytesCount,
                              final boolean isAdd) {

        final long serverId = serverTextChannel.getServer().getId();
        final long channelId = serverTextChannel.getId();
        final long userId = author.getId();

        if (isAdd) {
            totalStatisticDAO.incrementChannelStatsWithDate(serverId, channelId, userId, messageDate, messageLength, bytesCount);
        } else {
            totalStatisticDAO.decrementChannelStats(serverId, channelId, userId, messageLength, bytesCount);
        }

        Matcher smileMatcher = CUSTOM_EMOJI_SEARCH.matcher(messageContent);
        while (smileMatcher.find()) {
            String matched = smileMatcher.group();
            String[] sub = matched.split(":");
            if (sub.length == 3) {
                long customSmileId = CommonUtils.onlyNumbersToLong(sub[2]);
                serverTextChannel.getServer().getCustomEmojiById(customSmileId)
                        .map(KnownCustomEmoji::getId)
                        .ifPresent(emojiId -> {
                            if (isAdd) {
                                totalStatisticDAO.incrementEmojiWithDate(serverId, emojiId, messageDate);
                            } else {
                                totalStatisticDAO.decrementEmoji(serverId, emojiId);
                            }
                        });
            }
        }
    }

    public boolean isStatisticEnabled(@NotNull final Server server) {
        return serverPreferencesDAO.isStatisticEnabled(server.getId());
    }

    public void enableStatistic(@NotNull final Server server) {
        if (!serverPreferencesDAO.isStatisticEnabled(server.getId())) {
            serverPreferencesDAO.setStatisticEnabled(server.getId(), true);
            serverPreferencesDAO.setStatisticStartDate(server.getId(), Instant.now());
        }
    }

    public void disableStatistic(@NotNull final Server server) {
        if (serverPreferencesDAO.isStatisticEnabled(server.getId())) {
            serverPreferencesDAO.setStatisticEnabled(server.getId(), false);
        }
    }

    public void resetStatistic(@NotNull final Server server) {
        totalStatisticDAO.reset(server.getId());
        serverPreferencesDAO.setStatisticStartDate(server.getId(), Instant.now());
    }

    public Instant getStatisticStartDate(@NotNull final Server server) {
        return serverPreferencesDAO.getStatisticStartDate(server.getId());
    }

    public boolean hasStatisticStartDate(@NotNull final Server server) {
        return !serverPreferencesDAO.getStatisticStartDate(server.getId()).equals(ServerPreferencesDAO.STATISTIC_START_DATE_DEFAULT);
    }

    public SummaryReport getSummaryReport(@NotNull final Server server,
                                          @Nullable final Collection<KnownCustomEmoji> emojiFilter,
                                          @Nullable final Collection<ServerTextChannel> channelsFilter,
                                          @Nullable final Collection<User> usersFilter) {


        final List<EmojiTotalStatistic> emojiTotalStatistics = totalStatisticDAO.getEmojiUsagesStatistic(server.getId());
        final List<Long> emojiFilterIds = getEntitiesIds(emojiFilter);
        final TreeSet<KnownCustomEmoji> nonUsedEmoji = new TreeSet<>((o1, o2) -> o1.getName().compareTo(o2.getName()) * (-1));
        final TreeSet<SummaryEmojiStatistic> emojiSummaryStatistic = new TreeSet<>(Comparator.reverseOrder());
        final List<KnownCustomEmoji> usedEmoji = new ArrayList<>();

        emojiTotalStatistics.forEach(emojiTotalStatistic -> {
            Optional<KnownCustomEmoji> mayBeEmoji = server.getCustomEmojiById(emojiTotalStatistic.getEmojiId());
            if (mayBeEmoji.isEmpty()) {
                totalStatisticDAO.removeEmojiStats(server.getId(), emojiTotalStatistic.getEmojiId());
            } else {
                KnownCustomEmoji emoji = mayBeEmoji.get();
                if (!emojiFilterIds.isEmpty() && !emojiFilterIds.contains(emoji.getId())) {
                    return;
                }
                if (emojiTotalStatistic.getUsagesCount() <= 0L) {
                    nonUsedEmoji.add(emoji);
                } else {
                    usedEmoji.add(emoji);
                    SummaryEmojiStatistic statistic = new SummaryEmojiStatistic();
                    statistic.setEmoji(emoji);
                    statistic.setUsagesCount(emojiTotalStatistic.getUsagesCount());
                    statistic.setLastUsage(emojiTotalStatistic.getLastUsage().toInstant());
                    emojiSummaryStatistic.add(statistic);
                }
            }
        });

        server.getCustomEmojis().forEach(emoji -> {
            if (!emojiFilterIds.isEmpty() && !emojiFilterIds.contains(emoji.getId())) {
                return;
            }
            if (!usedEmoji.contains(emoji)) {
                nonUsedEmoji.add(emoji);
            }
        });

        final List<Long> usersFilterIds = getEntitiesIds(usersFilter);
        final List<Long> channelFilterIds = getEntitiesIds(channelsFilter);
        final Map<Long, SummaryChannelStatistic> channelSummaryStatistics = new HashMap<>();
        final Map<Long, SummaryUserStatistic> usersSummaryStatistics = new HashMap<>();

        totalStatisticDAO.getChannelsStatistics(server.getId()).forEach(textChannelTotalStatistic -> {
            if (!usersFilterIds.isEmpty() && !usersFilterIds.contains(textChannelTotalStatistic.getUserId())) {
                return;
            }
            if (!channelFilterIds.isEmpty() && !channelFilterIds.contains(textChannelTotalStatistic.getTextChannelId())) {
                return;
            }
            SummaryUserStatistic channelUserStatistic = new SummaryUserStatistic();
            channelUserStatistic.setMessagesCount(textChannelTotalStatistic.getMessagesCount());
            channelUserStatistic.setSymbolsCount(textChannelTotalStatistic.getSymbolsCount());
            channelUserStatistic.setBytesCount(textChannelTotalStatistic.getBytesCount());
            if (channelUserStatistic.getSummaryCount() <= 0L) {
                return;
            }
            channelUserStatistic.setUserId(textChannelTotalStatistic.getUserId());
            channelUserStatistic.setLastMessageDate(textChannelTotalStatistic.getLastMessageDate().toInstant());
            Optional<User> mayBeMember = server.getMemberById(textChannelTotalStatistic.getUserId());
            if (mayBeMember.isPresent()) {
                if (mayBeMember.get().isBot()) {
                    return;
                }
                channelUserStatistic.setUser(mayBeMember.get());
                channelUserStatistic.setUserPresent(true);
                channelUserStatistic.setLastKnownNick(mayBeMember.get().getDisplayName(server));
                channelUserStatistic.setDiscriminationName(mayBeMember.get().getDiscriminatedName());
            } else {
                channelUserStatistic.setUserPresent(false);
                String lastKnownNick = nameCacheService.findLastKnownName(server, textChannelTotalStatistic.getUserId()).orElse(null);
                String discriminationName = nameCacheService.findLastKnownName(textChannelTotalStatistic.getUserId()).orElse(null);
                channelUserStatistic.setLastKnownNick(lastKnownNick);
                channelUserStatistic.setDiscriminationName(discriminationName);
            }
            if (!channelSummaryStatistics.containsKey(textChannelTotalStatistic.getTextChannelId())) {
                SummaryChannelStatistic channelStatistic = new SummaryChannelStatistic();
                channelStatistic.setChannelId(textChannelTotalStatistic.getTextChannelId());
                Optional<ServerTextChannel> mayBeChannel = server.getTextChannelById(textChannelTotalStatistic.getTextChannelId());
                if (mayBeChannel.isPresent()) {
                    channelStatistic.setTextChannel(mayBeChannel.get());
                    channelStatistic.setTextChannelPresent(true);
                    channelStatistic.setLastKnownName("#" + mayBeChannel.get().getName());
                } else {
                    channelStatistic.setTextChannelPresent(false);
                    channelStatistic.setLastKnownName(nameCacheService.printEntityDetailed(textChannelTotalStatistic.getTextChannelId(), server));
                }
                channelSummaryStatistics.put(textChannelTotalStatistic.getTextChannelId(), channelStatistic);
            }
            channelSummaryStatistics.get(textChannelTotalStatistic.getTextChannelId())
                    .getSummaryUserStatistics()
                    .add(channelUserStatistic);
            if (!usersSummaryStatistics.containsKey(textChannelTotalStatistic.getUserId())) {
                SummaryUserStatistic userStatistic = new SummaryUserStatistic();
                userStatistic.setUserId(channelUserStatistic.getUserId());
                userStatistic.setUser(channelUserStatistic.getUser());
                userStatistic.setLastKnownNick(channelUserStatistic.getLastKnownNick());
                userStatistic.setDiscriminationName(channelUserStatistic.getDiscriminationName());
                userStatistic.setUserPresent(channelUserStatistic.isUserPresent());
                usersSummaryStatistics.put(textChannelTotalStatistic.getUserId(), userStatistic);
            }
            SummaryUserStatistic userStatistic = usersSummaryStatistics.get(textChannelTotalStatistic.getUserId());
            userStatistic.setMessagesCount(userStatistic.getMessagesCount() + textChannelTotalStatistic.getMessagesCount());
            userStatistic.setSymbolsCount(userStatistic.getSymbolsCount() + textChannelTotalStatistic.getSymbolsCount());
            userStatistic.setBytesCount(userStatistic.getBytesCount() + textChannelTotalStatistic.getBytesCount());
            Instant lastDate = textChannelTotalStatistic.getLastMessageDate().toInstant();
            if (userStatistic.getLastMessageDate() == null || userStatistic.getLastMessageDate().isBefore(lastDate)) {
                userStatistic.setLastMessageDate(lastDate);
            }
        });

        SummaryReport report = new SummaryReport();
        report.setEmojiSummaryStatistic(emojiSummaryStatistic);
        report.setNonUsedEmoji(nonUsedEmoji);
        report.setSummaryChannelStatistics(new TreeSet<>(Comparator.reverseOrder()));
        report.getSummaryChannelStatistics().addAll(channelSummaryStatistics.values());
        report.setSummaryUserStatistics(new TreeSet<>(Comparator.reverseOrder()));
        report.getSummaryUserStatistics().addAll(usersSummaryStatistics.values());
        report.setHasSinceDate(hasStatisticStartDate(server));
        report.setSinceDate(getStatisticStartDate(server));
        return report;
    }

    public LongEmbedMessage formatForMessage(@NotNull final SummaryReport summaryReport,
                                             final boolean emojiFilter,
                                             final boolean usersFilter,
                                             final boolean channelsFilter) {

        PrettyTime prettyTime = new PrettyTime(new Locale("en"));

        LongEmbedMessage message = new LongEmbedMessage()
                .append("Message statistics:", MessageDecoration.BOLD).appendNewLine();

        if (summaryReport.isHasSinceDate()) {
            message.append("Collected since: ").append(prettyTime.format(summaryReport.getSinceDate())).appendNewLine();
        }
        if ((!summaryReport.getEmojiSummaryStatistic().isEmpty() || !summaryReport.getNonUsedEmoji().isEmpty())
                && !channelsFilter && !usersFilter) {
            message.append("Server emoji usages:").appendNewLine();
            int i = 0;
            for (SummaryEmojiStatistic emojiTotalStatistic : summaryReport.getEmojiSummaryStatistic()) {
                if (i < (summaryReport.getEmojiSummaryStatistic().size() - 1)) {
                    message.append(TREE_MIDDLE);
                } else {
                    message.append(TREE_END);
                }
                message.append(emojiTotalStatistic.getUsagesCount())
                        .append(" : ")
                        .append(emojiTotalStatistic.getEmoji())
                        .append(" (")
                        .append("last used ")
                        .append(prettyTime.format(emojiTotalStatistic.getLastUsage()))
                        .append(")")
                        .appendNewLine();
                i++;
            }
            summaryReport.getNonUsedEmoji().stream()
                    .map(KnownCustomEmoji::getMentionTag)
                    .reduce(CommonUtils::reduceConcat)
                    .ifPresent(nonUsed -> message.append("Emoji that has never been used:").appendNewLine()
                            .append(nonUsed).appendNewLine());
        }
        if (!summaryReport.getSummaryUserStatistics().isEmpty() && !emojiFilter && !channelsFilter) {
            message.append("User message statistics:", MessageDecoration.CODE_SIMPLE).appendNewLine();
            int i = 0;
            for (SummaryUserStatistic summaryUserStatistic : summaryReport.getSummaryUserStatistics()) {
                boolean treeEnd = i >= summaryReport.getSummaryUserStatistics().size() - 1;
                appendUserSummaryStatistic(message, summaryUserStatistic, 0, treeEnd, false, prettyTime);
                i++;
            }
        }

        if (!summaryReport.getSummaryChannelStatistics().isEmpty() && !emojiFilter) {
            message.append("Text chat message statistics:", MessageDecoration.CODE_SIMPLE).appendNewLine();
            int channels = 0;
            for (SummaryChannelStatistic channelStatistic : summaryReport.getSummaryChannelStatistics()) {
                boolean channelsTreeEnd = channels >= summaryReport.getSummaryChannelStatistics().size() - 1;
                if (channelsTreeEnd) {
                    message.append(TREE_END);
                } else {
                    message.append(TREE_MIDDLE);
                }
                if (channelStatistic.getLastKnownName() != null) {
                    message.append(ServerSideResolver.getReadableContent(channelStatistic.getLastKnownName(), Optional.empty()));
                } else {
                    message.append("(unknown channel name)");
                }

                message.append("; ");
                message.append("messages: ").append(channelStatistic.getMessagesCount())
                        .append("; symbols: ").append(channelStatistic.getSummaryCount())
                        .append("; attaches: ").append(CommonUtils.humanReadableByteCount(channelStatistic.getBytesCount(), true))
                        .append("; last message: ").append(prettyTime.format(channelStatistic.getLastMessageDate()));
                if (!channelStatistic.isTextChannelPresent()) {
                    message.append("; [text channel not present on server]");
                }
                message.appendNewLine();
                int users = 0;
                for (SummaryUserStatistic userStatistic : channelStatistic.getSummaryUserStatistics()) {
                    boolean treeEnd = users >= channelStatistic.getSummaryUserStatistics().size() - 1;
                    appendUserSummaryStatistic(message, userStatistic, 1, treeEnd, channelsTreeEnd, prettyTime);
                    users++;
                }
                channels++;
            }
        }
        return message;
    }

    private void appendUserSummaryStatistic(@NotNull final LongEmbedMessage message,
                                            @NotNull final SummaryUserStatistic summaryUserStatistic,
                                            final int tabCount,
                                            final boolean treeEnd,
                                            final boolean channelsTreeEnd,
                                            @NotNull final PrettyTime prettyTime) {

        for (int i = 0; i < tabCount; i++) {
            if (channelsTreeEnd) {
                message.append("  ");
            } else {
                message.append(TREE_VERTICAL);
            }
        }

        if (treeEnd) {
            message.append(TREE_END);
        } else {
            message.append(TREE_MIDDLE);
        }


        if (summaryUserStatistic.getLastKnownNick() != null) {
            message.append(ServerSideResolver.getReadableContent(summaryUserStatistic.getLastKnownNick(), Optional.empty()));
        } else {
            message.append("(unknown nickname)");
        }
        message.append("; DN: ");
        if (summaryUserStatistic.getDiscriminationName() != null) {
            message.append(ServerSideResolver.getReadableContent(summaryUserStatistic.getDiscriminationName(), Optional.empty()));
        } else {
            message.append("(unknown discriminated name)");
        }
        message.append("; ");

        message.append("messages: ").append(summaryUserStatistic.getMessagesCount())
                .append("; symbols: ").append(summaryUserStatistic.getSummaryCount())
                .append("; attaches: ").append(CommonUtils.humanReadableByteCount(summaryUserStatistic.getBytesCount(), true))
                .append("; last message: ").append(prettyTime.format(summaryUserStatistic.getLastMessageDate()));
        if (!summaryUserStatistic.isUserPresent()) {
            message.append("; [user not present on server]");
        }
        message.appendNewLine();
    }

    @NotNull
    @UnmodifiableView
    private <T extends DiscordEntity> List<Long> getEntitiesIds(@Nullable Collection<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream()
                .map(DiscordEntity::getId)
                .sorted()
                .collect(Collectors.toUnmodifiableList());
    }
}
