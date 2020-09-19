package hellfrog.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ProcessOutputBuffer
        implements Runnable {

    private final Logger log = LogManager.getLogger(getClass().getSimpleName());
    private final BufferedReader outReader;
    private final StringBuilder stdOut = new StringBuilder();

    public ProcessOutputBuffer(@NotNull final InputStream procOutput) {
        outReader = new BufferedReader(new InputStreamReader(procOutput));
        new Thread(this).start();
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = outReader.readLine()) != null) {
                stdOut.append(line).append('\n');
            }
        } catch (IOException err) {
            String errMsg = String.format("Error while process stdout read: %s", err.getMessage());
            log.error(errMsg, err);
        } finally {
            try {
                outReader.close();
            } catch (IOException ignore) {
            }
        }
    }

    public String getOutput() {
        return stdOut.toString();
    }
}
