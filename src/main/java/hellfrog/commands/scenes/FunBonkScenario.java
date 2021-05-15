package hellfrog.commands.scenes;

public class FunBonkScenario
        extends FunScenario{

    private static final String PREFIX = "bonk";
    private static final String DESCRIPTION = "Bonk someone";

    public FunBonkScenario() {
        super(PREFIX, DESCRIPTION);
        super.setLonelyResultMessage("got bonked");
        super.setWithSomeoneResultMessage("bonked");
        super.setUrlPicturesSet(BONK_URLS);
    }
}
