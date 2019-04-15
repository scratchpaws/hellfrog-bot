package xyz.funforge.scratchypaws.hellfrog.settings.old;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.user.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.funforge.scratchypaws.hellfrog.common.CommonUtils;
import xyz.funforge.scratchypaws.hellfrog.common.MessageUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class ServerStatistic {

    @JsonIgnore
    private final ReentrantLock createSmileStatLock = new ReentrantLock();
    @JsonIgnore
    private final ReentrantLock createUserStatLock = new ReentrantLock();
    @JsonIgnore
    private final ReentrantLock createTextChatStatLock = new ReentrantLock();
    private volatile Boolean collectNonDefaultSmileStats = Boolean.FALSE;
    private volatile ConcurrentHashMap<Long, SmileStatistic> nonDefaultSmileStats = new ConcurrentHashMap<>();
    private volatile ConcurrentHashMap<Long, MessageStatistic> userMessagesStats = new ConcurrentHashMap<>();
    private volatile ConcurrentHashMap<Long, MessageStatistic> textChatStats = new ConcurrentHashMap<>();
    private volatile Long startDate = 0L;

    @JsonIgnore
    public static void appendResultStats(@NotNull MessageBuilder msg, @NotNull TreeMap<Long, List<String>> stats,
                                         int innerLevel) {
        stats.forEach((key, value) ->
                value.forEach((item) -> {
                    for (int i = 0; i < innerLevel; i++) {
                        msg.append(".   ");
                    }
                    msg.append(item).appendNewLine();
                }));
    }

    public Long getStartDate() {
        return startDate != null ? startDate : 0L;
    }

    public void setStartDate(Long startDate) {
        this.startDate = startDate != null ? startDate : 0L;
    }

    public boolean isCollectNonDefaultSmileStats() {
        return collectNonDefaultSmileStats != null && collectNonDefaultSmileStats;
    }

    public void setCollectNonDefaultSmileStats(boolean collectNonDefaultSmileStats) {
        this.collectNonDefaultSmileStats = collectNonDefaultSmileStats;
    }

    public ConcurrentHashMap<Long, SmileStatistic> getNonDefaultSmileStats() {
        return nonDefaultSmileStats;
    }

    public void setNonDefaultSmileStats(ConcurrentHashMap<Long, SmileStatistic> nonDefaultSmileStats) {
        this.nonDefaultSmileStats = nonDefaultSmileStats;
    }

    public ConcurrentHashMap<Long, MessageStatistic> getUserMessagesStats() {
        return userMessagesStats;
    }

    public void setUserMessagesStats(ConcurrentHashMap<Long, MessageStatistic> userMessagesStats) {
        this.userMessagesStats = userMessagesStats;
    }

    public ConcurrentHashMap<Long, MessageStatistic> getTextChatStats() {
        return textChatStats;
    }

    public void setTextChatStats(ConcurrentHashMap<Long, MessageStatistic> textChatStats) {
        this.textChatStats = textChatStats;
    }

    @JsonIgnore
    public SmileStatistic getSmileStatistic(long emojiId) {
        if (nonDefaultSmileStats.containsKey(emojiId)) {
            return nonDefaultSmileStats.get(emojiId);
        } else {
            createSmileStatLock.lock();
            try {
                if (!nonDefaultSmileStats.containsKey(emojiId)) {
                    SmileStatistic smileStatistic = new SmileStatistic();
                    nonDefaultSmileStats.put(emojiId, smileStatistic);
                }
                return nonDefaultSmileStats.get(emojiId);
            } finally {
                createSmileStatLock.unlock();
            }
        }
    }

    @JsonIgnore
    public MessageStatistic getUserMessageStatistic(User user) {
        return getMessageStats(userMessagesStats,
                user.getId(), user.getDiscriminatedName(),
                createUserStatLock);
    }

    @JsonIgnore
    public MessageStatistic getTextChatStatistic(ServerTextChannel textChannel) {
        return getMessageStats(textChatStats,
                textChannel.getId(), textChannel.getName(),
                createTextChatStatLock);
    }

    @JsonIgnore
    private MessageStatistic getMessageStats(ConcurrentHashMap<Long, MessageStatistic> statsMap,
                                             long entityId, String entityName,
                                             ReentrantLock locker) {
        if (statsMap.containsKey(entityId)) {
            return statsMap.get(entityId);
        } else {
            locker.lock();
            try {
                if (!statsMap.containsKey(entityId)) {
                    MessageStatistic messageStatistic = new MessageStatistic();
                    messageStatistic.setEntityId(entityId);
                    messageStatistic.setLastKnownName(entityName);
                    statsMap.put(entityId, messageStatistic);
                }
                return statsMap.get(entityId);
            } finally {
                locker.unlock();
            }
        }
    }

    public void clear() {
        if (nonDefaultSmileStats != null)
            nonDefaultSmileStats.clear();
        if (userMessagesStats != null)
            userMessagesStats.clear();
        if (textChatStats != null)
            textChatStats.clear();
    }

    @JsonIgnore
    public TreeMap<Long, List<String>> buildUserStats(List<User> userList) {
        return buildStatLines(userMessagesStats, userList, null, 1);
    }

    @JsonIgnore
    public TreeMap<Long, List<String>> buildTextChatStats(List<ServerTextChannel> channelList,
                                                          List<User> userList) {
        return buildStatLines(textChatStats, userList, channelList, 1);
    }

    @JsonIgnore
    private TreeMap<Long, List<String>> buildStatLines(@Nullable ConcurrentHashMap<Long, MessageStatistic> statMap,
                                                       @NotNull List<User> userList, @Nullable List<ServerTextChannel> channelList,
                                                       int innerLevel) {

        TreeMap<Long, List<String>> result = new TreeMap<>(Comparator.reverseOrder());

        if (statMap == null) return result;

        statMap.forEach((id, stat) -> {
            boolean isUserStat = stat.getChildStatistic() == null || stat.getChildStatistic().isEmpty();

            if (isUserStat && userList.size() > 0) {
                boolean found = userList.stream()
                        .anyMatch(u -> u.getId() == id);
                if (!found)
                    return;
            }

            if (!isUserStat && channelList != null && channelList.size() > 0) {
                boolean found = channelList.stream()
                        .anyMatch(ch -> ch.getId() == id);
                if (!found)
                    return;
            }

            if (userList.size() > 0 && !isUserStat) {
                boolean hasThisUsersStat = userList.stream()
                        .anyMatch(u -> stat.getChildItemStatistic(u.getId()).getCountOfMessages() > 0);
                if (!hasThisUsersStat)
                    return;
            }

            long summaryCount = stat.getSummaryCount();
            long messagesCount = stat.getCountOfMessages();
            long totalSymbolsCount = stat.getCountOfSymbols();
            long totalBytesCount = stat.getCountOfBytes();

            if (summaryCount > 0L) {
                MessageBuilder tmp = new MessageBuilder()
                        .append(String.valueOf(messagesCount))
                        .append(" - ")
                        .append(MessageUtils.escapeSpecialSymbols(stat.getLastKnownName()))
                        .append(", total symbols: ").append(totalSymbolsCount)
                        .append(", total attaches: ")
                        .append(CommonUtils.humanReadableByteCount(totalBytesCount, false))
                        .append(", last message: ");
                String lastMessageDate = stat.getLastDate();
                if (!CommonUtils.isTrStringEmpty(lastMessageDate)) {
                    tmp.append(lastMessageDate);
                } else {
                    tmp.append(" (unknown)");
                }

                if (stat.getChildStatistic() != null && stat.getChildStatistic().size() > 0) {
                    TreeMap<Long, List<String>> innerResult = buildStatLines(stat.getChildStatistic(),
                            userList, channelList, innerLevel + 1);
                    tmp.appendNewLine();
                    appendResultStats(tmp, innerResult, innerLevel + 1);
                }

                if (!result.containsKey(summaryCount)) {
                    result.put(summaryCount, new ArrayList<>());
                }

                result.get(summaryCount).add(tmp.getStringBuilder().toString());
            }
        });

        return result;
    }
}
