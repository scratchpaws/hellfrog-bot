package xyz.funforge.scratchypaws.hellfrog.settings.old;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Общие настройки бота
 */
public class CommonPreferences
        implements Serializable {

    /**
     * Префикс по-умолчанию. Используется при прямом обращении к боту, либо если
     * в настройках для сервера не указано иное
     */
    private volatile String commonBotPrefix = "r!b";
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
        this.globalBotOwners = new CopyOnWriteArrayList<>(globalBotOwners);
    }

    public boolean addGlobalBotOwner(long id) {
        if (id < 0 || globalBotOwners.contains(id)) return false;
        globalBotOwners.add(id);
        return true;
    }

    public boolean delGlobalBotOwner(long id) {
        if (id < 0 || !globalBotOwners.contains(id)) return false;
        globalBotOwners.remove(id);
        return true;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public boolean isEnableRemoteDebug() {
        return enableRemoteDebug != null && enableRemoteDebug;
    }

    public void setEnableRemoteDebug(boolean enableRemoteDebug) {
        this.enableRemoteDebug = enableRemoteDebug;
    }

    public Long getServerTransfer() {
        return serverTransfer;
    }

    public void setServerTransfer(Long serverTransfer) {
        this.serverTransfer = serverTransfer;
    }

    public Long getServerTextChatTransfer() {
        return serverTextChatTransfer;
    }

    public void setServerTextChatTransfer(Long serverTextChatTransfer) {
        this.serverTextChatTransfer = serverTextChatTransfer;
    }
}
