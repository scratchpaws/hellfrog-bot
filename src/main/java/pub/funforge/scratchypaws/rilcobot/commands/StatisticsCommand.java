package pub.funforge.scratchypaws.rilcobot.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageDecoration;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;
import pub.funforge.scratchypaws.rilcobot.settings.SettingsController;
import pub.funforge.scratchypaws.rilcobot.settings.old.ServerStatistic;

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

        super.enableOnlyServerCommandStrict();
        super.addCmdlineOption(enable, disable, show, reset, status);
    }

    @Override
    protected void executeCreateMessageEventServer(Server server, CommandLine cmdline, ArrayList<String> cmdlineArgs, TextChannel channel, MessageCreateEvent event, ArrayList<String> anotherLines) {

        if (!canExecuteServerCommand(event, server)) {
            showAccessDeniedServerMessage(channel);
            return;
        }

        boolean enableOption = cmdline.hasOption('e');
        boolean disableOption = cmdline.hasOption('d');
        boolean showOption = cmdline.hasOption('l');
        boolean resetOption = cmdline.hasOption('r');
        boolean statusOption = cmdline.hasOption('s');

        if (enableOption ^ disableOption ^ showOption ^ resetOption ^ statusOption) {
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

            if (resetOption) {
                serverStatistic.getNonDefaultSmileStats().clear();
                settingsController.saveServerSideStatistic(serverId);
                showInfoMessage("Statistic will be reset.", channel);
            }

            if (statusOption) {
                String state = serverStatistic.isCollectNonDefaultSmileStats() ? "enabled" : "disabled";
                showInfoMessage("Statistic collection " + state, channel);
            }

            if (showOption) {

                TreeMap<Long, List<String>> emojiStat = new TreeMap<>(Comparator.reverseOrder());

                serverStatistic.getNonDefaultSmileStats()
                        .forEach((id, stat) -> {
                            if (stat.getUsagesCount() != null && stat.getUsagesCount().get() > 0) {
                                long usagesCount = stat.getUsagesCount().get();
                                server.getCustomEmojiById(id)
                                        .ifPresentOrElse(emoji -> {
                                            MessageBuilder tmp = new MessageBuilder()
                                                    .append("- ")
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

                if (emojiStat.size() == 0) {
                    new MessageBuilder()
                            .append("Message statistic is empty")
                            .send(channel);
                } else {
                    new MessageBuilder()
                            .append("Collected statistic:", MessageDecoration.BOLD)
                            .appendNewLine()
                            .append(">> Custom emoji usage statistic:", MessageDecoration.ITALICS)
                            .send(channel);

                    int cnt = 0;
                    final int maxCnt = 20;
                    MessageBuilder msg = null;

                    for (Map.Entry<Long, List<String>> entry : emojiStat.entrySet()) {
                        for (String item : entry.getValue()) {
                            if (msg == null) {
                                msg = new MessageBuilder();
                            }
                            msg.append(item).appendNewLine();
                            cnt++;
                            if (cnt == maxCnt) {
                                msg.send(channel);
                                msg = null;
                                cnt = 0;
                            }
                        }
                    }

                    if (msg != null) {
                        msg.send(channel);
                    }
                }
            }
        } else {
            showErrorMessage("Only one option may be use.", channel);
        }
    }

    @Override
    protected void executeCreateMessageEventDirect(CommandLine cmdline, ArrayList<String> cmdlineArgs, TextChannel channel, MessageCreateEvent event, ArrayList<String> anotherLines) {
        showErrorMessage("Not allowed into private", channel);
    }
}
