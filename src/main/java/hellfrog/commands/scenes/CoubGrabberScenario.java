package hellfrog.commands.scenes;

import hellfrog.common.CommonConstants;
import hellfrog.common.CommonUtils;
import hellfrog.common.SimpleHttpClient;
import hellfrog.settings.SettingsController;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.javacord.api.entity.channel.PrivateChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.nibor.autolink.LinkExtractor;
import org.nibor.autolink.LinkSpan;
import org.nibor.autolink.LinkType;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;

public class CoubGrabberScenario
        extends OneShotScenario {

    private static final String PREFIX = "coub";
    private static final String DESCRIPTION = "Grab video from Coub url";
    private final Bucket bucket;
    private static final String COUB_HOSTNAME = "coub.com";
    private static final String VIDEO_FOR_SHARING = "cw_video_for_sharing";

    public CoubGrabberScenario() {
        super(PREFIX, DESCRIPTION);
        super.enableStrictByChannels();
        Bandwidth bandwidth = Bandwidth.simple(1L, Duration.ofSeconds(1L));
        bucket = Bucket4j.builder().addLimit(bandwidth).build();
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

    private void detachRun(@NotNull final MessageCreateEvent event) {
        CompletableFuture.runAsync(() -> grabCoubVideo(event));
    }

    private void grabCoubVideo(@NotNull final MessageCreateEvent event) {

        final String messageWoCommandPrefix = super.getMessageContentWithoutPrefix(event);
        if (CommonUtils.isTrStringEmpty(messageWoCommandPrefix)) {
            showErrorMessage("Coub video required", event);
            return;
        }

        try {
            bucket.asScheduler().consume(1);
        } catch (InterruptedException breakSignal) {
            return;
        }

        URI uri;
        try {
            uri = URI.create(messageWoCommandPrefix);
        } catch (IllegalArgumentException uriParseErr) {
            String errorMessage = String.format("Unable to parse URL \"%s\": %s", messageWoCommandPrefix, uriParseErr.getMessage());
            showErrorMessage(errorMessage, event);
            return;
        }

        if (!uri.getHost().equalsIgnoreCase(COUB_HOSTNAME)) {
            String errorMessage = String.format("URL \"%s\" is not from coub.com", messageWoCommandPrefix);
            showErrorMessage(errorMessage, event);
            return;
        }

        final SimpleHttpClient client = SettingsController.getInstance()
                .getHttpClientsPool()
                .borrowClient();
        try {
            final HttpGet request = new HttpGet(uri);
            String responseText;
            try (CloseableHttpResponse httpResponse = client.execute(request)) {
                final int statusCode = httpResponse.getStatusLine().getStatusCode();
                if (statusCode != HttpStatus.SC_OK) {
                    String statusCodeErr = String.format("Unable to fetch video from \"%s\": %d", messageWoCommandPrefix, statusCode);
                    showErrorMessage(statusCodeErr, event);
                    return;
                }
                final HttpEntity httpEntity = httpResponse.getEntity();
                if (httpEntity == null) {
                    responseText = "";
                } else {
                    try {
                        responseText = EntityUtils.toString(httpEntity);
                    } finally {
                        EntityUtils.consume(httpEntity);
                    }
                }
            } catch (IOException clientErr) {
                String errMsg = String.format("Unable to fetch video from \"%s\": %s", messageWoCommandPrefix, clientErr.getMessage());
                showErrorMessage(errMsg, event);
                return;
            }
            if (CommonUtils.isTrStringEmpty(responseText)) {
                String errMsg = String.format("Coub.com received empty page from \"%s\"", messageWoCommandPrefix);
                showErrorMessage(errMsg, event);
                return;
            }
            final Document parsedDom = Jsoup.parse(responseText, messageWoCommandPrefix);
            final Element coubPageCoubJson = parsedDom.getElementById("coubPageCoubJson");
            if (coubPageCoubJson == null) {
                String errMsg = String.format("Unable to find videos urls from \"%s\"", messageWoCommandPrefix);
                showErrorMessage(errMsg, event);
                return;
            }
            final String rawJson = coubPageCoubJson.outerHtml();
            boolean found = false;
            for (LinkSpan span : LinkExtractor.builder()
                    .linkTypes(EnumSet.of(LinkType.WWW, LinkType.URL))
                    .build()
                    .extractLinks(rawJson)) {
                final String videoUrl = rawJson.substring(span.getBeginIndex(), span.getEndIndex());
                if (videoUrl.contains(VIDEO_FOR_SHARING)) {
                    found = true;
                    URI videoURI;
                    try {
                        videoURI = new URI(videoUrl);
                    } catch (URISyntaxException err) {
                        String uriErrMessage = String.format("Unable to parse URI video from \"%s\"", messageWoCommandPrefix);
                        showErrorMessage(uriErrMessage, event);
                        return;
                    }
                    final HttpGet videoRequest = new HttpGet(videoURI);
                    byte[] videoFile;
                    try (CloseableHttpResponse httpResponse = client.execute(videoRequest)) {
                        final int statusCode = httpResponse.getStatusLine().getStatusCode();
                        if (statusCode != HttpStatus.SC_OK) {
                            String statusCodeErr = String.format("Unable to grab video from \"%s\": %d", messageWoCommandPrefix, statusCode);
                            showErrorMessage(statusCodeErr, event);
                            return;
                        }
                        final HttpEntity httpEntity = httpResponse.getEntity();
                        try {
                            videoFile = EntityUtils.toByteArray(httpEntity);
                        } finally {
                            EntityUtils.consume(httpEntity);
                        }
                    } catch (IOException videoDlErr) {
                        String videoDlMessage = String.format("Unable to download video from page \"%s\": %s",
                                messageWoCommandPrefix, videoDlErr.getMessage());
                        showErrorMessage(videoDlMessage, event);
                        return;
                    }
                    if (videoFile == null || videoFile.length == 0) {
                        String errMsg = String.format("Coub.com received empty video from url \"%s\"", messageWoCommandPrefix);
                        showErrorMessage(errMsg, event);
                        return;
                    }
                    if (videoFile.length > CommonConstants.MAX_FILE_SIZE) {
                        String errMsg = String.format("Video from \"%s\" too large", messageWoCommandPrefix);
                        showErrorMessage(errMsg, event);
                        return;
                    }
                    new MessageBuilder()
                            .addAttachment(videoFile, "video.mp4")
                            .send(event.getChannel());
                }
            }
            if (!found) {
                String errMsg = String.format("Video \"%s\" does not contain links for sharing", messageWoCommandPrefix);
                showErrorMessage(errMsg, event);
            }
        } finally {
            SettingsController.getInstance()
                    .getHttpClientsPool()
                    .returnClient(client);
        }
    }
}
