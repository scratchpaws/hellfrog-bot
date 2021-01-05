package hellfrog.settings.oldjson;

import java.util.Collections;
import java.util.Map;

public class JSONServerStatistic {

    // hellfrog.settings.db.ServerPreferencesDAO.isStatisticEnabled
    private boolean collectNonDefaultSmileStats = false;
    // hellfrog.settings.db.entity.EmojiTotalStatistic
    private Map<Long, JSONSmileStatistic> nonDefaultSmileStats = Collections.emptyMap();
    private Map<Long, JSONMessageStatistic> userMessagesStats = Collections.emptyMap();
    private Map<Long, JSONMessageStatistic> textChatStats = Collections.emptyMap();
    // hellfrog.settings.db.ServerPreferencesDAO.getStatisticStartDate
    private long startDate = 0L;

    public Long getStartDate() {
        return startDate;
    }

    public void setStartDate(Long startDate) {
        this.startDate = startDate != null ? startDate : 0L;
    }

    public boolean isCollectNonDefaultSmileStats() {
        return collectNonDefaultSmileStats;
    }

    public void setCollectNonDefaultSmileStats(boolean collectNonDefaultSmileStats) {
        this.collectNonDefaultSmileStats = collectNonDefaultSmileStats;
    }

    public Map<Long, JSONSmileStatistic> getNonDefaultSmileStats() {
        return nonDefaultSmileStats;
    }

    public void setNonDefaultSmileStats(Map<Long, JSONSmileStatistic> nonDefaultSmileStats) {
        this.nonDefaultSmileStats = nonDefaultSmileStats != null
                ? Collections.unmodifiableMap(nonDefaultSmileStats) : this.nonDefaultSmileStats;
    }

    public Map<Long, JSONMessageStatistic> getUserMessagesStats() {
        return userMessagesStats;
    }

    public void setUserMessagesStats(Map<Long, JSONMessageStatistic> userMessagesStats) {
        this.userMessagesStats = userMessagesStats != null
                ? Collections.unmodifiableMap(userMessagesStats) : this.userMessagesStats;
    }

    public Map<Long, JSONMessageStatistic> getTextChatStats() {
        return textChatStats;
    }

    public void setTextChatStats(Map<Long, JSONMessageStatistic> textChatStats) {
        this.textChatStats = textChatStats != null ? Collections.unmodifiableMap(textChatStats) : this.textChatStats;
    }
}
