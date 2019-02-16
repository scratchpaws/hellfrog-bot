package pub.funforge.scratchypaws.rilcobot.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.emoji.KnownCustomEmoji;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageDecoration;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;
import pub.funforge.scratchypaws.rilcobot.common.CommonUtils;
import pub.funforge.scratchypaws.rilcobot.settings.SettingsController;
import pub.funforge.scratchypaws.rilcobot.settings.old.ServerStatistic;
import pub.funforge.scratchypaws.rilcobot.settings.old.SmileStatistic;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

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
                MessageBuilder msg = new MessageBuilder()
                        .append("Collected statistic:", MessageDecoration.BOLD)
                        .appendNewLine();

                Map<Long, KnownCustomEmoji> emojiCache = new HashMap<>();
                Map<Long, Calendar> lastUseCache = new HashMap<>();
                Map<Long, Long> usagesCountCache = new HashMap<>();
                Set<Long> usagesCounts = new TreeSet<>(Comparator.reverseOrder());
                for (Map.Entry<Long, SmileStatistic> entry : serverStatistic.getNonDefaultSmileStats().entrySet()) {
                    long emojiId = entry.getKey();
                    server.getCustomEmojiById(emojiId)
                            .ifPresent(e -> emojiCache.put(emojiId, e));
                    SmileStatistic stats = entry.getValue();
                    AtomicLong lastUsage = stats.getLastUsage();
                    if (lastUsage != null && lastUsage.get() > 0) {
                        Calendar last = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                        last.setTimeInMillis(lastUsage.get());
                        lastUseCache.put(emojiId, last);
                    }
                    AtomicLong usagesCount = stats.getUsagesCount();
                    if (usagesCount != null) {
                        long cnt = usagesCount.get();
                        usagesCountCache.put(emojiId, cnt);
                        usagesCounts.add(cnt);
                    }
                }

                if (usagesCounts.size() == 0) {
                    msg.append("No emoji statistic")
                            .appendNewLine();
                } else {
                    msg.append("- Custom emoji usage statistic:", MessageDecoration.ITALICS)
                            .appendNewLine();
                    for (long usagesTableValue : usagesCounts) {
                        for (Map.Entry<Long, Long> usageStatEntry : usagesCountCache.entrySet()) {
                            long usagesCount = usageStatEntry.getValue();
                            if (usagesCount == usagesTableValue) {
                                msg.append("   - ")
                                        .append(String.valueOf(usagesCount))
                                        .append(" - ");
                                long emojiId = usageStatEntry.getKey();
                                if (emojiCache.containsKey(emojiId)) {
                                    msg.append(emojiCache.get(emojiId));
                                } else {
                                    msg.append("(n/a, id: " + emojiId + ")");
                                }
                                msg.append(" ");
                                if (lastUseCache.containsKey(emojiId)) {
                                    Calendar last = lastUseCache.get(emojiId);
                                    msg.append(String.format("last usage at %tF %<tT (GMT)", last));
                                } else {
                                    msg.append("(last usage unknown)");
                                }
                                msg.appendNewLine();
                            }
                        }
                    }
                }

                if (msg.getStringBuilder().length() >= 1990) {
                    List<String> splitted = CommonUtils.splitEqually(msg.getStringBuilder().toString(), 1990);
                    CompletableFuture.runAsync(() -> {
                        for (String cut : splitted) {
                            new MessageBuilder().append(cut)
                                    .send(channel)
                                    .exceptionally(t -> {
                                        System.out.println("Can't sent \"" + cut + "\": " + t);
                                        return null;
                                    });
                        }
                    });
                } else {
                    msg.send(channel);
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
