package hellfrog.commands.scenes;

import com.vdurmont.emoji.EmojiParser;
import hellfrog.common.MessageUtils;
import hellfrog.core.SessionState;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class TestScenario
        extends Scenario {

    private static final String PREFIX = "tst";
    private static final String DESCRIPTION = "A test scenario";

    private static final long FIRST_SCENE_ID = 1L;
    private static final String ENTERED_TEXT = "text";
    private static final List<String> STOP_BUTTON_EMOJI =
            Collections.singletonList(EmojiParser.parseToUnicode(":stop_sign:"));

    public TestScenario() {
        super(PREFIX, DESCRIPTION);
    }

    @Override
    protected void executeFirstRun(@NotNull MessageCreateEvent event) {
        event.getMessageAuthor().asUser().ifPresent(user -> {
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("Test scenario (initial)")
                    .setDescription("Enter some text into next message of this channel. Press stop emoji to stop");
            super.displayMessage(embed, event.getChannel()).ifPresent(message -> {
                if (super.addReactions(message, null, STOP_BUTTON_EMOJI)) {
                    ScenarioState scenarioState = new ScenarioState(FIRST_SCENE_ID);
                    scenarioState.put(ENTERED_TEXT, new StringBuilder());
                    SessionState sessionState = SessionState.forScenario(this)
                            .setScenarioState(scenarioState)
                            .setRemoveReaction(true)
                            .setMessage(message)
                            .setTextChannel(event.getChannel())
                            .setUser(user)
                            .build();
                    super.commitState(sessionState);
                }
            });
        });
    }

    @Override
    public void executeMessageStep(@NotNull MessageCreateEvent event, @NotNull SessionState sessionState) {
        event.getMessageAuthor().asUser().ifPresentOrElse(user -> {
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("Test scenario (append text)")
                    .setDescription("Enter additional text into next message of this channel. Press stop emoji to stop");
            super.displayMessage(embed, event.getChannel()).ifPresentOrElse(message -> {
                if (super.addReactions(message, null, STOP_BUTTON_EMOJI)) {
                    super.dropPreviousStateEmoji(sessionState);
                    SessionState newState = sessionState.toBuilder()
                            .setMessage(message)
                            .build();

                    StringBuilder enteredText = sessionState.getStateObject(ENTERED_TEXT, StringBuilder.class);
                    enteredText.append(event.getMessageContent()).append('\n');

                    super.commitState(newState);
                } else {
                    super.commitState(sessionState);
                }
            }, () -> super.commitState(sessionState));
        }, () -> super.commitState(sessionState));
    }

    @Override
    public void executeReactionStep(@NotNull SingleReactionEvent event, @NotNull SessionState sessionState) {
        boolean isValidStopReaction = event.getEmoji()
                .asUnicodeEmoji()
                .map(emoji -> emoji.equals(STOP_BUTTON_EMOJI.get(0)))
                .orElse(false);
        if (!isValidStopReaction) {
            super.commitState(sessionState);
        } else {
            EmbedBuilder embedBuilder = new EmbedBuilder()
                    .setTitle("Test scenario (completed)")
                    .setDescription("Break signaled. " +
                            "All previously entered text will be displayed in the next messages.");
            super.displayMessage(embedBuilder, event.getChannel()).ifPresent(message -> {
                super.dropPreviousStateEmoji(sessionState);
                StringBuilder enteredText = sessionState.getStateObject(ENTERED_TEXT, StringBuilder.class);
                MessageUtils.sendLongMessage(new MessageBuilder().append(enteredText.toString()), event.getChannel());
            });
        }
    }
}
