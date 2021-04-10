package hellfrog.commands.scenes;

public class FunKissScenario
        extends FunScenario {

    private static final String PREFIX = "kiss";
    private static final String DESCRIPTION = "Kiss someone";

    public FunKissScenario() {
        super(PREFIX, DESCRIPTION);
        super.setLonelyResultMessage("got kissed");
        super.setWithSomeoneResultMessage("kissed");
        super.setUrlPicturesSet(KISS_URLS);
    }
}
