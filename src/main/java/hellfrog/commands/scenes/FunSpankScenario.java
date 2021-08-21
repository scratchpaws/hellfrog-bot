package hellfrog.commands.scenes;

public class FunSpankScenario
        extends FunScenario {

    private static final String PREFIX = "spank";
    private static final String DESCRIPTION = "Spank someone";

    public FunSpankScenario() {
        super(PREFIX, DESCRIPTION);
        super.setLonelyResultMessage("got spanked");
        super.setWithSomeoneResultMessage("spanked");
        super.setUrlPicturesSet(SPANK_URLS);
    }
}
