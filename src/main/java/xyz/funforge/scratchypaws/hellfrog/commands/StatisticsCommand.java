package xyz.funforge.scratchypaws.hellfrog.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.emoji.KnownCustomEmoji;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageDecoration;
import org.javacord.api.entity.message.MessageSet;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import xyz.funforge.scratchypaws.hellfrog.common.MessageUtils;
import xyz.funforge.scratchypaws.hellfrog.core.ServerSideResolver;
import xyz.funforge.scratchypaws.hellfrog.reactions.MessageStats;
import xyz.funforge.scratchypaws.hellfrog.settings.SettingsController;
import xyz.funforge.scratchypaws.hellfrog.settings.old.ServerStatistic;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class StatisticsCommand
        extends BotCommand {

    private static final String PREFIX = "stat";
    private static final String DESCRIPTION = "Manage server statistics";

    private static final AtomicBoolean activeRebuildProcess = new AtomicBoolean(false);

    StatisticsCommand() {
        super(PREFIX, DESCRIPTION);

        Option enable = Option.builder("e")
                .longOpt("enable")
                .desc("Enable statistic collection")
                .build();

        Option disable = Option.builder("d")
                .longOpt("disable")
                .desc("Disable statistic collection")
                .build();

        Option show = Option.builder("l")
                .longOpt("list")
                .desc("Show statistic")
                .build();

        Option reset = Option.builder("r")
                .longOpt("reset")
                .desc("Reset statistic")
                .build();

        Option status = Option.builder("s")
                .longOpt("status")
                .desc("Show statistic activity")
                .build();

        Option smilesOnly = Option.builder("m")
                .longOpt("smiles")
                .hasArgs()
                .optionalArg(true)
                .argName("emoji")
                .desc("Show smiles statistic only")
                .build();

        Option textChat = Option.builder("c")
                .longOpt("chat")
                .hasArgs()
                .optionalArg(true)
                .argName("text chat")
                .desc("Show text chat statistic only")
                .build();

        Option userStats = Option.builder("u")
                .longOpt("user")
                .hasArg()
                .optionalArg(true)
                .argName("user")
                .desc("Show user messages statistic only")
                .build();

        Option rebuild = Option.builder("b")
                .desc("Full scan and rebuild statistic")
                .build();

        super.enableOnlyServerCommandStrict();
        super.addCmdlineOption(enable, disable, show, reset, status, smilesOnly, textChat, userStats, rebuild);
    }

    @Override
    protected void executeCreateMessageEventServer(Server server,
                                                   CommandLine cmdline, ArrayList<String> cmdlineArgs,
                                                   TextChannel channel,
                                                   MessageCreateEvent event,
                                                   ArrayList<String> anotherLines) {

        if (!canExecuteServerCommand(event, server)) {
            showAccessDeniedServerMessage(channel);
            return;
        }

        boolean enableOption = cmdline.hasOption('e');
        boolean disableOption = cmdline.hasOption('d');
        boolean showOption = cmdline.hasOption('l');
        boolean resetOption = cmdline.hasOption('r');
        boolean statusOption = cmdline.hasOption('s');
        boolean smilesOnly = cmdline.hasOption('m');
        boolean textChatsFilter = cmdline.hasOption('c');
        boolean usersStatsFilter = cmdline.hasOption('u');
        boolean rebuildOption = cmdline.hasOption('b');

        List<String> smileList = getOptionalArgsList(cmdline, 'm');
        List<String> textChats = getOptionalArgsList(cmdline, 'c');
        List<String> usersList = getOptionalArgsList(cmdline, 'u');

        if ((smilesOnly || textChatsFilter || usersStatsFilter)
                && (enableOption || disableOption || resetOption || rebuildOption)) {
            showErrorMessage("Display options (-m/-c) are set only when displaying statistics", channel);
            return;
        }

        if (smilesOnly && (textChatsFilter || usersStatsFilter)) {
            showErrorMessage("Display only emoji and filtering by users and text chat do not mix", channel);
            return;
        }

        if (enableOption ^ disableOption ^ showOption ^ resetOption ^ statusOption ^ rebuildOption) {
            long serverId = server.getId();
            SettingsController settingsController = SettingsController.getInstance();
            ServerStatistic serverStatistic = settingsController.getServerStatistic(serverId);

            if (enableOption) {
                if (serverStatistic.isCollectNonDefaultSmileStats()) {
                    showErrorMessage("Statistic collection already enabled", channel);
                } else {
                    serverStatistic.setCollectNonDefaultSmileStats(true);
                    settingsController.saveServerSideStatistic(serverId);
                    showInfoMessage("Statistic collection enabled", channel);
                }
            }

            if (disableOption) {
                if (!serverStatistic.isCollectNonDefaultSmileStats()) {
                    showErrorMessage("Statistic collection already disabled", channel);
                } else {
                    serverStatistic.setCollectNonDefaultSmileStats(false);
                    settingsController.saveServerSideStatistic(serverId);
                    showInfoMessage("Statistic collection disabled", channel);
                }
            }

            if (rebuildOption && activeRebuildProcess.get()) {
                showErrorMessage("Statistic rebuild already in progress", channel);
                return;
            }

            if (resetOption || rebuildOption) {
                serverStatistic.clear();
                settingsController.saveServerSideStatistic(serverId);
                showInfoMessage("Statistic will be reset.", channel);
            }

            if (statusOption) {
                String state = serverStatistic.isCollectNonDefaultSmileStats() ? "enabled" : "disabled";
                showInfoMessage("Statistic collection " + state, channel);
            }

            if (showOption) {

                ServerSideResolver.ParseResult<ServerTextChannel> parsedTextChannel = ServerSideResolver
                        .resolveTextChannelsList(server, textChats);

                ServerSideResolver.ParseResult<KnownCustomEmoji> parsedCustomEmoji = ServerSideResolver
                        .resolveKnownEmojiList(server, smileList);

                ServerSideResolver.ParseResult<User> parsedUsers = ServerSideResolver
                        .resolveUsersList(server, usersList);

                if (parsedTextChannel.hasNotFound()) {
                    showErrorMessage("Unable to find text channels: " +
                            parsedTextChannel.getNotFoundStringList(), channel);
                    return;
                }

                if (parsedCustomEmoji.hasNotFound()) {
                    showErrorMessage("Unable to find emoji: " +
                            parsedCustomEmoji.getNotFoundStringList(), channel);
                    return;
                }

                if (parsedUsers.hasNotFound()) {
                    showErrorMessage("Unable to find users: " +
                            parsedUsers.getNotFoundStringList(), channel);
                    return;
                }

                TreeMap<Long, List<String>> emojiStat = new TreeMap<>(Comparator.reverseOrder());
                TreeMap<Long, List<String>> userStats = serverStatistic.buildUserStats(parsedUsers.getFound());
                TreeMap<Long, List<String>> textChatStat = serverStatistic.buildTextChatStats(parsedTextChannel.getFound(),
                        parsedUsers.getFound());

                serverStatistic.getNonDefaultSmileStats()
                        .forEach((id, stat) -> {
                            if (stat.getUsagesCount() != null && stat.getUsagesCount().get() > 0L) {
                                long usagesCount = stat.getUsagesCount().get();
                                if (parsedCustomEmoji.hasFound()) {
                                    boolean found = parsedCustomEmoji.getFound()
                                            .stream()
                                            .anyMatch(e -> e.getId() == id);
                                    if (!found)
                                        return;
                                }
                                server.getCustomEmojiById(id)
                                        .ifPresentOrElse(emoji -> {
                                            MessageBuilder tmp = new MessageBuilder()
                                                    .append(String.valueOf(usagesCount))
                                                    .append(" - ")
                                                    .append(emoji)
                                                    .append(" ");
                                            if (stat.getLastUsage() != null && stat.getLastUsage().get() > 0) {
                                                Calendar last = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                                                last.setTimeInMillis(stat.getLastUsage().get());
                                                tmp.append(String.format("last usage at %tF %<tT (GMT)", last));
                                            }
                                            if (!emojiStat.containsKey(usagesCount)) {
                                                List<String> emptyList = new ArrayList<>();
                                                emojiStat.put(usagesCount, emptyList);
                                            }
                                            emojiStat.get(usagesCount)
                                                    .add(tmp.getStringBuilder().toString());
                                        }, () -> serverStatistic.getNonDefaultSmileStats()
                                                .remove(id));
                            }
                        });

                if (emojiStat.size() == 0 && userStats.size() == 0 && textChatStat.size() == 0) {
                    new MessageBuilder()
                            .append("Message statistic is empty")
                            .send(channel);
                } else {
                    MessageBuilder resultMessage = new MessageBuilder();

                    resultMessage.append("Collected statistic:", MessageDecoration.BOLD)
                            .appendNewLine();

                    if (emojiStat.size() > 0 && !textChatsFilter && !usersStatsFilter) {
                        resultMessage.append(">> Custom emoji usage statistic:", MessageDecoration.ITALICS)
                                .appendNewLine();

                        ServerStatistic.appendResultStats(resultMessage, emojiStat, 1);
                    }

                    if (userStats.size() > 0 && !smilesOnly && !textChatsFilter) {
                        resultMessage.append(">> User message statistics:", MessageDecoration.ITALICS)
                                .appendNewLine();

                        ServerStatistic.appendResultStats(resultMessage, userStats, 1);
                    }

                    if (textChatStat.size() > 0 && !smilesOnly) {
                        resultMessage.append(">> Text chat message statistics:", MessageDecoration.ITALICS)
                                .appendNewLine();

                        ServerStatistic.appendResultStats(resultMessage, textChatStat, 1);
                    }

                    MessageUtils.sendLongMessage(resultMessage, channel);
                }
            }

            if (rebuildOption) {
                EmbedBuilder embed = new EmbedBuilder();
                embed.setAuthor(event.getMessageAuthor());
                embed.setTimestampToNow();
                embed.setDescription("Starting...");
                embed.setFooter("Please wait...");
                embed.setTitle("Rebuilding server statistic...");
                new MessageBuilder()
                        .setEmbed(embed)
                        .send(channel)
                        .thenAccept(message ->
                                CompletableFuture.runAsync(() ->
                                        parallelRebuildStatistic(message, embed))
                        );
            }
        } else {
            showErrorMessage("Only one option may be use.", channel);
        }
    }

    @Override
    protected void executeCreateMessageEventDirect(CommandLine cmdline, ArrayList<String> cmdlineArgs, TextChannel channel, MessageCreateEvent event, ArrayList<String> anotherLines) {
        showErrorMessage("Not allowed into private", channel);
    }

    private void parallelRebuildStatistic(Message msg, EmbedBuilder embed) {
        activeRebuildProcess.set(true);
        try {
            msg.getServer().ifPresent(server -> server.getTextChannels().stream()
                    .filter(TextChannel::canYouReadMessageHistory)
                    .forEach(channel -> {
                        embed.setTimestampToNow()
                                .setDescription("Processing server text channel \""
                                        + channel.getName() + "\"")
                                .setFooter("Begin read messages...");
                        editSilently(msg, embed);

                        long currentMsgNumber = 0L;
                        Message currentHistMsg = null;
                        for (int trying = 1; trying <= 3; trying++) {
                            try {
                                embed.setFooter("Get last message of channel, trying "
                                        + trying + " of 3...");
                                editSilently(msg, embed);

                                MessageSet firstProbe = channel.getMessages(1)
                                        .get(10, TimeUnit.SECONDS);
                                Optional<Message> mayBeMsg = firstProbe.getOldestMessage();
                                if (mayBeMsg.isPresent()) {
                                    currentHistMsg = mayBeMsg.get();
                                    break;
                                } else {
                                    embed.setFooter("Channel has not last message");
                                    editSilently(msg, embed);
                                    return;
                                }
                            } catch (Exception err) {
                                embed.setFooter("Unable to get latest message: " + err);
                                if (doSleepExcepted(msg, embed)) return;
                            }
                        }

                        if (currentHistMsg == null)
                            return;
                        MessageSet set = null;

                        do {
                            for (int trying = 1; trying <= 3; trying++) {
                                try {
                                    boolean appendToHistory = set == null;
                                    embed.setFooter("Get next portion message of channel, trying "
                                            + trying + " of 3...");
                                    editSilently(msg, embed);

                                    set = currentHistMsg.getMessagesBefore(100)
                                            .get(10, TimeUnit.SECONDS);

                                    embed.setFooter("Ok, parsing...");
                                    editSilently(msg, embed);
                                    if (appendToHistory) {
                                        set.add(currentHistMsg);
                                    }
                                    break;
                                } catch (Exception err) {
                                    embed.setFooter("Unable to extract messages: " + err);
                                    if (doSleepExcepted(msg, embed)) return;
                                }
                            }

                            if (set != null && set.size() > 0) {
                                for (Message hist : set) {
                                    if (!hist.getAuthor().isUser())
                                        continue;
                                    if (hist.getAuthor().isYourself())
                                        continue;
                                    currentMsgNumber++;
                                    User author = null;
                                    Optional<User> mayBeUser = hist.getAuthor().asUser();
                                    if (mayBeUser.isPresent()) {
                                        author = mayBeUser.get();
                                    }
                                    MessageStats.collectStat(channel.getServer(),
                                            channel, author, true);
                                    if (currentMsgNumber % 50L == 0L) {
                                        embed.setFooter("Processed ~" + currentMsgNumber + " messages.");
                                        editSilently(msg, embed);
                                    }
                                }
                                Optional<Message> oldest = set.getOldestMessage();
                                if (oldest.isPresent()) {
                                    currentHistMsg = oldest.get();
                                } else {
                                    break;
                                }
                            } else {
                                embed.setFooter("Next portion message is empty");
                                editSilently(msg, embed);
                            }
                        } while (set != null && set.size() > 0);

                        embed.setFooter("Done");
                        editSilently(msg, embed);
                    })
            );

            embed.setFooter("...");
            embed.setDescription("Done");
            editSilently(msg, embed);
        } finally {
            activeRebuildProcess.set(false);
        }
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
            msg.edit(embed).get(10, TimeUnit.SECONDS);
        } catch (Exception ignore) {
        }
    }
}
