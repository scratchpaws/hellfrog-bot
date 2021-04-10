package hellfrog.commands.scenes;

public class FunShockScenario
        extends FunScenario {

    private static final String PREFIX = "shock";
    private static final String DESCRIPTION = "Shock by someone";

    public FunShockScenario() {
        super(PREFIX, DESCRIPTION);
        super.setLonelyResultMessage("got shocked");
        super.setWithSomeoneResultMessage("shocked by");
        super.setUrlPicturesSet(SHOCK_URLS);
    }
}
