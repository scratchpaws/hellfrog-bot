package hellfrog.common;

import hellfrog.core.ServerSideResolver;
import hellfrog.settings.SettingsController;
import org.javacord.api.entity.Mentionable;
import org.javacord.api.entity.message.*;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class LongEmbedMessage
        implements CommonConstants {

    private final StringBuilder messageBuffer = new StringBuilder();
    private Instant timestamp = null;
    private Color color = null;

    private String title = null;

    private String authorName = null;
    private String authorUrl = null;
    private String authorIconUrl = null;

    private final List<LongEmbedField> fields = new ArrayList<>();

    public static LongEmbedMessage withTitleInfoStyle(@NotNull final String title) {
        return new LongEmbedMessage()
                .setInfoStyle()
                .setTitle(title);
    }

    public static LongEmbedMessage withTitleScenarioStyle(@Nullable final String title) {
        return new LongEmbedMessage()
                .setScenarioStyle()
                .setTitle(title);
    }

    public LongEmbedMessage append(String str) {
        messageBuffer.append(str);
        return this;
    }

    public<T> LongEmbedMessage appendIfPresent(Optional<T> optional) {
        optional.ifPresent(this::append);
        return this;
    }

    public<T> LongEmbedMessage append(T value) {
        messageBuffer.append(value);
        return this;
    }

    public LongEmbedMessage append(char c) {
        messageBuffer.append(c);
        return this;
    }

    public LongEmbedMessage appendNewLine() {
        messageBuffer.append('\n');
        return this;
    }

    public LongEmbedMessage append(long lng) {
        messageBuffer.append(lng);
        return this;
    }

    public LongEmbedMessage append(Mentionable entity) {
        if (entity == null) {
            messageBuffer.append((Object) null);
        } else {
            messageBuffer.append(entity.getMentionTag());
        }
        return this;
    }

    public LongEmbedMessage append(String str, MessageDecoration decoration) {
        if (decoration != null) {
            messageBuffer.append(decoration.getPrefix());
        }
        messageBuffer.append(str);
        if (decoration != null) {
            messageBuffer.append(decoration.getSuffix());
        }
        return this;
    }

    public LongEmbedMessage append(String str, MessageDecoration... decorations) {
        if (decorations != null && decorations.length > 0) {
            for (MessageDecoration decoration : decorations) {
                messageBuffer.append(decoration.getPrefix());
            }
        }
        messageBuffer.append(str);
        if (decorations != null && decorations.length > 0) {
            for (int i = (decorations.length - 1); i >= 0; i--) {
                messageBuffer.append(decorations[i].getSuffix());
            }
        }
        return this;
    }

    public LongEmbedMessage append(LongEmbedMessage another) {
        if (another == null) {
            messageBuffer.append((Object)null);
        } else {
            messageBuffer.append(another.messageBuffer);
        }
        return this;
    }

    public LongEmbedMessage appendf(String format, Object... args) {
        messageBuffer.append(new Formatter().format(format, args).toString());
        return this;
    }

    public LongEmbedMessage appendReadable(String str, Optional<Server> mayBeServer) {
        messageBuffer.append(ServerSideResolver.getReadableContent(str, mayBeServer));
        return this;
    }

    public LongEmbedMessage appendReadable(String str) {
        return appendReadable(str, Optional.empty());
    }

    public LongEmbedMessage appendReadable(String str, Server server) {
        return appendReadable(str, Optional.ofNullable(server));
    }

    public LongEmbedMessage setTimestamp(@NotNull final Instant timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public LongEmbedMessage setTimestampToNow() {
        this.timestamp = Instant.now();
        return this;
    }

    public LongEmbedMessage setColor(Color color) {
        this.color = color;
        return this;
    }

    public LongEmbedMessage setInfoStyle() {
        this.color = Color.CYAN;
        this.setStyleAttributes();
        return this;
    }

    public LongEmbedMessage setErrorStyle() {
        this.color = Color.RED;
        this.setStyleAttributes();
        return this;
    }

    public LongEmbedMessage setScenarioStyle() {
        this.color = Color.GREEN;
        this.setStyleAttributes();
        return this;
    }

    public void setYourselfAuthor() {
        User yourself = SettingsController.getInstance().getDiscordApi().getYourself();
        this.setAuthor(yourself);
    }

    private void setStyleAttributes() {
        this.setTimestampToNow();
        setYourselfAuthor();
    }

    public LongEmbedMessage setAuthor(@NotNull final String authorName,
                                      @Nullable final String authorUrl,
                                      @Nullable final String authorIconUrl) {
        this.authorName = authorName;
        this.authorUrl = authorUrl;
        this.authorIconUrl = authorIconUrl;
        return this;
    }

    public LongEmbedMessage setAuthor(@NotNull final User author) {
        this.setAuthor(author.getName(), null, author.getAvatar().getUrl().toString());
        return this;
    }

    public LongEmbedMessage setAuthor(@NotNull final MessageAuthor author) {
        this.setAuthor(author.getDisplayName(), null, author.getAvatar().getUrl().toString());
        return this;
    }


    public LongEmbedMessage setTitle(String title) {
        this.title = title;
        return this;
    }

    public LongEmbedMessage addField(String name, String value) {
        this.fields.add(new LongEmbedField(name, value, false));
        return this;
    }

    public LongEmbedMessage addField(String name, String value, boolean inline) {
        this.fields.add(new LongEmbedField(name, value, inline));
        return this;
    }

    public LongEmbedMessage addInlineField(String name, String value) {
        this.fields.add(new LongEmbedField(name, value, true));
        return this;
    }

    @Nullable
    public Color getColor() {
        return color;
    }

    public CompletableFuture<Message> send(Messageable target) {

        final CompletableFuture<Message> future = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> {

            List<String> texts = getTexts();
            if (texts.isEmpty()) {
                BroadCast.getLogger()
                        .addErrorMessage(LongEmbedMessage.class.getSimpleName() + ": message length is empty");
                future.completeExceptionally(new RuntimeException("Message length is empty"));
                return;
            }

            MessageBuilder firstMessage = null;
            MessageBuilder lastMessage = null;
            EmbedBuilder firstEmbed = null;
            EmbedBuilder lastEmbed = null;

            final List<MessageBuilder> messageBuilders = new ArrayList<>();

            for (String text : texts) {
                MessageBuilder messageBuilder = new MessageBuilder();
                EmbedBuilder embedBuilder = new EmbedBuilder();
                messageBuilder.setEmbed(embedBuilder);

                embedBuilder.setDescription(text);
                if (color != null) {
                    embedBuilder.setColor(color);
                }

                if (firstMessage == null) {
                    firstMessage = messageBuilder;
                }
                if (firstEmbed == null) {
                    firstEmbed = embedBuilder;
                }
                lastMessage = messageBuilder;
                lastEmbed = embedBuilder;
                messageBuilders.add(messageBuilder);
            }

            if (timestamp != null) {
                lastEmbed.setTimestamp(timestamp);
            }
            if (title != null) {
                firstEmbed.setTitle(title);
            }
            if (CommonUtils.isTrStringNotEmpty(authorName)) {
                firstEmbed.setAuthor(authorName, authorUrl, authorIconUrl);
            }

            for (LongEmbedField field : fields) {
                lastEmbed.addField(field.getName(), field.getValue(), field.isInline());
            }

            sendMessageChain(future, null, target, messageBuilders, 0);
        });
        return future;
    }
    
    private void sendMessageChain(@NotNull final CompletableFuture<Message> resultFuture,
                                  @Nullable final Message previousResult,
                                  @NotNull final Messageable target,
                                  @NotNull final List<MessageBuilder> messagesList,
                                  final int currentIndex) {
        
        if (currentIndex >= messagesList.size()) {
            resultFuture.complete(previousResult);
            return;
        }
        
        MessageBuilder nextMessage = messagesList.get(currentIndex);
        nextMessage.send(target)
                .thenAccept(message -> sendMessageChain(resultFuture, message, target, messagesList, currentIndex + 1))
                .exceptionally(throwable -> {
                    resultFuture.completeExceptionally(throwable);
                    return null;
                });
    }

    @NotNull
    @UnmodifiableView
    private List<String> getTexts() {
        if (messageBuffer.length() <= 2000) {
            return List.of(messageBuffer.toString());
        } else {
            return CommonUtils.splitPreserveWords(messageBuffer.toString(), 2000);
        }
    }

    @Override
    public String toString() {
        return messageBuffer.toString();
    }
}
