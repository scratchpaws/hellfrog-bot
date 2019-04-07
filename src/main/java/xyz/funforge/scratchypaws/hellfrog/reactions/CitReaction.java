package xyz.funforge.scratchypaws.hellfrog.reactions;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.jetbrains.annotations.Nullable;
import xyz.funforge.scratchypaws.hellfrog.common.CommonUtils;
import xyz.funforge.scratchypaws.hellfrog.common.MessageUtils;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CitReaction
    extends MsgCreateReaction {

    private static final String PREFIX = "qt";
    private static final String DESCRIPTION = "Quote message by link";

    private static final Pattern QUOTE_SEARCH = Pattern.compile("^[qQtTцЦтТ]{2}.*channels/\\d+/\\d+/\\d+", Pattern.MULTILINE);
    private static final Pattern SIMPLE_SEARCH = Pattern.compile("^[qQtTцЦтТ]{2}\\s*\\d+");

    public CitReaction() {
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
        String otherMessage = null;

        Matcher quote = QUOTE_SEARCH.matcher(strMessage);
        if (quote.find()) {
            String found = quote.group();
            messageLink = MessageUtils.resolveByLink(found).orElse(null);
            otherMessage = CommonUtils.cutLeftString(strMessage, found);
            if (otherMessage != null) otherMessage = otherMessage.strip();
        }

        quote = SIMPLE_SEARCH.matcher(strMessage);
        if (quote.find()) {
            String found = quote.group();
            long messageId = CommonUtils.onlyNumbersToLong(found);
            messageLink = MessageUtils.findByIds(server.getId(), textChannel.getId(), messageId)
                    .orElse(null);
            otherMessage = CommonUtils.cutLeftString(strMessage, found);
            if (otherMessage != null) otherMessage = otherMessage.strip();
        }

        if (messageLink == null) return;

        String messageUrl = MessageUtils.getMessageUrl(messageLink);

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Quoted message:");
        embedBuilder.setTimestamp(messageLink.getLastEditTimestamp()
                .orElse(messageLink.getCreationTimestamp()));
        embedBuilder.setDescription(messageLink.getContent());

        messageLink.getUserAuthor().ifPresent(embedBuilder::setAuthor);
        embedBuilder.setUrl(messageUrl);

        new MessageBuilder()
                .setEmbed(embedBuilder)
                .send(textChannel);

        if (sourceMessage.canYouDelete())
            sourceMessage.delete();
    }
}
