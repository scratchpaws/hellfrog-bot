package hellfrog.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import hellfrog.common.CommonUtils;
import hellfrog.common.HttpClientsPool;
import hellfrog.core.*;
import hellfrog.settings.db.InstanceType;
import hellfrog.settings.db.MainDBController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApi;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class SettingsController {

    private static final Logger log = LogManager.getLogger("Settings controller");
    private final Path SETTINGS_PATH = Paths.get("./settings/");

    private final String apiKey;

    private final String COMMON_SETTINGS_FILE_NAME = "common.json";
    private final Path COMMON_SETTINGS = SETTINGS_PATH.resolve(COMMON_SETTINGS_FILE_NAME);
    private final String SERVER_SETTINGS_FILES_SUFFIX = "_server.json";
    private final String SERVER_STATISTICS_FILES_SUFFIX = "_stat.json";

    private final ConcurrentHashMap<Long, ServerPreferences> prefByServer = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, ServerStatistic> statByServer = new ConcurrentHashMap<>();

    private final ReentrantLock serverPrefCreateLock = new ReentrantLock();
    private final ReentrantLock serverPrefSaveLock = new ReentrantLock();
    private final ReentrantLock commonPrefSaveLock = new ReentrantLock();
    private final ReentrantLock serverStatCreateLock = new ReentrantLock();
    private final ReentrantLock serverStatSaveLock = new ReentrantLock();

    private final VoteController voteController;
    private final InvitesController invitesController;
    private final HttpClientsPool httpClientsPool;
    private final AutoSaveSettingsTask autoSaveSettingsTask;
    private final SessionsCheckTask sessionsCheckTask;
    private final CongratulationsController congratulationsController;
    private final ServiceLogsNotificator serviceLogsNotificator;
    private final AutoBackupService autoBackupService;

    private final AutoPromoteService autoPromoteService;

    private CommonPreferences commonPreferences = new CommonPreferences();
    private DiscordApi discordApi = null;
    private volatile Instant lastCommandUsage = null;
    private MainDBController mainDBController;

    private static final SettingsController instance = new SettingsController();

    private SettingsController() {

        try {
            mainDBController = MainDBController.getInstance(InstanceType.PROD);
        } catch (Exception err) {
            // todo: Transition warnings. In the future, errors in starting the database will lead to the crash of the application.
            String errMsg = String.format("Unable to start %s database: %s", InstanceType.PROD, err.getMessage());
            log.error(errMsg, err);
            LogsStorage.addErrorMessage(errMsg);
        }

        String _apiKey = "";
        try {
            _apiKey = ApiKeyStorage.readApiKey();
        } catch (IOException err) {
            // todo: todo: Transition warnings. In the future, errors in reading the file with the API key will result in application crash
            log.error(err.getMessage(), err);
            LogsStorage.addErrorMessage(err.getMessage());
        }
        apiKey = _apiKey;

        loadCommonSettings();
        loadServersSettings();

        httpClientsPool = new HttpClientsPool();
        voteController = new VoteController();
        invitesController = new InvitesController();
        autoSaveSettingsTask = new AutoSaveSettingsTask();
        sessionsCheckTask = new SessionsCheckTask();
        congratulationsController = new CongratulationsController();
        serviceLogsNotificator = new ServiceLogsNotificator();
        autoBackupService = new AutoBackupService();

        autoPromoteService = new AutoPromoteService(mainDBController.getAutoPromoteRolesDAO(),
                mainDBController.getRoleAssignDAO(),
                mainDBController.getServerPreferencesDAO());

        Runtime.getRuntime().addShutdownHook(new Thread(() ->
                SettingsController.getInstance().mainDBController.close()));
    }

    @Contract(pure = true)
    public static SettingsController getInstance() {
        return instance;
    }

    @Deprecated
    private void loadCommonSettings() {
        ObjectMapper objectMapper = buildMapper();

        try {
            if (!Files.exists(SETTINGS_PATH) || !Files.isDirectory(SETTINGS_PATH)) {
                Files.createDirectory(SETTINGS_PATH);
            }
        } catch (IOException err) {
            System.err.println("Unable to create settings directory: " + err);
            System.exit(2);
        }
        if (!Files.exists(COMMON_SETTINGS) || !Files.isRegularFile(COMMON_SETTINGS)) {
            System.err.println("Creating empty common config file with default parameters");
            System.err.println("Filename is - " + COMMON_SETTINGS);
            try (BufferedWriter writer = Files.newBufferedWriter(COMMON_SETTINGS, StandardCharsets.UTF_8)) {
                objectMapper.writeValue(writer, commonPreferences);
            } catch (IOException err) {
                System.err.println("Unable to create settings file: " + err);
                System.exit(2);
            }
        }

        CommonPreferences commonPrefDefault = new CommonPreferences();

        try (BufferedReader reader = Files.newBufferedReader(COMMON_SETTINGS)) {
            commonPreferences = objectMapper.readValue(reader, CommonPreferences.class);
        } catch (IOException err) {
            System.err.println("Unable to read common preferences from file " + COMMON_SETTINGS + ": " + err);
            System.exit(2);
        }

        if (CommonUtils.isTrStringEmpty(commonPreferences.getApiKey())) {
            startupError();
        }

        if (CommonUtils.isTrStringEmpty(commonPreferences.getCommonBotPrefix())) {
            commonPreferences.setCommonBotPrefix(commonPrefDefault.getCommonBotPrefix());
        }

        if (CommonUtils.isTrStringEmpty(commonPreferences.getBotName())) {
            commonPreferences.setBotName(commonPrefDefault.getBotName());
        }

        if (commonPreferences.getGlobalBotOwners() == null) {
            commonPreferences.setGlobalBotOwners(commonPrefDefault.getGlobalBotOwners());
        }
    }

    @Deprecated
    private void loadServersSettings() {
        ObjectMapper objectMapper = buildMapper();
        DirectoryStream.Filter<Path> onlyServerConfigs = entity ->
                entity.getFileName().toString().toLowerCase().endsWith(SERVER_SETTINGS_FILES_SUFFIX) &&
                        Files.isRegularFile(entity);

        try (DirectoryStream<Path> serverConfigsStream = Files.newDirectoryStream(SETTINGS_PATH,
                onlyServerConfigs)) {
            for (Path file : serverConfigsStream) {
                try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {

                    String serverIdRaw = CommonUtils.cutRightString(file.getFileName().toString(),
                            SERVER_SETTINGS_FILES_SUFFIX);
                    if (!CommonUtils.isLong(serverIdRaw))
                        throw new IOException("unable to parse server id from file name - " +
                                file.getFileName().toString());

                    long serverId = Long.parseLong(serverIdRaw);
                    ServerPreferences serverPreferences = objectMapper.readValue(reader, ServerPreferences.class);

                    if (CommonUtils.isTrStringEmpty(serverPreferences.getBotPrefix()))
                        serverPreferences.setBotPrefix(commonPreferences.getCommonBotPrefix());

                    if (serverPreferences.getSrvCommandRights() == null) {
                        serverPreferences.setSrvCommandRights(new ConcurrentHashMap<>());
                    }

                    if (serverPreferences.getActiveVotes() == null) {
                        serverPreferences.setActiveVotes(new CopyOnWriteArrayList<>());
                    }

                    prefByServer.put(serverId, serverPreferences);

                } catch (IOException err) {
                    System.err.println("Unable to read server config " + file + ": " + err);
                }
            }
        } catch (IOException err) {
            System.err.println("Unable to enumerate servers config files: " + err);
        }

        DirectoryStream.Filter<Path> onlyServerStat = entry ->
                Files.isRegularFile(entry) &&
                        entry.getFileName().toString().toLowerCase().endsWith(SERVER_STATISTICS_FILES_SUFFIX);

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(SETTINGS_PATH, onlyServerStat)) {
            for (Path entry : ds) {
                long serverId = CommonUtils.onlyNumbersToLong(entry.getFileName().toString());
                if (serverId == 0) {
                    System.err.println("Unable to parse server id from file name: " + entry);
                    continue;
                }
                try (BufferedReader bfReader = Files.newBufferedReader(entry, StandardCharsets.UTF_8)) {
                    ServerStatistic stat = objectMapper.readValue(bfReader, ServerStatistic.class);
                    if (stat.getNonDefaultSmileStats() == null) {
                        stat.setNonDefaultSmileStats(new ConcurrentHashMap<>());
                    }
                    if (stat.getUserMessagesStats() == null) {
                        stat.setUserMessagesStats(new ConcurrentHashMap<>());
                    }
                    if (stat.getTextChatStats() == null) {
                        stat.setTextChatStats(new ConcurrentHashMap<>());
                    }
                    statByServer.put(serverId, stat);
                } catch (NullPointerException | IOException statReadErr) {
                    System.err.println("Unable to read statistic file " + entry + ": " + statReadErr);
                }
            }
        } catch (IOException err) {
            System.err.println("Unable to enumerate server statistic files: " + err);
        }
    }

    @NotNull
    private ObjectMapper buildMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        return objectMapper;
    }

    private void startupError() {
        System.err.println("API key parameter not set");
        System.err.println("Please fill apiKey field and restart.");
        System.err.println("Also, you can check and fix another parameters.");
        throw new RuntimeException("Bot stopped");
    }

    @Deprecated
    public String getGlobalCommonPrefix() {
        return this.commonPreferences.getCommonBotPrefix();
    }

    @Deprecated
    public void setGlobalCommonPrefix(String globalCommonPrefix) {
        this.commonPreferences.setCommonBotPrefix(globalCommonPrefix);
        saveCommonPreferences();
    }

    @Deprecated
    public boolean addGlobalBotOwner(long userId) {
        boolean result = this.commonPreferences.addGlobalBotOwner(userId);
        saveCommonPreferences();
        return result;
    }

    @Deprecated
    public boolean delGlobalBotOwner(long userId) {
        boolean result = this.commonPreferences.delGlobalBotOwner(userId);
        saveCommonPreferences();
        return result;
    }

    @Deprecated
    public boolean isGlobalBotOwner(long userId) {
        return commonPreferences.getGlobalBotOwners().contains(userId);
    }

    @Deprecated
    public List<Long> getGlobalBotOwners() {
        return this.commonPreferences.getGlobalBotOwners();
    }

    @Deprecated
    public String getBotName() {
        return this.commonPreferences.getBotName();
    }

    @Deprecated
    public void setBotName(String botName) {
        this.commonPreferences.setBotName(botName);
        saveCommonPreferences();
    }

    @Deprecated
    public String getApiKey() {
        return this.commonPreferences.getApiKey();
    }

    @Deprecated
    public String getBotPrefix(long serverId) {
        return getServerPreferences(serverId).getBotPrefix();
    }

    @Deprecated
    public void setBotPrefix(long serverId, String botPrefix) {
        getServerPreferences(serverId).setBotPrefix(botPrefix);
        saveServerSideParameters(serverId);
    }

    @Deprecated
    public ServerPreferences getServerPreferences(long serverId) {
        if (!prefByServer.containsKey(serverId)) {
            serverPrefCreateLock.lock();
            try {
                if (!prefByServer.containsKey(serverId)) {
                    ServerPreferences serverPreferences = new ServerPreferences();
                    serverPreferences.setBotPrefix(commonPreferences.getCommonBotPrefix());
                    prefByServer.put(serverId, serverPreferences);
                }
            } finally {
                serverPrefCreateLock.unlock();
            }
        }
        return prefByServer.get(serverId);
    }

    @Deprecated
    public ServerStatistic getServerStatistic(long serverId) {
        if (!statByServer.containsKey(serverId)) {
            serverStatCreateLock.lock();
            try {
                if (!statByServer.containsKey(serverId)) {
                    ServerStatistic serverStatistic = new ServerStatistic();
                    statByServer.put(serverId, serverStatistic);
                }
            } finally {
                serverStatCreateLock.unlock();
            }
        }
        return statByServer.get(serverId);
    }

    @Deprecated
    public List<Long> getServerListWithConfig() {
        return new ArrayList<>(prefByServer.keySet());
    }

    @Deprecated
    public List<Long> getServerListWithStatistic() {
        return new ArrayList<>(prefByServer.keySet());
    }

    @Deprecated
    public void saveServerSideParameters(long serverId) {
        String fileName = serverId + SERVER_SETTINGS_FILES_SUFFIX;
        ServerPreferences toSave = getServerPreferences(serverId);
        serverPrefSaveLock.lock();
        try {
            saveWithReplace(toSave, fileName);
        } finally {
            serverPrefSaveLock.unlock();
        }
    }

    @Deprecated
    public void saveServerSideStatistic(long serverId) {
        String fileName = serverId + SERVER_STATISTICS_FILES_SUFFIX;
        ServerStatistic toSave = getServerStatistic(serverId);
        serverStatSaveLock.lock();
        try {
            saveWithReplace(toSave, fileName);
        } finally {
            serverStatSaveLock.unlock();
        }
    }

    @Deprecated
    public void saveCommonPreferences() {
        commonPrefSaveLock.lock();
        try {
            saveWithReplace(commonPreferences, COMMON_SETTINGS_FILE_NAME);
        } finally {
            commonPrefSaveLock.unlock();
        }
    }

    @Deprecated
    private void saveWithReplace(@NotNull Object value,
                                 @NotNull String fileName) {
        String tempFileName = fileName + ".tmp";
        Path tempFile = SETTINGS_PATH.resolve(tempFileName);
        Path configFile = SETTINGS_PATH.resolve(fileName);
        ObjectMapper objectMapper = buildMapper();
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
            objectMapper.writeValue(bufferedWriter, value);
        } catch (IOException err) {
            String errMsg = String.format("Unable to save temp file \"%s\": %s",
                    tempFile, err.getMessage());
            log.error(errMsg, err);
            return;
        }
        try {
            Files.move(tempFile, configFile, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException first) {
            try {
                Files.move(tempFile, configFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException err) {
                String errMsg = String.format("Unable to replace old settings file \"%s\" by temp file \"%s\": %s",
                        configFile, tempFile, err.getMessage());
                log.error(errMsg, err);
            }
        }
    }

    public VoteController getVoteController() {
        return voteController;
    }

    public InvitesController getInvitesController() {
        return invitesController;
    }

    public CongratulationsController getCongratulationsController() {
        return congratulationsController;
    }

    public AutoSaveSettingsTask getAutoSaveSettingsTask() {
        return autoSaveSettingsTask;
    }

    public DiscordApi getDiscordApi() {
        return discordApi;
    }

    public void setDiscordApi(DiscordApi discordApi) {
        this.discordApi = discordApi;
    }

    @Deprecated
    public boolean isEnableRemoteDebug() {
        return commonPreferences.isEnableRemoteDebug();
    }

    @Deprecated
    public void setEnableRemoteDebug(boolean enableRemoteDebug) {
        commonPreferences.setEnableRemoteDebug(enableRemoteDebug);
    }

    public void updateLastCommandUsage() {
        this.lastCommandUsage = Instant.now();
    }

    public Instant getLastCommandUsage() {
        return this.lastCommandUsage;
    }

    @Deprecated
    public Long getServerTransfer() {
        return commonPreferences.getServerTransfer();
    }

    @Deprecated
    public void setServerTransfer(Long serverTransfer) {
        commonPreferences.setServerTransfer(serverTransfer);
    }

    @Deprecated
    public Long getServerTextChatTransfer() {
        return commonPreferences.getServerTextChatTransfer();
    }

    @Deprecated
    public void setServerTextChatTransfer(Long serverTextChatTransfer) {
        commonPreferences.setServerTextChatTransfer(serverTextChatTransfer);
    }

    public SessionsCheckTask getSessionsCheckTask() {
        return sessionsCheckTask;
    }

    public synchronized void stopMainDatabase() {
        this.mainDBController.close();
    }

    public MainDBController getMainDBController() {
        return this.mainDBController;
    }

    public HttpClientsPool getHttpClientsPool() {
        return httpClientsPool;
    }

    public ServiceLogsNotificator getServiceLogsNotificator() {
        return serviceLogsNotificator;
    }

    public AutoBackupService getAutoBackupService() {
        return autoBackupService;
    }

    public AutoPromoteService getAutoPromoteService() {
        return autoPromoteService;
    }
}
