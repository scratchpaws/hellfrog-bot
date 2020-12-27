package hellfrog.common;

/**
 * Operation exception contains two-level message: for logs and administrators, and for display into user message
 */
public class OperationException
        extends Exception {

    private final String serviceMessage;
    private final String userMessage;

    public OperationException(final String serviceMessage, final String userMessage, final Throwable clause) {
        super(serviceMessage, clause);
        this.serviceMessage = serviceMessage;
        this.userMessage = userMessage;
    }

    public OperationException(final String serviceMessage, final String userMessage) {
        super(serviceMessage);
        this.serviceMessage = serviceMessage;
        this.userMessage = userMessage;
    }

    public OperationException(final String userMessage, final Throwable clause) {
        super(userMessage, clause);
        this.serviceMessage = userMessage;
        this.userMessage = userMessage;
    }

    public OperationException(final String userMessage) {
        super(userMessage);
        this.serviceMessage = userMessage;
        this.userMessage = userMessage;
    }

    public String getServiceMessage() {
        return serviceMessage;
    }

    public String getUserMessage() {
        return userMessage;
    }
}
