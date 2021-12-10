package hellfrog.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import hellfrog.common.CommonConstants;
import hellfrog.common.InMemoryAttach;
import hellfrog.common.MessageUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.Embed;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MessagesForwarder
        implements CommonConstants, MessageCreateListener {

    private static final Logger log = LogManager.getLogger("Message forwarder");

    private final Map<Long, Long> remapMap = new ConcurrentHashMap<>();
    private static final String TEST_SETTINGS_PRESET = "https://discord.com/channels/612645599132778517/904681292635971664/904681605535252531";
    private static final long OP_LARGE_TIMEOUT = 600_000L;

    public MessagesForwarder() {
    }

    public void readSettingsFromMessage() {
        MessageUtils.resolveByLink(TEST_SETTINGS_PRESET).ifPresent(msg -> {
            ObjectMapper reader = buildMapper();
            String mayBeJson = msg.getContent();
            try {
                SaveLoad saved = reader.readValue(mayBeJson, SaveLoad.class);
                if (saved.getValues() != null && !saved.getValues().isEmpty()) {
                    remapMap.putAll(saved.getValues());
                }
            } catch (Exception err) {
                String errMsg = String.format("Unable to parse test settings preset message: %s", err.getMessage());
                log.error(errMsg, err);
            }
        });
    }

    private void saveSettingsToMessage() {
        MessageUtils.resolveByLink(TEST_SETTINGS_PRESET).ifPresent(msg -> {
            ObjectMapper writer = buildMapper();
            try {
                SaveLoad saved = new SaveLoad();
                saved.getValues().putAll(remapMap);
                String jsonString = writer.writeValueAsString(saved);
                msg.edit(jsonString);
            } catch (Exception err) {
                String errMsg = String.format("Unable to store settings to preset message: %s", err.getMessage());
                log.error(errMsg, err);
            }
        });
    }

    public void addForward(final long fromChannelId, final long toChannelId) {
        remapMap.put(fromChannelId, toChannelId);
        saveSettingsToMessage();
    }

    public void removeForward(final long fromChannelId) {
        remapMap.remove(fromChannelId);
        saveSettingsToMessage();
    }

    public String getForwards() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Long, Long> remap : remapMap.entrySet()) {
            sb.append(remap.getKey()).append(" => ").append(remap.getValue()).append('\n');
        }
        return sb.toString();
    }

    @Override
    public void onMessageCreate(@Nullable MessageCreateEvent event) {
        if (event == null) return;

        final long sourceChannelId = event.getChannel().getId();
        for (long fromId : remapMap.keySet()) {
            if (fromId == sourceChannelId) {
                Long targetId = remapMap.get(fromId);
                if (targetId != null) {
                    event.getApi().getTextChannelById(targetId).ifPresent(targetChannel ->
                            CompletableFuture.runAsync(() -> forwardMessage(event.getMessage(), targetChannel)));
                }
            }
        }
    }

    public void forwardHistory(final @NotNull TextChannel sourceChannel,
                               final @NotNull TextChannel targetChannel,
                               final @NotNull Instant fromDate,
                               final @NotNull Instant toDate) {
        final List<Message> historyMessages = new ArrayList<>();
        String infoMessage = String.format("Start forwarding from %s to %s", sourceChannel, targetChannel);
        log.info(infoMessage);
        grabHistoryMessages(sourceChannel, targetChannel, fromDate, toDate, historyMessages, null);
    }

    public void grabHistoryMessages(final @NotNull TextChannel sourceChannel,
                                    final @NotNull TextChannel targetChannel,
                                    final @NotNull Instant fromDate,
                                    final @NotNull Instant toDate,
                                    final @NotNull List<Message> historyMessages,
                                    final @Nullable Message lastHistoryMessage) {

        if (lastHistoryMessage == null) {
            sourceChannel.getMessages(1)
                    .thenAccept(messages -> messages.getOldestMessage().ifPresent(firstMessage -> {
                        Instant creationTime = firstMessage.getCreationTimestamp();
                        if (creationTime.isAfter(toDate) || creationTime.isBefore(fromDate)) {
                            return;
                        }
                        addMessageIfInDates(firstMessage, fromDate, toDate, historyMessages);
                        grabHistoryMessages(sourceChannel, targetChannel, fromDate, toDate, historyMessages, firstMessage);
                    }));
        } else {
            lastHistoryMessage.getMessagesBefore(50)
                    .thenAccept(messages -> {
                        for (Message msg : messages) {
                            addMessageIfInDates(msg, fromDate, toDate, historyMessages);
                        }
                        messages.getOldestMessage()
                                .ifPresentOrElse(oldest -> {
                                    Instant creationTime = oldest.getCreationTimestamp();
                                    if (creationTime.isBefore(fromDate)) {
                                        sendHistoryMessages(historyMessages, targetChannel);
                                    } else {
                                        grabHistoryMessages(sourceChannel, targetChannel, fromDate, toDate, historyMessages, oldest);
                                    }
                                }, () -> sendHistoryMessages(historyMessages, targetChannel));
                    }).exceptionally(err -> {
                        sendHistoryMessages(historyMessages, targetChannel);
                        return null;
                    });
        }
    }

    private void sendHistoryMessages(@NotNull final List<Message> historyMessages,
                                     @NotNull final TextChannel targetChannel) {
        CompletableFuture.runAsync(() -> {
            String infoMessage = String.format("Start dending %d messages to %s", historyMessages.size(), targetChannel);
            log.info(infoMessage);
            historyMessages.sort(Comparator.naturalOrder());
            for (Message message : historyMessages) {
                try {
                    generateAndSendEmbeddedMessage(message, targetChannel);
                } catch (Exception err) {
                    String errMsg = String.format("Message forwarder: unable to forward message %d to %s: %s",
                            message.getId(), targetChannel, err.getMessage());
                    log.error(errMsg, err);
                }
            }
            infoMessage = String.format("Sending %d messages to %s complete", historyMessages.size(), targetChannel);
            log.info(infoMessage);
        });
    }

    private void addMessageIfInDates(@NotNull final Message message,
                                     @NotNull final Instant fromDate,
                                     @NotNull final Instant toDate,
                                     @NotNull final List<Message> historyMessages) {
        Instant creationTime = message.getCreationTimestamp();
        if (creationTime.equals(fromDate) || creationTime.equals(toDate)) {
            historyMessages.add(message);
        } else if (creationTime.isAfter(fromDate) && creationTime.isBefore(toDate)) {
            historyMessages.add(message);
        }
    }

    public void forwardMessage(final @NotNull Message sourceMessage, final @NotNull TextChannel targetChannel) {
        CompletableFuture.runAsync(() -> {
            try {
                generateAndSendEmbeddedMessage(sourceMessage, targetChannel);
            } catch (Exception err) {
                String errMsg = String.format("Message forwarder: unable to forward message %d to %s: %s",
                        sourceMessage.getId(), targetChannel, err.getMessage());
                log.error(errMsg, err);
            }
        });
    }

    private void generateAndSendEmbeddedMessage(final @NotNull Message sourceMessage, final @NotNull TextChannel targetChannel) {
        List<InMemoryAttach> inMemoryAttaches = MessageUtils.extractAttaches(sourceMessage.getAttachments());
        List<String> extractedUrls = MessageUtils.extractAllUrls(sourceMessage.getContent());

        try {
            List<Embed> quotedEmbeds = sourceMessage.getEmbeds().stream()
                    .filter(embed -> embed.getProvider().isEmpty())
                    .collect(Collectors.toList());
            if (quotedEmbeds.size() > 0) {
                quotedEmbeds.get(0).getProvider();
                for (Embed embed : quotedEmbeds) {
                    new MessageBuilder()
                            .setEmbed(embed.toBuilder())
                            .send(targetChannel).get(OP_LARGE_TIMEOUT, OP_TIME_UNIT);
                }
            }
            if (!extractedUrls.isEmpty())
                MessageUtils.writeUrls(extractedUrls, targetChannel);
            if (!inMemoryAttaches.isEmpty())
                sendAttachments(inMemoryAttaches, targetChannel);
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    private  void sendAttachments(List<InMemoryAttach> attachments, TextChannel ch) {
        if (!attachments.isEmpty()) {
            for (InMemoryAttach attach : attachments) {
                try {
                    new MessageBuilder()
                            .addAttachment(attach.getBytes(), attach.getFileName())
                            .send(ch).get(OP_LARGE_TIMEOUT, OP_TIME_UNIT);
                } catch (Exception ignore) {
                }
            }
        }
    }

    @NotNull
    private ObjectMapper buildMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        return objectMapper;
    }

    private static class SaveLoad {

        private Map<Long, Long> values = new HashMap<>();

        public Map<Long, Long> getValues() {
            return values != null ? values : new HashMap<>();
        }

        public void setValues(Map<Long, Long> values) {
            this.values = values != null ? values : new HashMap<>();
        }
    }
}
