package pub.funforge.scratchypaws.rilcobot.core;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.MessageDeleteEvent;
import org.javacord.api.event.message.MessageEditEvent;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.event.message.reaction.ReactionRemoveAllEvent;
import org.javacord.api.event.message.reaction.ReactionRemoveEvent;
import org.javacord.api.event.server.ServerJoinEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.listener.message.MessageDeleteListener;
import org.javacord.api.listener.message.MessageEditListener;
import org.javacord.api.listener.message.reaction.ReactionAddListener;
import org.javacord.api.listener.message.reaction.ReactionRemoveAllListener;
import org.javacord.api.listener.message.reaction.ReactionRemoveListener;
import org.javacord.api.listener.server.ServerJoinListener;
import org.javacord.core.entity.permission.PermissionsImpl;
import pub.funforge.scratchypaws.rilcobot.common.BroadCast;
import pub.funforge.scratchypaws.rilcobot.reactions.CustomEmojiReaction;
import pub.funforge.scratchypaws.rilcobot.reactions.DiceReaction;
import pub.funforge.scratchypaws.rilcobot.reactions.MsgCreateReaction;
import pub.funforge.scratchypaws.rilcobot.reactions.ReactReaction;
import pub.funforge.scratchypaws.rilcobot.settings.SettingsController;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EventsListener
        implements MessageCreateListener, MessageEditListener, MessageDeleteListener,
        ReactionAddListener, ReactionRemoveListener, ReactionRemoveAllListener,
        ServerJoinListener {

    private SettingsController settingsController;
    private CmdLineParser cmdLineParser;
    private List<MsgCreateReaction> msgCreateReactions = new ArrayList<>();
    private ReactReaction reactReaction = new ReactReaction();

    EventsListener() {
        this.settingsController = SettingsController.getInstance();
        this.cmdLineParser = new CmdLineParser();

        DiceReaction diceReaction = new DiceReaction();
        msgCreateReactions.add(diceReaction);
        CustomEmojiReaction emojiReaction = new CustomEmojiReaction();
        msgCreateReactions.add(emojiReaction);
        cmdLineParser.addMsgAddReactions(msgCreateReactions);
    }

    @Override
    public void onMessageCreate(MessageCreateEvent event) {
        String strMessage = event.getMessageContent();
        Optional<Server> mayBeServer = event.getServer();

        if (mayBeServer.isPresent()) {
            Server server = mayBeServer.get();
            String serverBotPrefixNoSep = settingsController.getBotPrefix(server.getId());
            if (strMessage.startsWith(serverBotPrefixNoSep)) {
                cmdLineParser.parseCmdLine(event);
            }
        } else {
            String globalBotPrefixNoSep = settingsController.getGlobalCommonPrefix();
            if (strMessage.startsWith(globalBotPrefixNoSep)) {
                cmdLineParser.parseCmdLine(event);
            }
        }

        for (MsgCreateReaction msgCreateReaction : msgCreateReactions) {
            if (msgCreateReaction.canReact(event)) {
                msgCreateReaction.onMessageCreate(event);
            }
        }
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
    }

    @Override
    public void onReactionRemoveAll(ReactionRemoveAllEvent event) {

    }

    @Override
    public void onServerJoin(ServerJoinEvent event) {
        Server server = event.getServer();
        server.getSystemChannel()
                .ifPresent(cmdLineParser::showFirstLoginHelp);
    }

    @Override
    public void onReactionRemove(ReactionRemoveEvent event) {
        reactReaction.parseReaction(event, false);
    }

    void onReady() {
        DiscordApi api = SettingsController.getInstance().getDiscordApi();
        String invite = api != null ?
                "Invite url: " + api.createBotInvite(new PermissionsImpl(470149318))
                : " ";
        String readyMsg = "Bot started. " + invite;
        BroadCast.sendBroadcastToAllBotOwners(readyMsg);
    }

}
