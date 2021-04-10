package hellfrog.commands.scenes;

public class FunDanceScenario
        extends FunScenario {

    private static final String PREFIX = "dance";
    private static final String DESCRIPTION = "Dance with someone";

    public FunDanceScenario() {
        super(PREFIX, DESCRIPTION);
        super.setLonelyResultMessage("started to dance");
        super.setWithSomeoneResultMessage("danced with");
        super.setUrlPicturesSet(DANCE_URLS);
    }
}
