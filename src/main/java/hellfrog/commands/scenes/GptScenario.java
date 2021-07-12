package hellfrog.commands.scenes;

import hellfrog.common.*;
import org.javacord.api.entity.channel.PrivateChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class GptScenario
        extends OneShotScenario {

    private static final String PREFIX = "gpt";
    private static final String DESCRIPTION = "Randomly appends the specified text";
    private final GptProvider porfirevichGpt = new PorfirevichProvider();
    private final GptProvider yalmGpt = new YalmProvider();
    private final GptProvider sberGpt = new SberGPT3Provider();

    public GptScenario() {
        super(PREFIX, DESCRIPTION);
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

        final Optional<String> replyMessage = super.getReplyAllAvailableReadableContentWithoutPrefix(event);
        final String messageWoCommandPrefix = replyMessage.isPresent() && CommonUtils.isTrStringNotEmpty(replyMessage.get())
                ? replyMessage.get() : super.getReadableMessageContentWithoutPrefix(event);
        final String clearedMessage = messageWoCommandPrefix
                .replace("**", "")
                .replace("@", "");
        if (CommonUtils.isTrStringEmpty(clearedMessage)) {
            showErrorMessage("Text required", event);
            return;
        }
        tryToAppend(event, clearedMessage, getShuffled(), 0);
    }

    private void tryToAppend(@NotNull final MessageCreateEvent event,
                             @NotNull final String messageWoCommandPrefix,
                             @NotNull final List<GptProvider> providerList,
                             final int currentIndex) {
        if (currentIndex >= providerList.size()) {
            return;
        }
        GptProvider provider = providerList.get(currentIndex);
        provider.appendText(messageWoCommandPrefix)
                .thenAccept(gptResult -> {
                    String longText = "**" + messageWoCommandPrefix + "**" + gptResult.getResultText();
                    List<String> listOfMessagesText = CommonUtils.splitEqually(longText, 1999);
                    for (String msgText : listOfMessagesText) {
                        EmbedBuilder embedBuilder = new EmbedBuilder()
                                .setDescription(msgText)
                                .setFooter(gptResult.getFooterText());
                        Optional<Message> msg = super.displayMessage(embedBuilder, event.getChannel());
                        if (msg.isEmpty()) {
                            return;
                        }
                    }
                })
                .exceptionally(err -> {
                    if (currentIndex < providerList.size() - 1) {
                        tryToAppend(event, messageWoCommandPrefix, providerList, currentIndex + 1);
                    } else {
                        String footerMessage = null;
                        if (err instanceof GptException) {
                            footerMessage = ((GptException) err).getFooterMessage();
                        }
                        new MessageBuilder()
                                .setEmbed(new EmbedBuilder()
                                        .setAuthor(event.getApi().getYourself())
                                        .setDescription(err.getMessage())
                                        .setFooter(footerMessage)
                                        .setTimestampToNow())
                                .send(event.getChannel());
                    }
                    return null;
                });
    }

    @NotNull
    @UnmodifiableView
    private List<GptProvider> getShuffled() {
        List<GptProvider> providers = new ArrayList<>();
        providers.add(porfirevichGpt); // preferable, accepts any requests, but not always available
        providers.add(yalmGpt); // on the capacity of the corporation, but does not take sensitive questions
        Collections.shuffle(providers);
        providers.add(sberGpt); // very slow, generates a lot of text, most of which is not related to the template
        return Collections.unmodifiableList(providers);
    }
}
