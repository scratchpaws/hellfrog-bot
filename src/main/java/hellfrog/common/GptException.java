package hellfrog.common;

public class GptException
        extends Exception {

    public GptException(String message) {
        super(message);
    }

    private String footerMessage;

    public String getFooterMessage() {
        return footerMessage;
    }

    public void setFooterMessage(String footerMessage) {
        this.footerMessage = footerMessage;
    }
}
