package cn;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import hellfrog.common.*;
import hellfrog.core.SessionState;
import hellfrog.core.SessionsCheckTask;
import hellfrog.settings.ServerStatistic;
import hellfrog.settings.SettingsController;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.emoji.KnownCustomEmoji;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.server.invite.InviteBuilder;
import org.javacord.api.entity.user.User;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Bd {

    private volatile static Server selectedServer = null;
    private volatile static TextChannel serverTextChannel = null;
    private volatile static User selectedUser = null;

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
                BroadCast.sendServiceMessage("Grab emoji from " + selectedServer +
                        " complete at file " + tmpZip);
                successCreate = true;
            } catch (IOException err) {
                err.printStackTrace();
                BroadCast.sendServiceMessage("Grab emoji from " + selectedServer +
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
                        BroadCast.sendServiceMessage("Cannot send archive, it" +
                                "'s very large");
                    } else {
                        new MessageBuilder()
                                .addAttachment(attach, tmpZip.getFileName().toString())
                                .send(selectedUser);
                        Files.deleteIfExists(tmpZip);
                    }
                } catch (IOException err) {
                    err.printStackTrace();
                    BroadCast.sendServiceMessage("Grab emoji from " + selectedServer +
                            " terminated with" + err.getMessage());
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
                                    BroadCast.sendServiceMessage("Unable to send" +
                                            " invite to " + _selectedUser + ": " + th);
                                    return null;
                                });
                        ;
                    });
        });

        return "OK";
    }

    @MethodInfo("Delete this bot message by url.")
    public static String rm(String url) {
        DiscordApi api = SettingsController.getInstance().getDiscordApi();
        if (api == null) return "API is null";

        if (CommonUtils.isTrStringEmpty(url)) return "URL String is empty";

        Message msg = MessageUtils.resolveByLink(url).orElse(null);
        if (msg == null) return "Message not exists";

        if (msg.getAuthor().isYourself()) {
            if (msg.canYouDelete()) {
                msg.delete();
                return "OK";
            } else {
                return "Cannot delete this message";
            }
        } else {
            return "It's not this bot message";
        }
    }

    @MethodInfo("Get statistics of selected server by sser() and send to user by sus()")
    public static String stat() {
        if (selectedServer == null)
            return "No server selected by sser() method";
        if (selectedUser == null)
            return "No user selected by sus() method";
        final Server server = selectedServer;
        final User user = selectedUser;
        SettingsController settingsController = SettingsController.getInstance();
        ServerStatistic serverStatistic = settingsController.getServerStatistic(server.getId());
        try (StringWriter strWriter = new StringWriter()) {
            String sinceStr = "";
            if (serverStatistic.getStartDate() > 0L) {
                Calendar since = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                since.setTimeInMillis(serverStatistic.getStartDate());
                sinceStr = String.format(" (since %tF %<tT (UTC))", since);
            }
            TreeMap<Long, List<String>> emojiStat = new TreeMap<>(Comparator.reverseOrder());
            TreeMap<Long, List<String>> userStats = serverStatistic.buildUserStats(new ArrayList<>());
            TreeMap<Long, List<String>> textChatStat = serverStatistic.buildTextChatStats(new ArrayList<>(),
                    new ArrayList<>());
            serverStatistic.getNonDefaultSmileStats()
                    .forEach((id, stat) -> {
                        if (stat.getUsagesCount() != null && stat.getUsagesCount().get() > 0L) {
                            long usagesCount = stat.getUsagesCount().get();
                            OptionalUtils.ifPresentOrElse(server.getCustomEmojiById(id),
                                    emoji -> {
                                        MessageBuilder tmp = new MessageBuilder()
                                                .append(String.valueOf(usagesCount))
                                                .append(" - ")
                                                .append(emoji)
                                                .append(" ");
                                        if (stat.getLastUsage() != null && stat.getLastUsage().get() > 0) {
                                            Calendar last = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                                            last.setTimeInMillis(stat.getLastUsage().get());
                                            tmp.append(String.format("last usage at %tF %<tT (UTC)", last));
                                        }
                                        if (!emojiStat.containsKey(usagesCount)) {
                                            List<String> emptyList = new ArrayList<>();
                                            emojiStat.put(usagesCount, emptyList);
                                        }
                                        emojiStat.get(usagesCount)
                                                .add(tmp.getStringBuilder().toString());
                                    }, () -> serverStatistic.getNonDefaultSmileStats()
                                            .remove(id));
                        }
                    });
            strWriter.append("Collected statistic").append(sinceStr).append(":")
                    .append('\n');
            strWriter.append(">> Custom emoji usage statistic:")
                    .append('\n');
            MessageBuilder msg = new MessageBuilder();
            ServerStatistic.appendResultStats(msg, emojiStat, 1);
            strWriter.append(msg.getStringBuilder().toString());
            strWriter.append(">> User message statistics:")
                    .append('\n');
            msg = new MessageBuilder();
            ServerStatistic.appendResultStats(msg, userStats, 1);
            strWriter.append(msg.getStringBuilder().toString());
            strWriter.append(">> Text chat message statistics:")
                    .append('\n');
            msg = new MessageBuilder();
            ServerStatistic.appendResultStats(msg, textChatStat, 1);
            strWriter.append(msg.getStringBuilder().toString());
            byte[] result = strWriter.toString().getBytes(StandardCharsets.UTF_8);
            new MessageBuilder()
                    .addAttachment(result, "stat.txt")
                    .send(user);
        } catch (IOException err) {
            return "Unable to generate stats: " + err.getMessage();
        }

        return "OK";
    }

    @MethodInfo("Terminate immediately all users scenario sessions")
    @NotNull
    public static String rstat() {
        SessionState.all().forEach(SessionsCheckTask::terminateSessionState);
        return "OK";
    }

    @MethodInfo("Show creation date and time of any Discord entity by ID")
    @NotNull
    public static String date(long entityId) {
        Instant instant = DiscordEntity.getCreationTimestamp(entityId);
        return instant.toString();
    }
}
