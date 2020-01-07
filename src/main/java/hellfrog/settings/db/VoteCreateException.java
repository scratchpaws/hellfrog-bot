package hellfrog.settings.db;

import org.jetbrains.annotations.NotNull;

public class VoteCreateException extends Exception {

    VoteCreateException() {
        super();
    }

    VoteCreateException(String message) {
        super(message);
    }

    VoteCreateException(String message,
                        Throwable cause) {
        super(message, cause);
    }

    VoteCreateException(Throwable cause) {
        super(cause);
    }

    VoteCreateException(String message,
                        Throwable cause,
                        boolean enableSuppression,
                        boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
