package hellfrog.commands.scenes;

import hellfrog.common.CommonUtils;
import hellfrog.common.DuckDuckGoSearch;
import hellfrog.common.OperationException;
import hellfrog.common.ddgentity.DDGSearchResult;
import hellfrog.core.ServerSideResolver;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import org.apache.commons.lang3.StringUtils;
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
import java.time.Duration;
import java.util.List;
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

        try {
            List<DDGSearchResult> searchResults = DuckDuckGoSearch.searchInEngine(messageWoCommandPrefix);

            MessageBuilder result = new MessageBuilder();
            for (int i = 0; i < searchResults.size() && i < 3; i++) {
                DDGSearchResult searchResult = searchResults.get(i);
                result.append(searchResult.getTitle(), MessageDecoration.BOLD)
                        .appendNewLine()
                        .append("<").append(searchResult.getUri()).append(">")
                        .appendNewLine()
                        .append(Jsoup.parse(searchResult.getDescription()).text())
                        .appendNewLine()
                        .appendNewLine();
            }

            String title = StringUtils.left(messageWoCommandPrefix, 200);

            new MessageBuilder()
                    .setEmbed(new EmbedBuilder()
                            .setDescription(ServerSideResolver.getReadableContent(result.getStringBuilder().toString(),
                                    event.getServer()))
                            .setTimestampToNow()
                            .setFooter("Powered by DuckDuckGo")
                            .setTitle(title)
                            .setColor(Color.LIGHT_GRAY))
                    .send(event.getChannel());
        } catch (OperationException err) {
            showErrorMessage(err.getUserMessage(), event);
        }
    }
}
