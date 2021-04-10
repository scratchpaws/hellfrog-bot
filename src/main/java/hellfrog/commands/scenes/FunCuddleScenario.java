package hellfrog.commands.scenes;

public class FunCuddleScenario
        extends FunScenario {

    private static final String PREFIX = "cuddle";
    private static final String DESCRIPTION = "Cuddle someone";

    public FunCuddleScenario() {
        super(PREFIX, DESCRIPTION);
        super.setLonelyResultMessage("got cuddled");
        super.setWithSomeoneResultMessage("cuddled");
        super.setUrlPicturesSet(CUDDLE_URLS);
    }
}
