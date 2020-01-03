package hellfrog.settings.oldjson;

import java.util.Collections;
import java.util.List;

// DB migration is ok
public class JSONCommonPreferences {

    private String commonBotPrefix = ">>"; // hellfrog.settings.db.CommonPreferencesDAO.getBotPrefix
    private String botName = "HellFrog"; // hellfrog.settings.db.CommonPreferencesDAO.getBotName
    private String apiKey = ""; // hellfrog.settings.db.CommonPreferencesDAO.getApiKey
    private long serverTransfer = 0L; // will be removed
    private long serverTextChatTransfer = 0L; // will be removed
    private boolean enableRemoteDebug = false; // will be removed
    private List<Long> globalBotOwners = Collections.emptyList(); // hellfrog.settings.db.BotOwnersDAO

    public String getCommonBotPrefix() {
        return commonBotPrefix;
    }

    public void setCommonBotPrefix(String commonBotPrefix) {
        this.commonBotPrefix = commonBotPrefix;
    }

    public String getBotName() {
        return botName;
    }

    public void setBotName(String botName) {
        this.botName = botName;
    }

    public List<Long> getGlobalBotOwners() {
        return globalBotOwners;
    }

    public void setGlobalBotOwners(List<Long> globalBotOwners) {
        this.globalBotOwners = globalBotOwners != null
                ? Collections.unmodifiableList(globalBotOwners) : this.globalBotOwners;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public boolean isEnableRemoteDebug() {
        return enableRemoteDebug;
    }

    public void setEnableRemoteDebug(boolean enableRemoteDebug) {
        this.enableRemoteDebug = enableRemoteDebug;
    }

    public long getServerTransfer() {
        return serverTransfer;
    }

    public void setServerTransfer(long serverTransfer) {
        this.serverTransfer = serverTransfer;
    }

    public long getServerTextChatTransfer() {
        return serverTextChatTransfer;
    }

    public void setServerTextChatTransfer(long serverTextChatTransfer) {
        this.serverTextChatTransfer = serverTextChatTransfer;
    }
}
