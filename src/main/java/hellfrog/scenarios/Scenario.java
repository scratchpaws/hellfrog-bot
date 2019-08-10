package hellfrog.scenarios;

import hellfrog.commands.ACLCommand;
import hellfrog.common.CodeSourceUtils;
import hellfrog.common.CommonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class Scenario
    extends ACLCommand {

    private static final List<Scenario> ALL = CodeSourceUtils.childClassInstancesCollector(Scenario.class);
    private static final Logger log = LogManager.getLogger(Scenario.class.getSimpleName());
    private List<SceneStep> steps = new ArrayList<>();

    public Scenario(@NotNull String prefix, @NotNull String description) {
        super(prefix, description);
    }

    protected void addStep(SceneStep step) {
        this.steps.add(step);
    }


    public boolean canExecute(String rawCommand) {
        return !CommonUtils.isTrStringEmpty(rawCommand)
            && rawCommand.strip().equalsIgnoreCase(super.getPrefix());
    }

    public static List<Scenario> getALL() {
        return ALL;
    }

}
