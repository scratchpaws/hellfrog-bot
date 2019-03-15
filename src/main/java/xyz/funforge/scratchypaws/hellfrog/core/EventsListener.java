package xyz.funforge.scratchypaws.hellfrog.core;

import besus.utils.collection.Sequental;
import org.apache.tools.ant.types.Commandline;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageDecoration;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.MessageDeleteEvent;
import org.javacord.api.event.message.MessageEditEvent;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.event.message.reaction.ReactionRemoveAllEvent;
import org.javacord.api.event.message.reaction.ReactionRemoveEvent;
import org.javacord.api.event.server.ServerJoinEvent;
import org.javacord.api.event.server.member.ServerMemberEvent;
import org.javacord.api.event.server.member.ServerMemberJoinEvent;
import org.javacord.api.event.server.member.ServerMemberLeaveEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.listener.message.MessageDeleteListener;
import org.javacord.api.listener.message.MessageEditListener;
import org.javacord.api.listener.message.reaction.ReactionAddListener;
import org.javacord.api.listener.message.reaction.ReactionRemoveAllListener;
import org.javacord.api.listener.message.reaction.ReactionRemoveListener;
import org.javacord.api.listener.server.ServerJoinListener;
import org.javacord.api.listener.server.member.ServerMemberJoinListener;
import org.javacord.api.listener.server.member.ServerMemberLeaveListener;
import org.javacord.core.entity.permission.PermissionsImpl;
import xyz.funforge.scratchypaws.hellfrog.commands.BotCommand;
import xyz.funforge.scratchypaws.hellfrog.common.BroadCast;
import xyz.funforge.scratchypaws.hellfrog.common.CommonUtils;
import xyz.funforge.scratchypaws.hellfrog.reactions.MsgCreateReaction;
import xyz.funforge.scratchypaws.hellfrog.reactions.ReactReaction;
import xyz.funforge.scratchypaws.hellfrog.reactions.VoteReactFilter;
import xyz.funforge.scratchypaws.hellfrog.settings.SettingsController;
import xyz.funforge.scratchypaws.hellfrog.settings.old.ServerPreferences;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class EventsListener
        implements MessageCreateListener, MessageEditListener, MessageDeleteListener,
        ReactionAddListener, ReactionRemoveListener, ReactionRemoveAllListener,
        ServerJoinListener, ServerMemberJoinListener, ServerMemberLeaveListener {

    private static final String VERSION_STRING = "0.1.14b";

    private ReactReaction reactReaction = new ReactReaction();
    private VoteReactFilter asVoteReaction = new VoteReactFilter();

    @Override
    public void onMessageCreate(MessageCreateEvent event) {
        String strMessage = event.getMessageContent();
        Optional<Server> mayBeServer = event.getServer();

        if (mayBeServer.isPresent()) {
            Server server = mayBeServer.get();
            String serverBotPrefixNoSep = SettingsController
                    .getInstance()
                    .getBotPrefix(server.getId());
            if (strMessage.startsWith(serverBotPrefixNoSep)) {
                parseCmdLine(event);
            }
        } else {
            String globalBotPrefixNoSep = SettingsController
                    .getInstance()
                    .getGlobalCommonPrefix();
            if (strMessage.startsWith(globalBotPrefixNoSep)) {
                parseCmdLine(event);
            }
        }

        MsgCreateReaction.all().stream()
                .filter(r -> r.canReact(event))
                .forEach(r -> r.onMessageCreate(event));
    }

    private void parseCmdLine(MessageCreateEvent event) {
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
            BotCommand.all().stream()
                    .filter(c -> c.getPrefix().equals(commandPrefix))
                    .forEach(c -> c.executeCreateMessageEvent(event, rawCmdline, anotherStrings));
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

                BotCommand.all().stream()
                        .forEach(c -> helpUsage.append(c.getPrefix())
                                .append(" - ")
                                .append(c.getCommandDescription())
                                .appendNewLine()
                        );

                Sequental<MsgCreateReaction> msgCreateReactions = MsgCreateReaction.all();
                if (msgCreateReactions.stream().count() > 0) {
                    helpUsage.append("The following reactions with access control available:",
                            MessageDecoration.BOLD)
                            .appendNewLine();
                    msgCreateReactions.stream()
                            .filter(MsgCreateReaction::isAccessControl)
                            .forEach(r -> helpUsage.append(r.getCommandPrefix())
                                    .append(" - ")
                                    .append(r.getCommandDescription())
                                    .appendNewLine()
                            );
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

    private void showFirstLoginHelp(ServerTextChannel channel) {
        MessageBuilder msgBuilder = new MessageBuilder();
        String botPrefix = SettingsController.getInstance()
                .getServerPreferences(channel.getServer().getId())
                .getBotPrefix();
        msgBuilder.append("Current bot prefix is \"" + botPrefix + "\"");
        msgBuilder.appendNewLine();
        msgBuilder.append("Type \"" + botPrefix + " help\" for more help.");
        msgBuilder.send(channel);
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {

    }

    @Override
    public void onMessageEdit(MessageEditEvent event) {

    }

    @Override
    public void onReactionAdd(ReactionAddEvent event) {
        reactReaction.parseReaction(event, true);
        asVoteReaction.parseAction(event);
    }

    @Override
    public void onReactionRemoveAll(ReactionRemoveAllEvent event) {

    }

    @Override
    public void onServerJoin(ServerJoinEvent event) {
        Server server = event.getServer();
        server.getSystemChannel()
                .ifPresent(this::showFirstLoginHelp);
    }

    @Override
    public void onReactionRemove(ReactionRemoveEvent event) {
        reactReaction.parseReaction(event, false);
    }

    void onReady() {
        DiscordApi api = SettingsController.getInstance().getDiscordApi();
        String invite = api != null ?
                "Invite url: " + api.createBotInvite(new PermissionsImpl(1544940737))
                : " ";
        String readyMsg = "Bot started. " + invite;
        System.err.println(readyMsg);
        BroadCast.sendBroadcastToAllBotOwners(readyMsg);
    }

    @Override
    public void onServerMemberJoin(ServerMemberJoinEvent event) {
        onServerJoinLeft(event, true);
    }

    @Override
    public void onServerMemberLeave(ServerMemberLeaveEvent event) {
        onServerJoinLeft(event, false);
    }

    private void onServerJoinLeft(ServerMemberEvent event, boolean isJoin) {
        long serverId = event.getServer().getId();
        ServerPreferences preferences = SettingsController.getInstance()
                .getServerPreferences(serverId);
        if (preferences.isJoinLeaveDisplay() && preferences.getJoinLeaveChannel() > 0) {
            Optional<ServerTextChannel> mayBeChannel = event.getServer()
                    .getTextChannelById(preferences.getJoinLeaveChannel());
            mayBeChannel.ifPresent(c -> {
                String message = new MessageBuilder()
                        .append(event.getUser())
                        .append(" (")
                        .append(event.getUser().getDiscriminatedName())
                        .append(")")
                        .append(isJoin ? " joined to " : " just left the server ")
                        .append(event.getServer().getName(), MessageDecoration.BOLD)
                        .append(" at ")
                        .append(CommonUtils.getCurrentGmtTimeAsString())
                        .getStringBuilder()
                        .toString();
                new MessageBuilder()
                        .setEmbed(new EmbedBuilder()
                                .setDescription(message)
                                .setColor(Color.BLUE))
                        .send(c);
            });
        }
    }
}
