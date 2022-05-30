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
import java.util.regex.Pattern;

public class CoubGrabberScenario
        extends OneShotScenario {

    private static final String PREFIX = "coub";
    private static final String DESCRIPTION = "Grab video from Coub url";
    private static final String VIDEO_FOR_SHARING = "cw_video_for_sharing";
    private final Bucket bucket;
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

        final URI coubUrl = CoubGrabber.findFirst(userParameter);
        if (coubUrl == null) {
            String errMsg = String.format("Can't find the coub url in text \"%s\"",
                    ServerSideResolver.getReadableContent(userParameter, event.getServer()));
            showErrorMessage(errMsg, event);
            return;
        }

        try {
            CoubGrabber.grabCoub(coubUrl, event.getChannel(), event.getMessageAuthor().asUser().orElse(null), event.getServer().orElse(null));
        } catch (RuntimeException err) {
            showErrorMessage(err.getMessage(), event);
        }
    }
}
