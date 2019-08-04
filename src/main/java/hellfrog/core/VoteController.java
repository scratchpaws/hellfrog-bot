package hellfrog.core;

import hellfrog.settings.ActiveVote;
import hellfrog.settings.SettingsController;
import hellfrog.settings.VotePoint;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.emoji.KnownCustomEmoji;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageDecoration;
import org.javacord.api.entity.message.Reaction;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class VoteController
        implements Runnable {

    private ScheduledFuture<?> scheduledFuture;

    public VoteController() {
        ScheduledExecutorService voiceService = Executors.newSingleThreadScheduledExecutor();
        scheduledFuture = voiceService.scheduleWithFixedDelay(this, 60L, 60L, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        SettingsController settingsController = SettingsController.getInstance();
        DiscordApi discordApi = settingsController.getDiscordApi();
        if (discordApi == null) return;

        for (long knownServerId : settingsController.getServerListWithConfig()) {
            List<ActiveVote> activeVotes = settingsController.getServerPreferences(knownServerId).getActiveVotes();
            for (ActiveVote activeVote : activeVotes) {
                if (activeVote.isHasTimer()) {
                    Instant now = Instant.now();
                    Instant voteEndDate = Instant.ofEpochSecond(activeVote.getEndDate());
                    long estimateMinutes = ChronoUnit.MINUTES.between(now, voteEndDate);
                    if (estimateMinutes <= 0) {
                        interruptVote(knownServerId, activeVote.getId());
                    }
                } else {
                    if (activeVote.getTextChatId() == null) {
                        interruptVote(knownServerId, activeVote.getId());
                    } else {
                        if (discordApi.getServerTextChannelById(activeVote.getTextChatId()).isEmpty()) {
                            interruptVote(knownServerId, activeVote.getId());
                        }
                    }
                }
            }
        }
    }

    public void interruptVote(final long serverId, final short voteId) {
        CompletableFuture.runAsync(() -> parallelInterrupt(serverId, voteId));
    }

    private void parallelInterrupt(long serverId, short voteId) {
        SettingsController settingsController = SettingsController.getInstance();
        DiscordApi discordApi = settingsController.getDiscordApi();
        if (discordApi == null)
            return;

        List<ActiveVote> activeVotes = settingsController.getServerPreferences(serverId).getActiveVotes();
        for (ActiveVote activeVote : activeVotes) {
            if (activeVote.getId() == voteId) {

                try {
                    if (activeVote.getTextChatId() != null &&
                            activeVote.getMessageId() != null &&
                            activeVote.getVotePoints() != null &&
                            activeVote.getReadableVoteText() != null) {

                        Map<VotePoint, Integer> pointsLevel = new HashMap<>();
                        for (VotePoint point : activeVote.getVotePoints()) {
                            pointsLevel.put(point, 0);
                        }

                        Optional<ServerTextChannel> mayBeChannel = discordApi
                                .getServerTextChannelById(activeVote.getTextChatId());
                        if (mayBeChannel.isPresent()) {
                            ServerTextChannel channel = mayBeChannel.get();
                            Message voteMsg = channel.getMessageById(activeVote.getMessageId()).join();

                            List<Long> alreadyVoted = new ArrayList<>();
                            Map<Long, KnownCustomEmoji> emojiCache = new HashMap<>();

                            activeVote.getVotePoints()
                                    .stream()
                                    .filter(VotePoint::isCustomEmojiVP)
                                    .filter(vp -> vp.getCustomEmoji() != null && vp.getCustomEmoji() > 0)
                                    .map(vp -> channel.getServer().getCustomEmojiById(vp.getCustomEmoji()))
                                    .filter(Optional::isPresent)
                                    .map(Optional::get)
                                    .forEach(kqe -> emojiCache.put(kqe.getId(), kqe));

                            TreeSet<Long> globalVoted = new TreeSet<>();
                            boolean withDefaultPoint = (activeVote.isWithDefaultPoint()
                                    || activeVote.getWinThreshold() > 0)
                                    && activeVote.getVotePoints().size() > 0;

                            for (Reaction reaction : voteMsg.getReactions()) {

                                Emoji emoji = reaction.getEmoji();
                                VotePoint associatePoint = null;
                                for (VotePoint point : activeVote.getVotePoints()) {
                                    if (point.equalsEmoji(emoji)) {
                                        associatePoint = point;
                                        break;
                                    }
                                }

                                if (associatePoint == null) {
                                    continue;
                                }

                                if (withDefaultPoint && activeVote.getVotePoints().get(0).equalsEmoji(emoji)) {
                                    continue;
                                    // дефолтный обсчитываем далее
                                }

                                int addToPoint = 0;

                                List<User> votedUsers = reaction.getUsers().join();
                                for (User votedUser : votedUsers) {
                                    if (votedUser.isBot()) continue;
                                    if (activeVote.isExceptionalVote() &&
                                            alreadyVoted.contains(votedUser.getId())) continue;

                                    if (activeVote.getRolesFilter() != null && activeVote.getRolesFilter().size() > 0) {
                                        for (Role role : votedUser.getRoles(channel.getServer())) {
                                            if (activeVote.getRolesFilter().contains(role.getId())) {
                                                addToPoint++;
                                                alreadyVoted.add(votedUser.getId());
                                                break;
                                            }
                                        }
                                    } else {
                                        alreadyVoted.add(votedUser.getId());
                                        addToPoint++;
                                    }
                                }
                                List<Long> collectedUserIds = votedUsers.stream()
                                        .map(User::getId)
                                        .collect(Collectors.toList());
                                globalVoted.addAll(collectedUserIds);

                                int currentPoints = pointsLevel.get(associatePoint);
                                currentPoints += addToPoint;
                                pointsLevel.put(associatePoint, currentPoints);
                            }

                            if (withDefaultPoint) {
                                VotePoint defaultPoint = activeVote.getVotePoints().get(0);
                                long noCaresUser = channel.getServer().getMembers().stream()
                                        .filter(m -> !m.isBot())
                                        .filter(m -> {
                                            Collection<PermissionType> pt = channel.getEffectiveAllowedPermissions(m);
                                            return pt.contains(PermissionType.READ_MESSAGES)
                                                    && pt.contains(PermissionType.ADD_REACTIONS);
                                        }).filter(m -> {
                                            if (activeVote.getRolesFilter() != null && activeVote.getRolesFilter().size() > 0) {
                                                for (Role role : m.getRoles(channel.getServer())) {
                                                    if (activeVote.getRolesFilter().contains(role.getId())) {
                                                        return true;
                                                    }
                                                }
                                                return false;
                                            }
                                            return true;
                                        }).filter(m -> !globalVoted.contains(m.getId()))
                                        .count();
                                pointsLevel.put(defaultPoint, (int) noCaresUser);
                            }

                            MessageBuilder result = new MessageBuilder()
                                    .append("Voting is over:", MessageDecoration.BOLD)
                                    .append("  ")
                                    .append(activeVote.getReadableVoteText())
                                    .appendNewLine()
                                    .append("Results:", MessageDecoration.BOLD)
                                    .appendNewLine();

                            int winnerLevel = 0;
                            for (Map.Entry<VotePoint, Integer> pointResult : pointsLevel.entrySet()) {
                                int level = pointResult.getValue();
                                VotePoint point = pointResult.getKey();
                                winnerLevel = Math.max(winnerLevel, level);

                                buildVotePointString(emojiCache, result, point);
                                result.append(" -- votes: ")
                                        .append(level)
                                        .appendNewLine();
                            }
                            if (winnerLevel == 0) {
                                result.append("Nobody cares", MessageDecoration.BOLD);
                            } else {
                                List<VotePoint> winners = new ArrayList<>();
                                if (activeVote.getWinThreshold() > 0 && withDefaultPoint) {
                                    long threshold = activeVote.getWinThreshold();
                                    List<VotePoint> points = activeVote.getVotePoints();
                                    VotePoint defPoint = points.get(0);
                                    VotePoint selectable = points.get(1);
                                    int getForSelectable = pointsLevel.get(selectable);
                                    if (getForSelectable >= threshold) {
                                        winners.add(selectable);
                                    } else {
                                        winners.add(defPoint);
                                    }
                                } else {
                                    for (Map.Entry<VotePoint, Integer> pointResult : pointsLevel.entrySet()) {
                                        if (pointResult.getValue() == winnerLevel)
                                            winners.add(pointResult.getKey());
                                    }
                                }
                                if (winners.size() == 1) {
                                    result.append("Winner:", MessageDecoration.BOLD)
                                            .appendNewLine();
                                } else if (winners.size() == pointsLevel.size()) {
                                    result.append("Draw:", MessageDecoration.BOLD)
                                            .appendNewLine();
                                } else {
                                    result.append("Multiple winners: ", MessageDecoration.BOLD)
                                            .appendNewLine();
                                }
                                for (VotePoint point : winners) {
                                    buildVotePointString(emojiCache, result, point);
                                    result.appendNewLine();
                                }
                            }
                            result.send(channel);
                        }
                    }
                } catch (Exception err) {
                    err.printStackTrace();
                } finally {
                    activeVotes.remove(activeVote);
                    settingsController.saveServerSideParameters(serverId);
                }
            }
        }
    }

    private void buildVotePointString(Map<Long, KnownCustomEmoji> emojiCache, MessageBuilder result, VotePoint point) {
        result.append("  *")
                .append(point.buildVoteString(emojiCache));
    }

    public void stop() {
        scheduledFuture.cancel(false);
        while (!scheduledFuture.isCancelled() || !scheduledFuture.isDone()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException brk) {
                scheduledFuture.cancel(true);
            }
        }
    }
}
