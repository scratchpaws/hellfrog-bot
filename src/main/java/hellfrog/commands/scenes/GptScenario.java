package hellfrog.commands.scenes;

import com.fasterxml.jackson.databind.ObjectMapper;
import hellfrog.common.BroadCast;
import hellfrog.common.CommonUtils;
import hellfrog.common.HttpClientAnyCookieProvider;
import hellfrog.core.SessionState;
import hellfrog.settings.SettingsController;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.PrivateChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
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
        Optional<Server> mayBeServer = event.getServer();
        String botPrefix;
        if (mayBeServer.isPresent()) {
            Server server = mayBeServer.get();
            botPrefix = SettingsController.getInstance()
                    .getBotPrefix(server.getId());
        } else {
            botPrefix = SettingsController.getInstance()
                    .getGlobalCommonPrefix();
        }
        String messageWoBotPrefix =
                CommonUtils.cutLeftString(event.getReadableMessageContent(), botPrefix).trim();
        final String messageWoCommandPrefix =
                CommonUtils.cutLeftString(messageWoBotPrefix, PREFIX).trim();
        if (CommonUtils.isTrStringEmpty(messageWoCommandPrefix)) {
            return;
        }
        DiscordApi discordApi = event.getApi();
        if (discordApi == null) {
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
        HttpClientContext httpClientContext = HttpClientContext.create();
        Registry<CookieSpecProvider> registry = RegistryBuilder.<CookieSpecProvider>create()
                .register("easy", new HttpClientAnyCookieProvider())
                .build();
        RequestConfig requestConfig = RequestConfig.custom()
                .setCookieSpec("easy")
                .build();
        httpClientContext.setRequestConfig(requestConfig);
        httpClientContext.setCookieSpecRegistry(registry);
        CookieStore cookieStore = new BasicCookieStore();
        httpClientContext.setCookieStore(cookieStore);
        HttpPost post = new HttpPost(GPT_URI);
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            post.addHeader("Origin", "https://porfirevich.ru");
            post.addHeader("Referer", "https://porfirevich.ru/");
            post.addHeader("Host", "models.dobro.ai");
            post.addHeader("Content-Type", "text/plain;charset=UTF-8");
            post.addHeader("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:68.0) " +
                            "Gecko/20100101 Firefox/68.0");
            post.setEntity(new StringEntity(postData, StandardCharsets.UTF_8));
            String responseText;
            try (CloseableHttpResponse httpResponse = client.execute(post, httpClientContext)) {
                HttpEntity entity = httpResponse.getEntity();
                if (entity == null) {
                    return;
                }
                responseText = EntityUtils.toString(entity);
                EntityUtils.consume(entity);
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
        } catch (IOException clientCreateError) {
            String errMsg = String.format("Unable create HTTP-client: %s", clientCreateError.getMessage());
            log.error(errMsg, clientCreateError);
            BroadCast.sendServiceMessage(errMsg);
        }
    }
}
