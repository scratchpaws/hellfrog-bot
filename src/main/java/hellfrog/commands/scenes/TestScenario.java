package hellfrog.commands.scenes;

import org.javacord.api.event.message.MessageCreateEvent;
import org.jetbrains.annotations.NotNull;

public class TestScenario
    extends Scenario {

    private static final String PREFIX = "tst";
    private static final String DESCRIPTION = "A test scenario";

    public TestScenario() {
        super(PREFIX, DESCRIPTION);
    }

    @Override
    protected void firstRun(@NotNull MessageCreateEvent event) {
        showInfoMessage("Test", event);
    }
}
