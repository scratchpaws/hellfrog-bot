package hellfrog.commands.scenes;

public class FunBiteScenario
        extends FunScenario {

    private static final String PREFIX = "bite";
    private static final String DESCRIPTION = "Bite someone";

    public FunBiteScenario() {
        super(PREFIX, DESCRIPTION);
        super.setLonelyResultMessage("got bitten");
        super.setWithSomeoneResultMessage("bit");
        super.setUrlPicturesSet(BITE_URLS);
    }
}
