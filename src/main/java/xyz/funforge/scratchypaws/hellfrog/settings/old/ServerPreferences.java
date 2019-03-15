package xyz.funforge.scratchypaws.hellfrog.settings.old;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Индивидуальные настройки для каждого сервера по-отдельности
 */
public class ServerPreferences
        implements Serializable {

    /**
     * Префикс для вызова команд бота
     */
    private volatile String botPrefix = "h!f";

    private ConcurrentHashMap<String, CommandRights> srvCommandRights = new ConcurrentHashMap<>();
    private CopyOnWriteArrayList<ActiveVote> activeVotes = new CopyOnWriteArrayList<>();

    private volatile Boolean joinLeaveDisplay = false;
    private volatile Long joinLeaveChannel = null;

    @JsonIgnore
    private transient ReentrantLock commandRightsGenLock = new ReentrantLock();

    /**
     * Получить префикс для вызова бота на данном сервере
     *
     * @return префикс бота
     */
    public String getBotPrefix() {
        return botPrefix;
    }

    /**
     * Установить префикс бота для вызова на данном сервере
     *
     * @param botPrefix префикс бота
     */
    public void setBotPrefix(String botPrefix) {
        this.botPrefix = botPrefix;
    }

    public ConcurrentHashMap<String, CommandRights> getSrvCommandRights() {
        return srvCommandRights;
    }

    public void setSrvCommandRights(ConcurrentHashMap<String, CommandRights> commandRights) {
        this.srvCommandRights = new ConcurrentHashMap<>(commandRights);
    }

    public CommandRights getRightsForCommand(String commandPrefix) {
        if (srvCommandRights.containsKey(commandPrefix)) {
            return srvCommandRights.get(commandPrefix);
        }
        commandRightsGenLock.lock();
        try {
            if (!srvCommandRights.containsKey(commandPrefix)) {
                CommandRights commandRights = new CommandRights();
                commandRights.setCommandPrefix(commandPrefix);
                srvCommandRights.put(commandPrefix, commandRights);
            }
        } finally {
            commandRightsGenLock.unlock();
        }
        return srvCommandRights.get(commandPrefix);
    }

    public List<ActiveVote> getActiveVotes() {
        return activeVotes;
    }

    public void setActiveVotes(List<ActiveVote> activeVotes) {
        this.activeVotes = new CopyOnWriteArrayList<>(activeVotes);
    }

    public void setJoinLeaveDisplay(boolean state) {
        this.joinLeaveDisplay = state;
    }

    public boolean isJoinLeaveDisplay() {
        return this.joinLeaveDisplay != null && this.joinLeaveDisplay;
    }

    public void setJoinLeaveChannel(long textChannelId) {
        this.joinLeaveChannel = textChannelId;
    }

    public long getJoinLeaveChannel() {
        return Objects.requireNonNullElse(this.joinLeaveChannel, 0L);
    }
}
