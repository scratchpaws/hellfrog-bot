package hellfrog.settings.oldjson;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class JSONCommandRights {

    private String commandPrefix = "";
    private List<Long> allowUsers = Collections.emptyList();
    private List<Long> allowRoles = Collections.emptyList();
    private List<Long> allowChannels = Collections.emptyList();

    public String getCommandPrefix() {
        return commandPrefix;
    }

    public void setCommandPrefix(String commandPrefix) {
        this.commandPrefix = commandPrefix;
    }

    public List<Long> getAllowUsers() {
        return allowUsers;
    }

    public void setAllowUsers(List<Long> allowUsers) {
        this.allowUsers = allowUsers != null ? Collections.unmodifiableList(allowUsers) : this.allowUsers;
    }

    public List<Long> getAllowRoles() {
        return allowRoles;
    }

    public void setAllowRoles(List<Long> allowRoles) {
        this.allowRoles = allowRoles != null ? Collections.unmodifiableList(allowRoles) : this.allowRoles;
    }

    public List<Long> getAllowChannels() {
        return allowChannels;
    }

    public void setAllowChannels(List<Long> allowChannels) {
        this.allowChannels = allowChannels != null ? Collections.unmodifiableList(allowChannels) : this.allowChannels;
    }

    public boolean isAllowUser(long userId) {
        return userId > 0L && allowUsers.contains(userId);
    }

    public boolean isAllowRole(long roleId) {
        return roleId > 0L && allowRoles.contains(roleId);
    }

    public boolean isAllowChat(long channelId) {
        return channelId > 0L && allowChannels.contains(channelId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JSONCommandRights that = (JSONCommandRights) o;
        return Objects.equals(commandPrefix, that.commandPrefix) &&
                Objects.equals(allowUsers, that.allowUsers) &&
                Objects.equals(allowRoles, that.allowRoles) &&
                Objects.equals(allowChannels, that.allowChannels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commandPrefix, allowUsers, allowRoles, allowChannels);
    }
}
