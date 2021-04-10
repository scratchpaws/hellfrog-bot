package hellfrog.commands.scenes;

public class FunSlapScenario
        extends FunScenario {

    private static final String PREFIX = "slap";
    private static final String DESCRIPTION = "Slap someone";

    public FunSlapScenario() {
        super(PREFIX, DESCRIPTION);
        super.setLonelyResultMessage("got slapped");
        super.setWithSomeoneResultMessage("slapped");
        super.setUrlPicturesSet(SLAP_URLS);
    }
}
