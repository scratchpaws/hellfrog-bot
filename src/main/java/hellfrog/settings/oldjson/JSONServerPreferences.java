package hellfrog.settings.oldjson;

import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JSONServerPreferences {

    // hellfrog.settings.db.ServerPreferencesDAO.getPrefix
    // migrate available
    private String botPrefix = ">>";
    private Map<String, JSONCommandRights> srvCommandRights = Collections.emptyMap();
    private List<JSONActiveVote> activeVotes = Collections.emptyList();
    // hellfrog.settings.db.ServerPreferencesDAO.isJoinLeaveDisplay
    // migrate available
    private boolean joinLeaveDisplay = false;
    // hellfrog.settings.db.ServerPreferencesDAO.getJoinLeaveChannel
    // migrate available
    private long joinLeaveChannel = 0L;
    // hellfrog.settings.db.ServerPreferencesDAO.isNewAclMode
    // migrate available
    private boolean newAclMode = false;
    private boolean autoPromoteEnabled = false;
    private Long autoPromoteRoleId = null;
    private long autoPromoteTimeout = 0L;
    private List<Long> communityControlUsers = Collections.emptyList();
    private long communityControlThreshold = 0L;
    private long communityControlRoleId = 0L;
    private long communityControlCustomEmojiId = 0L;
    private String communityControlEmoji = null;
    private Map<Long, JSONWtfMap> wtfMapper = Collections.emptyMap();

    public String getBotPrefix() {
        return botPrefix;
    }

    public void setBotPrefix(@Nullable String botPrefix) {
        this.botPrefix = botPrefix != null ? botPrefix : this.botPrefix;
    }

    public Map<String, JSONCommandRights> getSrvCommandRights() {
        return srvCommandRights;
    }

    public void setSrvCommandRights(Map<String, JSONCommandRights> commandRights) {
        this.srvCommandRights = Collections.unmodifiableMap(commandRights);
    }

    public List<JSONActiveVote> getActiveVotes() {
        return activeVotes;
    }

    public void setActiveVotes(List<JSONActiveVote> activeVotes) {
        this.activeVotes = activeVotes != null
                ? Collections.unmodifiableList(activeVotes) : this.activeVotes;
    }

    public boolean isJoinLeaveDisplay() {
        return joinLeaveDisplay;
    }

    public void setJoinLeaveDisplay(boolean state) {
        this.joinLeaveDisplay = state;
    }

    public long getJoinLeaveChannel() {
        return joinLeaveChannel;
    }

    public void setJoinLeaveChannel(long textChannelId) {
        this.joinLeaveChannel = textChannelId;
    }

    public boolean getNewAclMode() {
        return newAclMode;
    }

    public void setNewAclMode(boolean newAclMode) {
        this.newAclMode = newAclMode;
    }

    public boolean getAutoPromoteEnabled() {
        return autoPromoteEnabled;
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
        return Math.max(autoPromoteTimeout, 0L);
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
                Collections.unmodifiableList(communityControlUsers) : this.communityControlUsers;
    }

    public Long getCommunityControlThreshold() {
        return communityControlThreshold;
    }

    public void setCommunityControlThreshold(Long communityControlThreshold) {
        this.communityControlThreshold = communityControlThreshold != null ?
                communityControlThreshold : 0L;
    }

    public Long getCommunityControlRoleId() {
        return communityControlRoleId;
    }

    public void setCommunityControlRoleId(Long communityControlRoleId) {
        this.communityControlRoleId = communityControlRoleId != null ?
                communityControlRoleId : 0L;
    }

    public Long getCommunityControlCustomEmojiId() {
        return communityControlCustomEmojiId;
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

    public Map<Long, JSONWtfMap> getWtfMapper() {
        return wtfMapper;
    }

    public void setWtfMapper(Map<Long, JSONWtfMap> wtfMapper) {
        this.wtfMapper = wtfMapper != null ? Collections.unmodifiableMap(wtfMapper) : this.wtfMapper;
    }
}
