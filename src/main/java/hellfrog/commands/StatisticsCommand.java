package hellfrog.commands;

import hellfrog.common.MessageUtils;
import hellfrog.common.OptionalUtils;
import hellfrog.core.ServerSideResolver;
import hellfrog.settings.ServerStatistic;
import hellfrog.settings.SettingsController;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.emoji.KnownCustomEmoji;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageDecoration;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.*;

public class StatisticsCommand
        extends BotCommand {

    private static final String PREFIX = "stat";
    private static final String DESCRIPTION = "Manage server statistics";

    public StatisticsCommand() {
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

        super.enableOnlyServerCommandStrict();
        super.addCmdlineOption(enable, disable, show, reset, status, smilesOnly, textChat, userStats);
    }

    @Override
    protected void executeCreateMessageEventServer(Server server,
                                                   CommandLine cmdline, ArrayList<String> cmdlineArgs,
                                                   TextChannel channel,
                                                   MessageCreateEvent event,
                                                   ArrayList<String> anotherLines) {

        if (!canExecuteServerCommand(event, server)) {
            showAccessDeniedServerMessage(event);
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

        List<String> smileList = getOptionalArgsList(cmdline, 'm');
        List<String> textChats = getOptionalArgsList(cmdline, 'c');
        List<String> usersList = getOptionalArgsList(cmdline, 'u');

        if ((smilesOnly || textChatsFilter || usersStatsFilter)
                && (enableOption || disableOption || resetOption)) {
            showErrorMessage("Display options (-m/-c) are set only when displaying statistics", event);
            return;
        }

        if (smilesOnly && (textChatsFilter || usersStatsFilter)) {
            showErrorMessage("Display only emoji and filtering by users and text chat do not mix", event);
            return;
        }

        if (enableOption ^ disableOption ^ showOption ^ resetOption ^ statusOption) {
            long serverId = server.getId();
            SettingsController settingsController = SettingsController.getInstance();
            ServerStatistic serverStatistic = settingsController.getServerStatistic(serverId);

            if (enableOption) {
                if (serverStatistic.isCollectNonDefaultSmileStats()) {
                    showErrorMessage("Statistic collection already enabled", event);
                } else {
                    serverStatistic.setCollectNonDefaultSmileStats(true);
                    long startDate = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis();
                    serverStatistic.setStartDate(startDate);
                    settingsController.saveServerSideStatistic(serverId);
                    showInfoMessage("Statistic collection enabled", event);
                }
            }

            if (disableOption) {
                if (!serverStatistic.isCollectNonDefaultSmileStats()) {
                    showErrorMessage("Statistic collection already disabled", event);
                } else {
                    serverStatistic.setCollectNonDefaultSmileStats(false);
                    settingsController.saveServerSideStatistic(serverId);
                    showInfoMessage("Statistic collection disabled", event);
                }
            }

            if (resetOption) {
                serverStatistic.clear();
                long startDate = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis();
                serverStatistic.setStartDate(startDate);
                settingsController.saveServerSideStatistic(serverId);
                showInfoMessage("Statistic will be reset.", event);
            }

            if (statusOption) {
                String state = serverStatistic.isCollectNonDefaultSmileStats() ? "enabled" : "disabled";
                showInfoMessage("Statistic collection " + state, event);
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
                            parsedTextChannel.getNotFoundStringList(), event);
                    return;
                }

                if (parsedCustomEmoji.hasNotFound()) {
                    showErrorMessage("Unable to find emoji: " +
                            parsedCustomEmoji.getNotFoundStringList(), event);
                    return;
                }

                if (parsedUsers.hasNotFound()) {
                    showErrorMessage("Unable to find users: " +
                            parsedUsers.getNotFoundStringList(), event);
                    return;
                }


                String sinceStr = "";
                if (serverStatistic.getStartDate() > 0L) {
                    Calendar since = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    since.setTimeInMillis(serverStatistic.getStartDate());
                    sinceStr = String.format(" (since %tF %<tT (UTC))", since);
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
                                OptionalUtils.ifPresentOrElse(server.getCustomEmojiById(id),
                                        emoji -> {
                                            MessageBuilder tmp = new MessageBuilder()
                                                    .append(String.valueOf(usagesCount))
                                                    .append(" - ")
                                                    .append(emoji)
                                                    .append(" ");
                                            if (stat.getLastUsage() != null && stat.getLastUsage().get() > 0) {
                                                Calendar last = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                                                last.setTimeInMillis(stat.getLastUsage().get());
                                                tmp.append(String.format("last usage at %tF %<tT (UTC)", last));
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

                if (emojiStat.isEmpty() && userStats.isEmpty() && textChatStat.isEmpty()) {
                    new MessageBuilder()
                            .append("Message statistic is empty")
                            .send(getMessageTargetByRights(event));
                } else {
                    MessageBuilder resultMessage = new MessageBuilder();

                    resultMessage.append("Collected statistic" + sinceStr + ":", MessageDecoration.BOLD)
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

                    MessageUtils.sendLongMessage(resultMessage, getMessageTargetByRights(event));
                }
            }
        } else {
            showErrorMessage("Only one option may be use.", event);
        }
    }

    @Override
    protected void executeCreateMessageEventDirect(CommandLine cmdline,
                                                   ArrayList<String> cmdlineArgs,
                                                   TextChannel channel,
                                                   MessageCreateEvent event,
                                                   ArrayList<String> anotherLines) {

        showErrorMessage("Not allowed into private", event);
    }
}
