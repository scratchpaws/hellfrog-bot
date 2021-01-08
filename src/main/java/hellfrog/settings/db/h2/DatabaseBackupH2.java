package hellfrog.settings.db.h2;

import hellfrog.common.BroadCast;
import hellfrog.common.CommonUtils;
import hellfrog.core.LogsStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

class DatabaseBackupH2 {


    private static final String BACKUP_DB_FILE_NAME = "hellfrog_backup";
    private static final String BACKUP_EXTENSION = ".zip";
    private static final String BACKUPS_DIR_NAME = "backups";
    private static final int MAX_BACKUP_FILES_COUNT = 3;

    private final Logger log = LogManager.getLogger("DB backup");

    private final String connectionURL;
    private final String connectionUser;
    private final String connectionPassword;
    private final Path codeSourcePath;

    DatabaseBackupH2(@NotNull final String connectionURL,
                     @NotNull final String connectionUser,
                     @NotNull final String connectionPassword,
                     @NotNull final Path codeSourcePath) {

        this.codeSourcePath = codeSourcePath;
        this.connectionURL = connectionURL;
        this.connectionUser = connectionUser;
        this.connectionPassword = connectionPassword;
    }

    void backupAction() {
        BroadCast.MessagesLogger messagesLogger = BroadCast.getLogger();

        String message = "Database backup creation initiated";
        log.info(message);
        messagesLogger.addInfoMessage(message);

        try {
            Path backupsRootPath = getBackupsRootPath();
            if (backupsRootPath == null) {
                return;
            }

            final String backupFilePath = getBackupFilePath(backupsRootPath);

            String pathForLog = backupFilePath.replace(backupsRootPath.toString()
                    .replace('\\', '/'), "");
            message = String.format("Backup file: %s", pathForLog);
            log.info(message);
            messagesLogger.addInfoMessage(message);

            try (Connection sourceConnection = DriverManager.getConnection(connectionURL, connectionUser, connectionPassword)) {
                try (Statement statement = sourceConnection.createStatement()) {
                    statement.executeUpdate("BACKUP TO '" + backupFilePath + "'");
                }
                message = "Backup file created successfully";
                log.info(message);
                messagesLogger.addInfoMessage(message);

            } catch (SQLException err) {
                String errMsg = String.format("DB backup error: %s", err.getMessage().replace(backupsRootPath.toString(), ""));
                log.error(errMsg, err);
                messagesLogger.addErrorMessage(errMsg);
                return;
            }

            clearOldBackups(messagesLogger, backupsRootPath);

            message = "Backup creation completed";
            log.info(message);
            messagesLogger.addInfoMessage(message);
        } finally {
            messagesLogger.send();
        }
    }

    private void clearOldBackups(@NotNull final BroadCast.MessagesLogger messagesLogger,
                                 @NotNull final Path backupsRootPath) {

        DirectoryStream.Filter<Path> onlyAnotherBackups = path -> Files.isRegularFile(path)
                && path.getFileName().toString().endsWith(BACKUP_EXTENSION);

        List<Path> anotherBackups = new ArrayList<>();

        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(backupsRootPath, onlyAnotherBackups)) {
            for (Path item : dirStream) {
                anotherBackups.add(item);
            }
        } catch (IOException err) {
            String errMsg = String.format("Unable to read directory \"%s\" with other backup files for rotation: %s",
                    backupsRootPath.getFileName().toString(), err.getMessage());
            log.error(errMsg, err);
            messagesLogger.addErrorMessage(errMsg);
        }

        Collections.sort(anotherBackups);

        if (anotherBackups.size() > MAX_BACKUP_FILES_COUNT) {
            final long removalCount = anotherBackups.size() - MAX_BACKUP_FILES_COUNT;
            List<Path> toRemove = anotherBackups.stream()
                    .limit(removalCount)
                    .collect(Collectors.toUnmodifiableList());
            String filesLog = toRemove.stream()
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .reduce(CommonUtils::reduceConcat)
                    .orElse("(empty)");

            String message = String.format("Rotation of old backups, need to delete %d files: %s",
                    removalCount, filesLog);
            log.info(message);
            messagesLogger.addInfoMessage(message);

            for (Path item : toRemove) {
                try {
                    Files.deleteIfExists(item);
                } catch (IOException err) {
                    String errMsg = String.format("Unable to remove old backup file \"%s\": %s",
                            item.getFileName().toString(), err.getMessage());
                    log.error(errMsg, err);
                    messagesLogger.addErrorMessage(errMsg);
                }
            }
        }
    }

    @NotNull
    private String getBackupFilePath(@NotNull final Path backupsRootPath) {
        String dateFileName = String.format("%s_%tF_%<tH-%<tM-%<tS%s",
                BACKUP_DB_FILE_NAME,
                Calendar.getInstance(TimeZone.getTimeZone("UTC")),
                BACKUP_EXTENSION);
        return backupsRootPath.resolve(dateFileName).toString().replace('\\', '/');
    }

    @Nullable
    private Path getBackupsRootPath() {
        Path backupsRootPath = codeSourcePath.resolve(BACKUPS_DIR_NAME);
        try {
            if (Files.notExists(backupsRootPath)) {
                Files.createDirectories(backupsRootPath);
            }
        } catch (IOException err) {
            String errMsg = String.format("Unable to create backups directory \"%s\": %s",
                    backupsRootPath.getFileName().toString(), err.getMessage());
            log.error(errMsg, err);
            LogsStorage.addErrorMessage(errMsg);
            return null;
        }

        if (!Files.isDirectory(backupsRootPath)) {
            String errMsg = String.format("Backups directory \"%s\" is not a directory",
                    backupsRootPath.getFileName().toString());
            log.error(errMsg);
            LogsStorage.addErrorMessage(errMsg);
            return null;
        }
        return backupsRootPath;
    }
}
