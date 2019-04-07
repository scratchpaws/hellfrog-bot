package xyz.funforge.scratchypaws.hellfrog.common;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.server.Server;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import xyz.funforge.scratchypaws.hellfrog.settings.SettingsController;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtils {

    public static final Pattern MESSAGE_LINK_SEARCH = Pattern.compile("https.*discord.*channels/\\d+/\\d+/\\d+", Pattern.MULTILINE);

    public static Optional<Message> resolveByLink(String messageWithLink) {
        if (CommonUtils.isTrStringEmpty(messageWithLink)) return Optional.empty();

        String url = findMessageUrl(messageWithLink);
        if (url != null && url.contains("channels")) {
                String[] rawIds = url.split("channels");
                if (rawIds.length >= 2) {
                    String idsLine = rawIds[1];
                    String[] ids = idsLine.split("/");
                    if (ids.length >= 4) {
                        // первая / после channels и пустая
                        long serverId = CommonUtils.onlyNumbersToLong(ids[1]);
                        long textChatId = CommonUtils.onlyNumbersToLong(ids[2]);
                        long messageId = CommonUtils.onlyNumbersToLong(ids[3]);

                        return MessageUtils.findByIds(serverId, textChatId, messageId);
                    }
                }
            }

        return Optional.empty();
    }

    @NotNull
    public static String getMessageUrl(Message message) {
        if (message == null) return "";
        if (message.getServer().isPresent()) {
            // https://discordapp.com/channels/525287388818178048/548202501304746014/564393688571314176
            long serverId = message.getServer().get().getId();
            long chatId = message.getChannel().getId();
            long messageId = message.getId();
            return "https://discordapp.com/channels/" + serverId + "/" + chatId + "/" + messageId;
        } else {
            // https://discordapp.com/channels/@me/530001909319204875/564381397934931968
            long chatId = message.getChannel().getId();
            long messageId = message.getId();
            return "https://discordapp.com/channels/@me/" + chatId + "/" + messageId;
        }
    }

    @Contract("null -> null")
    public static String findMessageUrl(String message) {
        if (message == null) return null;
        Matcher linkMatcher = MESSAGE_LINK_SEARCH.matcher(message);
        if (linkMatcher.find()) {
            return linkMatcher.group();
        } else {
            return null;
        }
    }

    public static void sendLongMessage(MessageBuilder messageBuilder, TextChannel channel) {

        if (messageBuilder.getStringBuilder().length() <= 2000) {
            messageBuilder.send(channel);
        } else {
            String[] lines = messageBuilder.getStringBuilder().toString()
                    .split("\n");
            List<String> rebuilds = new ArrayList<>(lines.length);
            for (String line : lines) {
                if (line.length() > 1999) {
                    rebuilds.addAll(CommonUtils.splitEqually(line, 1999));
                } else {
                    rebuilds.add(line);
                }
            }

            MessageBuilder msg = new MessageBuilder();
            for (String line : rebuilds) {
                int current = msg.getStringBuilder().length();
                int lineLen = line.length();
                if (current + lineLen + 1 > 2000) { // учитываем в т.ч. \n как 1 символ переноса
                    msg.send(channel);
                    msg = new MessageBuilder();
                }
                msg.append(line).appendNewLine();
            }
            if (msg.getStringBuilder().length() > 0) {
                msg.send(channel);
            }
        }
    }

    public static Optional<Message> findByIds(long serverId, long textChatId, long messageId) {
        DiscordApi api = SettingsController.getInstance().getDiscordApi();
        if (api == null) return Optional.empty();

        Optional<Server> mayBeServer = api.getServerById(serverId);
        if (mayBeServer.isPresent()) {
            Server srv = mayBeServer.get();
            Optional<ServerTextChannel> mayBeChannel = srv.getTextChannelById(textChatId);
            if (mayBeChannel.isPresent()) {
                ServerTextChannel tch = mayBeChannel.get();

                try {
                    return Optional.ofNullable(tch.getMessageById(messageId).get(10L, TimeUnit.SECONDS));
                } catch (Exception ignore) {}
            }
        }

        return Optional.empty();
    }
}
