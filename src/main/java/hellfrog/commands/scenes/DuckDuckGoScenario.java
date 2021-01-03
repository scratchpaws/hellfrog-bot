package hellfrog.commands.scenes;

import com.fasterxml.jackson.databind.ObjectMapper;
import hellfrog.commands.scenes.ddgentity.DDGWebResult;
import hellfrog.commands.scenes.ddgentity.DDGWebResults;
import hellfrog.common.BroadCast;
import hellfrog.common.CommonUtils;
import hellfrog.common.SimpleHttpClient;
import hellfrog.core.ServerSideResolver;
import hellfrog.settings.SettingsController;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.javacord.api.entity.channel.PrivateChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageDecoration;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;

import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * DuckDuckGo search scenario. Based by DDG API (https://duckduckgo.com/api)
 */
public class DuckDuckGoScenario
        extends OneShotScenario {

    private static final String PREFIX = "ddg";
    private static final String DESCRIPTION = "Search by DuckDuckGo";
    private final Bucket bucket;

    public DuckDuckGoScenario() {
        super(PREFIX, DESCRIPTION);
        Bandwidth bandwidth = Bandwidth.simple(1L, Duration.ofSeconds(1L));
        bucket = Bucket4j.builder().addLimit(bandwidth).build();
        super.enableStrictByChannels();
        super.skipStrictByChannelWithAclBUg();
    }

    @Override
    protected void onPrivate(@NotNull MessageCreateEvent event,
                             @NotNull PrivateChannel privateChannel,
                             @NotNull User user,
                             boolean isBotOwner) {
        detachRun(event);
    }

    @Override
    protected void onServer(@NotNull MessageCreateEvent event,
                            @NotNull Server server,
                            @NotNull ServerTextChannel serverTextChannel,
                            @NotNull User user,
                            boolean isBotOwner) {
        detachRun(event);
    }

    private void detachRun(@NotNull MessageCreateEvent event) {
        CompletableFuture.runAsync(() ->
                requestDDGRequest(event));
    }

    private void requestDDGRequest(@NotNull final MessageCreateEvent event) {
        try {
            bucket.asScheduler().consume(1);
        } catch (InterruptedException breakSignal) {
            return;
        }

        final String messageWoCommandPrefix =
                super.getReadableMessageContentWithoutPrefix(event);
        if (CommonUtils.isTrStringEmpty(messageWoCommandPrefix)) {
            return;
        }

        SimpleHttpClient client = SettingsController.getInstance()
                .getHttpClientsPool()
                .borrowClient();
        BroadCast.MessagesLogger messagesLogger = BroadCast.getLogger();
        try {
            URI nonJsPageQuery;
            try {
                nonJsPageQuery = new URIBuilder()
                        .setHost("duckduckgo.com")
                        .setScheme("https")
                        .addParameter("q", messageWoCommandPrefix)
                        .addParameter("t", "ffsb")
                        .build();
            } catch (URISyntaxException err) {
                messagesLogger.addErrorMessage("DuckDuckGo URI build error: " + err.getMessage());
                showErrorMessage("Internal bot error", event);
                return;
            }

            HttpGet request = new HttpGet(nonJsPageQuery);
            String responseText = executeHttpRequest(client, request, event, messagesLogger);
            if (CommonUtils.isTrStringEmpty(responseText)) {
                return;
            }

            int firstScriptLocation = responseText.indexOf("/t.js?q=");
            if (firstScriptLocation < 0) {
                showErrorMessage("No search results found", event);
                return;
            }
            int secondScriptLocation = responseText.indexOf("/d.js?q=", firstScriptLocation + 1);
            if (secondScriptLocation < 0) {
                showErrorMessage("No search results found", event);
                return;
            }

            String firstScriptQuery;
            String secondScriptQuery;

            try {
                firstScriptQuery = responseText.substring(firstScriptLocation,
                        responseText.indexOf("');", firstScriptLocation));
                secondScriptQuery = responseText.substring(secondScriptLocation,
                        responseText.indexOf("');", secondScriptLocation));
            } catch (IndexOutOfBoundsException err) {
                showErrorMessage("Unable to parse search result", event);
                messagesLogger.addErrorMessage(DuckDuckGoScenario.class.getSimpleName() + " parse error: " +
                        err.getMessage());
                return;
            }

            URI firstScriptURI;
            URI secondScriptURI;
            try {
                firstScriptURI = new URI("https://duckduckgo.com" + firstScriptQuery);
                secondScriptURI = new URI("https://duckduckgo.com" + secondScriptQuery);
            } catch (URISyntaxException err) {
                showErrorMessage("Unable to parse search result", event);
                messagesLogger.addErrorMessage(DuckDuckGoScenario.class.getSimpleName() + " parse error: " +
                        err.getMessage());
                return;
            }

            HttpGet firstScriptGet = new HttpGet(firstScriptURI);
            HttpGet secondScriptGet = new HttpGet(secondScriptURI);

            String firstResponse = executeHttpRequest(client, firstScriptGet, event, messagesLogger);
            String secondResponse = executeHttpRequest(client, secondScriptGet, event, messagesLogger);
            if (CommonUtils.isTrStringEmpty(firstResponse) && CommonUtils.isTrStringEmpty(secondResponse)) {
                showErrorMessage("No search results found", event);
                return;
            }

            responseText = CommonUtils.isTrStringEmpty(firstResponse) ? secondResponse : firstResponse;

            int innerJsJsonPosStart;
            int innerJsJsonPosEnd;
            String innerJsonText;
            try {
                innerJsJsonPosStart = responseText.indexOf("DDG.pageLayout.load('d',[");
                if (innerJsJsonPosStart < 0) {
                    showErrorMessage("No search results found", event);
                    return;
                }
                innerJsJsonPosEnd = responseText.indexOf("]);", innerJsJsonPosStart);
                if (innerJsJsonPosEnd < 0) {
                    showErrorMessage("No search results found", event);
                    return;
                }
                innerJsonText = "{\"items\" : ["
                        + responseText.substring(innerJsJsonPosStart + "DDG.pageLayout.load('d',[".length(), innerJsJsonPosEnd)
                        + "]}";
            } catch (IndexOutOfBoundsException err) {
                showErrorMessage("Unable to parse search result", event);
                messagesLogger.addErrorMessage(DuckDuckGoScenario.class.getSimpleName() + " parse error: " +
                        err.getMessage());
                return;
            }

            ObjectMapper objectMapper = new ObjectMapper();
            DDGWebResults ddgWebResults;
            try {
                ddgWebResults = objectMapper.readValue(innerJsonText, DDGWebResults.class);
            } catch (Exception err) {
                String errMsg = String.format("Unable decode json \"%s\": %s", responseText,
                        err.getMessage());
                showErrorMessage("Unable to parse search result", event);
                messagesLogger.addErrorMessage(errMsg);
                log.error(errMsg, err);
                return;
            }

            if (ddgWebResults.length() == 0) {
                showErrorMessage("No search results found", event);
                return;
            }

            MessageBuilder result = new MessageBuilder();
            for (int i = 0; i < ddgWebResults.length() && i < 3; i++) {
                DDGWebResult ddgWebResult = ddgWebResults.getItems()[i];
                result.append(Jsoup.parse(ddgWebResult.getT()).text(), MessageDecoration.BOLD)
                        .appendNewLine()
                        .append("<").append(Jsoup.parse(ddgWebResult.getU()).text()).append(">")
                        .appendNewLine()
                        .append(Jsoup.parse(ddgWebResult.getA()).text())
                        .appendNewLine()
                        .appendNewLine();
            }

            String title = StringUtils.left(messageWoCommandPrefix, 200);

            new MessageBuilder()
                    .setEmbed(new EmbedBuilder()
                            .setDescription(ServerSideResolver.quoteEveryoneTags(result.getStringBuilder().toString()))
                            .setUrl(nonJsPageQuery.toString())
                            .setTimestampToNow()
                            .setFooter("Powered by DuckDuckGo")
                            .setTitle(title)
                            .setColor(Color.LIGHT_GRAY))
                    .send(event.getChannel());

        } finally {
            SettingsController.getInstance()
                    .getHttpClientsPool()
                    .returnClient(client);
            messagesLogger.send();
        }
    }

    private String executeHttpRequest(@NotNull SimpleHttpClient client,
                                      @NotNull HttpUriRequest request,
                                      @NotNull MessageCreateEvent event,
                                      @NotNull BroadCast.MessagesLogger messagesLogger) {
        try (CloseableHttpResponse httpResponse = client.execute(request)) {
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                String message = String.format("Service HTTP error: %d", statusCode);
                messagesLogger.addErrorMessage(message);
                showErrorMessage(message, event);
            }
            HttpEntity entity = httpResponse.getEntity();
            try {
                if (entity == null) {
                    return "";
                } else {
                    return EntityUtils.toString(entity);
                }

            } finally {
                EntityUtils.consume(entity);
            }
        } catch (Exception err) {
            String errMsg = String.format("Unable send request to DDG-server: %s", err.getMessage());
            log.error(errMsg, err);
            messagesLogger.addErrorMessage(errMsg);
            return "";
        }
    }
}
