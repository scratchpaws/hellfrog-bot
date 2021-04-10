package hellfrog.commands.scenes;

public class FunBlushScenario
        extends FunScenario {

    private static final String PREFIX = "blush";
    private static final String DESCRIPTION = "Blushes";

    public FunBlushScenario() {
        super(PREFIX, DESCRIPTION);
        super.setLonelyResultMessage("got blushed");
        super.setWithSomeoneResultMessage("got blushed at");
        super.setUrlPicturesSet(BLUSH_URLS);
    }
}
