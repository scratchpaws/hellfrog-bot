package hellfrog.scenarios;

import org.jetbrains.annotations.Contract;

public class SceneStep {

    private final StepType type;
    private final String message;

    SceneStep(StepType type,
              String message) {

        this.type = type;
        this.message = message;
    }

    public SceneStepBuilder builder() {
        return new SceneStepBuilder();
    }

    public StepType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }
}
