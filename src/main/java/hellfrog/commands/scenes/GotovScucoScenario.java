package hellfrog.commands.scenes;

import hellfrog.common.BroadCast;
import hellfrog.common.SimpleHttpClient;
import hellfrog.common.Tuple;
import hellfrog.settings.SettingsController;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
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
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GotovScucoScenario
        extends OneShotScenario {

    private static final String PREFIX = "gotov";
    private static final String DESCRIPTION = "Get a custom recipe from gotov-suka.ru";
    private static final URI SERVICE_URI = URI.create("https://gotov-suka.ru/get_recipe/-1/-1/");
    private final Bucket bucket;

    public GotovScucoScenario() {
        super(PREFIX, DESCRIPTION);
        Bandwidth bandwidth = Bandwidth.simple(1L, Duration.ofSeconds(1L));
        bucket = Bucket4j.builder().addLimit(bandwidth).build();
        super.enableStrictByChannels();
        super.skipStrictByChannelWithAclBUg();
    }

    @Override
    protected void onPrivate(@NotNull MessageCreateEvent event, @NotNull PrivateChannel privateChannel, @NotNull User user, boolean isBotOwner) {
        detachRun(event);
    }

    @Override
    protected void onServer(@NotNull MessageCreateEvent event, @NotNull Server server, @NotNull ServerTextChannel serverTextChannel, @NotNull User user, boolean isBotOwner) {
        detachRun(event);
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

        SimpleHttpClient client = SettingsController.getInstance()
                .getHttpClientsPool()
                .borrowClient();
        BroadCast.MessagesLogger messagesLogger = BroadCast.getLogger();
        try {
            HttpGet request = new HttpGet(SERVICE_URI);
            String responseText;
            URI targetURL;
            try (CloseableHttpResponse httpResponse = client.execute(request)) {
                HttpEntity entity = httpResponse.getEntity();
                if (entity == null) {
                    responseText = "";
                } else {
                    try {
                        responseText = EntityUtils.toString(entity);
                    } finally {
                        EntityUtils.consume(entity);
                    }
                }
                int statusCode = httpResponse.getStatusLine().getStatusCode();
                if (statusCode != HttpStatus.SC_OK) {
                    String message = String.format("Service HTTP error: %d", statusCode);
                    messagesLogger.addErrorMessage(message);
                    showErrorMessage(message, event);
                }
                targetURL = client.getLatestURI(request);
            } catch (Exception err) {
                String errMsg = String.format("Unable send request to GPT-server: %s", err.getMessage());
                log.error(errMsg, err);
                messagesLogger.addErrorMessage(errMsg);
                return;
            }
            Document htmlDocument = Jsoup.parse(responseText, targetURL.toString());
            String title = "";
            Elements headers = htmlDocument.getElementsByClass("lead");
            if (!headers.isEmpty()) {
                title = headers.first().text();
            }
            List<Tuple<String, String>> ingredients = new ArrayList<>();
            Elements tables = htmlDocument.getElementsByTag("table");
            if (!tables.isEmpty()) {
                Element tableWithIngredients = tables.first();
                Elements tableRolls = tableWithIngredients.getElementsByTag("tr");
                for (Element roll : tableRolls) {
                    Elements cells = roll.getElementsByTag("td");
                    if (cells.size() >= 2) {
                        String product = cells.get(0).text();
                        String count = cells.get(1).text();
                        ingredients.add(Tuple.of(product, count));
                    }
                }
            }
            String imageUrl = "";
            Elements imageDivs = htmlDocument.getElementsByClass("col-lg-7");
            if (!imageDivs.isEmpty()) {
                Elements images = imageDivs.first().getElementsByTag("img");
                if (!images.isEmpty()) {
                    String source = images.first().attr("src");
                    try {
                        URI resultUrl = new URIBuilder()
                                .setScheme(SERVICE_URI.getScheme())
                                .setHost(SERVICE_URI.getHost())
                                .setPath(source)
                                .build();
                        imageUrl = resultUrl.toASCIIString();
                    } catch (URISyntaxException warn) {
                        String errMsg = String.format("Unable create URI of %s: %s",
                                source, warn.getMessage());
                        log.warn(errMsg, warn);
                        messagesLogger.addErrorMessage(errMsg);
                    }
                }
            }
            MessageBuilder description = new MessageBuilder()
                    .append("Тебе, сука, понадобятся:", MessageDecoration.UNDERLINE)
                    .appendNewLine();
            if (ingredients.isEmpty()) {
                description.append("(ингридиентов, сука, на сайте нет)")
                        .appendNewLine();
            } else {
                ingredients.forEach(ingredient -> description.append(ingredient.left)
                        .append(" ")
                        .append(ingredient.right)
                        .appendNewLine());
            }
            description.appendNewLine()
                    .append("Если руки не из жопы, получится:", MessageDecoration.UNDERLINE);
            new MessageBuilder()
                    .setEmbed(new EmbedBuilder()
                            .setTitle(title)
                            .setUrl(targetURL.toASCIIString())
                            .setDescription(description.getStringBuilder().toString())
                            .setTimestampToNow()
                            .setFooter("Нажми на заголовок, что бы открыть сайт")
                            .setImage(imageUrl))
                    .send(event.getChannel());
        } finally {
            SettingsController.getInstance()
                    .getHttpClientsPool()
                    .returnClient(client);
            messagesLogger.send();
        }
    }
}
