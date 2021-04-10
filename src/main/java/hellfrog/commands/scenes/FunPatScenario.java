package hellfrog.commands.scenes;

public class FunPatScenario
        extends FunScenario {

    private static final String PREFIX = "pat";
    private static final String DESCRIPTION = "Pat someone";

    public FunPatScenario() {
        super(PREFIX, DESCRIPTION);
        super.setLonelyResultMessage("got pat");
        super.setWithSomeoneResultMessage("pat");
        super.setUrlPicturesSet(PAT_URLS);
    }
}
