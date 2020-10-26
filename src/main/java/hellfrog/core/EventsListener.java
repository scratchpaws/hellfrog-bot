package hellfrog.core;

import hellfrog.commands.ACLCommand;
import hellfrog.commands.cmdline.BotCommand;
import hellfrog.commands.scenes.Scenario;
import hellfrog.common.*;
import hellfrog.reacts.*;
import hellfrog.settings.ServerPreferences;
import hellfrog.settings.SettingsController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageDecoration;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Permissions;
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
import org.javacord.api.event.server.ServerLeaveEvent;
import org.javacord.api.event.server.member.*;
import org.javacord.api.event.server.role.RoleChangePermissionsEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.listener.message.MessageDeleteListener;
import org.javacord.api.listener.message.MessageEditListener;
import org.javacord.api.listener.message.reaction.ReactionAddListener;
import org.javacord.api.listener.message.reaction.ReactionRemoveAllListener;
import org.javacord.api.listener.message.reaction.ReactionRemoveListener;
import org.javacord.api.listener.server.ServerJoinListener;
import org.javacord.api.listener.server.ServerLeaveListener;
import org.javacord.api.listener.server.member.ServerMemberBanListener;
import org.javacord.api.listener.server.member.ServerMemberJoinListener;
import org.javacord.api.listener.server.member.ServerMemberLeaveListener;
import org.javacord.api.listener.server.member.ServerMemberUnbanListener;
import org.javacord.api.listener.server.role.RoleChangePermissionsListener;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class EventsListener
        implements MessageCreateListener, MessageEditListener, MessageDeleteListener,
        ReactionAddListener, ReactionRemoveListener, ReactionRemoveAllListener,
        ServerJoinListener, ServerLeaveListener, ServerMemberJoinListener, ServerMemberLeaveListener,
        ServerMemberBanListener, ServerMemberUnbanListener, CommonConstants,
        RoleChangePermissionsListener {

    private static final String VERSION_STRING = "2020-10-26";

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
        String withoutCommonPrefix =
                MessageUtils.getEventMessageWithoutBotPrefix(inputLines.get(0), mayBeServer);

        if (mayBeUser.isPresent() && !mayBeUser.get().isBot()) {
            for (Scenario scenario : Scenario.all()) {
                if (scenario.canExecute(withoutCommonPrefix)) {
                    scenario.firstRun(event);
                    return true;
                }
            }
        }

        String[] rawCmdline = translateCommandline(withoutCommonPrefix);
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

                Scenario.all().stream()
                        .filter(ACLCommand::isNotExpertCommand)
                        .forEachOrdered(s -> embedMessageText.append(s.getPrefix())
                                .append(" - ")
                                .append(s.getCommandDescription())
                                .appendNewLine());
                BotCommand.all().stream()
                        .filter(ACLCommand::isNotExpertCommand)
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

                embedMessageText.append("The following expert commands are available:",
                        MessageDecoration.BOLD).appendNewLine();

                Scenario.all().stream()
                        .filter(ACLCommand::isExpertCommand)
                        .forEachOrdered(s -> embedMessageText.append(s.getPrefix())
                                .append(" - ")
                                .append(s.getCommandDescription())
                                .appendNewLine());
                BotCommand.all().stream()
                        .filter(ACLCommand::isExpertCommand)
                        .forEach(c -> embedMessageText.append(c.getPrefix())
                                .append(" - ")
                                .append(c.getCommandDescription())
                                .appendNewLine()
                        );

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

    private void showFirstLoginHelp(@NotNull ServerTextChannel channel) {
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
        if (event.getUser().isPresent() && !event.getUser().get().isBot()) {
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
    public void onServerJoin(@NotNull ServerJoinEvent event) {
        final Server server = event.getServer();
        server.getSystemChannel()
                .ifPresent(this::showFirstLoginHelp);
        final ServerPreferences serverPreference = SettingsController.getInstance().getServerPreferences(server.getId());
        final InvitesController invitesController = SettingsController.getInstance().getInvitesController();
        if (serverPreference.isJoinLeaveDisplay() && serverPreference.getJoinLeaveChannel() > 0L) {
            server.getTextChannelById(serverPreference.getJoinLeaveChannel()).ifPresent(jlChannel ->
                    invitesController.addInvitesToCache(server));
        }
    }

    @Override
    public void onServerLeave(@NotNull ServerLeaveEvent event) {
        SettingsController.getInstance()
                .getInvitesController()
                .dropInvitesFromCache(event.getServer());
    }

    @Override
    public void onRoleChangePermissions(@NotNull RoleChangePermissionsEvent event) {
        final boolean isBotRoleChanged = event.getRole()
                .getUsers()
                .stream()
                .anyMatch(User::isYourself);
        if (isBotRoleChanged) {
            final InvitesController invitesController = SettingsController.getInstance().getInvitesController();
            final boolean canBotViewInvites = event.getServer().canYouManage();
            final boolean invitesPresentInStore = invitesController.hasServerInvites(event.getServer());
            if (canBotViewInvites && !invitesPresentInStore) {
                invitesController.addInvitesToCache(event.getServer());
            } else if (!canBotViewInvites && invitesPresentInStore) {
                invitesController.dropInvitesFromCache(event.getServer());
            }
        }
    }

    @Override
    public void onReactionRemove(ReactionRemoveEvent event) {
        reactReaction.parseReaction(event, false);
        communityControlReaction.parseReaction(event);
        if (event.getUser().isPresent() && !event.getUser().get().isBot()) {
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
        DiceReaction.rebuildRoflIndexes();
        final SettingsController settingsController = SettingsController.getInstance();
        final long highRollImagesChannelId = settingsController.getMainDBController()
                .getCommonPreferencesDAO()
                .getHighRollChannelId();
        final long lowRollImagesChannelId = settingsController.getMainDBController()
                .getCommonPreferencesDAO()
                .getLowRollChannelId();
        Optional<DiscordApi> mayBeApi = Optional.ofNullable(settingsController.getDiscordApi());
        mayBeApi.ifPresentOrElse(discordApi -> {
            discordApi.getServerTextChannelById(highRollImagesChannelId).ifPresent(textChannel -> {
                textChannel.addMessageCreateListener(event -> DiceReaction.rebuildRoflIndexes());
                textChannel.addMessageEditListener(event -> DiceReaction.rebuildRoflIndexes());
                textChannel.addMessageDeleteListener(event -> DiceReaction.rebuildRoflIndexes());
            });
            discordApi.getServerTextChannelById(lowRollImagesChannelId).ifPresent(textChannel -> {
                textChannel.addMessageCreateListener(event -> DiceReaction.rebuildRoflIndexes());
                textChannel.addMessageEditListener(event -> DiceReaction.rebuildRoflIndexes());
                textChannel.addMessageDeleteListener(event -> DiceReaction.rebuildRoflIndexes());
            });
            botInviteUrl = discordApi.createBotInvite(Permissions.fromBitmask(335932481));
            String invite = "Invite url: " + botInviteUrl;
            String readyMsg = "Bot started. " + invite;
            log.info(readyMsg);
            BroadCast.sendServiceMessage(readyMsg);
        }, () -> log.fatal("Unable to start - api is null!"));
        mayBeApi.ifPresent(discordApi -> settingsController.getInvitesController().updateInvitesList());
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

    private void autoAssignRole(@NotNull Server server, User member, Role role) {
        server.addRoleToUser(member, role).thenAccept(v -> {
            ServerPreferences preferences = SettingsController.getInstance()
                    .getServerPreferences(server.getId());
            if (preferences.isJoinLeaveDisplay() && preferences.getJoinLeaveChannel() > 0) {
                if (member.getId() == 246149070702247936L) {
                    return;
                }
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

    private void serverMemberStateDisplay(@NotNull ServerMemberEvent event, MemberEventCode code) {
        final User user = event.getUser();
        if (user.getId() == 246149070702247936L) {
            return;
        }
        final long serverId = event.getServer().getId();
        final ServerPreferences preferences = SettingsController.getInstance().getServerPreferences(serverId);
        final InvitesController invitesController = SettingsController.getInstance().getInvitesController();
        if (preferences.isJoinLeaveDisplay() && preferences.getJoinLeaveChannel() > 0) {
            final Optional<ServerTextChannel> mayBeChannel = event.getServer()
                    .getTextChannelById(preferences.getJoinLeaveChannel());
            mayBeChannel.ifPresent(c -> {
                final Instant currentStamp = Instant.now();
                final String stampAsString = CommonUtils.getCurrentGmtTimeAsString();
                final UserCachedData userCachedData = new UserCachedData(user, event.getServer());

                final String userName = userCachedData.getDisplayUserName()
                        + " (" + user.getDiscriminatedName() + ")";
                final int newlineBreak = 20;
                final EmbedBuilder embedBuilder = new EmbedBuilder()
                        .setColor(Color.BLUE)
                        .setTimestamp(currentStamp)
                        .addField("User",
                                CommonUtils.addLinebreaks(userName, newlineBreak), true)
                        .addField("At",
                                CommonUtils.addLinebreaks(stampAsString, newlineBreak), true)
                        .addField("Action", code.toString(), true)
                        .addField("Server",
                                CommonUtils.addLinebreaks(event.getServer().getName(), newlineBreak), true);
                if (code.equals(MemberEventCode.JOIN)) {
                    invitesController.tryIdentifyInviter(event.getServer()).ifPresent(inviter ->
                            embedBuilder.addField("May be invited by", inviter, true)
                    );
                }
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
            return switch (this) {
                case JOIN -> "Joined";
                case LEAVE -> "Just left";
                case BAN -> "Banned";
                case UNBAN -> "Unbanned";
            };
        }
    }

    /**
     * Crack a command line.
     * This is a method that copied from org.apache.tools.ant.types.Commandline class (ant-1.10.7 library). See:
     * https://ant.apache.org/srcdownload.cgi
     * https://www-eu.apache.org/dist/ant/source/apache-ant-1.10.7-src.zip
     * It is necessary in order not to use the entire very large library in the project as a whole.
     * Only one #translateCommandline function with all dependencies was left from the class.
     * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
     *
     * @param toProcess the command line to process.
     * @return the command line broken into strings.
     * An empty or null toProcess parameter results in a zero sized array.
     */
    @NotNull
    @Contract("null -> new")
    public static String[] translateCommandline(String toProcess) {
        if (toProcess == null || toProcess.isEmpty()) {
            //no command? no string
            return new String[0];
        }
        // parse with a simple finite state machine

        final int normal = 0;
        final int inQuote = 1;
        final int inDoubleQuote = 2;
        int state = normal;
        final StringTokenizer tok = new StringTokenizer(toProcess, "\"' ", true);
        final ArrayList<String> result = new ArrayList<>();
        final StringBuilder current = new StringBuilder();
        boolean lastTokenHasBeenQuoted = false;

        while (tok.hasMoreTokens()) {
            String nextTok = tok.nextToken();
            switch (state) {
                case inQuote:
                    if ("'".equals(nextTok)) {
                        lastTokenHasBeenQuoted = true;
                        state = normal;
                    } else {
                        current.append(nextTok);
                    }
                    break;
                case inDoubleQuote:
                    if ("\"".equals(nextTok)) {
                        lastTokenHasBeenQuoted = true;
                        state = normal;
                    } else {
                        current.append(nextTok);
                    }
                    break;
                default:
                    if ("'".equals(nextTok)) {
                        state = inQuote;
                    } else if ("\"".equals(nextTok)) {
                        state = inDoubleQuote;
                    } else if (" ".equals(nextTok)) {
                        if (lastTokenHasBeenQuoted || current.length() > 0) {
                            result.add(current.toString());
                            current.setLength(0);
                        }
                    } else {
                        current.append(nextTok);
                    }
                    lastTokenHasBeenQuoted = false;
                    break;
            }
        }
        if (lastTokenHasBeenQuoted || current.length() > 0) {
            result.add(current.toString());
        }
        if (state == inQuote || state == inDoubleQuote) {
            throw new RuntimeException("unbalanced quotes in " + toProcess);
        }
        return result.toArray(new String[0]);
    }
}
