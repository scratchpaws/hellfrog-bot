package bruva.settings;

import bruva.settings.DAO.CommonSettingsDAO;
import bruva.settings.Entity.CommonName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class SettingsController {

    private static final Path SETTINGS_PATH = Paths.get("./settings/");
    private static final Path API_KEY_FILE = SETTINGS_PATH.resolve("api_key.txt");
    private static final SettingsController INSTANCE = new SettingsController();
    private static final Logger log = LogManager.getLogger(SettingsController.class.getSimpleName());
    private static final CommonSettingsDAO COMMON_SETTINGS_DAO = new CommonSettingsDAO();

    public static SettingsController getInstance() {
        return INSTANCE;
    }

    public Optional<String> getApiKey() {
        try {
            return Optional.of(Files.readString(API_KEY_FILE, StandardCharsets.UTF_8)
                    .replaceAll("[\r\n]", "")
                    .trim());
        } catch (IOException err) {
            log.error("Unable to read api key from file " + API_KEY_FILE + ": " + err.getMessage(),
                    err);
            return Optional.empty();
        }
    }

    public Optional<String> getBotName() {
        try {
            return Optional.of(COMMON_SETTINGS_DAO.get(CommonName.BOT_NAME));
        } catch (Exception err) {
            log.error("Unable to get setting of bot name: " + err.getMessage(), err);
            return Optional.empty();
        }
    }

    public boolean setBotName(String newBotName) {
        try {
            COMMON_SETTINGS_DAO.set(CommonName.BOT_NAME, newBotName);
            return true;
        } catch (Exception err) {
            String errorMessage = "Unable to save settings of bot name: " + err.getMessage();
            log.error(errorMessage, err);
            return false;
        }
    }

    public Optional<String> getBotPrefix() {
        try {
            return Optional.of(COMMON_SETTINGS_DAO.get(CommonName.BOT_PREFIX));
        } catch (Exception err) {
            log.error("Unable to get common bot prefix: " + err.getMessage(), err);
            return Optional.empty();
        }
    }

    public boolean setBotPrefix(String newBotPrefix) {
        try {
            COMMON_SETTINGS_DAO.set(CommonName.BOT_PREFIX, newBotPrefix);
            return true;
        } catch (Exception err) {
            log.error("Unable to set common bot prefix: " + err.getMessage(), err);
            return false;
        }
    }

    public Optional<Boolean> isRemoteDebugEnabled() {
        try {
            return Optional.of(Boolean.valueOf(COMMON_SETTINGS_DAO.get(CommonName.REMOTE_DEBUG)));
        } catch (Exception err) {
            log.error("Unable to get remote debug status: " + err.getMessage(), err);
            return Optional.empty();
        }
    }

    public boolean setRemoteDebugEnable(boolean status) {
        try {
            COMMON_SETTINGS_DAO.set(CommonName.REMOTE_DEBUG, Boolean.toString(status));
            return true;
        } catch (Exception err) {
            log.error("Unable to change remote debug status: " + err.getMessage(), err);
            return false;
        }
    }
}
