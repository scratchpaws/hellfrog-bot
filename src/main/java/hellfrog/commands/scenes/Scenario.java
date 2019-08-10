package hellfrog.commands.scenes;

import hellfrog.commands.ACLCommand;
import hellfrog.common.CodeSourceUtils;
import hellfrog.common.CommonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.event.message.MessageCreateEvent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class Scenario
    extends ACLCommand {

    private static final List<Scenario> ALL = CodeSourceUtils.childClassInstancesCollector(Scenario.class);
    private static final Logger log = LogManager.getLogger(Scenario.class.getSimpleName());
    private static final boolean RUN_SCENARIO_RESULT = true;

    public Scenario(@NotNull String prefix, @NotNull String description) {
        super(prefix, description);
    }

    public boolean canExecute(String rawCommand) {
        return !CommonUtils.isTrStringEmpty(rawCommand)
            && rawCommand.strip().equalsIgnoreCase(super.getPrefix());
    }

    @Contract(pure = true)
    public static List<Scenario> all() {
        return ALL;
    }

    public boolean runScenario(@NotNull MessageCreateEvent event) {

        super.updateLastUsage();

        boolean canAccess = event.getServer()
                .map(server -> canExecuteServerCommand(event, server))
                .orElse(true);
        if (!canAccess) {
            showAccessDeniedServerMessage(event);
            return RUN_SCENARIO_RESULT;
        }

        if (isOnlyServerCommand() && !event.isServerMessage()) {
            showErrorMessage("This command can't be run into private channel", event);
            return RUN_SCENARIO_RESULT;
        }

        firstRun(event);
        return RUN_SCENARIO_RESULT;
    }

    protected abstract void firstRun(@NotNull MessageCreateEvent event);
}
