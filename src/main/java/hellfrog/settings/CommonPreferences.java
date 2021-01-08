package hellfrog.settings;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Общие настройки бота
 */
@Deprecated
public class CommonPreferences
        implements Serializable {

    /**
     * Префикс по-умолчанию. Используется при прямом обращении к боту, либо если
     * в настройках для сервера не указано иное
     */
    private volatile String commonBotPrefix = ">>";
    /**
     * Имя бота, отображается в вообщениях
     */
    private volatile String botName = "HellFrog";

    private volatile String apiKey = "";

    private volatile Long serverTransfer = null;

    private volatile Long serverTextChatTransfer = null;

    /**
     * Отладка, по-умолчанию выключена и требует явной активации
     */
    private volatile Boolean enableRemoteDebug = false;

    /**
     * Глобальные владельцы. Имеют права на команды с ключом --global и какие-либо важные
     * команды
     */
    private CopyOnWriteArrayList<Long> globalBotOwners = new CopyOnWriteArrayList<>();

    /**
     * Получить основной префикс бота
     *
     * @return
     */
    @Deprecated
    public String getCommonBotPrefix() {
        return commonBotPrefix;
    }

    @Deprecated
    public void setCommonBotPrefix(String commonBotPrefix) {
        this.commonBotPrefix = commonBotPrefix;
    }

    @Deprecated
    public String getBotName() {
        return botName;
    }

    @Deprecated
    public void setBotName(String botName) {
        this.botName = botName;
    }

    @Deprecated
    public List<Long> getGlobalBotOwners() {
        return globalBotOwners;
    }

    @Deprecated
    public void setGlobalBotOwners(List<Long> globalBotOwners) {
        this.globalBotOwners = new CopyOnWriteArrayList<>(globalBotOwners);
    }

    @Deprecated
    public boolean addGlobalBotOwner(long id) {
        if (id < 0 || globalBotOwners.contains(id)) return false;
        globalBotOwners.add(id);
        return true;
    }

    @Deprecated
    public boolean delGlobalBotOwner(long id) {
        if (id < 0 || !globalBotOwners.contains(id)) return false;
        globalBotOwners.remove(id);
        return true;
    }

    @Deprecated
    public String getApiKey() {
        return apiKey;
    }

    @Deprecated
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    @Deprecated
    public boolean isEnableRemoteDebug() {
        return enableRemoteDebug != null && enableRemoteDebug;
    }

    @Deprecated
    public void setEnableRemoteDebug(boolean enableRemoteDebug) {
        this.enableRemoteDebug = enableRemoteDebug;
    }

    @Deprecated
    public Long getServerTransfer() {
        return serverTransfer;
    }

    @Deprecated
    public void setServerTransfer(Long serverTransfer) {
        this.serverTransfer = serverTransfer;
    }

    @Deprecated
    public Long getServerTextChatTransfer() {
        return serverTextChatTransfer;
    }

    @Deprecated
    public void setServerTextChatTransfer(Long serverTextChatTransfer) {
        this.serverTextChatTransfer = serverTextChatTransfer;
    }
}
