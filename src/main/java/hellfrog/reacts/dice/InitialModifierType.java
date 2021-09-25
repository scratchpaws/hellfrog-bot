package hellfrog.reacts.dice;

public enum InitialModifierType {
    NONE(false),
    SUB_SUM(true),
    SUB_VAL(false),
    ADD_SUM(true),
    ADD_VAL(false);

    public final boolean summary;
    InitialModifierType(final boolean summary) {
        this.summary = summary;
    }
}
