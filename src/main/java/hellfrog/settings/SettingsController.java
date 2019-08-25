package hellfrog.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import hellfrog.common.CommonUtils;
import hellfrog.core.ServerStatisticTask;
import hellfrog.core.SessionsCheckTask;
import hellfrog.core.VoteController;
import hellfrog.settings.db.MainDBController;
import org.javacord.api.DiscordApi;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class SettingsController {

    private final Path SETTINGS_PATH = Paths.get("./settings/");

    private final Path COMMON_SETTINGS = SETTINGS_PATH.resolve("common.json");
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
    private final ServerStatisticTask serverStatisticTask;
    private final SessionsCheckTask sessionsCheckTask;
    private CommonPreferences commonPreferences = new CommonPreferences();
    private DiscordApi discordApi = null;
    private volatile Instant lastCommandUsage = null;
    private MainDBController mainDBController = new MainDBController();

    private static final SettingsController instance = new SettingsController();

    private SettingsController() {
        loadCommonSettings();
        loadServersSettings();
        voteController = new VoteController();
        serverStatisticTask = new ServerStatisticTask();
        sessionsCheckTask = new SessionsCheckTask();

        Runtime.getRuntime().addShutdownHook(new Thread(() ->
                SettingsController.getInstance().mainDBController.close()));
    }

    @Contract(pure = true)
    public static SettingsController getInstance() {
        return instance;
    }

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

    public String getGlobalCommonPrefix() {
        return this.commonPreferences.getCommonBotPrefix();
    }

    public void setGlobalCommonPrefix(String globalCommonPrefix) {
        this.commonPreferences.setCommonBotPrefix(globalCommonPrefix);
        saveCommonPreferences();
    }

    public boolean addGlobalBotOwner(long userId) {
        boolean result = this.commonPreferences.addGlobalBotOwner(userId);
        saveCommonPreferences();
        return result;
    }

    public boolean delGlobalBotOwner(long userId) {
        boolean result = this.commonPreferences.delGlobalBotOwner(userId);
        saveCommonPreferences();
        return result;
    }

    public boolean isGlobalBotOwner(long userId) {
        return commonPreferences.getGlobalBotOwners().contains(userId);
    }

    public List<Long> getGlobalBotOwners() {
        return this.commonPreferences.getGlobalBotOwners();
    }

    public String getBotName() {
        return this.commonPreferences.getBotName();
    }

    public void setBotName(String botName) {
        this.commonPreferences.setBotName(botName);
        saveCommonPreferences();
    }

    public String getApiKey() {
        return this.commonPreferences.getApiKey();
    }

    public String getBotPrefix(long serverId) {
        return getServerPreferences(serverId).getBotPrefix();
    }

    public void setBotPrefix(long serverId, String botPrefix) {
        getServerPreferences(serverId).setBotPrefix(botPrefix);
        saveServerSideParameters(serverId);
    }

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

    public List<Long> getServerListWithConfig() {
        return new ArrayList<>(prefByServer.keySet());
    }

    public List<Long> getServerListWithStatistic() {
        return new ArrayList<>(prefByServer.keySet());
    }

    public void saveServerSideParameters(long serverId) {
        ObjectMapper objectMapper = buildMapper();
        String fileName = serverId + SERVER_SETTINGS_FILES_SUFFIX;
        Path serverSideConfig = SETTINGS_PATH.resolve(fileName);
        ServerPreferences toSave = getServerPreferences(serverId);
        serverPrefSaveLock.lock();
        try {
            try (BufferedWriter bufferedWriter = Files.newBufferedWriter(serverSideConfig, StandardCharsets.UTF_8)) {
                objectMapper.writeValue(bufferedWriter, toSave);
            } catch (IOException err) {
                System.err.println("Unable to save server-side settings: " + err);
            }
        } finally {
            serverPrefSaveLock.unlock();
        }
    }

    public void saveServerSideStatistic(long serverId) {
        ObjectMapper objectMapper = buildMapper();
        String fileName = serverId + SERVER_STATISTICS_FILES_SUFFIX;
        Path serverSideStatPath = SETTINGS_PATH.resolve(fileName);
        ServerStatistic toSave = getServerStatistic(serverId);
        serverStatSaveLock.lock();
        try {
            try (BufferedWriter bufferedWriter = Files.newBufferedWriter(serverSideStatPath, StandardCharsets.UTF_8)) {
                objectMapper.writeValue(bufferedWriter, toSave);
            } catch (IOException err) {
                System.err.println("Unable to save server-side statistic: " + err);
            }
        } finally {
            serverStatSaveLock.unlock();
        }
    }

    public void saveCommonPreferences() {
        ObjectMapper objectMapper = buildMapper();
        commonPrefSaveLock.lock();
        try {
            try (BufferedWriter bufferedWriter = Files.newBufferedWriter(COMMON_SETTINGS)) {
                objectMapper.writeValue(bufferedWriter, commonPreferences);
            } catch (IOException err) {
                System.err.println("Unable to save common bot settings: " + err);
            }
        } finally {
            commonPrefSaveLock.unlock();
        }
    }

    public VoteController getVoteController() {
        return voteController;
    }

    public ServerStatisticTask getServerStatisticTask() {
        return serverStatisticTask;
    }

    public DiscordApi getDiscordApi() {
        return discordApi;
    }

    public void setDiscordApi(DiscordApi discordApi) {
        this.discordApi = discordApi;
    }

    public boolean isEnableRemoteDebug() {
        return commonPreferences.isEnableRemoteDebug();
    }

    public void setEnableRemoteDebug(boolean enableRemoteDebug) {
        commonPreferences.setEnableRemoteDebug(enableRemoteDebug);
    }

    public void updateLastCommandUsage() {
        this.lastCommandUsage = Instant.now();
    }

    public Instant getLastCommandUsage() {
        return this.lastCommandUsage;
    }

    public Long getServerTransfer() {
        return commonPreferences.getServerTransfer();
    }

    public void setServerTransfer(Long serverTransfer) {
        commonPreferences.setServerTransfer(serverTransfer);
    }

    public Long getServerTextChatTransfer() {
        return commonPreferences.getServerTextChatTransfer();
    }

    public void setServerTextChatTransfer(Long serverTextChatTransfer) {
        commonPreferences.setServerTextChatTransfer(serverTextChatTransfer);
    }

    public SessionsCheckTask getSessionsCheckTask() {
        return sessionsCheckTask;
    }

    public synchronized void stopMainDatabase() {
        this.mainDBController.close();
    }
}
