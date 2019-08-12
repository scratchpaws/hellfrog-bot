package hellfrog.core;

import hellfrog.commands.cmdline.BotCommand;
import hellfrog.commands.scenes.Scenario;
import hellfrog.common.BroadCast;
import hellfrog.common.CommonUtils;
import hellfrog.common.UserCachedData;
import hellfrog.reacts.*;
import hellfrog.settings.ServerPreferences;
import hellfrog.settings.SettingsController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tools.ant.types.Commandline;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageDecoration;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
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
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class EventsListener
        implements MessageCreateListener, MessageEditListener, MessageDeleteListener,
        ReactionAddListener, ReactionRemoveListener, ReactionRemoveAllListener,
        ServerJoinListener, ServerMemberJoinListener, ServerMemberLeaveListener,
        ServerMemberBanListener, ServerMemberUnbanListener {

    private static final String VERSION_STRING = "0.1.22b";

    private final ReactReaction reactReaction = new ReactReaction();
    private final VoteReactFilter asVoteReaction = new VoteReactFilter();
    private final MessageStats messageStats = new MessageStats();
    private final TwoPhaseTransfer twoPhaseTransfer = new TwoPhaseTransfer();
    private final CommunityControlReaction communityControlReaction = new CommunityControlReaction();
    private static final Logger log = LogManager.getLogger(EventsListener.class.getSimpleName());
    private static final Logger cmdlog = LogManager.getLogger("Commands debug");

    private String botInviteUrl = "";

    @Override
    public void onMessageCreate(MessageCreateEvent event) {

        boolean isPlainMessage = true;

        messageStats.onMessageCreate(event);

        String strMessage = event.getMessageContent();
        Optional<Server> mayBeServer = event.getServer();
        Optional<User> mayBeUser = event.getMessageAuthor().asUser();

        String botMentionTag = event.getApi().getYourself().getMentionTag();
        String botMentionNicknameTag = event.getApi().getYourself().getNicknameMentionTag();
        String botPrefix;
        if (mayBeServer.isPresent()) {
            Server server = mayBeServer.get();
            botPrefix = SettingsController.getInstance()
                    .getBotPrefix(server.getId());
        } else {
            botPrefix = SettingsController.getInstance()
                    .getGlobalCommonPrefix();
        }
        boolean startedNewScenario = false;
        if (strMessage.startsWith(botPrefix) || strMessage.startsWith(botMentionTag)
            || strMessage.startsWith(botMentionNicknameTag)) {
            startedNewScenario = parseCmdLine(event);
            isPlainMessage = false;
        }

        if (!startedNewScenario && mayBeUser.isPresent() && !mayBeUser.get().isBot()) {
            for (SessionState sessionState : SessionState.all()) {
                if (sessionState.isAccept(event)) {
                    SessionState.all().remove(sessionState);
                    sessionState.getScenario().executeMessageStep(event, sessionState);
                    return;
                }
            }
        }

        isPlainMessage &= MsgCreateReaction.all().stream()
                .filter(r -> !(r instanceof CustomEmojiReaction))
                .noneMatch(r -> r.canReact(event));

        MsgCreateReaction.all().stream()
                .filter(r -> r.canReact(event))
                .forEach(r -> r.onMessageCreate(event));

        if (isPlainMessage)
            twoPhaseTransfer.transferAction(event);
    }

    private boolean parseCmdLine(@NotNull MessageCreateEvent event) {

        Optional<User> mayBeUser = event.getMessageAuthor().asUser();
        if (mayBeUser.isPresent() && !mayBeUser.get().isBot()) {
            for (SessionState sessionState : SessionState.all()) {
                if (sessionState.isAccept(event)) {
                    return false;
                }
            }
        }

        String cmdlineString = event.getMessageContent();
        if (CommonUtils.isTrStringEmpty(cmdlineString)) return false;
        Optional<Server> mayBeServer = event.getServer();

        List<String> inputLines = Arrays.asList(cmdlineString.split("(\\r\\n|\\r|\\n)"));
        if (inputLines.isEmpty()) return false;
        ArrayList<String> anotherStrings = inputLines.size() > 1 ?
                new ArrayList<>(inputLines.subList(1, inputLines.size())) : new ArrayList<>(0);

        SettingsController settingsController = SettingsController.getInstance();
        String botMentionTag = event.getApi().getYourself().getMentionTag();
        String botMentionNicknameTag = event.getApi().getYourself().getNicknameMentionTag();
        String botPrefix;
        String withoutCommonPrefix;
        if (inputLines.get(0).startsWith(botMentionTag)) {
            botPrefix = botMentionTag;
        } else if (inputLines.get(0).startsWith(botMentionNicknameTag)) {
            botPrefix = botMentionNicknameTag;
        } else if (mayBeServer.isPresent()) {
            Server server = mayBeServer.get();
            botPrefix = settingsController.getBotPrefix(server.getId());
        } else {
            botPrefix = settingsController.getGlobalCommonPrefix();
        }
        withoutCommonPrefix = getCmdlineWithoutPrefix(botPrefix, inputLines.get(0));

        if (mayBeUser.isPresent() && !mayBeUser.get().isBot()) {
            for (Scenario scenario : Scenario.all()) {
                if (scenario.canExecute(withoutCommonPrefix)) {
                    scenario.firstRun(event);
                    return true;
                }
            }
        }

        String[] rawCmdline = Commandline.translateCommandline(withoutCommonPrefix);
        cmdlog.info(Arrays.toString(rawCmdline));

        if (rawCmdline.length >= 1) {
            String commandPrefix = rawCmdline[0].toLowerCase();
            BotCommand.all().stream()
                    .filter(c -> c.getPrefix().equals(commandPrefix))
                    .forEach(c -> c.executeCreateMessageEvent(event, rawCmdline, anotherStrings));
            if (commandPrefix.equals("help") ||
                    commandPrefix.equals("-h") ||
                    commandPrefix.equals("--help")) {

                MessageBuilder embedMessageText = new MessageBuilder()
                        .append(settingsController.getBotName())
                        .append(" ")
                        .append(VERSION_STRING, MessageDecoration.BOLD)
                        .appendNewLine()
                        .append("Yet another Discord (tm)(c)(r) bot")
                        .appendNewLine()
                        .append("[Invite URL](").append(botInviteUrl).append(")")
                        .appendNewLine()
                        .append("The following commands are available:", MessageDecoration.BOLD)
                        .appendNewLine();

                Scenario.all()
                        .forEach(s -> embedMessageText.append(s.getPrefix())
                                .append(" - ")
                                .append(s.getCommandDescription())
                                .appendNewLine());
                BotCommand.all()
                        .forEach(c -> embedMessageText.append(c.getPrefix())
                                .append(" - ")
                                .append(c.getCommandDescription())
                                .appendNewLine()
                        );

                List<MsgCreateReaction> msgCreateReactions = MsgCreateReaction.all();
                if (!msgCreateReactions.isEmpty()) {
                    embedMessageText.append("The following reactions with access control available:",
                            MessageDecoration.BOLD)
                            .appendNewLine();
                    msgCreateReactions.stream()
                            .filter(MsgCreateReaction::isAccessControl)
                            .forEach(r -> embedMessageText.append(r.getCommandPrefix())
                                    .append(" - ")
                                    .append(r.getCommandDescription())
                                    .appendNewLine()
                            );
                }

                event.getMessageAuthor().asUser().ifPresent(user -> new MessageBuilder()
                        .setEmbed(new EmbedBuilder()
                                .setAuthor(event.getApi().getYourself())
                                .setColor(Color.GREEN)
                                .setTimestampToNow()
                                .setDescription(embedMessageText.getStringBuilder().toString()))
                        .send(user));
            }
        }
        return false;
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
        communityControlReaction.parseReaction(event);
        if (!event.getUser().isBot()) {
            for (SessionState sessionState : SessionState.all()) {
                if (sessionState.isAccept(event)) {
                    if (sessionState.isRemoveReaction()) {
                        event.removeReaction();
                    }
                    SessionState.all().remove(sessionState);
                    sessionState.getScenario().executeReactionStep(event, sessionState);
                    break;
                } else {
                    if (sessionState.getMessageId() == event.getMessageId()) {
                        event.removeReaction();
                    }
                }
            }
        }
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
        communityControlReaction.parseReaction(event);
        if (!event.getUser().isBot()) {
            for (SessionState sessionState : SessionState.all()) {
                if (sessionState.isAccept(event)) {
                    SessionState.all().remove(sessionState);
                    sessionState.getScenario().executeReactionStep(event, sessionState);
                    break;
                }
            }
        }
    }

    void onReady() {
        DiscordApi api = SettingsController.getInstance().getDiscordApi();
        botInviteUrl = api != null ? api.createBotInvite(new PermissionsImpl(67497153)) : "";
        String invite = "Invite url: " + botInviteUrl;
        String readyMsg = "Bot started. " + invite;
        log.info(readyMsg);
        BroadCast.sendBroadcastToAllBotOwners(readyMsg);
    }

    @Override
    public void onServerMemberJoin(ServerMemberJoinEvent event) {
        serverMemberStateDisplay(event, MemberEventCode.JOIN);
        Server server = event.getServer();
        ServerPreferences preferences = SettingsController.getInstance()
                .getServerPreferences(event.getServer().getId());
        if (preferences.getAutoPromoteEnabled()
                && preferences.getAutoPromoteRoleId() != null
                && server.getRoleById(preferences.getAutoPromoteRoleId()).isPresent()) {

            final long timeout = preferences.getAutoPromoteTimeout();
            final long userId = event.getUser().getId();
            final long serverId = event.getServer().getId();
            final long roleId = preferences.getAutoPromoteRoleId() != null ?
                    preferences.getAutoPromoteRoleId() : 0L;

            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(timeout * 1000L);
                    DiscordApi api = SettingsController.getInstance().getDiscordApi();
                    if (api != null) {
                        api.getServerById(serverId).ifPresent(srv ->
                                srv.getMemberById(userId).ifPresent(member ->
                                        srv.getRoleById(roleId).ifPresent(role ->
                                                autoAssignRole(srv, member, role)
                                        )));
                    }
                } catch (InterruptedException ignore) {
                }
            });
        }
    }

    private void autoAssignRole(Server server, User member, Role role) {
        server.addRoleToUser(member, role).thenAccept(v -> {
            ServerPreferences preferences = SettingsController.getInstance()
                    .getServerPreferences(server.getId());
            if (preferences.isJoinLeaveDisplay() && preferences.getJoinLeaveChannel() > 0) {
                Optional<ServerTextChannel> mayBeChannel =
                        server.getTextChannelById(preferences.getJoinLeaveChannel());
                mayBeChannel.ifPresent(c -> {
                    Instant currentStamp = Instant.now();
                    UserCachedData userCachedData = new UserCachedData(member, server);
                    String userName = userCachedData.getDisplayUserName()
                            + " (" + member.getDiscriminatedName() + ")";
                    final int newlineBreak = 20;
                    EmbedBuilder embedBuilder = new EmbedBuilder()
                            .setColor(Color.BLUE)
                            .setTimestamp(currentStamp)
                            .addField("User",
                                    CommonUtils.addLinebreaks(userName, newlineBreak), true)
                            .addField("Assigned role",
                                    CommonUtils.addLinebreaks(role.getName(), newlineBreak), true);
                    if (userCachedData.isHasAvatar()) {
                        embedBuilder.setThumbnail(userCachedData.getAvatarBytes(), userCachedData.getAvatarExtension());
                    }
                    new MessageBuilder()
                            .setEmbed(embedBuilder)
                            .send(c);
                });
            }
        });
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
                UserCachedData userCachedData = new UserCachedData(event.getUser(), event.getServer());

                String userName = userCachedData.getDisplayUserName()
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
                if (userCachedData.isHasAvatar()) {
                    embedBuilder.setThumbnail(userCachedData.getAvatarBytes(), userCachedData.getAvatarExtension());
                }
                new MessageBuilder()
                        .setEmbed(embedBuilder)
                        .send(c);
            });
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
