package hellfrog.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hellfrog.common.sbergpt.GptRequest;
import hellfrog.common.sbergpt.GptResponse;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class SberGPT3Provider
        implements GptProvider {

    private static final URI GPT_URI = URI.create("https://api.aicloud.sbercloud.ru/public/v1/public_inference/gpt3/predict");
    private static final String FOOTER_TEXT = "by Sber GPT-3";
    private final Bucket bucket;
    private final Logger log = LogManager.getLogger(this.getClass().getSimpleName());

    public SberGPT3Provider() {
        Bandwidth bandwidth = Bandwidth.simple(1L, Duration.ofSeconds(3L));
        bucket = Bucket4j.builder().addLimit(bandwidth).build();
    }

    @Override
    public CompletableFuture<GptResult> appendText(String sourceText) {
        final CompletableFuture<GptResult> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            try {
                bucket.asScheduler().consume(1);
            } catch (InterruptedException err) {
                future.completeExceptionally(err);
                return;
            }

            BroadCast.MessagesLogger messagesLogger = BroadCast.getLogger();
            ObjectMapper objectMapper = new ObjectMapper();
            GptRequest gptRequest = new GptRequest();
            gptRequest.setText(sourceText);

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
                    if (statusCode != HttpStatus.SC_OK && statusCode != HttpStatus.SC_UNPROCESSABLE_ENTITY) {
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

                if (gptResponse.getDetail() != null && !gptResponse.getDetail().isEmpty()) {
                    String errMsg = "Fail to send request to GPT: :\n" + gptResponse.getDetail().stream()
                            .map(validationError ->
                                    validationError.getMsg() + (validationError.getLoc() != null && !validationError.getLoc().isEmpty() ?
                                            " (" + validationError.getLoc().stream().reduce(CommonUtils::reduceConcat) + ")" : ""))
                            .reduce(CommonUtils::reduceNewLine).orElse("(unknown)");
                    GptException gptException = new GptException(errMsg);
                    future.completeExceptionally(gptException);
                    return;
                }

                if (CommonUtils.isTrStringEmpty(gptResponse.getPredictions())) {
                    GptException gptException = new GptException("The cortex Chip answered silence");
                    future.completeExceptionally(gptException);
                    return;
                }

                GptResult result = new GptResult(CommonUtils.cutLeftString(gptResponse.getPredictions(), sourceText), FOOTER_TEXT);
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
        post.addHeader("accept", "application/json");
        post.addHeader("Content-Type", "application/json");
        post.setEntity(new StringEntity(postData, StandardCharsets.UTF_8));
        return post;
    }
}
