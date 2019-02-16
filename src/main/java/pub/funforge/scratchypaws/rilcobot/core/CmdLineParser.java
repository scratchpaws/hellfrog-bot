package pub.funforge.scratchypaws.rilcobot.core;

import org.apache.tools.ant.types.Commandline;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageDecoration;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;
import pub.funforge.scratchypaws.rilcobot.commands.*;
import pub.funforge.scratchypaws.rilcobot.common.CommonUtils;
import pub.funforge.scratchypaws.rilcobot.reactions.MsgCreateReaction;
import pub.funforge.scratchypaws.rilcobot.settings.SettingsController;

import java.util.*;

public class CmdLineParser {

    private static final String VERSION_STRING = "0.1.12a";
    private static final Map<String, BotCommand> botCommands = new HashMap<>();
    private static final List<MsgCreateReaction> msgCreateReactions = new ArrayList<>();

    public CmdLineParser() {
        PrefixCommand prefixCommand = new PrefixCommand();
        botCommands.put(prefixCommand.getPrefix(), prefixCommand);
        VoteCommand voteCommand = new VoteCommand();
        botCommands.put(voteCommand.getPrefix(), voteCommand);
        RightsCommand rightsCommand = new RightsCommand(botCommands, msgCreateReactions);
        botCommands.put(rightsCommand.getPrefix(), rightsCommand);
        ServiceCommand serviceCommand = new ServiceCommand();
        botCommands.put(serviceCommand.getPrefix(), serviceCommand);
        UpgradeCommand upgradeCommand = new UpgradeCommand();
        botCommands.put(upgradeCommand.getPrefix(), upgradeCommand);
        StatisticsCommand statisticsCommand = new StatisticsCommand();
        botCommands.put(statisticsCommand.getPrefix(), statisticsCommand);
    }

    void addMsgAddReactions(List<MsgCreateReaction> reactions) {
        if (reactions != null)
            msgCreateReactions.addAll(reactions);
    }

    void parseCmdLine(MessageCreateEvent event) {
        String cmdlineString = event.getMessageContent();
        if (CommonUtils.isTrStringEmpty(cmdlineString)) return;
        Optional<Server> mayBeServer = event.getServer();

        List<String> inputLines = Arrays.asList(cmdlineString.split("(\\r\\n|\\r|\\n)"));
        if (inputLines.size() == 0) return;
        ArrayList<String> anotherStrings = inputLines.size() > 1 ?
                new ArrayList<>(inputLines.subList(1, inputLines.size())) : new ArrayList<>(0);

        SettingsController settingsController = SettingsController.getInstance();
        String withoutCommonPrefix;
        if (mayBeServer.isPresent()) {
            Server server = mayBeServer.get();
            String serverBotPrefixNoSep = settingsController.getBotPrefix(server.getId());
            withoutCommonPrefix = getCmdlineWithoutPrefix(serverBotPrefixNoSep, inputLines.get(0));
        } else {
            String globalBotPrefixNoSep = settingsController.getGlobalCommonPrefix();
            withoutCommonPrefix = getCmdlineWithoutPrefix(globalBotPrefixNoSep, inputLines.get(0));
        }

        String[] rawCmdline = Commandline.translateCommandline(withoutCommonPrefix);
        System.out.println(Arrays.toString(rawCmdline));

        if (rawCmdline.length >= 1) {
            String commandPrefix = rawCmdline[0].toLowerCase();
            if (botCommands.containsKey(commandPrefix)) {
                botCommands.get(commandPrefix).executeCreateMessageEvent(event, rawCmdline, anotherStrings);
            }
            if (commandPrefix.equals("help") ||
                    commandPrefix.equals("-h") ||
                    commandPrefix.equals("--help")) {

                MessageBuilder helpUsage = new MessageBuilder()
                        .append(settingsController.getBotName())
                        .append(" ")
                        .append(VERSION_STRING, MessageDecoration.BOLD)
                        .appendNewLine()
                        .append("Yet another Discord (tm)(c)(r) bot")
                        .appendNewLine()
                        .append("The following commands are available:", MessageDecoration.BOLD)
                        .appendNewLine();

                for (Map.Entry<String, BotCommand> cmd : botCommands.entrySet()) {
                    helpUsage.append(cmd.getKey())
                            .append(" - ")
                            .append(cmd.getValue().getCommandDescription())
                            .appendNewLine();
                }

                if (msgCreateReactions.size() > 0) {
                    helpUsage.append("The following reactions with access control available:",
                            MessageDecoration.BOLD)
                            .appendNewLine();
                    msgCreateReactions.forEach(r -> {
                        if (r.isAccessControl()) {
                            helpUsage.append(r.getCommandPrefix())
                                    .append(" - ")
                                    .append(r.getCommandDescription())
                                    .appendNewLine();
                        }
                    });
                }

                helpUsage.send(event.getChannel());
            }
        }
    }

    public String getCmdlineWithoutPrefix(String prefixNoSep, String cmdLine) {
        String prefixWithSep = prefixNoSep + " ";
        if (cmdLine.startsWith(prefixWithSep)) {
            return CommonUtils.cutLeftString(cmdLine, prefixWithSep);
        } else if (cmdLine.startsWith(prefixNoSep)) {
            return CommonUtils.cutLeftString(cmdLine, prefixNoSep);
        } else {
            return "";
        }
    }

    void showFirstLoginHelp(ServerTextChannel channel) {
        MessageBuilder msgBuilder = new MessageBuilder();
        String botPrefix = SettingsController.getInstance()
                .getServerPreferences(channel.getServer().getId())
                .getBotPrefix();
        msgBuilder.append("Current bot prefix is \"" + botPrefix + "\"");
        msgBuilder.appendNewLine();
        msgBuilder.append("Type \"" + botPrefix + " help\" for more help.");
        msgBuilder.send(channel);
    }
}
