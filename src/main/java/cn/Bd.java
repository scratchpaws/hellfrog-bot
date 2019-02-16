package cn;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.emoji.KnownCustomEmoji;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.server.invite.InviteBuilder;
import org.javacord.api.entity.user.User;
import org.jetbrains.annotations.NotNull;
import pub.funforge.scratchypaws.rilcobot.common.BroadCast;
import pub.funforge.scratchypaws.rilcobot.common.CodeSourceUtils;
import pub.funforge.scratchypaws.rilcobot.core.ServerSideResolver;
import pub.funforge.scratchypaws.rilcobot.reactions.DiceReaction;
import pub.funforge.scratchypaws.rilcobot.settings.SettingsController;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Bd {

    private volatile static Server selectedServer = null;
    private volatile static TextChannel serverTextChannel = null;
    private volatile static User selectedUser = null;
    private static final ReentrantLock dicesModifyLock = new ReentrantLock();

    @NotNull
    @MethodInfo("Get info of all other methods")
    public static String info() {
        StringBuilder sb = new StringBuilder()
                .append("Bot debug interface.\n")
                .append("Awailable methods:\n");
        Arrays.stream(Bd.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(MethodInfo.class))
                .map(method -> {
                    MethodInfo mi = method.getDeclaredAnnotation(MethodInfo.class);
                    String methodName = method.getName();
                    String arguments = Arrays.stream(method.getParameters())
                            .map(p -> {
                                String className = p.getType().getName();
                                String name = p.getName();
                                return className + " " + name;
                            }).reduce((s1, s2) -> s1 + ", " + s2)
                            .orElse("");
                    String returnType = method.getReturnType().getName();
                    return returnType + " " + methodName + "(" + arguments + "); - " + mi.value() + "\n";
                }).forEach(sb::append);
        return sb.toString();
    }

    @MethodInfo("Get servers list that this bot instance connected")
    public static List<Server> gcs() {
        DiscordApi discordApi = SettingsController.getInstance().getDiscordApi();
        if (discordApi == null) return new ArrayList<>();
        return new ArrayList<>(discordApi.getServers());
    }

    @MethodInfo("Convert output value to JSON value string")
    public static String jsn(Object obj) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException err) {
            return "Cant convert value to JSON string: " + err.getMessage();
        }
    }

    @MethodInfo("Select server for other methods. Input args: long id - server id")
    public static String sser(long id) {
        DiscordApi discordApi = SettingsController.getInstance().getDiscordApi();
        if (discordApi == null) return "Discord API not available";

        Optional<Server> srv = discordApi.getServerById(id);
        if (srv.isPresent()) {
            selectedServer = srv.get();
            return "OK";
        }
        return "Unable to find server";
    }

    @MethodInfo("Set text channel for other methods. Input args: long id - textchat id")
    public static String scht(long id) {
        DiscordApi discordApi = SettingsController.getInstance().getDiscordApi();
        if (discordApi == null) return "Discord API not available";

        Optional<TextChannel> textChannel = discordApi.getTextChannelById(id);
        if (textChannel.isPresent()) {
            serverTextChannel = textChannel.get();
            return "OK";
        }
        return "Unable to find text channel";
    }

    @MethodInfo("Set user for other methods. Input args: long id - user id")
    public static String sus(long id) {
        DiscordApi discordApi = SettingsController.getInstance().getDiscordApi();
        if (discordApi == null) return "Discord API not available";

        try {
            selectedUser = discordApi.getUserById(id).join();
        } catch (Exception err) {
            return "Unable to find user: " + err;
        }
        return "OK";
    }

    @NotNull
    @MethodInfo("Send text message to user. Required set user wia sus(). Input args: string message - text message")
    public static String msgu(String message) {
        DiscordApi discordApi = SettingsController.getInstance().getDiscordApi();
        if (discordApi == null) return "Discord API not available";
        if (selectedUser == null) return "Server text channel not selected wia sus()";

        try {
            new MessageBuilder()
                    .append(message)
                    .send(selectedUser)
                    .join();
        } catch (Exception err) {
            return "Unable to send message: " + err;
        }
        return "OK";
    }

    @NotNull
    @MethodInfo("Sent text message to chat. Required set user wia scht(). Input args: string message - text message")
    public static String msgc(String message) {
        DiscordApi discordApi = SettingsController.getInstance().getDiscordApi();
        if (discordApi == null) return "Discord API not available";
        if (serverTextChannel == null) return "Server text channel not selected wia scht()";

        try {
            new MessageBuilder()
                    .append(message)
                    .send(serverTextChannel)
                    .join();
        } catch (Exception err) {
            return "Unable to send message: " + err;
        }
        return "OK";
    }

    @MethodInfo("Get all members of server. Required set server wia sser().")
    public static List<User> gu() {
        DiscordApi discordApi = SettingsController.getInstance().getDiscordApi();
        if (discordApi == null) return new ArrayList<>();
        if (selectedServer == null) return new ArrayList<>();

        return new ArrayList<>(selectedServer.getMembers());
    }

    @MethodInfo("Grab all custom emoji from selected server and send to selected user. " +
            "Required set server wia sser() and user wia sus().")
    public static String grem() {
        DiscordApi discordApi = SettingsController.getInstance().getDiscordApi();
        if (discordApi == null) return "Discord API not awailable";
        if (selectedServer == null) return "No server selected wia sser()";
        if (selectedUser == null) return "No user selected wia sus()";

        Path parent;
        try {
            parent = CodeSourceUtils.getCodeSourceParent();
        } catch (IOException err) {
            return "Unablt to get code source directory: " + err;
        }

        Path tmpZip;
        try {
            tmpZip = Files.createTempFile(parent, "arch_", ".zip");
        } catch (IOException err) {
            return "Unable to create temporary file: " + err;
        }

        CompletableFuture.runAsync(() -> {
            boolean successCreate = false;
            try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(
                    Files.newOutputStream(tmpZip)))) {
                for (KnownCustomEmoji emoji : selectedServer.getCustomEmojis()) {
                    String fileName = emoji.getName() + (emoji.isAnimated() ? ".gif" : ".png");
                    ZipEntry zipEntry = new ZipEntry(fileName);
                    byte[] rawData = emoji.getImage().asByteArray().join();
                    zipEntry.setSize(rawData.length);
                    zos.putNextEntry(zipEntry);
                    zos.write(rawData);
                    zos.closeEntry();
                }
                BroadCast.sendBroadcastToAllBotOwners("Grab emoji from " + selectedServer +
                        " complete at file " + tmpZip);
                successCreate = true;
            } catch (IOException err) {
                err.printStackTrace();
                BroadCast.sendBroadcastToAllBotOwners("Grab emoji from " + selectedServer +
                        " terminated with " + err.getMessage());
                try {
                    Files.deleteIfExists(tmpZip);
                } catch (IOException ignore) {
                }
            }
            if (successCreate) {
                try {
                    byte[] attach = Files.readAllBytes(tmpZip);
                    if (attach.length >= (8 * 1024 * 1024)) {
                        BroadCast.sendBroadcastToAllBotOwners("Cannot send archive, it" +
                                "'s very large");
                    } else {
                        new MessageBuilder()
                                .addAttachment(attach, tmpZip.getFileName().toString())
                                .send(selectedUser);
                        Files.deleteIfExists(tmpZip);
                    }
                } catch (IOException err) {
                    err.printStackTrace();
                    BroadCast.sendBroadcastToAllBotOwners("Grab emoji from " + selectedServer +
                            " terminated with" + err.getMessage());
                }
            }
        });
        return "OK";
    }

    @MethodInfo("Grab users and roles list from server and send to selected user. " +
            "Required set server wia sser() and user wia sus().")
    public static String grab() {
        DiscordApi discordApi = SettingsController.getInstance().getDiscordApi();
        if (discordApi == null) return "Discord API not awailable";
        if (selectedServer == null) return "Server not selected by sser()";

        TextChannel _selectedChannel = serverTextChannel;
        User _selectedUser = selectedUser;
        Server _selectedServer = selectedServer;
        CompletableFuture.runAsync(() -> {
            if (_selectedChannel != null || _selectedUser != null) {
                StringBuilder sw = new StringBuilder();
                MessageBuilder res = new MessageBuilder();
                sw.append("--- Members list: ---\n");
                for (User member : _selectedServer.getMembers()) {
                    String displayName = member.getDisplayName(_selectedServer);
                    String id = member.getIdAsString();
                    String discriminatedName = member.getDiscriminatedName();
                    sw.append(displayName).
                            append(" (").append(discriminatedName).append(") [")
                            .append(id).append("]")
                            .append(", roles: ");
                    _selectedServer.getRoles(member)
                            .stream()
                            .map(Role::getName)
                            .reduce((r1, r2) -> r1 + ", " + r2)
                            .ifPresent(sw::append);
                    sw.append("; grants: ")
                            .append(ServerSideResolver.getGrants(selectedServer.getPermissions(member)))
                            .append('\n');
                }
                sw.append("--- Roles list: ---\n");
                Role everyone = _selectedServer.getEveryoneRole();
                sw.append("Everyone role: ").append(everyone)
                        .append("; grants: ")
                        .append(ServerSideResolver.getGrants(everyone.getPermissions()))
                        .append('\n');
                for (Role role : _selectedServer.getRoles()) {
                    String name = role.getName();
                    String id = role.getIdAsString();
                    sw.append(name)
                            .append(" [")
                            .append(id)
                            .append("], ");
                    role.getColor()
                            .ifPresent(c ->
                                    sw.append("color - #")
                                            .append(c.getRGB())
                                            .append(", "));
                    sw.append("mention tag: ")
                            .append(role.getMentionTag())
                            .append(", ");
                    sw.append("position: ").append(role.getPosition()).append(", ");
                    sw.append("permissions: ").append(ServerSideResolver.getGrants(role.getPermissions()));
                    sw.append("\n");
                }
                sw.append("--- Emoji list: ---\n");
                for (KnownCustomEmoji knownCustomEmoji : _selectedServer.getCustomEmojis()) {
                    String mentionTag = knownCustomEmoji.getMentionTag();
                    sw.append(mentionTag).append("\n");
                }
                byte[] attach = sw.toString().getBytes(StandardCharsets.UTF_8);
                res.addAttachment(attach, "MemberList.txt");
                if (selectedUser != null) {
                    res.send(_selectedUser);
                } else {
                    res.send(_selectedChannel);
                }
            }
        });
        return "OK";
    }

    @MethodInfo("Send temporary invite to selected user for selected server. " +
            "Required set server wia sser() and user wia sus().")
    public static String invite() {
        if (selectedServer == null || selectedUser == null) {
            return "User or server not selected";
        }

        Server _selectedServer = selectedServer;
        User _selectedUser = selectedUser;

        CompletableFuture.runAsync(() -> {
            _selectedServer.getSystemChannel()
                    .ifPresent(ch -> {
                        new InviteBuilder(ch)
                                .setMaxUses(1)
                                .setMaxAgeInSeconds(60 * 30)
                                .create()
                                .thenAccept(i -> new MessageBuilder()
                                        .append(i.getUrl())
                                        .send(_selectedUser))
                                .exceptionally(th -> {
                                    BroadCast.sendBroadcastToAllBotOwners("Unable to send" +
                                            " invite to " + _selectedUser + ": " + th);
                                    return null;
                                });
                        ;
                    });
        });

        return "OK";
    }
}
