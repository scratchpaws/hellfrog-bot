package xyz.funforge.scratchypaws.hellfrog.reactions;

import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.Embed;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.funforge.scratchypaws.hellfrog.common.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class QuoteReaction
        extends MsgCreateReaction
        implements CommonConstants {


    private static final String PREFIX = "qt";
    private static final String DESCRIPTION = "Quote message by link (use qt [link|message id] for quote)";

    private static final Pattern QUOTE_SEARCH = Pattern.compile("^[qtцт]{2}.*channels/\\d+/\\d+/\\d+",
            Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern SIMPLE_SEARCH = Pattern.compile("^[qtцт]{2}\\s*\\d+",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public QuoteReaction() {
        super.setCommandPrefix(PREFIX);
        super.setCommandDescription(DESCRIPTION);
        super.enableAccessControl();
        super.enableStrictByChannel();
    }

    @Override
    public boolean canReact(MessageCreateEvent event) {
        String message = event.getMessageContent();
        return QUOTE_SEARCH.matcher(message).find()
                || SIMPLE_SEARCH.matcher(message).find();
    }

    @Override
    void parallelExecuteReact(String strMessage,
                              @Nullable Server server, @Nullable User user,
                              TextChannel textChannel, Instant messageCreateDate, Message sourceMessage) {
        if (server == null || user == null) return;

        Message messageLink = null;
        String responseMessage = null;

        Matcher quote = QUOTE_SEARCH.matcher(strMessage);
        if (quote.find()) {
            String found = quote.group();
            messageLink = MessageUtils.resolveByLink(found).orElse(null);
            responseMessage = strMessage.substring(quote.end()).strip();
        }

        quote = SIMPLE_SEARCH.matcher(strMessage);
        if (quote.find()) {
            String found = quote.group();
            long messageId = CommonUtils.onlyNumbersToLong(found);
            messageLink = MessageUtils.findByIds(server.getId(), textChannel.getId(), messageId)
                    .orElse(null);
            responseMessage = strMessage.substring(quote.end()).strip();
        }

        if (messageLink == null) return;

        generateAndSendEmbeddedMessage("Quoted message:",
                messageLink, messageLink.getContent(), textChannel);

        if (!CommonUtils.isTrStringEmpty(responseMessage)) {
            generateAndSendEmbeddedMessage("Response message:",
                    sourceMessage, responseMessage, textChannel);
        }

        if (sourceMessage.canYouDelete()) {
            try {
                Thread.sleep(5_000L);
            } catch (InterruptedException ignore) {
            }
            sourceMessage.delete();
        }
    }

    private void generateAndSendEmbeddedMessage(String embedTitle, @NotNull Message sourceMessage, String messageText, TextChannel textChannel) {
        List<InMemoryAttach> inMemoryAttaches = MessageUtils.extractAttaches(sourceMessage.getAttachments());
        List<String> extractedUrls = MessageUtils.extractAllUrls(messageText);

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(embedTitle);
        embedBuilder.setTimestamp(sourceMessage.getLastEditTimestamp()
                .orElse(sourceMessage.getCreationTimestamp()));
        embedBuilder.setDescription(messageText);
        sourceMessage.getUserAuthor().ifPresent(user -> {
            UserCachedData userCachedData = new UserCachedData(user, sourceMessage.getServer().orElse(null));
            String serverChannelInfo = "";
            if (sourceMessage.getServerTextChannel().isPresent()) {
                ServerTextChannel sourceChannel = sourceMessage.getServerTextChannel().get();
                serverChannelInfo = " from " + sourceChannel.getServer().getName() + "#" + sourceChannel.getName();
            }
            String authorString = userCachedData.getDisplayUserName() + " (" + userCachedData.getDiscriminatorName() + ")"
                    + serverChannelInfo;
            embedBuilder.setAuthor(authorString,
                    null, userCachedData.getAvatarBytes(),
                    userCachedData.getAvatarExtension());
            embedBuilder.setThumbnail(userCachedData.getAvatarBytes(), userCachedData.getAvatarExtension());
        });
        String footer = determinateFooter(inMemoryAttaches, extractedUrls, sourceMessage.getEmbeds());
        embedBuilder.setFooter(footer);

        try {
            new MessageBuilder()
                    .setEmbed(embedBuilder)
                    .send(textChannel).get(OP_WAITING_TIMEOUT, TimeUnit.SECONDS);

            List<Embed> quotedEmbeds = sourceMessage.getEmbeds().stream()
                    .filter(embed -> embed.getProvider().isEmpty())
                    .collect(Collectors.toList());
            if (quotedEmbeds.size() > 0) {
                quotedEmbeds.get(0).getProvider();
                for (Embed embed : quotedEmbeds) {
                    new MessageBuilder()
                            .setEmbed(embed.toBuilder())
                            .send(textChannel).get(OP_WAITING_TIMEOUT, TimeUnit.SECONDS);
                }
            }
            if (!extractedUrls.isEmpty())
                MessageUtils.writeUrls(extractedUrls, textChannel);
            if (!inMemoryAttaches.isEmpty())
                MessageUtils.sendAttachments(inMemoryAttaches, textChannel);
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    @Nullable
    @Contract("null, _, _ -> fail; !null, null, _ -> fail; !null, !null, null -> fail")
    private String determinateFooter(List<InMemoryAttach> attaches, List<String> links, List<Embed> embeds) {

        if (attaches == null || links == null || embeds == null)
            throw new IllegalArgumentException("Attaches or urls links cannot be null");

        List<String> that = new ArrayList<>(3);

        if (!attaches.isEmpty()) {
            that.add("attaches");
        }
        if (!links.isEmpty()) {
            that.add("links");
        }
        if (!embeds.isEmpty()) {
            that.add("embeds");
        }

        if (!attaches.isEmpty() || !links.isEmpty() || !embeds.isEmpty()) {
            return "Message contains "
                    + that.stream().reduce((s1, s2) -> s1 + ", " + s2).orElse("")
                    + ". See below";
        } else {
            return null;
        }
    }
}
