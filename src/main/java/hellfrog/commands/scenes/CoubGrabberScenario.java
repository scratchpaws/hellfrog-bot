package hellfrog.commands.scenes;

import hellfrog.common.*;
import hellfrog.core.ServerSideResolver;
import hellfrog.settings.SettingsController;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
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
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CoubGrabberScenario
        extends OneShotScenario {

    private static final String PREFIX = "coub";
    private static final String DESCRIPTION = "Grab video from Coub url";
    private static final String VIDEO_FOR_SHARING = "cw_video_for_sharing";
    private final Bucket bucket;
    private final Pattern COUB_DETECT = Pattern.compile("/(embed|view)/\\w*", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private final Pattern MP4_VIDEO = Pattern.compile("mp4_.*_size.*\\.mp4", Pattern.CASE_INSENSITIVE);
    private final Pattern MP4_AUDIO = Pattern.compile("m4a_.*\\.m4a", Pattern.CASE_INSENSITIVE);
    private final Pattern MP3_AUDIO = Pattern.compile(".*\\.mp3$", Pattern.CASE_INSENSITIVE);
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public CoubGrabberScenario() {
        super(PREFIX, DESCRIPTION);
        super.enableStrictByChannels();
        Bandwidth bandwidth = Bandwidth.simple(1L, Duration.ofSeconds(1L));
        bucket = Bucket.builder().addLimit(bandwidth).build();
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

        final String userParameter = super.getMessageContentWithoutPrefix(event);
        if (CommonUtils.isTrStringEmpty(userParameter)) {
            showErrorMessage("Coub video required", event);
            return;
        }

        final URI coubUrl = findCoubURI(userParameter);
        if (coubUrl == null) {
            String errMsg = String.format("Can't find the coub url in text \"%s\"",
                    ServerSideResolver.getReadableContent(userParameter, event.getServer()));
            showErrorMessage(errMsg, event);
            return;
        }

        bucket.asScheduler().consume(1, executor);

        final SimpleHttpClient client = SettingsController.getInstance()
                .getHttpClientsPool()
                .borrowClient();
        try {
            ParsedCoubPage parsedCoubPage;
            try {
                parsedCoubPage = parseCoubPage(client, coubUrl);
            } catch (RuntimeException err) {
                showErrorMessage(err.getMessage(), event);
                return;
            }
            final List<String> jsonUrls = CommonUtils.detectAllUrls(parsedCoubPage.coubSourcesJson);
            final String userName = event.getMessageAuthor().asUser().map(user ->
                    event.getServer().map(user::getDisplayName).orElse(user.getName()))
                    .orElse("<unknown>");
            final String uncleanDescription = "<" + coubUrl + "> (from " + userName + ")"
                    + (CommonUtils.isTrStringNotEmpty(parsedCoubPage.audioTag) ? "\nTrack: " + parsedCoubPage.audioTag : "");
            final String description = ServerSideResolver.getReadableContent(uncleanDescription, event.getServer());
            boolean found = false;
            String mp4Video = null;
            String mp4Audio = null;
            String mp3Audio = null;
            for (String videoUrl : jsonUrls) {
                if (videoUrl.contains(VIDEO_FOR_SHARING)) {
                    found = true;
                    Path videoForSharePath = null;
                    try {
                        videoForSharePath = downloadCoubPart(client, videoUrl, coubUrl);
                        byte[] videoFile = Files.readAllBytes(videoForSharePath);
                        if (videoFile.length > CommonConstants.MAX_FILE_SIZE) {
                            String errMsg = String.format("Video from \"%s\" too large", coubUrl);
                            showErrorMessage(errMsg, event);
                            return;
                        }
                        new MessageBuilder()
                                .append(description)
                                .addAttachment(videoFile, "video.mp4")
                                .send(event.getChannel());
                    } catch (IOException err) {
                        String attachErrMsg = String.format("Unable to attach video from \"%s\", I/O error", coubUrl);
                        log.error(attachErrMsg, err);
                        showErrorMessage(attachErrMsg, event);
                        return;
                    } catch (RuntimeException err) {
                        showErrorMessage(err.getMessage(), event);
                        return;
                    } finally {
                        removeTempFile(videoForSharePath);
                    }
                } else if (mp4Video == null && MP4_VIDEO.matcher(videoUrl).find()) {
                    mp4Video = videoUrl;
                } else if (mp4Audio == null && MP4_AUDIO.matcher(videoUrl).find()) {
                    mp4Audio = videoUrl;
                } else if (mp3Audio == null && MP3_AUDIO.matcher(videoUrl).find()) {
                    mp3Audio = videoUrl;
                }
            }
            if (!found) {
                if (mp4Video != null && (mp4Audio != null || mp3Audio != null)) {
                    try {
                        byte[] mergedVideo = mergeSeparatedSources(client, mp4Video, mp4Audio, mp3Audio, coubUrl);
                        if (mergedVideo.length > CommonConstants.MAX_FILE_SIZE) {
                            String errMsg = String.format("Video from \"%s\" too large", coubUrl);
                            showErrorMessage(errMsg, event);
                            return;
                        }
                        new MessageBuilder()
                                .append(description)
                                .addAttachment(mergedVideo, "video.mp4")
                                .send(event.getChannel());
                    } catch (RuntimeException err) {
                        showErrorMessage(err.getMessage(), event);
                    }
                } else {
                    String errMsg = String.format("Video \"%s\" does not contain links for sharing", coubUrl);
                    showErrorMessage(errMsg, event);
                }
            }
        } finally {
            SettingsController.getInstance()
                    .getHttpClientsPool()
                    .returnClient(client);
        }
    }

    @Nullable
    private URI findCoubURI(@NotNull final String userParameter) {
        Matcher detectMatcher = COUB_DETECT.matcher(userParameter);
        if (detectMatcher.find()) {
            String urlValue = "https://coub.com" + detectMatcher.group().replace("/embed/", "/view/");
            try {
                return new URI(urlValue);
            } catch (URISyntaxException err) {
                return null;
            }
        }
        return null;
    }

    private ParsedCoubPage parseCoubPage(@NotNull final SimpleHttpClient client,
                                         @NotNull final URI coubUrl) throws RuntimeException {

        final HttpGet request = new HttpGet(coubUrl);
        String responseText;
        try (CloseableHttpResponse httpResponse = client.execute(request)) {
            final int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                String statusCodeErr = String.format("Unable to download page from \"%s\": HTTP %d", coubUrl, statusCode);
                throw new RuntimeException(statusCodeErr);
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
            String errMsg = String.format("Unable to download page from \"%s\": %s", coubUrl, clientErr.getMessage());
            log.error(errMsg, clientErr);
            throw new RuntimeException(errMsg);
        }
        if (CommonUtils.isTrStringEmpty(responseText)) {
            String errMsg = String.format("Coub.com received empty page from \"%s\"", coubUrl);
            throw new RuntimeException(errMsg);
        }
        final Document parsedDom = Jsoup.parse(responseText, coubUrl.toString());
        final Elements titles = parsedDom.getElementsByClass("musicTitle");
        final Elements authors = parsedDom.getElementsByClass("musicAuthor");
        final String title = getFirstContent(titles);
        final String author = getFirstContent(authors);
        final String audioTag = CommonUtils.isTrStringNotEmpty(title) || CommonUtils.isTrStringNotEmpty(author)
                ? author + " - " + title : "";
        final Element coubPageCoubJson = parsedDom.getElementById("coubPageCoubJson");
        if (coubPageCoubJson == null) {
            String errMsg = String.format("Unable to find videos urls from page \"%s\"", coubUrl);
            throw new RuntimeException(errMsg);
        }
        return new ParsedCoubPage(coubPageCoubJson.outerHtml(), audioTag);
    }

    private String getFirstContent(@NotNull final Elements elements) {
        if (elements.size() == 0) {
            return "";
        } else {
            return elements.get(0).text();
        }
    }

    private byte[] mergeSeparatedSources(@NotNull final SimpleHttpClient client,
                                         @NotNull final String mp4Video,
                                         @Nullable final String mp4Audio,
                                         @Nullable final String mp3Audio,
                                         @NotNull final URI coubUrl) throws RuntimeException {
        if (mp4Audio == null && mp3Audio == null) {
            String errMsg = String.format("Unable to find coub audio from page \"%s\"", coubUrl);
            throw new RuntimeException(errMsg);
        }
        Path mp4VideoPath = null;
        Path mp4AudioPath = null;
        Path mp3AudioPath = null;
        Path resultPath = null;
        try {
            mp4VideoPath = downloadCoubPart(client, mp4Video, coubUrl);
            if (mp4Audio == null) {
                mp3AudioPath = downloadCoubPart(client, mp3Audio, coubUrl);
                mp4AudioPath = FFMpegUtils.convertToM4A(mp3AudioPath);
            } else {
                mp4AudioPath = downloadCoubPart(client, mp4Audio, coubUrl);
            }
            final FFMpegDuration videoDuration = FFMpegUtils.getMediaDuration(mp4VideoPath);
            final FFMpegDuration audioDuration = FFMpegUtils.getMediaDuration(mp4AudioPath);
            final FFMpegDuration targetDuration = videoDuration.getTotalMillis() < audioDuration.getTotalMillis()
                    ? videoDuration : audioDuration;
            resultPath = FFMpegUtils.mergeVideo(mp4VideoPath, mp4AudioPath, targetDuration);
            return Files.readAllBytes(resultPath);
        } catch (IOException err) {
            String errMsg = String.format("Unable to convert video from page \"%s\"", coubUrl);
            log.error(errMsg, err);
            throw new RuntimeException(errMsg);
        } finally {
            removeTempFile(mp4AudioPath);
            removeTempFile(mp4VideoPath);
            removeTempFile(mp3AudioPath);
            removeTempFile(resultPath);
        }
    }

    private Path downloadCoubPart(@NotNull final SimpleHttpClient client,
                                  @NotNull final String partUrl,
                                  @NotNull final URI coubUrl) throws RuntimeException {
        URI uri;
        Path result;
        try {
            uri = new URI(partUrl);
        } catch (URISyntaxException err) {
            String uriErrMessage = String.format("Unable to parse URI media from \"%s\"", coubUrl);
            log.error(uriErrMessage, err);
            throw new RuntimeException(uriErrMessage);
        }
        final HttpGet mediaRequest = new HttpGet(uri);
        try (CloseableHttpResponse httpResponse = client.execute(mediaRequest)) {
            final int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                String statusCodeErr = String.format("Unable to grab media from \"%s\": %d", coubUrl, statusCode);
                throw new RuntimeException(statusCodeErr);
            }
            final HttpEntity httpEntity = httpResponse.getEntity();
            try {
                try {
                    result = CodeSourceUtils.getCodeSourceParent().resolve(Path.of(uri.getPath()).getFileName());
                    try (BufferedOutputStream bufOut = new BufferedOutputStream(Files.newOutputStream(result))) {
                        httpEntity.writeTo(bufOut);
                    }
                } catch (IOException fileSaveErr) {
                    String saveErrMsg = String.format("Unable to save media from \"%s\"", coubUrl);
                    log.error(saveErrMsg, fileSaveErr);
                    throw new RuntimeException(saveErrMsg);
                }
            } finally {
                EntityUtils.consume(httpEntity);
            }
        } catch (IOException err) {
            String errMsg = String.format("Unable to save media part from \"%s\"", coubUrl);
            log.error(errMsg, err);
            throw new RuntimeException(errMsg);
        }
        try {
            if (Files.size(result) == 0L) {
                String zeroSizeErr = String.format("Coub.com received empty media part from url \"%s\"", coubUrl);
                throw new RuntimeException(zeroSizeErr);
            }
        } catch (IOException err) {
            String errMsg = String.format("Unable to fetch media part size from url \"%s\"", coubUrl);
            log.error(errMsg, err);
            throw new RuntimeException(errMsg);
        }
        return result;
    }

    private void removeTempFile(@Nullable final Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException err) {
                String errMsg = String.format("Unable to delete temporary file \"%s\": %s", path, err.getMessage());
                log.error(errMsg, err);
            }
        }
    }

    private static class ParsedCoubPage {
        final String coubSourcesJson;
        final String audioTag;

        public ParsedCoubPage(String coubSourcesJson, String audioTag) {
            this.coubSourcesJson = coubSourcesJson;
            this.audioTag = audioTag;
        }
    }
}
