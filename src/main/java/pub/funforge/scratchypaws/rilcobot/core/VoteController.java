package pub.funforge.scratchypaws.rilcobot.core;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.emoji.CustomEmoji;
import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageDecoration;
import org.javacord.api.entity.message.Reaction;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
import pub.funforge.scratchypaws.rilcobot.settings.SettingsController;
import pub.funforge.scratchypaws.rilcobot.settings.old.ActiveVote;
import pub.funforge.scratchypaws.rilcobot.settings.old.VotePoint;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;

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
                            Map<Long, CustomEmoji> emojiCache = new HashMap<>();
                            for (Reaction reaction : voteMsg.getReactions()) {

                                Emoji emoji = reaction.getEmoji();
                                VotePoint assotiatedPoint = null;
                                if (emoji.isUnicodeEmoji() && emoji.asUnicodeEmoji().isPresent()) {
                                    String unicodeEmoji = emoji.asUnicodeEmoji().get();
                                    for (VotePoint point : activeVote.getVotePoints()) {
                                        if (point.getEmoji() != null && point.getEmoji().equals(unicodeEmoji)) {
                                            assotiatedPoint = point;
                                            break;
                                        }
                                    }
                                } else if (emoji.isCustomEmoji() && emoji.asCustomEmoji().isPresent()) {
                                    CustomEmoji customEmoji = emoji.asCustomEmoji().get();
                                    long customId = customEmoji.getId();
                                    for (VotePoint point : activeVote.getVotePoints()) {
                                        if (point.getCustomEmoji() != null && point.getCustomEmoji().equals(customId)) {
                                            assotiatedPoint = point;
                                            emojiCache.put(customId, customEmoji);
                                            break;
                                        }
                                    }
                                } else {
                                    continue;
                                }

                                if (assotiatedPoint == null) {
                                    continue;
                                }

                                int addToPoint = 0;

                                List<User> votedUsers = reaction.getUsers().join();
                                for (User votedUser : votedUsers) {
                                    if (votedUser.isBot()) continue;
                                    if (activeVote.isExceptionalVote() &&
                                            alreadyVoted.contains(votedUser.getId())) continue;
                                    alreadyVoted.add(votedUser.getId());

                                    if (activeVote.getRolesFilter() != null && activeVote.getRolesFilter().size() > 0) {
                                        for (Role role : votedUser.getRoles(channel.getServer())) {
                                            if (activeVote.getRolesFilter().contains(role.getId())) {
                                                addToPoint++;
                                                break;
                                            }
                                        }
                                    } else {
                                        addToPoint++;
                                    }
                                }

                                int currentPoints = pointsLevel.get(assotiatedPoint);
                                currentPoints += addToPoint;
                                pointsLevel.put(assotiatedPoint, currentPoints);
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
                                for (Map.Entry<VotePoint, Integer> pointResult : pointsLevel.entrySet()) {
                                    if (pointResult.getValue() == winnerLevel)
                                        winners.add(pointResult.getKey());
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

    private void buildVotePointString(Map<Long, CustomEmoji> emojiCache, MessageBuilder result, VotePoint point) {
        result.append("  *");
        if (point.getEmoji() != null) {
            result.append(point.getEmoji());
        } else if (point.getCustomEmoji() != null) {
            result.append(emojiCache.get(point.getCustomEmoji()));
        }
        result.append(" -- ")
                .append(point.getPointText());
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
