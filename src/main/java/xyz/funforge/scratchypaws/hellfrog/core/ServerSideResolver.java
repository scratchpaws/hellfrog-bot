package xyz.funforge.scratchypaws.hellfrog.core;

import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.emoji.KnownCustomEmoji;
import org.javacord.api.entity.permission.PermissionState;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Permissions;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import xyz.funforge.scratchypaws.hellfrog.common.CommonUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerSideResolver {

    private static final Pattern USER_TAG_REGEXP = Pattern.compile("^<@!?[0-9]+>$"); // <@!516251605864153099>
    private static final Pattern USER_TAG_SEARCH = Pattern.compile("<@!?[0-9]+>", Pattern.MULTILINE);
    private static final Pattern USER_LOGIN_WITH_DISCRIMINATE_REGEXP = Pattern.compile("^.+?#(\\d{4})+$"); // ScratchPaws#2898
    private static final Pattern ROLE_TAG_REGEXP = Pattern.compile("^<@&[0-9]+>$"); // <@&525728457225797656>
    private static final Pattern ROLE_TAG_SEARCH = Pattern.compile("<@&[0-9]+>", Pattern.MULTILINE);
    private static final Pattern CHANNEL_TAG_REGEXP = Pattern.compile("^<#[0-9]+>$"); // <#525287388818178050>
    private static final Pattern CHANNEL_TAG_SEARCH = Pattern.compile("<#[0-9]+>", Pattern.MULTILINE);
    private static final Pattern EMOJI_TAG_REGEXP = Pattern.compile("^<a?:.+?:\\d+>$"); // <:swiborg:530385828157980694>
    private static final Pattern CUSTOM_EMOJI_SHORT = Pattern.compile("^:[A-z_0-9]+:$");
    private static final Pattern CUSTOM_COMBO_EMOJI_SEARCH = Pattern.compile("(<a?:.+?:\\d+>|:[A-z_0-9]+:)", Pattern.MULTILINE);

    public static Optional<User> resolveUser(Server server, String rawValue) {
        // 1. вначале ищем по явному id
        if (CommonUtils.isLong(rawValue)) {
            long unresolvedUserId = Long.parseLong(rawValue);
            Optional<User> member = server.getMemberById(unresolvedUserId);
            if (member.isPresent()) {
                return member;
            }
        }
        // 2. далее ищем по явным упоминаниям-тегированиям
        Matcher matcher = USER_TAG_REGEXP.matcher(rawValue);
        if (matcher.find()) {
            long unresolvedUserId = CommonUtils.onlyNumbersToLong(rawValue);
            Optional<User> member = server.getMemberById(unresolvedUserId);
            if (member.isPresent())
                return member;
        }
        // 3. Затем по discriminated name
        matcher = USER_LOGIN_WITH_DISCRIMINATE_REGEXP.matcher(rawValue);
        if (matcher.find()) {
            Optional<User> member = server.getMemberByDiscriminatedName(rawValue);
            if (member.isPresent())
                return member;
        }
        // 4. Затем просто по name
        Optional<User> member = CommonUtils.getFirstOrEmpty(server.getMembersByName(rawValue));
        if (member.isPresent()) {
            return member;
        }
        // 5. И наконец по нику
        return CommonUtils.getFirstOrEmpty(server.getMembersByDisplayName(rawValue));
    }

    public static Optional<Role> resolveRole(Server server, String rawValue) {
        // 1. Вначале ищем по явному id роли
        if (CommonUtils.isLong(rawValue)) {
            long unresolvedRole = Long.parseLong(rawValue);
            Optional<Role> role = server.getRoleById(unresolvedRole);
            if (role.isPresent())
                return role;
        }
        // 2. Далее ищем по явным указаниям-теггированиям
        Matcher matcher = ROLE_TAG_REGEXP.matcher(rawValue);
        if (matcher.find()) {
            long unresolvedRole = CommonUtils.onlyNumbersToLong(rawValue);
            Optional<Role> role = server.getRoleById(unresolvedRole);
            if (role.isPresent())
                return role;
        }
        // 3. И наконец ищем по имени роли
        return CommonUtils.getFirstOrEmpty(server.getRolesByName(rawValue));
    }

    public static Optional<ServerTextChannel> resolveChannel(Server server, String rawValue) {
        // 1. Вначале ищем по явному id канала
        if (CommonUtils.isLong(rawValue)) {
            long unresolvedChannel = CommonUtils.onlyNumbersToLong(rawValue);
            Optional<ServerTextChannel> textChannel = server.getTextChannelById(unresolvedChannel);
            if (textChannel.isPresent())
                return textChannel;
        }
        // 2. Далее ищем по упоминанию канала
        Matcher matcher = CHANNEL_TAG_REGEXP.matcher(rawValue);
        if (matcher.find()) {
            long unresolvedChannel = CommonUtils.onlyNumbersToLong(rawValue);
            Optional<ServerTextChannel> textChannel = server.getTextChannelById(unresolvedChannel);
            if (textChannel.isPresent())
                return textChannel;
        }
        // 4. И наконец просто по имени канала
        return CommonUtils.getFirstOrEmpty(server.getTextChannelsByName(rawValue));
    }

    public static Optional<KnownCustomEmoji> resolveCustomEmoji(Server server, String rawText) {
        // 1. Вначале ищем кастомные
        Matcher matcher = EMOJI_TAG_REGEXP.matcher(rawText);
        if (matcher.find()) {
            String clearedEmojiRawId = getCustomEmojiRawId(rawText);
            long unresolvedEmojiId = CommonUtils.onlyNumbersToLong(clearedEmojiRawId);
            Optional<KnownCustomEmoji> mayBeEmoji = server.getCustomEmojiById(unresolvedEmojiId);
            if (mayBeEmoji.isPresent()) {
                return mayBeEmoji;
            }
        }

        return Optional.empty();
    }

    @Contract(pure = true)
    public static String getCustomEmojiRawId(@NotNull String rawEmoji) {
        String[] split = rawEmoji.split(":");
        if (split.length == 3) {
            return split[2];
        }
        return rawEmoji;
    }

    public static <T> ParseResult<T> emptyResult() {
        ParseResult<T> result = new ParseResult<>();
        result.setFound(new ArrayList<>(0));
        result.setNotFound(new ArrayList<>(0));
        return result;
    }

    public static ParseResult<User> resolveUsersList(Server server, @NotNull List<String> rawUserList) {
        ParseResult<User> result = new ParseResult<>();
        List<User> resolvedUsers = new ArrayList<>(rawUserList.size());
        List<String> unresolvedUsers = new ArrayList<>(rawUserList.size());
        rawUserList.forEach((rawUser) -> {
            Optional<User> mayBeUser = resolveUser(server, rawUser);
            if (mayBeUser.isPresent()) {
                resolvedUsers.add(mayBeUser.get());
            } else {
                unresolvedUsers.add(rawUser);
            }
        });
        result.setFound(resolvedUsers);
        result.setNotFound(unresolvedUsers);
        return result;
    }

    public static ParseResult<Role> resolveRolesList(Server server, @NotNull List<String> rawRolesList) {
        ParseResult<Role> result = new ParseResult<>();
        List<Role> resolvedRoles = new ArrayList<>(rawRolesList.size());
        List<String> unresolvedRoles = new ArrayList<>(rawRolesList.size());
        rawRolesList.forEach((rawRole) -> {
            Optional<Role> mayBeRole = ServerSideResolver.resolveRole(server, rawRole);
            if (mayBeRole.isPresent()) {
                resolvedRoles.add(mayBeRole.get());
            } else {
                unresolvedRoles.add(rawRole);
            }
        });
        result.setFound(resolvedRoles);
        result.setNotFound(unresolvedRoles);
        return result;
    }

    public static ParseResult<ServerTextChannel> resolveTextChannelsList(Server server, @NotNull List<String> rawTextChannelList) {
        ParseResult<ServerTextChannel> result = new ParseResult<>();
        List<ServerTextChannel> resolvedChannels = new ArrayList<>(rawTextChannelList.size());
        List<String> unresolvedChannels = new ArrayList<>(rawTextChannelList.size());
        rawTextChannelList.forEach((rawChannel) -> {
            Optional<ServerTextChannel> mayBeChannel = ServerSideResolver.resolveChannel(server, rawChannel);
            if (mayBeChannel.isPresent()) {
                resolvedChannels.add(mayBeChannel.get());
            } else {
                unresolvedChannels.add(rawChannel);
            }
        });
        result.setFound(resolvedChannels);
        result.setNotFound(unresolvedChannels);
        return result;
    }

    public static ParseResult<KnownCustomEmoji> resolveKnownEmojiList(Server server, @NotNull List<String> rawEmojiList) {
        ParseResult<KnownCustomEmoji> result = new ParseResult<>();
        List<KnownCustomEmoji> resolvedEmoji = new ArrayList<>(rawEmojiList.size());
        List<String> unresolvedEmoji = new ArrayList<>(rawEmojiList.size());
        rawEmojiList.forEach((rawEmoji) -> {
            Optional<KnownCustomEmoji> mayBeEmoji = ServerSideResolver.resolveCustomEmoji(server, rawEmoji);
            if (mayBeEmoji.isPresent()) {
                resolvedEmoji.add(mayBeEmoji.get());
            } else {
                unresolvedEmoji.add(rawEmoji);
            }
        });
        result.setFound(resolvedEmoji);
        result.setNotFound(unresolvedEmoji);
        return result;
    }

    public static String resolveMentions(Server server, String message) {
        Matcher userMentionMatcher = USER_TAG_SEARCH.matcher(message);
        while (userMentionMatcher.find()) {
            String userMention = userMentionMatcher.group();
            Optional<User> resolvedUser = resolveUser(server, userMention);
            if (resolvedUser.isPresent()) {
                message = message.replace(userMention, "@" + server.getDisplayName(resolvedUser.get()));
            }
        }
        Matcher roleMentionMatcher = ROLE_TAG_SEARCH.matcher(message);
        while (roleMentionMatcher.find()) {
            String roleMention = roleMentionMatcher.group();
            Optional<Role> resulvedRole = resolveRole(server, roleMention);
            if (resulvedRole.isPresent()) {
                message = message.replace(roleMention, "@" + resulvedRole.get().getName());
            }
        }
        Matcher textChannelMentionMatcher = CHANNEL_TAG_SEARCH.matcher(message);
        while (textChannelMentionMatcher.find()) {
            String textChannelMention = textChannelMentionMatcher.group();
            Optional<ServerTextChannel> serverTextChannel = resolveChannel(server, textChannelMention);
            if (serverTextChannel.isPresent()) {
                message = message.replace(textChannelMention, "#" + serverTextChannel.get().getName());
            }
        }
        return message;
    }

    public static String getGrants(Permissions permissions) {
        Optional<String> value = Arrays.stream(PermissionType.values())
                .filter(t -> !permissions.getState(t).equals(PermissionState.UNSET))
                .map(t -> t + " - " + permissions.getState(t))
                .reduce((s1, s2) -> s1 + ", " + s2);
        return value.orElse("");
    }

    @NotNull
    public static String findReplaceSimpleEmoji(String rawMessage, Server server) {
        StringBuilder result = new StringBuilder(rawMessage);
        Matcher finder = CUSTOM_COMBO_EMOJI_SEARCH.matcher(rawMessage);
        while (finder.find()) {
            String found = finder.group();
            if (EMOJI_TAG_REGEXP.matcher(found).find()) continue;
            server.getCustomEmojis().stream()
                    .filter(n -> (":" + n.getName() + ":").equalsIgnoreCase(found))
                    .findFirst().ifPresent(kke -> {
                        int beginIndex = result.indexOf(found);
                        int endIndex = beginIndex + found.length();
                        result.replace(beginIndex, endIndex, kke.getMentionTag());
                    }
            );
        }
        return result.toString();
    }

    public static class ParseResult<T> {

        private List<T> found;
        private List<String> notFound;

        public List<T> getFound() {
            return found;
        }

        void setFound(List<T> found) {
            this.found = found;
        }

        public List<String> getNotFound() {
            return notFound;
        }

        void setNotFound(List<String> notFound) {
            this.notFound = notFound;
        }

        public boolean hasFound() {
            return found != null && found.size() > 0;
        }

        public boolean hasNotFound() {
            return notFound != null && notFound.size() > 0;
        }

        public String getNotFoundStringList() {
            if (hasNotFound()) {
                Optional<String> result = notFound.stream()
                        .reduce((r1, r2) -> r1 + ", " + r2);
                if (result.isPresent())
                    return result.get();
            }

            return "";
        }
    }
}
