package hellfrog.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import hellfrog.common.CodeSourceUtils;
import hellfrog.common.CommonConstants;
import hellfrog.common.CommonUtils;
import hellfrog.common.Congratulation;
import hellfrog.settings.ServerPreferences;
import hellfrog.settings.SettingsController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAttachment;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public class CongratulationsController
        implements Runnable, CommonConstants {

    private final ScheduledFuture<?> scheduledFuture;
    private static final String CONGRATULATIONS_ATTACHMENTS_DIR = "congratulations_attaches";
    private static final String CONGRATULATIONS_DIR = "congratulations";
    private final Logger log = LogManager.getLogger(this.getClass().getSimpleName());
    private static final DirectoryStream.Filter<Path> ONLY_JSONS_FILTER = (path) -> Files.isRegularFile(path)
            && path.toString().toLowerCase().endsWith(".json");

    public CongratulationsController() {
        ScheduledExecutorService voiceService = Executors.newSingleThreadScheduledExecutor();
        scheduledFuture = voiceService.scheduleWithFixedDelay(this, 30L, 30L, TimeUnit.MINUTES);
    }

    @Override
    public void run() {
        final SettingsController settingsController = SettingsController.getInstance();
        final DiscordApi api = settingsController.getDiscordApi();

        for (long serverId : settingsController.getServerListWithConfig()) {
            api.getServerById(serverId).ifPresent(server -> {
                final ServerPreferences serverPreferences = settingsController.getServerPreferences(serverId);

                Long congratulationChannelId = serverPreferences.getCongratulationChannel();
                if (congratulationChannelId != null && congratulationChannelId > 0L) {
                    server.getTextChannelById(congratulationChannelId).ifPresent(targetChannel -> {

                        String timezoneId = serverPreferences.getTimezone();
                        TimeZone timeZone = TimeZone.getDefault();
                        if (CommonUtils.isTrStringNotEmpty(timezoneId)) {
                            try {
                                timeZone = TimeZone.getTimeZone(timezoneId);
                            } catch (Exception tzErr) {
                                String errMsg = String.format("Unable to parse timezone \"%s\": %s",
                                        timezoneId, tzErr.getMessage());
                                log.error(errMsg, tzErr);
                            }
                        }

                        Calendar currentDateTime = Calendar.getInstance(timeZone);

                        try {
                            Path textsPath = CodeSourceUtils.getCodeSourceParent()
                                    .resolve(CONGRATULATIONS_DIR)
                                    .resolve(server.getIdAsString());
                            Path attachesPath = CodeSourceUtils.getCodeSourceParent()
                                    .resolve(CONGRATULATIONS_ATTACHMENTS_DIR)
                                    .resolve(server.getIdAsString());
                            boolean existsAttachesPath = false;

                            if (Files.notExists(textsPath) || !Files.isDirectory(textsPath)) {
                                return;
                            }
                            if (Files.exists(attachesPath) && Files.isDirectory(attachesPath)) {
                                existsAttachesPath = true;
                            }

                            List<Path> texts = new ArrayList<>();
                            try (DirectoryStream<Path> textsStream = Files.newDirectoryStream(textsPath, ONLY_JSONS_FILTER)) {
                                for (Path item : textsStream) {
                                    long messageId = CommonUtils.onlyNumbersToLong(item.getFileName().toString());
                                    Instant instant = DiscordEntity.getCreationTimestamp(messageId);
                                    ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, timeZone.toZoneId());
                                    Calendar messageDate = GregorianCalendar.from(zdt);
                                    if (messageDate.get(Calendar.DAY_OF_YEAR) != currentDateTime.get(Calendar.DAY_OF_YEAR)) {
                                        texts.add(item);
                                    }
                                }
                            } catch (IOException err) {
                                String errMsg = String.format("Unable to list congratulations messages directory \"%s\": %s",
                                        textsPath, err.getMessage());
                                log.error(errMsg, err);
                                return;
                            }

                            if (texts.isEmpty()) {
                                return;
                            }

                            int selected = ThreadLocalRandom.current().nextInt(0, texts.size());
                            Path inputJson = texts.get(selected);
                            boolean dropJson = false;
                            List<Path> attachesToDrop = new ArrayList<>();

                            try (BufferedReader reader = Files.newBufferedReader(inputJson, StandardCharsets.UTF_8)) {
                                ObjectMapper objectMapper = buildMapper();
                                Congratulation congratulation = objectMapper.readValue(reader, Congratulation.class);
                                if (congratulation.getAuthorId() > 0L && CommonUtils.isTrStringNotEmpty(congratulation.getMessage())) {
                                    try {
                                        User user = api.getUserById(congratulation.getAuthorId())
                                                .get(CommonConstants.OP_WAITING_TIMEOUT, CommonConstants.OP_TIME_UNIT);
                                        MessageBuilder messageBuilder = new MessageBuilder();
                                        if (existsAttachesPath && congratulation.getAttaches() != null && !congratulation.getAttaches().isEmpty()) {
                                            for (String attachStringPath : congratulation.getAttaches()) {
                                                try {
                                                    Path attachPath = Path.of(attachStringPath);
                                                    if (Files.exists(attachPath) && Files.isRegularFile(attachPath)) {
                                                        if (Files.size(attachPath) <= CommonConstants.MAX_FILE_SIZE) {
                                                            byte[] data = Files.readAllBytes(attachPath);
                                                            messageBuilder.addAttachment(data, attachPath.getFileName().toString());
                                                        }
                                                        attachesToDrop.add(attachesPath);
                                                    }
                                                } catch (IOException readErr) {
                                                    String errMsg = String.format("Unable to read attachments from \"%s\": %s",
                                                            attachStringPath, readErr.getMessage());
                                                    log.error(errMsg, readErr);
                                                }
                                            }
                                        }
                                        messageBuilder.setEmbed(new EmbedBuilder()
                                                .setAuthor(user)
                                                .setColor(Color.GREEN)
                                                .setTimestampToNow()
                                                .setDescription(congratulation.getMessage()));

                                        try {
                                            messageBuilder.send(targetChannel)
                                                    .get(CommonConstants.OP_WAITING_TIMEOUT, CommonConstants.OP_TIME_UNIT);
                                            dropJson = true;
                                        } catch (TimeoutException | InterruptedException | ExecutionException sendErr) {
                                            String errMsg = String.format("Unable to check that message from \"%s\" sent: %s",
                                                    inputJson, sendErr.getMessage());
                                            log.error(errMsg, sendErr);
                                        }
                                    } catch (TimeoutException | InterruptedException | ExecutionException getUserErr) {
                                        String errMsg = String.format("Unable to find user from \"%s\": %s",
                                                inputJson, getUserErr.getMessage());
                                        log.error(errMsg, getUserErr);
                                    }
                                }
                            } catch (IOException readJsonErr) {
                                String errMsg = String.format("Unable to read congratulation json \"%s\": %s",
                                        inputJson, readJsonErr.getMessage());
                                log.error(errMsg, readJsonErr);
                            }

                            if (dropJson) {
                                try {
                                    Files.deleteIfExists(inputJson);
                                } catch (IOException err) {
                                    String errMsg = String.format("Unable to cleanup congratulation json \"%s\": %s",
                                            inputJson, err.getMessage());
                                    log.error(errMsg, err);
                                }
                                for (Path item : attachesToDrop) {
                                    try {
                                        Files.deleteIfExists(item);
                                    } catch (IOException err) {
                                        String errMsg = String.format("Unable to cleanup congratulation attachment \"%s\": %s",
                                                item, err.getMessage());
                                        log.error(errMsg, err);
                                    }
                                }
                            }
                        } catch (IOException rpathErr) {
                            String errMsg = String.format("Unable to resolve root classpath dir: %s", rpathErr.getMessage());
                            log.error(errMsg, rpathErr);
                        }
                    });
                }
            });
        }
    }

    public void stop() {
        scheduledFuture.cancel(false);
        while (!scheduledFuture.isCancelled() || !scheduledFuture.isDone()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException brk) {
                scheduledFuture.cancel(true);
            }
        }
    }

    public void processAndSave(@NotNull final MessageCreateEvent event) {
        event.getMessageAuthor().asUser().ifPresent(author -> {
            if (author.isYourself() || author.isBot()) {
                return;
            }

            event.getServer().ifPresent(server -> {
                event.getServerTextChannel().ifPresent(ch -> {
                    final SettingsController settingsController = SettingsController.getInstance();
                    final ServerPreferences serverPreferences = settingsController.getServerPreferences(server.getId());

                    Long congratulationChannelId = serverPreferences.getCongratulationChannel();
                    if (congratulationChannelId != null && congratulationChannelId > 0L) {
                        server.getTextChannelById(congratulationChannelId).ifPresent(sourceChannel -> {
                            if (ch.getId() == sourceChannel.getId()) {
                                final Message message = event.getMessage();
                                String text = message.getContent();
                                long authorId = message.getAuthor().getId();
                                List<String> attaches = new ArrayList<>();
                                try {
                                    Path textsPath = CodeSourceUtils.getCodeSourceParent()
                                            .resolve(CONGRATULATIONS_DIR)
                                            .resolve(server.getIdAsString());
                                    Path attachesPath = CodeSourceUtils.getCodeSourceParent()
                                            .resolve(CONGRATULATIONS_ATTACHMENTS_DIR)
                                            .resolve(server.getIdAsString());
                                    try {
                                        if (Files.notExists(textsPath)) {
                                            Files.createDirectories(textsPath);
                                        }
                                    } catch (IOException crDirErr) {
                                        String errMsg = String.format("Unable to create congratulations texts root directory \"%s\": %s",
                                                attachesPath, crDirErr.getMessage());
                                        log.error(errMsg, crDirErr);
                                        return;
                                    }
                                    try {
                                        if (Files.notExists(attachesPath)) {
                                            Files.createDirectories(attachesPath);
                                        }
                                        for (MessageAttachment attachment : message.getAttachments()) {
                                            String saveName = message.getId() + "_" + attachment.getFileName();
                                            Path savePath = attachesPath.resolve(saveName);
                                            try {
                                                byte[] data = attachment.downloadAsByteArray().get(CommonConstants.OP_WAITING_TIMEOUT, CommonConstants.OP_TIME_UNIT);
                                                if (data.length > CommonConstants.MAX_FILE_SIZE) {
                                                    continue;
                                                }
                                                Files.write(savePath, data, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
                                                attaches.add(savePath.toString());
                                            } catch (IOException err) {
                                                String errMsg = String.format("Unable to write attach to file \"%s\": %s",
                                                        savePath, err.getMessage());
                                                log.error(errMsg, err);
                                            } catch (TimeoutException | InterruptedException | ExecutionException err) {
                                                String errMsg = String.format("Unable to download attachment from message \"%s\": %s",
                                                        message.getLink(), err.getMessage());
                                                log.error(errMsg, err);
                                            }
                                        }
                                    } catch (IOException crDirErr) {
                                        String errMsg = String.format("Unable to create congratulations attachments root directory \"%s\": %s",
                                                attachesPath, crDirErr.getMessage());
                                        log.error(errMsg, crDirErr);
                                    }

                                    Congratulation congratulation = new Congratulation();
                                    congratulation.setMessage(text);
                                    congratulation.setAuthorId(authorId);
                                    congratulation.setAttaches(attaches);
                                    congratulation.setServerId(server.getId());

                                    Path textFilePath = textsPath.resolve(message.getIdAsString() + ".json");
                                    try (BufferedWriter writer = Files.newBufferedWriter(textFilePath, StandardCharsets.UTF_8)) {
                                        ObjectMapper mapper = buildMapper();
                                        mapper.writeValue(writer, congratulation);
                                    } catch (IOException err) {
                                        String errMsg = String.format("Unable to save message \"%s\" to file \"%s\": %s",
                                                message.getLink(), textFilePath, err.getMessage());
                                        log.error(errMsg, err);
                                        return;
                                    }

                                    if (message.canYouDelete()) {
                                        message.delete();
                                    }

                                } catch (IOException rpathErr) {
                                    String errMsg = String.format("Unable to resolve root classpath dir: %s", rpathErr.getMessage());
                                    log.error(errMsg, rpathErr);
                                }
                            }
                        });
                    }
                });
            });
        });

    }

    @NotNull
    private ObjectMapper buildMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        return objectMapper;
    }
}
