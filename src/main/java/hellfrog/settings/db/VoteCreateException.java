package hellfrog.settings.db;

public class VoteCreateException extends Exception {

    public VoteCreateException() {
        super();
    }

    public VoteCreateException(String message) {
        super(message);
    }

    public VoteCreateException(String message,
                               Throwable cause) {
        super(message, cause);
    }

    public VoteCreateException(Throwable cause) {
        super(cause);
    }

    public VoteCreateException(String message,
                               Throwable cause,
                               boolean enableSuppression,
                               boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
