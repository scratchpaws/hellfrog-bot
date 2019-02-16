package pub.funforge.scratchypaws.rilcobot.settings.old;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class CommandRights {

    private String commandPrefix = "";
    private CopyOnWriteArrayList<Long> allowUsers = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<Long> allowRoles = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<Long> allowChannels = new CopyOnWriteArrayList<>();

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
        this.allowUsers = new CopyOnWriteArrayList<>(allowUsers);
    }

    public List<Long> getAllowRoles() {
        return allowRoles;
    }

    public void setAllowRoles(List<Long> allowRoles) {
        this.allowRoles = new CopyOnWriteArrayList<>(allowRoles);
    }

    public List<Long> getAllowChannels() {
        return allowChannels;
    }

    public void setAllowChannels(List<Long> allowChannels) {
        this.allowChannels = new CopyOnWriteArrayList<>(allowChannels);
    }

    public boolean addAllowUser(long userId) {
        if (userId <= 0 || allowUsers.contains(userId)) return false;
        allowUsers.add(userId);
        return true;
    }

    public boolean addAllowRole(long roleId) {
        if (roleId <= 0 || allowRoles.contains(roleId)) return false;
        allowRoles.add(roleId);
        return true;
    }

    public boolean isAllowUser(long userId) {
        return userId > 0 && allowUsers.contains(userId);
    }

    public boolean isAllowRole(long roleId) {
        return roleId > 0 && allowRoles.contains(roleId);
    }

    public boolean delAllowUser(long userId) {
        if (userId <= 0 || !allowUsers.contains(userId)) return false;
        allowUsers.remove(userId);
        return true;
    }

    public boolean delAllowRole(long roleId) {
        if (roleId <= 0 || !allowRoles.contains(roleId)) return false;
        allowRoles.remove(roleId);
        return true;
    }

    public boolean addAllowChannel(long channelId) {
        if (channelId <= 0 || allowChannels.contains(channelId)) return false;
        allowChannels.add(channelId);
        return true;
    }

    public boolean delAllowChannel(long channelId) {
        if (channelId <= 0 || !allowChannels.contains(channelId)) return false;
        allowChannels.remove(channelId);
        return true;
    }

    public boolean isAllowChat(long channelId) {
        return channelId > 0 && allowChannels.contains(channelId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommandRights that = (CommandRights) o;
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
