package hellfrog.settings;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Индивидуальные настройки для каждого сервера по-отдельности
 */
public class ServerPreferences
        implements Serializable {

    @JsonIgnore
    private final ReentrantLock commandRightsGenLock = new ReentrantLock();
    /**
     * Префикс для вызова команд бота
     */
    private volatile String botPrefix = ">>";
    private ConcurrentHashMap<String, CommandRights> srvCommandRights = new ConcurrentHashMap<>();
    private CopyOnWriteArrayList<ActiveVote> activeVotes = new CopyOnWriteArrayList<>();
    private volatile Boolean joinLeaveDisplay = false;
    private volatile Long joinLeaveChannel = null;
    private volatile Boolean newAclMode = false;

    private volatile Boolean autoPromoteEnabled = false;
    private volatile Long autoPromoteRoleId = null;
    private volatile Long autoPromoteTimeout = 0L;

    private CopyOnWriteArrayList<Long> communityControlUsers = new CopyOnWriteArrayList<>();
    private volatile Long communityControlThreshold = 0L;
    private volatile Long communityControlRoleId = 0L;
    private volatile Long communityControlCustomEmojiId = 0L;
    private volatile String communityControlEmoji = null;

    private ConcurrentHashMap<Long, WtfMap> wtfMapper = new ConcurrentHashMap<>();

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

    public boolean isJoinLeaveDisplay() {
        return this.joinLeaveDisplay != null && this.joinLeaveDisplay;
    }

    public void setJoinLeaveDisplay(boolean state) {
        this.joinLeaveDisplay = state;
    }

    public long getJoinLeaveChannel() {
        return this.joinLeaveChannel != null ? this.joinLeaveChannel : 0L;
    }

    public void setJoinLeaveChannel(long textChannelId) {
        this.joinLeaveChannel = textChannelId;
    }

    public boolean getNewAclMode() {
        return newAclMode != null && newAclMode;
    }

    public void setNewAclMode(boolean newAclMode) {
        this.newAclMode = newAclMode;
    }

    public Boolean getAutoPromoteEnabled() {
        return autoPromoteEnabled != null && autoPromoteEnabled;
    }

    public void setAutoPromoteEnabled(Boolean autoPromoteEnabled) {
        this.autoPromoteEnabled = autoPromoteEnabled != null ? autoPromoteEnabled : false;
    }

    public Long getAutoPromoteRoleId() {
        return autoPromoteRoleId;
    }

    public void setAutoPromoteRoleId(Long autoPromoteRoleId) {
        this.autoPromoteRoleId = autoPromoteRoleId;
    }

    public Long getAutoPromoteTimeout() {
        return autoPromoteTimeout != null && autoPromoteTimeout >= 0L
                ? autoPromoteTimeout : 0L;
    }

    public void setAutoPromoteTimeout(Long autoPromoteTimeout) {
        this.autoPromoteTimeout = autoPromoteTimeout != null && autoPromoteTimeout >= 0L
            ? autoPromoteTimeout : 0L;
    }

    public List<Long> getCommunityControlUsers() {
        return communityControlUsers;
    }

    public void setCommunityControlUsers(List<Long> communityControlUsers) {
        this.communityControlUsers = communityControlUsers != null ?
            new CopyOnWriteArrayList<>(communityControlUsers) : new CopyOnWriteArrayList<>();
    }

    public Long getCommunityControlThreshold() {
        return communityControlThreshold != null ? communityControlThreshold : 0L;
    }

    public void setCommunityControlThreshold(Long communityControlThreshold) {
        this.communityControlThreshold = communityControlThreshold != null ?
            communityControlThreshold : 0L;
    }

    public Long getCommunityControlRoleId() {
        return communityControlRoleId != null ? communityControlRoleId : 0L;
    }

    public void setCommunityControlRoleId(Long communityControlRoleId) {
        this.communityControlRoleId = communityControlRoleId != null ?
            communityControlRoleId : 0L;
    }

    public Long getCommunityControlCustomEmojiId() {
        return communityControlCustomEmojiId != null ?
                communityControlCustomEmojiId : 0L;
    }

    public void setCommunityControlCustomEmojiId(Long communityControlCustomEmojiId) {
        this.communityControlCustomEmojiId = communityControlCustomEmojiId != null ?
            communityControlCustomEmojiId : 0L;
    }

    public String getCommunityControlEmoji() {
        return communityControlEmoji;
    }

    public void setCommunityControlEmoji(String communityControlEmoji) {
        this.communityControlEmoji = communityControlEmoji;
    }

    public ConcurrentHashMap<Long, WtfMap> getWtfMapper() {
        return wtfMapper;
    }

    public void setWtfMapper(ConcurrentHashMap<Long, WtfMap> wtfMapper) {
        this.wtfMapper = wtfMapper != null ? wtfMapper : new ConcurrentHashMap<>();
    }
}
