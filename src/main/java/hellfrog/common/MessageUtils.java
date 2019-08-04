package hellfrog.common;

import hellfrog.settings.SettingsController;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAttachment;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.Messageable;
import org.javacord.api.entity.server.Server;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nibor.autolink.LinkExtractor;
import org.nibor.autolink.LinkSpan;
import org.nibor.autolink.LinkType;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtils
    implements CommonConstants {

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

    public static void sendLongMessage(MessageBuilder messageBuilder, Messageable messageable) {

        if (messageBuilder.getStringBuilder().length() <= 2000) {
            messageBuilder.send(messageable);
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
                    msg.send(messageable);
                    msg = new MessageBuilder();
                }
                msg.append(line).appendNewLine();
            }
            if (msg.getStringBuilder().length() > 0) {
                msg.send(messageable);
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
                } catch (Exception ignore) {
                }
            }
        }

        return Optional.empty();
    }

    @NotNull
    public static String escapeSpecialSymbols(@Nullable String value) {
        if (CommonUtils.isTrStringEmpty(value)) return "";
        return value.replace("\\", "\\\\")
                .replace("`", "\\`")
                .replace("*", "\\*")
                .replace("_", "\\_")
                .replace("~", "\\~")
                .replace("|", "\\|");
    }

    public static void deleteMessageIfCan(@Nullable Message msg) {
        if (msg == null) return;
        if (msg.canYouDelete())
            msg.delete();
    }

    /**
     * Извлечение аттачей из сообщения в память.
     * Аттачи, которые неудалось извлечь - игнорируются.
     *
     * @param attachments аттачи из сообщения
     * @return сохранённые в памяти аттачи
     */
    public static List<InMemoryAttach> extractAttaches(Collection<MessageAttachment> attachments) {
        List<InMemoryAttach> result = new ArrayList<>(attachments.size());
        for (MessageAttachment attachment : attachments) {
            if (attachment.getSize() > MAX_FILE_SIZE) continue;
            try {
                String name = attachment.getFileName();
                byte[] attachBytes = attachment.downloadAsByteArray().get(OP_WAITING_TIMEOUT, TimeUnit.SECONDS);
                result.add(new InMemoryAttach(name, attachBytes));
            } catch (Exception ignore) {
            }
        }

        return result;
    }

    /**
     * Создание сообщения со списком URL, что бы для них отобразился предпросмотр.
     *
     * @param urls список URL
     * @param ch   текстовый канал, куда необходимо отправить сообщение
     */
    public static void writeUrls(@NotNull List<String> urls,
                                 TextChannel ch) {

        if (!urls.isEmpty()) {
            MessageBuilder linkBuilder = new MessageBuilder();
            urls.forEach(s -> linkBuilder.append(s).appendNewLine());
            MessageUtils.sendLongMessage(linkBuilder, ch);
        }
    }

    /**
     * Извлечение списка URL из содержимого исходного сообщения пользователя
     *
     * @param messageContent сообщение пользователя
     * @return список обнаруженных URL
     */
    public static List<String> extractAllUrls(String messageContent) {
        List<String> urls = new ArrayList<>();
        if (!CommonUtils.isTrStringEmpty(messageContent)) {
            for (LinkSpan span : LinkExtractor.builder()
                    .linkTypes(EnumSet.of(LinkType.WWW, LinkType.URL))
                    .build()
                    .extractLinks(messageContent)) {
                String url = messageContent.substring(span.getBeginIndex(), span.getEndIndex());
                if (url.endsWith("||")) {
                    url = url.substring(0, url.length() - 2);
                }
                urls.add(url);
            }
        }
        return Collections.unmodifiableList(urls);
    }

    /**
     * Отправка аттачей
     *
     * @param attachments список аттачей
     * @param ch          канал для отправки двухфазных сообщений
     */
    public static void sendAttachments(List<InMemoryAttach> attachments, TextChannel ch) {
        if (!attachments.isEmpty()) {
            for (InMemoryAttach attach : attachments) {
                try {
                    new MessageBuilder()
                            .addAttachment(attach.getBytes(), attach.getFileName())
                            .send(ch).get(OP_WAITING_TIMEOUT, TimeUnit.SECONDS);
                } catch (Exception ignore) {}
            }
        }
    }
}
