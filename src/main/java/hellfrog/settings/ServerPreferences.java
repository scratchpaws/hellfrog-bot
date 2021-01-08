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
@Deprecated
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

    private volatile Long congratulationChannel = null;
    private volatile String timezone = null;

    /**
     * Получить префикс для вызова бота на данном сервере
     *
     * @return префикс бота
     */
    @Deprecated
    public String getBotPrefix() {
        return botPrefix;
    }

    /**
     * Установить префикс бота для вызова на данном сервере
     *
     * @param botPrefix префикс бота
     */
    @Deprecated
    public void setBotPrefix(String botPrefix) {
        this.botPrefix = botPrefix;
    }

    @Deprecated
    public ConcurrentHashMap<String, CommandRights> getSrvCommandRights() {
        return srvCommandRights;
    }

    @Deprecated
    public void setSrvCommandRights(ConcurrentHashMap<String, CommandRights> commandRights) {
        this.srvCommandRights = new ConcurrentHashMap<>(commandRights);
    }

    @Deprecated
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

    @Deprecated
    public List<ActiveVote> getActiveVotes() {
        return activeVotes;
    }

    @Deprecated
    public void setActiveVotes(List<ActiveVote> activeVotes) {
        this.activeVotes = new CopyOnWriteArrayList<>(activeVotes);
    }

    @Deprecated
    public boolean isJoinLeaveDisplay() {
        return this.joinLeaveDisplay != null && this.joinLeaveDisplay;
    }

    @Deprecated
    public void setJoinLeaveDisplay(boolean state) {
        this.joinLeaveDisplay = state;
    }

    @Deprecated
    public long getJoinLeaveChannel() {
        return this.joinLeaveChannel != null ? this.joinLeaveChannel : 0L;
    }

    @Deprecated
    public void setJoinLeaveChannel(long textChannelId) {
        this.joinLeaveChannel = textChannelId;
    }

    @Deprecated
    public boolean getNewAclMode() {
        return newAclMode != null && newAclMode;
    }

    @Deprecated
    public void setNewAclMode(boolean newAclMode) {
        this.newAclMode = newAclMode;
    }

    @Deprecated
    public Boolean getAutoPromoteEnabled() {
        return autoPromoteEnabled != null && autoPromoteEnabled;
    }

    @Deprecated
    public void setAutoPromoteEnabled(Boolean autoPromoteEnabled) {
        this.autoPromoteEnabled = autoPromoteEnabled != null ? autoPromoteEnabled : false;
    }

    @Deprecated
    public Long getAutoPromoteRoleId() {
        return autoPromoteRoleId;
    }

    @Deprecated
    public void setAutoPromoteRoleId(Long autoPromoteRoleId) {
        this.autoPromoteRoleId = autoPromoteRoleId;
    }

    @Deprecated
    public Long getAutoPromoteTimeout() {
        return autoPromoteTimeout != null && autoPromoteTimeout >= 0L
                ? autoPromoteTimeout : 0L;
    }

    @Deprecated
    public void setAutoPromoteTimeout(Long autoPromoteTimeout) {
        this.autoPromoteTimeout = autoPromoteTimeout != null && autoPromoteTimeout >= 0L
                ? autoPromoteTimeout : 0L;
    }

    @Deprecated
    public List<Long> getCommunityControlUsers() {
        return communityControlUsers;
    }

    @Deprecated
    public void setCommunityControlUsers(List<Long> communityControlUsers) {
        this.communityControlUsers = communityControlUsers != null ?
                new CopyOnWriteArrayList<>(communityControlUsers) : new CopyOnWriteArrayList<>();
    }

    @Deprecated
    public Long getCommunityControlThreshold() {
        return communityControlThreshold != null ? communityControlThreshold : 0L;
    }

    @Deprecated
    public void setCommunityControlThreshold(Long communityControlThreshold) {
        this.communityControlThreshold = communityControlThreshold != null ?
                communityControlThreshold : 0L;
    }

    @Deprecated
    public Long getCommunityControlRoleId() {
        return communityControlRoleId != null ? communityControlRoleId : 0L;
    }

    @Deprecated
    public void setCommunityControlRoleId(Long communityControlRoleId) {
        this.communityControlRoleId = communityControlRoleId != null ?
                communityControlRoleId : 0L;
    }

    @Deprecated
    public Long getCommunityControlCustomEmojiId() {
        return communityControlCustomEmojiId != null ?
                communityControlCustomEmojiId : 0L;
    }

    @Deprecated
    public void setCommunityControlCustomEmojiId(Long communityControlCustomEmojiId) {
        this.communityControlCustomEmojiId = communityControlCustomEmojiId != null ?
                communityControlCustomEmojiId : 0L;
    }

    @Deprecated
    public String getCommunityControlEmoji() {
        return communityControlEmoji;
    }

    @Deprecated
    public void setCommunityControlEmoji(String communityControlEmoji) {
        this.communityControlEmoji = communityControlEmoji;
    }

    @Deprecated
    public ConcurrentHashMap<Long, WtfMap> getWtfMapper() {
        return wtfMapper;
    }

    @Deprecated
    public void setWtfMapper(ConcurrentHashMap<Long, WtfMap> wtfMapper) {
        this.wtfMapper = wtfMapper != null ? wtfMapper : new ConcurrentHashMap<>();
    }

    @Deprecated
    public Long getCongratulationChannel() {
        return congratulationChannel;
    }

    @Deprecated
    public void setCongratulationChannel(Long congratulationChannel) {
        this.congratulationChannel = congratulationChannel;
    }

    @Deprecated
    public String getTimezone() {
        return timezone;
    }

    @Deprecated
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
}
