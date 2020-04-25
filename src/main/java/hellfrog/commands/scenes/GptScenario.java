package hellfrog.commands.scenes;

import com.fasterxml.jackson.databind.ObjectMapper;
import hellfrog.common.BroadCast;
import hellfrog.common.CommonUtils;
import hellfrog.common.MessageUtils;
import hellfrog.common.SimpleHttpClient;
import hellfrog.core.ServerSideResolver;
import hellfrog.core.SessionState;
import hellfrog.settings.SettingsController;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.javacord.api.entity.channel.PrivateChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class GptScenario
        extends Scenario {

    private static final String PREFIX = "gpt";
    private static final String DESCRIPTION = "Randomly appends the specified text";
    private static final URI GPT_URI = URI.create("https://models.dobro.ai/gpt2/medium/");
    private final Bucket bucket;

    public GptScenario() {
        super(PREFIX, DESCRIPTION);
        Bandwidth bandwidth = Bandwidth.simple(1L, Duration.ofSeconds(3L));
        bucket = Bucket4j.builder().addLimit(bandwidth).build();
    }

    @Override
    protected void executePrivateFirstRun(@NotNull MessageCreateEvent event,
                                          @NotNull PrivateChannel privateChannel,
                                          @NotNull User user,
                                          boolean isBotOwner) {
        detachRun(event);
    }

    @Override
    protected void executeServerFirstRun(@NotNull MessageCreateEvent event,
                                         @NotNull Server server,
                                         @NotNull ServerTextChannel serverTextChannel,
                                         @NotNull User user,
                                         boolean isBotOwner) {
        detachRun(event);
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

    private void detachRun(@NotNull MessageCreateEvent event) {
        CompletableFuture.runAsync(() ->
                requestExternalText(event));
    }

    private void requestExternalText(@NotNull final MessageCreateEvent event) {
        try {
            bucket.asScheduler().consume(1);
        } catch (InterruptedException breakSignal) {
            return;
        }
        String eventMessage = event.getMessageContent();
        String messageWoBotPrefix = MessageUtils.getEventMessageWithoutBotPrefix(eventMessage,
                event.getServer());
        messageWoBotPrefix = ServerSideResolver.getReadableContent(messageWoBotPrefix, event.getServer());
        final String messageWoCommandPrefix =
                CommonUtils.cutLeftString(messageWoBotPrefix, PREFIX).trim();
        if (CommonUtils.isTrStringEmpty(messageWoCommandPrefix)) {
            return;
        }
        ObjectMapper objectMapper = new ObjectMapper();
        GptRequest gptRequest = new GptRequest();
        gptRequest.setPrompt(messageWoCommandPrefix);
        String postData;
        try {
            postData = objectMapper.writeValueAsString(gptRequest);
        } catch (Exception err) {
            showErrorMessage("Internal bot error", event);
            String errMsg = String.format("Unable serialize \"%s\" to JSON: %s",
                    gptRequest.toString(), err.getMessage());
            BroadCast.sendServiceMessage(errMsg);
            log.error(errMsg, err);
            return;
        }
        SimpleHttpClient client = SettingsController.getInstance()
                .getHttpClientsPool()
                .borrowClient();
        try {
            HttpPost post = generatePost(postData);
            String responseText;
            try (CloseableHttpResponse httpResponse = client.execute(post)) {
                HttpEntity entity = httpResponse.getEntity();
                if (entity == null) {
                    responseText = "";
                } else {
                    responseText = EntityUtils.toString(entity);
                    EntityUtils.consume(entity);
                }
                int statusCode = httpResponse.getStatusLine().getStatusCode();
                if (statusCode != HttpStatus.SC_OK) {
                    String message = String.format("Service HTTP error: %d", statusCode);
                    BroadCast.sendServiceMessage(message);
                    showErrorMessage(message, event);
                }
            } catch (Exception err) {
                String errMsg = String.format("Unable send request to GPT-server: %s", err.getMessage());
                log.error(errMsg, err);
                BroadCast.sendServiceMessage(errMsg);
                return;
            }

            GptResponse gptResponse;
            try {
                gptResponse = objectMapper.readValue(responseText, GptResponse.class);
            } catch (Exception err) {
                String errMsg = String.format("Unable decode json \"%s\": %s", responseText,
                        err.getMessage());
                BroadCast.sendServiceMessage(errMsg);
                log.error(errMsg, err);
                return;
            }

            if (CommonUtils.isTrStringNotEmpty(gptResponse.getDetail())) {
                BroadCast.sendServiceMessage("Fail to send request to GPT: " +
                        gptResponse.getDetail());
                return;
            }
            if (gptResponse.getReplies() == null || gptResponse.getReplies().isEmpty()) {
                return;
            }
            List<String> listOfMessagesText = CommonUtils.splitEqually(
                    "**" + messageWoCommandPrefix + "** " + gptResponse.getReplies().get(0), 1999);
            for (String msgText : listOfMessagesText) {
                EmbedBuilder embedBuilder = new EmbedBuilder()
                        .setDescription(msgText);
                Optional<Message> msg = super.displayMessage(embedBuilder, event.getChannel());
                if (msg.isEmpty()) {
                    return;
                }
            }
        } finally {
            SettingsController.getInstance()
                    .getHttpClientsPool()
                    .returnClient(client);
        }
    }

    @NotNull
    private HttpPost generatePost(@NotNull String postData) {
        HttpPost post = new HttpPost(GPT_URI);
        post.addHeader("Origin", "https://porfirevich.ru");
        post.addHeader("Referer", "https://porfirevich.ru/");
        post.addHeader("Host", "models.dobro.ai");
        post.addHeader("Content-Type", "text/plain;charset=UTF-8");
        post.setEntity(new StringEntity(postData, StandardCharsets.UTF_8));
        return post;
    }
}