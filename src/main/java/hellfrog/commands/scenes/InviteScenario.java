package hellfrog.commands.scenes;

import hellfrog.common.BroadCast;
import hellfrog.core.SessionState;
import hellfrog.settings.SettingsController;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.PrivateChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.server.invite.Invite;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class InviteScenario
        extends Scenario {

    private static final String PREFIX = "invite";
    private static final String DESCRIPTION = "Get invite to the \"official\" bot server";

    public InviteScenario() {
        super(PREFIX, DESCRIPTION);
    }

    @Override
    protected void executePrivateFirstRun(@NotNull MessageCreateEvent event,
                                          @NotNull PrivateChannel privateChannel,
                                          @NotNull User user,
                                          boolean isBotOwner) {
        sendInvite(event, user);
    }

    @Override
    protected void executeServerFirstRun(@NotNull MessageCreateEvent event,
                                         @NotNull Server server,
                                         @NotNull ServerTextChannel serverTextChannel,
                                         @NotNull User user,
                                         boolean isBotOwner) {
        sendInvite(event, user);
    }

    private void sendInvite(@NotNull MessageCreateEvent event, @NotNull User user) {
        DiscordApi discordApi = event.getApi();
        final long officialBotServerId = SettingsController.getInstance()
                .getMainDBController()
                .getCommonPreferencesDAO()
                .getOfficialBotServerId();
        discordApi.getServerById(officialBotServerId).ifPresentOrElse(server ->
                        server.getSystemChannel().ifPresentOrElse(textChannel ->
                                generateAndSendInvite(event,
                                        server,
                                        textChannel,
                                        user), () ->
                                super.showErrorMessage("Server " + server.getName()
                                        + "does not has default channel", event))
                , () ->
                        super.showErrorMessage("Bot hasn't official server", event)
        );
    }

    private void generateAndSendInvite(@NotNull MessageCreateEvent event,
                                       @NotNull Server officialServer,
                                       @NotNull ServerTextChannel officialServerDefaultChannel,
                                       @NotNull User targetUser) {
        if (officialServerDefaultChannel.canYouCreateInstantInvite()) {
            try {
                Invite invite = officialServerDefaultChannel.createInviteBuilder()
                        .setMaxUses(1)
                        .setMaxAgeInSeconds(3 * 60)
                        .setUnique(true)
                        .setTemporary(true)
                        .setAuditLogReason("Required by user id " + targetUser.getId())
                        .create()
                        .get(OP_WAITING_TIMEOUT, OP_TIME_UNIT);
                new MessageBuilder()
                        .append(invite.getUrl().toString())
                        .setEmbed(new EmbedBuilder()
                                .setTitle("Invite to official bot server")
                                .setTimestampToNow()
                                .setColor(Color.blue)
                                .setDescription("I created an invite for you on the official bot server. " +
                                        "It is disposable and valid for 3 minutes."))
                        .send(targetUser);
            } catch (Exception err) {
                String errorMessage = "Unable to create invite for user "
                        + targetUser.getMentionTag() + " (id: " + targetUser.getId() + "): "
                        + err.getMessage();
                log.error(errorMessage, err);
                super.showErrorMessage("Due to an internal error, I " +
                        "cannot create an invite for you.", event);
                BroadCast.sendServiceMessage(errorMessage);
            }
        } else {
            super.showErrorMessage("I do not have permission to create " +
                    "an invitation to the channel " + officialServerDefaultChannel.getName()
                    + " on server " + officialServer.getName(), event);
        }
    }

    @Override
    protected boolean privateMessageStep(@NotNull MessageCreateEvent event,
                                         @NotNull PrivateChannel privateChannel,
                                         @NotNull User user,
                                         @NotNull SessionState sessionState,
                                         boolean isBotOwner) {
        return false;
    }

    @Override
    protected boolean serverMessageStep(@NotNull MessageCreateEvent event,
                                        @NotNull Server server,
                                        @NotNull ServerTextChannel serverTextChannel,
                                        @NotNull User user,
                                        @NotNull SessionState sessionState,
                                        boolean isBotOwner) {
        return false;
    }

    @Override
    protected boolean privateReactionStep(boolean isAddReaction,
                                          @NotNull SingleReactionEvent event,
                                          @NotNull PrivateChannel privateChannel,
                                          @NotNull User user,
                                          @NotNull SessionState sessionState,
                                          boolean isBotOwner) {
        return false;
    }

    @Override
    protected boolean serverReactionStep(boolean isAddReaction,
                                         @NotNull SingleReactionEvent event,
                                         @NotNull Server server,
                                         @NotNull ServerTextChannel serverTextChannel,
                                         @NotNull User user,
                                         @NotNull SessionState sessionState,
                                         boolean isBotOwner) {
        return false;
    }
}
