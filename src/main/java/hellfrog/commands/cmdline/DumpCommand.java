package hellfrog.commands.cmdline;

import hellfrog.common.CommonUtils;
import hellfrog.core.ServerSideResolver;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.emoji.KnownCustomEmoji;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.Messageable;
import org.javacord.api.entity.permission.Permissions;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DumpCommand
        extends BotCommand {

    private static final String PREF = "dmp";
    private static final String DESCRIPTIONS = "Dump server info and data";

    private final Option infoOption = Option.builder("i")
            .longOpt("info")
            .hasArg()
            .optionalArg(true)
            .argName("server id")
            .desc("Dump server info (settings, channels, roles, members)")
            .build();

    public DumpCommand() {
        super(PREF, DESCRIPTIONS);

        super.setAdminCommand();
        super.addCmdlineOption(infoOption);
    }

    @Override
    protected void executeCreateMessageEventServer(Server server, CommandLine cmdline, ArrayList<String> cmdlineArgs, TextChannel channel, MessageCreateEvent event, ArrayList<String> anotherLines) {
        executeAction(cmdline, channel, event);
    }

    @Override
    protected void executeCreateMessageEventDirect(CommandLine cmdline, ArrayList<String> cmdlineArgs, TextChannel channel, MessageCreateEvent event, ArrayList<String> anotherLines) {
        executeAction(cmdline, channel, event);
    }

    private void executeAction(CommandLine cmdline, TextChannel channel, MessageCreateEvent event) {

        boolean serverInfo = cmdline.hasOption(infoOption.getOpt());
        String serverInfoIdValue = cmdline.getOptionValue(infoOption.getOpt(), "");

        if (serverInfo) {
            long serverId = 0L;
            if (!CommonUtils.isTrStringEmpty(serverInfoIdValue) && !CommonUtils.isLong(serverInfoIdValue)) {
                showErrorMessage("Parameter must be a server id", event);
                return;
            }
            if (event.getServer().isPresent()) {
                serverId = event.getServer().get().getId();
            }
            if (!CommonUtils.isTrStringEmpty(serverInfoIdValue) && CommonUtils.isLong(serverInfoIdValue)) {
                serverId = CommonUtils.onlyNumbersToLong(serverInfoIdValue);
            }
            if (serverId == 0L) {
                showErrorMessage("Server Id required", event);
                return;
            }

            event.getApi().getServerById(serverId).ifPresentOrElse(server -> {
                boolean isCanServerExecute = super.canExecuteServerCommand(event, server, channel.getId());
                boolean isCanGlobalExecute = super.canExecuteGlobalCommand(event);
                if (isCanGlobalExecute || isCanServerExecute) {
                    Messageable target = getMessageTargetByRights(event);
                    detachedGrabServerInfo(server, target);
                } else {
                    showAccessDeniedGlobalMessage(event);
                }
            }, () -> showErrorMessage("Server not found by this id", event));
        }
    }

    private void detachedGrabServerInfo(final Server server, final Messageable target) {
        StringBuilder sw = new StringBuilder();
        MessageBuilder res = new MessageBuilder();
        String serverName = server.getName();
        String currentDateTime = CommonUtils.getCurrentGmtTimeAsString();
        sw.append("Dump of server: ").append(serverName).append('\n');
        sw.append("From date: ").append(currentDateTime).append('\n');
        sw.append('\n');
        server.getSystemChannel().ifPresent(defaultChannel ->
                sw.append("Default channel: ").append(defaultChannel.getName()).append('\n'));
        String notificationLevel = server.getDefaultMessageNotificationLevel()
                .name()
                .toLowerCase()
                .replace("_", " ");
        sw.append("Default notification level: ").append(notificationLevel).append('\n');
        sw.append("Members count: ").append(server.getMemberCount()).append('\n');
        server.getApplicationId().ifPresent(appId ->
                sw.append("Created by bot with id: ").append(appId).append('\n'));
        String verificationLevel = server.getVerificationLevel()
                .name()
                .toLowerCase()
                .replace("_ ", " ");
        sw.append("Verification level: ").append(verificationLevel).append('\n');
        String explicitContentLevelFilter = server.getExplicitContentFilterLevel()
                .name()
                .toLowerCase()
                .replace("_", " ");
        sw.append("Explicit content level filter: ").append(explicitContentLevelFilter).append('\n');
        String multiFactorLevel = server.getMultiFactorAuthenticationLevel()
                .name()
                .toLowerCase()
                .replace("_ ", " ");
        sw.append("Multi factor authentication level: ").append(multiFactorLevel).append('\n');
        server.getAfkChannel().ifPresent(afkChannel ->
                sw.append("AFK channel: ").append(afkChannel.getName()).append('\n'));
        sw.append("AFK timeout: ").append(server.getAfkTimeoutInSeconds()).append(" sec.\n");
        String serverRegion = server.getRegion().getName();
        sw.append("Region: ").append(serverRegion).append('\n');
        sw.append('\n');
        sw.append("--- Roles list: ---\n");
        List<Role> roles = new ArrayList<>(server.getRoles());
        roles.sort((o1, o2) -> o2.getPosition() - o1.getPosition());
        for (Role role : roles) {
            String name = role.getName();
            String id = role.getIdAsString();
            sw.append(name).append('\n')
                    .append("  id: ").append(id).append('\n');
            role.getColor()
                    .ifPresent(c ->
                            sw.append("  color: #")
                                    .append(Integer.toHexString(c.getRed()))
                                    .append(Integer.toHexString(c.getGreen()))
                                    .append(Integer.toHexString(c.getBlue()))
                                    .append('\n'));
            sw.append("  mention tag: ").append(role.getMentionTag()).append('\n');
            sw.append("  position: ").append(role.getPosition()).append('\n');
            sw.append("  grants:").append('\n');
            ServerSideResolver.getAllowedGrants(role).ifPresent(list -> {
                sw.append("    allowed:\n");
                for (String line : CommonUtils.addLinebreaks(list, 110).split("\n"))
                    sw.append("      ").append(line).append('\n');
            });
            ServerSideResolver.getDeniedGrants(role).ifPresent(list -> {
                sw.append("    denied: ").append(list).append('\n');
                for (String line : CommonUtils.addLinebreaks(list, 110).split("\n"))
                    sw.append("      ").append(line).append('\n');
            });
        }
        sw.append('\n');
        sw.append("--- Channels: ---\n");
        List<ServerChannel> channels = server.getChannels();
        for (ServerChannel serverChannel : channels) {
            String channelName = serverChannel.getName();
            int channelPosition = serverChannel.getPosition();
            int rawPosition = serverChannel.getRawPosition();
            sw.append(channelName).append('\n');
            sw.append("  position: ").append(channelPosition).append('\n');
            sw.append("  raw position: ").append(rawPosition).append('\n');
            sw.append("  id: ").append(serverChannel.getId()).append('\n');
            serverChannel.asChannelCategory().ifPresent(channelCategory -> {
                sw.append("  type: category\n");
                sw.append("  nsfw: ").append(channelCategory.isNsfw() ? "enabled" : "disabled").append('\n');
            });
            serverChannel.asServerTextChannel().ifPresent(serverTextChannel -> {
                sw.append("  type: text channel\n");
                sw.append("  topic: [").append(serverTextChannel.getTopic()).append(']').append('\n');
                sw.append("  mention tag: ").append(serverTextChannel.getMentionTag()).append('\n');
                sw.append("  slowmode: ").append(serverTextChannel.hasSlowmode() ? "enabled" : "disabled")
                        .append(", delay: ").append(serverTextChannel.getSlowmodeDelayInSeconds())
                        .append(" sec.\n");
                sw.append("  nsfw: ").append(serverTextChannel.isNsfw() ? "enabled" : "disabled").append('\n');
            });
            serverChannel.asServerVoiceChannel().ifPresent(serverVoiceChannel -> {
                sw.append("  type: voice channel\n");
                int bitrate = serverVoiceChannel.getBitrate();
                sw.append("  bitrate: ").append(bitrate).append('\n');
                serverVoiceChannel.getUserLimit().ifPresent(limit ->
                        sw.append("  users limit: ").append(limit).append('\n'));
            });
            Map<Long, Permissions> overridesRolePerms = serverChannel.getOverwrittenRolePermissions();
            Map<Long, Permissions> overridesUserPerms = serverChannel.getOverwrittenUserPermissions();
            if (!overridesRolePerms.isEmpty() || !overridesUserPerms.isEmpty()) {
                sw.append("  permissions:\n");
                overridesRolePerms.forEach((roleId, rights) ->
                        server.getRoleById(roleId).ifPresent(role -> {
                            Optional<String> allowed = ServerSideResolver.getAllowedGrants(rights);
                            Optional<String> denied = ServerSideResolver.getDeniedGrants(rights);
                            sw.append("    target role:\n");
                            sw.append("      name: ").append(role.getName()).append('\n');
                            allowed.ifPresent(list ->
                                    sw.append("      allowed: ").append(list).append('\n'));
                            denied.ifPresent(list ->
                                    sw.append("      denied: ").append(list).append('\n'));
                        }));
                overridesUserPerms.forEach((userId, rights) ->
                        server.getMemberById(userId).ifPresent(user -> {
                            Optional<String> allowed = ServerSideResolver.getAllowedGrants(rights);
                            Optional<String> denied = ServerSideResolver.getDeniedGrants(rights);
                            sw.append("    target user:\n");
                            sw.append("      name: ").append(user.getName()).append('\n');
                            sw.append("      discriminated name: ").append(user.getDiscriminatedName()).append('\n');
                            user.getNickname(server).ifPresent(nickname ->
                                    sw.append("      nickname: ").append(nickname).append('\n'));
                            allowed.ifPresent(list ->
                                    sw.append("      allowed: ").append(list).append('\n'));
                            denied.ifPresent(list ->
                                    sw.append("      denied: ").append(list).append('\n'));
                        })
                );
            }
        }
        sw.append('\n');
        sw.append("--- Members list: ---\n");
        Optional<User> mayBeOwner = server.getOwner();
        for (User member : server.getMembers()) {
            String displayName = member.getDisplayName(server);
            String id = member.getIdAsString();
            String discriminatedName = member.getDiscriminatedName();
            sw.append(displayName).append('\n')
                    .append("  full name: ").append(discriminatedName).append('\n')
                    .append("  id: ").append(id).append('\n');
            mayBeOwner.ifPresent(owner -> {
                if (owner.equals(member)) {
                    sw.append("  This user is a server owner\n");
                }
            });
            server.getRoles(member)
                    .stream()
                    .map(Role::getName)
                    .filter(r -> !r.equals("@everyone"))
                    .reduce((r1, r2) -> r1 + ", " + r2)
                    .ifPresent(r -> sw.append("  roles: ").append(r).append('\n'));
        }
        server.getCustomEmojis().stream()
                .map(KnownCustomEmoji::getMentionTag)
                .reduce(CommonUtils::reduceConcat)
                .ifPresent(list ->
                        sw.append('\n')
                                .append("--- Emoji list: ---\n")
                                .append(CommonUtils.addLinebreaks(list, 120))
                );
        byte[] attach = sw.toString().getBytes(StandardCharsets.UTF_8);
        res.addAttachment(attach, "ServerInfo_" + server.getId() + ".txt");
        res.send(target);
    }
}
