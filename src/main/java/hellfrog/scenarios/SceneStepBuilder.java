package hellfrog.scenarios;

public class SceneStepBuilder {

    SceneStepBuilder() {}

    private StepType type;
    private String message;

    public StepType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public SceneStepBuilder setType(StepType type) {
        this.type = type;
        return this;
    }

    public SceneStepBuilder setMessage(String message) {
        this.message = message;
        return this;
    }

    public SceneStep build() {
        if (type == null)
            throw new IllegalArgumentException("Step type required");
        if (message == null)
            throw new IllegalArgumentException("Step message required");

        return new SceneStep(type, message);
    }
}
