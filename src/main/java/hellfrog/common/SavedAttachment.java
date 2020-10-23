package hellfrog.common;

import org.javacord.api.entity.message.MessageAttachment;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

public class SavedAttachment
        implements Closeable, AutoCloseable {

    private static final long WAITING_DOWNLOAD = 60L;
    private final String fileName;
    private Path tempFile;

    private final ReentrantLock writeLock = new ReentrantLock();

    public SavedAttachment(@NotNull MessageAttachment attachment) throws IOException {
        this.fileName = attachment.getFileName();
        try {
            byte[] attachBytes = attachment.downloadAsByteArray().get(WAITING_DOWNLOAD, TimeUnit.SECONDS);
            tempFile = CodeSourceUtils.createTempFile();
            try (BufferedOutputStream bufout = new BufferedOutputStream(Files.newOutputStream(tempFile))) {
                bufout.write(attachBytes);
                bufout.flush();
            }
        } catch (TimeoutException cancelled) {
            throw new IOException("download time exceeded: " + cancelled, cancelled);
        } catch (Exception downloadError) {
            throw new IOException("other error: " + downloadError, downloadError);
        }
    }

    public void moveTo(@NotNull Path destination) throws IOException {
        writeLock.lock();
        try {
            if (tempFile == null) {
                throw new IOException("temporary file already deleted");
            }
            Files.move(tempFile, destination, StandardCopyOption.REPLACE_EXISTING);
            tempFile = null;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void close() {
        if (tempFile != null) {
            writeLock.lock();
            try {
                if (Files.exists(tempFile) && Files.isRegularFile(tempFile)) {
                    Files.delete(tempFile);
                }
                tempFile = null;
            } catch (IOException err) {
                err.printStackTrace();
            } finally {
                writeLock.unlock();
            }
        }
    }

    public String getFileName() {
        return fileName;
    }
}
