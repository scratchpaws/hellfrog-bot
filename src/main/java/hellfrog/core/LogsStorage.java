package hellfrog.core;

import hellfrog.common.CommonUtils;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class LogsStorage {

    private static final Queue<String> INFO_MESSAGES = new ConcurrentLinkedQueue<>();
    private static final Queue<String> WARN_MESSAGES = new ConcurrentLinkedQueue<>();
    private static final Queue<String> ERROR_MESSAGES = new ConcurrentLinkedQueue<>();

    public static void addInfoMessage(@Nullable final String message) {
        if (CommonUtils.isTrStringEmpty(message)) {
            return;
        }
        INFO_MESSAGES.add(message);
    }

    public static void addWarnMessage(@Nullable final String message) {
        if (CommonUtils.isTrStringEmpty(message)) {
            return;
        }
        WARN_MESSAGES.add(message);
    }

    public static void addErrorMessage(@Nullable final String message) {
        if (CommonUtils.isTrStringEmpty(message)) {
            return;
        }
        ERROR_MESSAGES.add(message);
    }

    static Queue<String> getInfoMessages() {
        return INFO_MESSAGES;
    }

    static Queue<String> getWarnMessages() {
        return WARN_MESSAGES;
    }

    static Queue<String> getErrorMessages() {
        return ERROR_MESSAGES;
    }

    @TestOnly
    public static boolean isErrorsEmpty() {
        return ERROR_MESSAGES.isEmpty();
    }

    @TestOnly
    public static boolean isWarnsEmpty() {
        return WARN_MESSAGES.isEmpty();
    }
}
