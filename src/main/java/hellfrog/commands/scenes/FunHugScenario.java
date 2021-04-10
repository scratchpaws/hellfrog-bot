package hellfrog.commands.scenes;

public class FunHugScenario
        extends FunScenario {

    private static final String PREFIX = "hug";
    private static final String DESCRIPTION = "Hug someone";

    public FunHugScenario() {
        super(PREFIX, DESCRIPTION);
        super.setLonelyResultMessage("got hugged");
        super.setWithSomeoneResultMessage("hugged");
        super.setUrlPicturesSet(HUG_URLS);
    }
}
