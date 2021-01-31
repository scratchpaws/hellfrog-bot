package hellfrog.commands.cmdline;

import hellfrog.common.LongEmbedMessage;
import hellfrog.core.ServerSideResolver;
import hellfrog.core.StatisticService;
import hellfrog.core.statistic.summary.SummaryReport;
import hellfrog.settings.SettingsController;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.emoji.KnownCustomEmoji;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;
import java.util.List;

public class StatisticsCommand
        extends BotCommand {

    private static final String PREFIX = "stat";
    private static final String DESCRIPTION = "Manage server statistics";

    private final Option enable = Option.builder("e")
            .longOpt("enable")
            .desc("Enable statistic collection")
            .build();

    private final Option disable = Option.builder("d")
            .longOpt("disable")
            .desc("Disable statistic collection")
            .build();

    private final Option show = Option.builder("l")
            .longOpt("list")
            .desc("Show statistic")
            .build();

    private final Option reset = Option.builder("r")
            .longOpt("reset")
            .desc("Reset statistic")
            .build();

    private final Option status = Option.builder("s")
            .longOpt("status")
            .desc("Show statistic activity")
            .build();

    private final Option smilesOnly = Option.builder("m")
            .longOpt("smiles")
            .hasArgs()
            .optionalArg(true)
            .argName("emoji")
            .desc("Show smiles statistic only")
            .build();

    private final Option textChat = Option.builder("c")
            .longOpt("chat")
            .hasArgs()
            .optionalArg(true)
            .argName("text chat")
            .desc("Show text chat statistic only")
            .build();

    private final Option userStats = Option.builder("u")
            .longOpt("user")
            .hasArg()
            .optionalArg(true)
            .argName("user")
            .desc("Show user messages statistic only")
            .build();

    public StatisticsCommand() {
        super(PREFIX, DESCRIPTION);
        super.enableOnlyServerCommandStrict();
        super.setAdminCommand();
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

        boolean enableOption = cmdline.hasOption(enable.getOpt());
        boolean disableOption = cmdline.hasOption(disable.getOpt());
        boolean showOption = cmdline.hasOption(show.getOpt());
        boolean resetOption = cmdline.hasOption(reset.getOpt());
        boolean statusOption = cmdline.hasOption(status.getOpt());
        boolean smilesOnly = cmdline.hasOption(this.smilesOnly.getOpt());
        boolean textChatsFilter = cmdline.hasOption(textChat.getOpt());
        boolean usersStatsFilter = cmdline.hasOption(userStats.getOpt());

        List<String> smileList = getOptionalArgsList(cmdline, this.smilesOnly);
        List<String> textChats = getOptionalArgsList(cmdline, textChat);
        List<String> usersList = getOptionalArgsList(cmdline, userStats);

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

            StatisticService statisticService = SettingsController.getInstance().getStatisticService();

            if (enableOption) {
                if (statisticService.isStatisticEnabled(server)) {
                    showErrorMessage("Statistic collection already enabled", event);
                } else {
                    statisticService.enableStatistic(server);
                    showInfoMessage("Statistic collection enabled", event);
                }
            }

            if (disableOption) {
                if (!statisticService.isStatisticEnabled(server)) {
                    showErrorMessage("Statistic collection already disabled", event);
                } else {
                    statisticService.disableStatistic(server);
                    showInfoMessage("Statistic collection disabled", event);
                }
            }

            if (resetOption) {
                statisticService.resetStatistic(server);
                showInfoMessage("Statistic will be reset.", event);
            }

            if (statusOption) {
                String state = statisticService.isStatisticEnabled(server) ? "enabled" : "disabled";
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

                SummaryReport summaryReport = statisticService.getSummaryReport(server,
                        parsedCustomEmoji.getFound(),
                        parsedTextChannel.getFound(),
                        parsedUsers.getFound());

                if (summaryReport.isEmpty()) {
                    super.showInfoMessage("Message statistic is empty", event);
                } else {
                    LongEmbedMessage message = statisticService.formatForMessage(summaryReport, smilesOnly, usersStatsFilter, textChatsFilter);
                    message.setPlain();
                    super.showMessage(message, event);
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
