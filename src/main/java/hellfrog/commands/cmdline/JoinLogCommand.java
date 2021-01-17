package hellfrog.commands.cmdline;

import hellfrog.common.CommonUtils;
import hellfrog.core.ServerSideResolver;
import hellfrog.settings.ServerPreferences;
import hellfrog.settings.SettingsController;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;
import java.util.Optional;

/**
 * Логгирование присоединившихся и отсоединившихся участников сервера
 */
public class JoinLogCommand
        extends BotCommand {

    private static final String BOT_PREFIX = "jlog";
    private static final String DESCRIPTION = "Configuring event logging.";
    private static final String FOOTER = "When activated, joins, lefts and bans members on the server, automatic " +
            "role assignment are displayed in a given channel. In addition, if the bot has access rights that allow it " +
            "to monitor invitations, the prospective inviter is displayed in the log when the members joins.";

    private final Option channelOption = Option.builder("c")
            .longOpt("channel")
            .hasArg()
            .argName("Text channel")
            .desc("Text channel where the join and left will be displayed")
            .build();

    private final Option enableOption = Option.builder("e")
            .longOpt("enable")
            .desc("Enable logging")
            .build();

    private final Option disableOption = Option.builder("d")
            .longOpt("disable")
            .desc("Disable logging")
            .build();

    private final Option statusOption = Option.builder("s")
            .longOpt("status")
            .desc("Show logging status")
            .build();

    public JoinLogCommand() {
        super(BOT_PREFIX, DESCRIPTION);
        super.enableOnlyServerCommandStrict();

        super.addCmdlineOption(channelOption, enableOption, disableOption, statusOption);
        super.setFooter(FOOTER);
    }

    @Override
    protected void executeCreateMessageEventServer(Server server,
                                                   CommandLine cmdline,
                                                   ArrayList<String> cmdlineArgs,
                                                   TextChannel channel,
                                                   MessageCreateEvent event,
                                                   ArrayList<String> anotherLines) {

        if (!canExecuteServerCommand(event, server, channel.getId())) {
            showAccessDeniedServerMessage(event);
            return;
        }

        boolean isChannelSet = cmdline.hasOption('c');
        String textChannelName = isChannelSet ? cmdline.getOptionValue(channelOption.getOpt()) : "";

        boolean enableFlag = cmdline.hasOption(enableOption.getOpt());
        boolean disableFlag = cmdline.hasOption(disableOption.getOpt());
        boolean statusFlag = cmdline.hasOption(statusOption.getOpt());

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
                    showErrorMessage("Unable to resolve text channel", event);
                    return;
                } else {
                    targetChannel = mayBeChannel.get().getId();
                }
            }

            if (enableFlag) {
                if (targetChannel == 0 && previousChannel == 0) {
                    showErrorMessage("Text channel is not set", event);
                    return;
                }

                if (currentState && targetChannel == 0) {
                    showErrorMessage("Logging already enabled", event);
                    return;
                }

                if (targetChannel == 0 && previousChannel > 0 && mayBeChannel.isEmpty()) {
                    showErrorMessage("Unable to enable logging - text channel no longer exists", event);
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

                showInfoMessage(msg.getStringBuilder().toString(), event);
            }

            if (disableFlag) {
                if (!currentState) {
                    showErrorMessage("Logging already disabled.", event);
                } else {
                    preferences.setJoinLeaveDisplay(false);
                    settingsController.saveServerSideParameters(server.getId());
                    showInfoMessage("Join/Leave logging disabled", event);
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
                showInfoMessage(msg.getStringBuilder().toString(), event);
            }
        } else if (!enableFlag && !disableFlag) {
            showErrorMessage("Action required", event);
        } else {
            showErrorMessage("Only one action may be execute", event);
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
