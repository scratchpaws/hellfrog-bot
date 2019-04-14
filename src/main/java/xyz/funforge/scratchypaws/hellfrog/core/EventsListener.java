package xyz.funforge.scratchypaws.hellfrog.core;

import besus.utils.collection.Sequental;
import org.apache.tools.ant.types.Commandline;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.Icon;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageDecoration;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.MessageDeleteEvent;
import org.javacord.api.event.message.MessageEditEvent;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.event.message.reaction.ReactionRemoveAllEvent;
import org.javacord.api.event.message.reaction.ReactionRemoveEvent;
import org.javacord.api.event.server.ServerJoinEvent;
import org.javacord.api.event.server.member.*;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.listener.message.MessageDeleteListener;
import org.javacord.api.listener.message.MessageEditListener;
import org.javacord.api.listener.message.reaction.ReactionAddListener;
import org.javacord.api.listener.message.reaction.ReactionRemoveAllListener;
import org.javacord.api.listener.message.reaction.ReactionRemoveListener;
import org.javacord.api.listener.server.ServerJoinListener;
import org.javacord.api.listener.server.member.ServerMemberBanListener;
import org.javacord.api.listener.server.member.ServerMemberJoinListener;
import org.javacord.api.listener.server.member.ServerMemberLeaveListener;
import org.javacord.api.listener.server.member.ServerMemberUnbanListener;
import org.javacord.core.entity.permission.PermissionsImpl;
import xyz.funforge.scratchypaws.hellfrog.commands.BotCommand;
import xyz.funforge.scratchypaws.hellfrog.common.BroadCast;
import xyz.funforge.scratchypaws.hellfrog.common.CommonUtils;
import xyz.funforge.scratchypaws.hellfrog.reactions.MessageStats;
import xyz.funforge.scratchypaws.hellfrog.reactions.MsgCreateReaction;
import xyz.funforge.scratchypaws.hellfrog.reactions.ReactReaction;
import xyz.funforge.scratchypaws.hellfrog.reactions.VoteReactFilter;
import xyz.funforge.scratchypaws.hellfrog.settings.SettingsController;
import xyz.funforge.scratchypaws.hellfrog.settings.old.ServerPreferences;

import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class EventsListener
        implements MessageCreateListener, MessageEditListener, MessageDeleteListener,
        ReactionAddListener, ReactionRemoveListener, ReactionRemoveAllListener,
        ServerJoinListener, ServerMemberJoinListener, ServerMemberLeaveListener,
        ServerMemberBanListener, ServerMemberUnbanListener {

    private static final String VERSION_STRING = "0.1.19b";

    private static final String TWO_PHASE_MESSAGE =
            "AAAAAAAAAAAAAAAAAAAAAAAAAAA".repeat(10);

    private final ReactReaction reactReaction = new ReactReaction();
    private final VoteReactFilter asVoteReaction = new VoteReactFilter();
    private final MessageStats messageStats = new MessageStats();

    @Override
    public void onMessageCreate(MessageCreateEvent event) {

       /* // это дебаг всех сообщений его выключить перед коммитом,
        // иначе лог разойдётся до космических величин
        String content = event.getMessageContent();
        System.out.println(content);
        event.getMessage().getEmbeds().forEach(e ->
                System.out.println(e.getDescription()));*/

        messageStats.onMessageCreate(event);

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

        twoPhaseTransfer(event);
    }

    private void parseCmdLine(MessageCreateEvent event) {
        String cmdlineString = event.getMessageContent();
        if (CommonUtils.isTrStringEmpty(cmdlineString)) return;
        Optional<Server> mayBeServer = event.getServer();

        List<String> inputLines = Arrays.asList(cmdlineString.split("(\\r\\n|\\r|\\n)"));
        if (inputLines.isEmpty()) return;
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
        if (channel.getServer().canYouChangeOwnNickname()) {
            User ownUser = channel.getApi().getYourself();
            channel.getServer().updateNickname(ownUser, SettingsController.getInstance().getBotName() + " ("
                    + botPrefix + " help)");
        }
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        messageStats.onMessageDelete(event);
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
        BotCommand.all(); // заранее инициируем поиск и инстантинацию классов команд
        MsgCreateReaction.all();
        DiscordApi api = SettingsController.getInstance().getDiscordApi();
        String invite = api != null ?
                "Invite url: " + api.createBotInvite(new PermissionsImpl(1544940737))
                : " ";
        String readyMsg = "Bot started. " + invite;
        System.err.println(readyMsg);
        BroadCast.sendBroadcastToAllBotOwners(readyMsg);

        // todo: выключать в продуктиве. будет спамить
        // нужно для отладки логгирования входов/выходов. см JoinLogCommand
        /*Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(() -> {
            if (api != null)
            api.getServerById(525287388818178048L) // test_ground.
                    .ifPresent(s -> {
                        List<User> memberList = new ArrayList<>(s.getMembers());
                        if (memberList.size() == 0) return;
                        User selected = memberList.get(ThreadLocalRandom.current()
                            .nextInt(0, memberList.size()));
                        ServerMemberJoinEvent je = new ServerMemberJoinEventImpl(s, selected);
                        serverMemberStateDisplay(je, MemberEventCode.JOIN);
                    });
        }, 10L, 15L, TimeUnit.SECONDS);*/
    }

    @Override
    public void onServerMemberJoin(ServerMemberJoinEvent event) {
        serverMemberStateDisplay(event, MemberEventCode.JOIN);
    }

    @Override
    public void onServerMemberLeave(ServerMemberLeaveEvent event) {
        serverMemberStateDisplay(event, MemberEventCode.LEAVE);
    }

    @Override
    public void onServerMemberBan(ServerMemberBanEvent event) {
        serverMemberStateDisplay(event, MemberEventCode.BAN);
    }

    @Override
    public void onServerMemberUnban(ServerMemberUnbanEvent event) {
        serverMemberStateDisplay(event, MemberEventCode.UNBAN);
    }

    private void serverMemberStateDisplay(ServerMemberEvent event, MemberEventCode code) {
        long serverId = event.getServer().getId();
        ServerPreferences preferences = SettingsController.getInstance()
                .getServerPreferences(serverId);
        if (preferences.isJoinLeaveDisplay() && preferences.getJoinLeaveChannel() > 0) {
            Optional<ServerTextChannel> mayBeChannel = event.getServer()
                    .getTextChannelById(preferences.getJoinLeaveChannel());
            mayBeChannel.ifPresent(c -> {
                Instant currentStamp = Instant.now();
                String stampAsString = CommonUtils.getCurrentGmtTimeAsString();
                String displayUserName;
                if (event.getServer().getMembers().contains(event.getUser())) {
                    displayUserName = event.getServer().getDisplayName(event.getUser());
                } else {
                    displayUserName = event.getUser().getName();
                }
                boolean hasAvatarData = false;
                byte[] avatarBytes = new byte[0];
                String avatarName;
                String avatarExt = "";
                try {
                    Icon avatar = event.getUser().getAvatar();
                    avatarName = avatar.getUrl().getFile();
                    String[] nameEl = avatarName.split("\\.");
                    if (nameEl.length > 0) {
                        avatarExt = nameEl[nameEl.length - 1];
                        avatarBytes = avatar.asByteArray().get(20, TimeUnit.SECONDS);
                        if (avatarBytes.length > 0) {
                            hasAvatarData = true;
                        }
                    }
                } catch (Exception ignore) {
                }

                String userName = displayUserName
                        + " (" + event.getUser().getDiscriminatedName() + ")";
                final int newlineBreak = 20;
                EmbedBuilder embedBuilder = new EmbedBuilder()
                        .setColor(Color.BLUE)
                        .setTimestamp(currentStamp)
                        .addField("User",
                                CommonUtils.addLinebreaks(userName, newlineBreak), true)
                        .addField("At",
                                CommonUtils.addLinebreaks(stampAsString, newlineBreak), true)
                        .addField("Action", code.toString(), true)
                        .addField("Server",
                                CommonUtils.addLinebreaks(event.getServer().getName(), newlineBreak), true);
                if (hasAvatarData) {
                    embedBuilder.setThumbnail(avatarBytes, avatarExt);
                }
                new MessageBuilder()
                        .setEmbed(embedBuilder)
                        .send(c);
            });
        }
    }

    private void twoPhaseTransfer(MessageCreateEvent event) {
        if (event.getServer().isPresent()) return;
        if (event.getMessageAuthor().isYourself()) return;
        SettingsController settingsController = SettingsController.getInstance();
        Long serverTransfer = settingsController.getServerTransfer();
        Long textChannelTransfer = settingsController.getServerTextChatTransfer();

        if (serverTransfer != null && textChannelTransfer != null) {
            event.getApi().getServerById(serverTransfer).ifPresent(server ->
                    server.getTextChannelById(textChannelTransfer).ifPresent(ch -> {

                MessageAuthor author = event.getMessageAuthor();
                String rawMessage = event.getMessageContent();

                new MessageBuilder()
                        .setEmbed(new EmbedBuilder()
                                .setDescription(TWO_PHASE_MESSAGE))
                        .send(ch).thenAccept(ph1 -> ph1.edit(new EmbedBuilder()
                                .setDescription(TWO_PHASE_MESSAGE)
                                .setTimestampToNow())
                                .thenAccept(v1 -> {

                                    ph1.edit(new EmbedBuilder()
                                            .setAuthor(author)
                                            .setDescription(rawMessage)
                                            .setTimestampToNow()
                                            .setFooter("Wait 5 sec...")).thenAccept(v2 -> {

                                        try {
                                            Thread.sleep(5_000L);
                                        } catch (InterruptedException ignore) {
                                        }
                                        ph1.edit(new EmbedBuilder()
                                                .setAuthor("<anonymous>")
                                                .setDescription(rawMessage)
                                                .setTimestamp(ph1.getCreationTimestamp()));
                                    });
                                }));
            }));
        }
    }


    private enum MemberEventCode {
        JOIN, LEAVE, BAN, UNBAN;

        @Override
        public String toString() {
            switch (this) {
                case JOIN:
                    return "Joined";
                case LEAVE:
                    return "Just left";
                case BAN:
                    return "Banned";
                case UNBAN:
                    return "Unbanned";

                default:
                    return "";
            }
        }
    }
}
