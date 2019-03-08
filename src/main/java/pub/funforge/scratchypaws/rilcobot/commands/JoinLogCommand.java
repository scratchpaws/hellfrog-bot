package pub.funforge.scratchypaws.rilcobot.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;
import pub.funforge.scratchypaws.rilcobot.common.CommonUtils;
import pub.funforge.scratchypaws.rilcobot.core.ServerSideResolver;
import pub.funforge.scratchypaws.rilcobot.settings.SettingsController;
import pub.funforge.scratchypaws.rilcobot.settings.old.ServerPreferences;

import java.util.ArrayList;
import java.util.Optional;

/**
 * Логгирование присоединившихся и отсоединившихся участников сервера
 */
public class JoinLogCommand
    extends BotCommand {

    private static final String BOT_PREFIX = "jlog";
    private static final String DESCRIPTION = "Join and left logging.";
    private static final String FOOTER = "Log join and left server members to server text channel.";

    public JoinLogCommand() {
        super(BOT_PREFIX, DESCRIPTION);
        super.enableOnlyServerCommandStrict();

        Option chatName = Option.builder("c")
                .longOpt("channel")
                .hasArg()
                .argName("Text channel")
                .desc("Text channel where the join and left will be displayed")
                .build();

        Option enable = Option.builder("e")
                .longOpt("enable")
                .desc("Enable logging")
                .build();

        Option disable = Option.builder("d")
                .longOpt("disable")
                .desc("Disable logging")
                .build();

        Option status = Option.builder("s")
                .longOpt("status")
                .desc("Show logging status")
                .build();

        super.addCmdlineOption(chatName, enable, disable, status);
        super.setFooter(FOOTER);
    }

    @Override
    protected void executeCreateMessageEventServer(Server server,
                                                   CommandLine cmdline,
                                                   ArrayList<String> cmdlineArgs,
                                                   TextChannel channel,
                                                   MessageCreateEvent event,
                                                   ArrayList<String> anotherLines) {

        boolean isChannelSet = cmdline.hasOption('c');
        String textChannelName = isChannelSet ? cmdline.getOptionValue('c') : "";

        boolean enableFlag = cmdline.hasOption('e');
        boolean disableFlag = cmdline.hasOption('d');
        boolean statusFlag = cmdline.hasOption('s');

        if (enableFlag ^ disableFlag ^ statusFlag) {
            SettingsController settingsController = SettingsController.getInstance();
            ServerPreferences preferences = settingsController.getServerPreferences(server.getId());
            long targetChannel = 0;
            long previousChannel = preferences.getJoinLeaveChannel();
            boolean currentState = preferences.isJoinLeaveDisplay();
            Optional<ServerTextChannel> mayBeChannel = previousChannel > 0 ?
                    server.getTextChannelById(previousChannel) : Optional.empty();

            if (!CommonUtils.isTrStringEmpty(textChannelName)) {
                mayBeChannel = ServerSideResolver.resolveChannel(server, textChannelName);
                if (mayBeChannel.isEmpty()) {
                    showErrorMessage("Unable to resolve text channel", channel);
                    return;
                } else {
                    targetChannel = mayBeChannel.get().getId();
                }
            }

            if (enableFlag) {
                if (targetChannel == 0 && previousChannel == 0) {
                    showErrorMessage("Text channel is not set", channel);
                    return;
                }

                if (currentState && targetChannel == 0) {
                    showErrorMessage("Logging already enabled", channel);
                    return;
                }

                if (targetChannel == 0 && previousChannel > 0 && mayBeChannel.isEmpty()) {
                    showErrorMessage("Unable to enable logging - text channel no longer exists", channel);
                    return;
                }

                if (targetChannel > 0) {
                    preferences.setJoinLeaveChannel(targetChannel);
                }

                preferences.setJoinLeaveDisplay(true);
                settingsController.saveServerSideParameters(server.getId());

                MessageBuilder msg = new MessageBuilder().append("Join/Leave logging enabled");
                mayBeChannel.ifPresent(serverTextChannel ->
                        msg.append(" to channel ")
                        .append(serverTextChannel));

                showInfoMessage(msg.getStringBuilder().toString(), channel);
            }

            if (disableFlag) {
                if (!currentState) {
                    showErrorMessage("Logging already disabled.", channel);
                } else {
                    preferences.setJoinLeaveDisplay(false);
                    settingsController.saveServerSideParameters(server.getId());
                    showInfoMessage("Join/Leave logging disabled", channel);
                }
            }

            if (statusFlag) {
                MessageBuilder msg = new MessageBuilder()
                        .append("Join/Leave logging ")
                        .append(currentState ? "enabled" : "disabled");
                if (currentState) {
                    if (mayBeChannel.isPresent()) {
                        msg.append(" to channel ")
                                .append(mayBeChannel.get());
                    } else {
                        msg.append(", but target text channel not found. Logging will not work.");
                    }
                }
                showInfoMessage(msg.getStringBuilder().toString(), channel);
            }
        } else {
            showErrorMessage("Only one action may be execute", channel);
        }
    }

    @Override
    protected void executeCreateMessageEventDirect(CommandLine cmdline,
                                                   ArrayList<String> cmdlineArgs,
                                                   TextChannel channel,
                                                   MessageCreateEvent event,
                                                   ArrayList<String> anotherLines) {
        // сюда не доходят
    }
}
