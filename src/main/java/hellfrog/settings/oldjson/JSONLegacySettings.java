package hellfrog.settings.oldjson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import hellfrog.common.CommonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class JSONLegacySettings {

    private final Path SETTINGS_PATH = Paths.get("./settings/");
    private final Path COMMON_SETTINGS = SETTINGS_PATH.resolve("common.json");
    private final String SERVER_SETTINGS_FILES_SUFFIX = "_server.json";
    private final String SERVER_STATISTICS_FILES_SUFFIX = "_stat.json";
    private final Map<Long, JSONServerPreferences> prefByServer = new HashMap<>();
    private final Map<Long, JSONServerStatistic> statByServer = new HashMap<>();
    private final Logger log = LogManager.getLogger("JSON legacy loader");
    private JSONCommonPreferences jsonCommonPreferences = new JSONCommonPreferences();

    public JSONLegacySettings() {
        loadCommonSettings();
        loadServersSettings();
    }

    @NotNull
    private ObjectMapper buildMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        return objectMapper;
    }

    private void loadCommonSettings() {

        if (Files.exists(COMMON_SETTINGS) && Files.isRegularFile(COMMON_SETTINGS)) {
            log.info("Found legacy common settings file \"{}\"", COMMON_SETTINGS.toString());
            ObjectMapper objectMapper = buildMapper();
            JSONCommonPreferences commonPrefDefault = new JSONCommonPreferences();

            try (BufferedReader reader = Files.newBufferedReader(COMMON_SETTINGS, StandardCharsets.UTF_8)) {
                jsonCommonPreferences = objectMapper.readValue(reader, JSONCommonPreferences.class);
            } catch (IOException err) {
                log.error("Unable to read legacy common settings from file \"" + COMMON_SETTINGS + "\": "
                        + err.getMessage(), err);
                return;
            }

            if (CommonUtils.isTrStringEmpty(jsonCommonPreferences.getCommonBotPrefix())) {
                jsonCommonPreferences.setCommonBotPrefix(commonPrefDefault.getCommonBotPrefix());
            }

            if (CommonUtils.isTrStringEmpty(jsonCommonPreferences.getBotName())) {
                jsonCommonPreferences.setBotName(commonPrefDefault.getBotName());
            }

            if (jsonCommonPreferences.getGlobalBotOwners() == null) {
                jsonCommonPreferences.setGlobalBotOwners(commonPrefDefault.getGlobalBotOwners());
            }
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
                log.info("Found legacy server config settings file \"{}\"", file.toString());
                try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {

                    String serverIdRaw = CommonUtils.cutRightString(file.getFileName().toString(),
                            SERVER_SETTINGS_FILES_SUFFIX);
                    if (!CommonUtils.isLong(serverIdRaw))
                        throw new IOException("unable to parse server id from file name - " +
                                file.getFileName().toString());

                    long serverId = Long.parseLong(serverIdRaw);
                    JSONServerPreferences serverPreferences =
                            objectMapper.readValue(reader, JSONServerPreferences.class);

                    if (CommonUtils.isTrStringEmpty(serverPreferences.getBotPrefix()))
                        serverPreferences.setBotPrefix(jsonCommonPreferences.getCommonBotPrefix());

                    if (serverPreferences.getSrvCommandRights() == null) {
                        serverPreferences.setSrvCommandRights(new HashMap<>());
                    }

                    if (serverPreferences.getActiveVotes() == null) {
                        serverPreferences.setActiveVotes(new ArrayList<>());
                    }

                    prefByServer.put(serverId, serverPreferences);

                } catch (IOException err) {
                    log.error("Unable to read legacy server config settings file \"" + file + "\": "
                            + err.getMessage(), err);
                }
            }
        } catch (IOException err) {
            log.error("Unable to enumerate legacy server config settings files: " + err.getMessage(), err);
        }

        DirectoryStream.Filter<Path> onlyServerStat = entry ->
                Files.isRegularFile(entry) &&
                        entry.getFileName().toString().toLowerCase().endsWith(SERVER_STATISTICS_FILES_SUFFIX);

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(SETTINGS_PATH, onlyServerStat)) {
            for (Path entry : ds) {
                log.info("Found legacy server statistics file \"{}\"", entry.toString());
                long serverId = CommonUtils.onlyNumbersToLong(entry.getFileName().toString());
                if (serverId == 0) {
                    log.error("Unable to parse server id from file name: " + entry);
                    continue;
                }
                try (BufferedReader bfReader = Files.newBufferedReader(entry, StandardCharsets.UTF_8)) {
                    JSONServerStatistic stat = objectMapper.readValue(bfReader, JSONServerStatistic.class);
                    if (stat.getNonDefaultSmileStats() == null) {
                        stat.setNonDefaultSmileStats(new HashMap<>());
                    }
                    if (stat.getUserMessagesStats() == null) {
                        stat.setUserMessagesStats(new HashMap<>());
                    }
                    if (stat.getTextChatStats() == null) {
                        stat.setTextChatStats(new HashMap<>());
                    }
                    statByServer.put(serverId, stat);
                } catch (NullPointerException | IOException statReadErr) {
                    log.error("Unable to read legacy server statistics file " + entry + ": " + statReadErr);
                }
            }
        } catch (IOException err) {
            log.error("Unable to enumerate legacy server statistics files: " + err);
        }
    }

    public JSONCommonPreferences getJsonCommonPreferences() {
        return jsonCommonPreferences;
    }

    public Map<Long, JSONServerPreferences> getPrefByServer() {
        return Collections.unmodifiableMap(prefByServer);
    }

    public Map<Long, JSONServerStatistic> getStatByServer() {
        return Collections.unmodifiableMap(statByServer);
    }
}
