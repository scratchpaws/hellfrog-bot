package hellfrog.commands.cmdline;

import hellfrog.common.CommonUtils;
import hellfrog.common.LongEmbedMessage;
import hellfrog.core.ServerSideResolver;
import hellfrog.settings.SettingsController;
import hellfrog.settings.db.ServerPreferencesDAO;
import hellfrog.settings.db.entity.ServerNameCache;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;
import java.util.Optional;

public class EventLogCommand
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

    public EventLogCommand() {
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

        boolean isChannelSet = cmdline.hasOption(channelOption.getOpt());
        String textChannelName = isChannelSet ? cmdline.getOptionValue(channelOption.getOpt()) : "";

        boolean enableFlag = cmdline.hasOption(enableOption.getOpt());
        boolean disableFlag = cmdline.hasOption(disableOption.getOpt());
        boolean statusFlag = cmdline.hasOption(statusOption.getOpt());

        if (enableFlag ^ disableFlag ^ statusFlag) {
            ServerPreferencesDAO preferencesDAO = SettingsController.getInstance()
                    .getMainDBController()
                    .getServerPreferencesDAO();
            long targetChannel = 0;
            long previousChannel = preferencesDAO.getJoinLeaveChannel(server.getId());
            boolean currentState = preferencesDAO.isJoinLeaveDisplay(server.getId());
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
                    preferencesDAO.setJoinLeaveChannel(server.getId(), targetChannel);
                }

                preferencesDAO.setJoinLeaveDisplay(server.getId(), true);
                LongEmbedMessage message = LongEmbedMessage.withTitleInfoStyle("Event logging settings")
                        .append("Event logging enabled");
                mayBeChannel.ifPresent(serverTextChannel ->
                        message.append(" to channel ")
                                .append(serverTextChannel));

                showMessage(message, event);
            }

            if (disableFlag) {
                if (!currentState) {
                    showErrorMessage("Event logging already disabled.", event);
                } else {
                    preferencesDAO.setJoinLeaveDisplay(server.getId(), false);
                    showInfoMessage("Event logging disabled", event);
                }
            }

            if (statusFlag) {
                LongEmbedMessage message = LongEmbedMessage.withTitleInfoStyle("Event logging settings")
                        .append("Event logging  ")
                        .append(currentState && mayBeChannel.isPresent() ? "enabled" : "disabled");
                if (currentState) {
                    if (mayBeChannel.isPresent()) {
                        message.append(" to channel ")
                                .append(mayBeChannel.get());
                    } else {
                        message.append(" (text channel ");
                        SettingsController.getInstance().getNameCacheService()
                                .find(server, previousChannel)
                                .map(ServerNameCache::getName)
                                .ifPresent(s -> message.append(" \"").appendReadable(s, server).append("\" "));
                        message.append("of the event log was not found, need to specify another channel to enable)");
                    }
                }
                showMessage(message, event);
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
