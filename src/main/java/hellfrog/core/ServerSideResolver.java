package hellfrog.core;

import hellfrog.common.CommonConstants;
import hellfrog.common.CommonUtils;
import hellfrog.settings.SettingsController;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ChannelCategory;
import org.javacord.api.entity.channel.PrivateChannel;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.emoji.CustomEmoji;
import org.javacord.api.entity.emoji.KnownCustomEmoji;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Permissions;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.util.DiscordRegexPattern;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.javacord.api.entity.message.Message.ESCAPED_CHARACTER;

public class ServerSideResolver
        implements CommonConstants {

    private static final Pattern USER_TAG_REGEXP = Pattern.compile("^<@!?[0-9]+>$"); // <@!516251605864153099>
    private static final Pattern USER_TAG_SEARCH = Pattern.compile("<@!?[0-9]+>", Pattern.MULTILINE);
    private static final Pattern USER_LOGIN_WITH_DISCRIMINATE_REGEXP = Pattern.compile("^.+?#(\\d{4})+$"); // ScratchPaws#2898
    private static final Pattern ROLE_TAG_REGEXP = Pattern.compile("^<@&[0-9]+>$"); // <@&525728457225797656>
    private static final Pattern ROLE_TAG_SEARCH = Pattern.compile("<@&[0-9]+>", Pattern.MULTILINE);
    private static final Pattern CHANNEL_TAG_REGEXP = Pattern.compile("^<#[0-9]+>$"); // <#525287388818178050>
    private static final Pattern CHANNEL_TAG_SEARCH = Pattern.compile("<#[0-9]+>", Pattern.MULTILINE);
    private static final Pattern EMOJI_TAG_REGEXP = Pattern.compile("^<a?:.+?:\\d+>$"); // <:swiborg:530385828157980694>
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
            Optional<User> member = server.getMemberByDiscriminatedNameIgnoreCase(rawValue);
            if (member.isPresent())
                return member;
        }
        // 4. Затем просто по name
        Optional<User> member = CommonUtils.getFirstOrEmpty(server.getMembersByNameIgnoreCase(rawValue));
        if (member.isPresent()) {
            return member;
        }
        // 5. И наконец по нику
        member = CommonUtils.getFirstOrEmpty(server.getMembersByDisplayNameIgnoreCase(rawValue));
        if (member.isPresent()) {
            return member;
        }
        // 6. Пытаемся отыскать по глобальному id
        try {
            User found = server.getApi()
                    .getUserById(CommonUtils.onlyNumbersToLong(rawValue))
                    .get(OP_WAITING_TIMEOUT, OP_TIME_UNIT);
            return Optional.ofNullable(found);
        } catch (Exception err) {
            return Optional.empty();
        }
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
        return CommonUtils.getFirstOrEmpty(server.getRolesByNameIgnoreCase(rawValue));
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
        return CommonUtils.getFirstOrEmpty(server.getTextChannelsByNameIgnoreCase(rawValue));
    }

    public static Optional<ChannelCategory> resolveCategory(Server server, String rawValue) {
        // 1. Вначале ищем по явному ID канала
        if (CommonUtils.isLong(rawValue)) {
            long unresolvedCategoryId = CommonUtils.onlyNumbersToLong(rawValue);
            Optional<ChannelCategory> channelCategory = server.getChannelCategoryById(unresolvedCategoryId);
            if (channelCategory.isPresent())
                return channelCategory;
        }
        // 2. Далее ищем по имени категории
        return CommonUtils.getFirstOrEmpty(server.getChannelCategoriesByNameIgnoreCase(rawValue));
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

    @NotNull
    public static ParseResult<ServerTextChannel> resolveTextChannelsList(Server server,
                                                                         @NotNull List<String> rawTextChannelList) {
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

    @NotNull
    public static ParseResult<ChannelCategory> resolveChannelCategoriesList(Server server,
                                                                            @NotNull List<String> rawTextCategoriesList) {
        ParseResult<ChannelCategory> result = new ParseResult<>();
        List<ChannelCategory> resolvedCategories = new ArrayList<>(rawTextCategoriesList.size());
        List<String> unresolvedCategories = new ArrayList<>(rawTextCategoriesList.size());
        rawTextCategoriesList.forEach(rawCategory -> {
            Optional<ChannelCategory> mayBeCategory = ServerSideResolver.resolveCategory(server, rawCategory);
            if (mayBeCategory.isPresent()) {
                resolvedCategories.add(mayBeCategory.get());
            } else {
                unresolvedCategories.add(rawCategory);
            }
        });
        result.setFound(resolvedCategories);
        result.setNotFound(unresolvedCategories);
        return result;
    }

    @NotNull
    public static ParseResult<ServerChannel> resolveTextChannelsAndCategoriesList(Server server,
                                                                                  @NotNull List<String> rawChannelsList) {
        ParseResult<ServerChannel> result = new ParseResult<>();
        List<ServerChannel> resolvedChannels = new ArrayList<>(rawChannelsList.size());
        List<String> unresolvedChannels = new ArrayList<>(rawChannelsList.size());
        rawChannelsList.forEach((rawChannel) -> {
            Optional<ServerTextChannel> mayBeTextChannel = ServerSideResolver.resolveChannel(server, rawChannel);
            if (mayBeTextChannel.isPresent()) {
                resolvedChannels.add(mayBeTextChannel.get());
            } else {
                Optional<ChannelCategory> mayBeCategory = ServerSideResolver.resolveCategory(server, rawChannel);
                if (mayBeCategory.isPresent()) {
                    resolvedChannels.add(mayBeCategory.get());
                } else {
                    unresolvedChannels.add(rawChannel);
                }
            }
        });
        result.setFound(resolvedChannels);
        result.setNotFound(unresolvedChannels);
        return result;
    }

    public static ParseResult<KnownCustomEmoji> resolveKnownEmojiList(Server server,
                                                                      @NotNull List<String> rawEmojiList) {
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

    public static @NotNull String resolveMentions(Server server, String message) {
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
        return resolveEveryones(message);
    }

    public static @NotNull String resolveEveryones(@Nullable String message) {
        if (CommonUtils.isTrStringEmpty(message)) {
            return "";
        }
        return message.replace("@everyone", "`@everyone`")
                .replace("@here", "`@here`");
    }

    /**
     * Method, works same as {@link Message#getReadableContent()} (contain same code).
     *
     * @param messageContent message text
     * @param mayBeServer    optional server object
     * @return text with replaced mentions
     */
    public static String getReadableContent(@Nullable String messageContent,
                                            Optional<Server> mayBeServer) {
        DiscordApi api = SettingsController.getInstance().getDiscordApi();
        if (CommonUtils.isTrStringEmpty(messageContent)) {
            return "";
        }
        Matcher userMention = DiscordRegexPattern.USER_MENTION.matcher(messageContent);
        while (userMention.find()) {
            String userId = userMention.group("id");
            Optional<User> userOptional = api.getCachedUserById(userId);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                String userName = mayBeServer.map(user::getDisplayName).orElseGet(user::getName);
                messageContent = userMention.replaceFirst("@" + userName);
                userMention.reset(messageContent);
            }
        }
        Matcher roleMention = DiscordRegexPattern.ROLE_MENTION.matcher(messageContent);
        while (roleMention.find()) {
            String roleId = roleMention.group("id");
            String roleName = mayBeServer
                    .flatMap(server -> server
                            .getRoleById(roleId)
                            .map(Role::getName))
                    .orElse("deleted-role");
            messageContent = roleMention.replaceFirst("@" + roleName);
            roleMention.reset(messageContent);
        }
        Matcher channelMention = DiscordRegexPattern.CHANNEL_MENTION.matcher(messageContent);
        while (channelMention.find()) {
            String channelId = channelMention.group("id");
            String channelName = mayBeServer
                    .flatMap(server -> server
                            .getTextChannelById(channelId)
                            .map(ServerChannel::getName))
                    .orElse("deleted-channel");
            messageContent = channelMention.replaceFirst("#" + channelName);
            channelMention.reset(messageContent);
        }
        Matcher customEmoji = DiscordRegexPattern.CUSTOM_EMOJI.matcher(messageContent);
        while (customEmoji.find()) {
            String emojiId = customEmoji.group("id");
            String name = api
                    .getCustomEmojiById(emojiId)
                    .map(CustomEmoji::getName)
                    .orElseGet(() -> customEmoji.group("name"));
            messageContent = customEmoji.replaceFirst(":" + name + ":");
            customEmoji.reset(messageContent);
        }
        return resolveEveryones(ESCAPED_CHARACTER.matcher(messageContent).replaceAll("${char}"));
    }

    @NotNull
    public static Optional<String> getAllowedGrants(@NotNull Server server, User user) {
        return getAllowedGrants(server.getPermissions(user));
    }

    @NotNull
    public static Optional<String> getDeniedGrants(@NotNull Server server, User user) {
        return getDeniedGrants(server.getPermissions(user));
    }

    @NotNull
    public static Optional<String> getAllowedGrants(@NotNull Role role) {
        return getAllowedGrants(role.getPermissions());
    }

    @NotNull
    public static Optional<String> getDeniedGrants(@NotNull Role role) {
        return getDeniedGrants(role.getPermissions());
    }

    @NotNull
    public static Optional<String> getAllowedGrants(@NotNull Permissions permissions) {
        return enumeratePermissionTypes(permissions.getAllowedPermission());
    }

    @NotNull
    public static Optional<String> getDeniedGrants(@NotNull Permissions permissions) {
        return enumeratePermissionTypes(permissions.getDeniedPermissions());
    }

    @NotNull
    public static Optional<String> enumeratePermissionTypes(@NotNull Collection<PermissionType> types) {
        return types.stream()
                .map(PermissionType::name)
                .sorted()
                .map(n -> n.toLowerCase().replace("_", " "))
                .reduce(CommonUtils::reduceConcat);
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

    @NotNull
    public static String getFullUserDescriptionByEvent(@NotNull MessageCreateEvent event) {
        Optional<User> mayBeUser = event.getMessageAuthor().asUser();
        Optional<Server> mayBeServer = event.getServer();
        Optional<ServerTextChannel> mayBeTextChannel = event.getServerTextChannel();
        Optional<PrivateChannel> mayBePrivate = event.getPrivateChannel();
        String messageContent = "User: " + mayBeUser.map(user -> mayBeServer.map(server -> server.getDisplayName(user)).orElse(user.getName())
                + " (" + user.getDiscriminatedName() + ", id: " + user.getId() + ")")
                .orElse("(UNKNOWN USER)")
                + mayBeServer.map(server -> ", server: " + server.getName() + " (id: " + server.getId() + ")")
                .orElse("")
                + mayBeTextChannel.map(channel -> ", text channel: " + channel.getName() + " (id: " + channel.getId() + ")")
                .orElse("")
                + mayBePrivate.map(channel -> " (private message)").orElse("");
        return ESCAPED_CHARACTER.matcher(messageContent).replaceAll("${char}");
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
                        .reduce(CommonUtils::reduceConcat);
                if (result.isPresent())
                    return result.get();
            }

            return "";
        }
    }
}
