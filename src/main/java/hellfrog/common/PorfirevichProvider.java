package hellfrog.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hellfrog.common.porfirevich.GptRequest;
import hellfrog.common.porfirevich.GptResponse;
import hellfrog.settings.SettingsController;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class PorfirevichProvider
        implements GptProvider {

    private static final URI GPT_URI = URI.create("https://pelevin.gpt.dobro.ai/generate/");
    private static final String FOOTER_TEXT = "by Porfirevich";
    private final Bucket bucket;
    private final Logger log = LogManager.getLogger(this.getClass().getSimpleName());
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public PorfirevichProvider() {
        Bandwidth bandwidth = Bandwidth.simple(1L, Duration.ofSeconds(3L));
        bucket = Bucket.builder().addLimit(bandwidth).build();
    }

    @Override
    public CompletableFuture<GptResult> appendText(final String sourceText) {
        final CompletableFuture<GptResult> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {

            bucket.asScheduler().consume(1, scheduler);

            BroadCast.MessagesLogger messagesLogger = BroadCast.getLogger();
            ObjectMapper objectMapper = new ObjectMapper();
            GptRequest gptRequest = new GptRequest();
            gptRequest.setPrompt(sourceText);

            String postData;
            try {
                postData = objectMapper.writeValueAsString(gptRequest);
            } catch (JsonProcessingException err) {
                GptException gptException = new GptException("Internal bot error");
                String serviceMessage = String.format("%s: Unable serialize \"%s\" to JSON: %s",
                        this.getClass().getSimpleName(), gptRequest, err.getMessage());
                future.completeExceptionally(gptException);
                messagesLogger.addErrorMessage(serviceMessage).send();
                log.error(serviceMessage, err);
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
                        GptException gptException = new GptException("The cortex chip does not response");
                        gptException.setFooterMessage(String.format("HTTP code: %d", statusCode));
                        String serviceMessage = String.format("%s: Service HTTP error: %d",
                                this.getClass().getSimpleName(), statusCode);
                        future.completeExceptionally(gptException);
                        messagesLogger.addErrorMessage(serviceMessage);
                        return;
                    }
                } catch (Exception err) {
                    GptException gptException = new GptException("Internal bot error");
                    String serviceMessage = String.format("%s: Unable send request to GPT-server: %s",
                            this.getClass().getSimpleName(), err.getMessage());
                    future.completeExceptionally(gptException);
                    messagesLogger.addErrorMessage(serviceMessage);
                    log.error(serviceMessage, err);
                    return;
                }

                GptResponse gptResponse;
                try {
                    gptResponse = objectMapper.readValue(responseText, GptResponse.class);
                } catch (JsonProcessingException err) {
                    GptException gptException = new GptException("Can't recognize the flow of consciousness of the cortex chip");
                    String serviceMessage = String.format("Unable decode json \"%s\": %s", responseText,
                            err.getMessage());
                    future.completeExceptionally(gptException);
                    messagesLogger.addErrorMessage(serviceMessage);
                    log.error(serviceMessage, err);
                    return;
                }

                if (CommonUtils.isTrStringNotEmpty(gptResponse.getDetail())) {
                    GptException gptException = new GptException(gptResponse.getDetail());
                    future.completeExceptionally(gptException);
                    messagesLogger.addErrorMessage("Fail to send request to GPT: " +
                            gptResponse.getDetail());
                    return;
                }
                if (gptResponse.getReplies() == null || gptResponse.getReplies().isEmpty()) {
                    GptException gptException = new GptException("Cortex Chip answered silence");
                    future.completeExceptionally(gptException);
                    return;
                }

                GptResult result = new GptResult(gptResponse.getReplies().get(0), FOOTER_TEXT);
                future.complete(result);
            } finally {
                SettingsController.getInstance()
                        .getHttpClientsPool()
                        .returnClient(client);
                messagesLogger.send();
            }
        });
        return future;
    }

    @NotNull
    private HttpPost generatePost(@NotNull String postData) {
        HttpPost post = new HttpPost(GPT_URI);
        post.addHeader("Origin", "https://porfirevich.ru");
        //post.addHeader("Referer", "https://porfirevich.ru/");
        post.addHeader("Host", "pelevin.gpt.dobro.ai");
        post.addHeader("Content-Type", "text/plain;charset=UTF-8");
        post.setEntity(new StringEntity(postData, StandardCharsets.UTF_8));
        return post;
    }
}
