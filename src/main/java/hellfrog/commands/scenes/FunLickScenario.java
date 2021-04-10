package hellfrog.commands.scenes;

public class FunLickScenario
        extends FunScenario {

    private static final String PREFIX = "lick";
    private static final String DESCRIPTION = "Lick someone";

    public FunLickScenario() {
        super(PREFIX, DESCRIPTION);
        super.setLonelyResultMessage("got licked");
        super.setWithSomeoneResultMessage("licked");
        super.setUrlPicturesSet(LICK_URLS);
    }
}
