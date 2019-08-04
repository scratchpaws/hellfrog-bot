package bruva.settings;

import bruva.settings.DAO.BotOwnersDAO;
import bruva.settings.DAO.CommonSettingsDAO;
import bruva.settings.Entity.CommonName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApi;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

public class SettingsController {

    private static final Path SETTINGS_PATH = Paths.get("./settings/");
    private static final Path API_KEY_FILE = SETTINGS_PATH.resolve("api_key.txt");
    private static final SettingsController INSTANCE = new SettingsController();
    private static final Logger log = LogManager.getLogger(SettingsController.class.getSimpleName());
    private CommonSettingsDAO commonSettingsDAO = new CommonSettingsDAO();
    private BotOwnersDAO botOwnersDAO = new BotOwnersDAO();
    private DiscordApi firstShard = null;
    private TreeSet<DiscordApi> allShards = new TreeSet<>(Comparator.comparingInt(DiscordApi::getCurrentShard));

    public static SettingsController getInstance() {
        return INSTANCE;
    }

    public static Path getApiKeyFile() {
        return API_KEY_FILE;
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
            return Optional.of(commonSettingsDAO.get(CommonName.BOT_NAME));
        } catch (Exception err) {
            log.error("Unable to get setting of bot name: " + err.getMessage(), err);
            return Optional.empty();
        }
    }

    public boolean setBotName(String newBotName) {
        try {
            commonSettingsDAO.set(CommonName.BOT_NAME, newBotName);
            return true;
        } catch (Exception err) {
            String errorMessage = "Unable to save settings of bot name: " + err.getMessage();
            log.error(errorMessage, err);
            return false;
        }
    }

    public Optional<String> getBotPrefix() {
        try {
            return Optional.of(commonSettingsDAO.get(CommonName.BOT_PREFIX));
        } catch (Exception err) {
            log.error("Unable to get common bot prefix: " + err.getMessage(), err);
            return Optional.empty();
        }
    }

    public boolean setBotPrefix(String newBotPrefix) {
        try {
            commonSettingsDAO.set(CommonName.BOT_PREFIX, newBotPrefix);
            return true;
        } catch (Exception err) {
            log.error("Unable to set common bot prefix: " + err.getMessage(), err);
            return false;
        }
    }

    public Optional<Boolean> isRemoteDebugEnabled() {
        try {
            return Optional.of(Boolean.parseBoolean(commonSettingsDAO.get(CommonName.REMOTE_DEBUG)));
        } catch (Exception err) {
            log.error("Unable to get remote debug status: " + err.getMessage(), err);
            return Optional.empty();
        }
    }

    public boolean setRemoteDebugEnable(boolean status) {
        try {
            commonSettingsDAO.set(CommonName.REMOTE_DEBUG, Boolean.toString(status));
            return true;
        } catch (Exception err) {
            log.error("Unable to change remote debug status: " + err.getMessage(), err);
            return false;
        }
    }

    public Optional<List<Long>> getAllBotOwners() {
        try {
            return Optional.of(botOwnersDAO.getAll());
        } catch (Exception err) {
            log.error("Unable to get all bot owners: " + err.getMessage(), err);
            return Optional.empty();
        }
    }

    public Optional<Boolean> isBotOwner(long userId) {
        try {
            return Optional.of(botOwnersDAO.isBotOwner(userId));
        } catch (Exception err) {
            log.error("Unable to check that user with id " + userId + " is bot owner: " + err.getMessage(), err);
            return Optional.empty();
        }
    }

    public boolean addBotOwner(long userId) {
        try {
            return botOwnersDAO.addBotOwner(userId);
        } catch (Exception err) {
            log.error("Unable to add bot owner with id " + userId + ": " + err.getMessage(), err);
            return false;
        }
    }

    public boolean deleteBotOwner(long userId) {
        try {
            return botOwnersDAO.deleteBotOwner(userId);
        } catch (Exception err) {
            log.error("Unable to delete bot owner with id " + userId + ": " + err.getMessage(), err);
            return false;
        }
    }

    @Nullable
    public DiscordApi getFirstShard() {
        return firstShard;
    }

    public void setFirstShard(DiscordApi firstShard) {
        this.firstShard = firstShard;
    }

    public void addShard(DiscordApi shard) {
        this.allShards.add(shard);
    }

    @Nullable
    public DiscordApi getPrivateMessagesShard() {
        return this.firstShard; // Private messages are always sent to the first shard
    }

    @Nullable
    public DiscordApi getShardForServer(long serverId) {
        for (DiscordApi shard : allShards) {
            boolean isResponsible = (serverId >> 22) % allShards.size() == shard.getCurrentShard();
            if (isResponsible)
                return shard;
        }

        return firstShard;
    }
}
