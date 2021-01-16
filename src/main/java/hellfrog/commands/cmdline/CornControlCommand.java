package hellfrog.commands.cmdline;

import com.vdurmont.emoji.EmojiParser;
import hellfrog.common.CommonUtils;
import hellfrog.common.LongEmbedMessage;
import hellfrog.core.ServerSideResolver;
import hellfrog.settings.SettingsController;
import hellfrog.settings.db.CommunityControlDAO;
import hellfrog.settings.db.entity.CommunityControlSettings;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.emoji.KnownCustomEmoji;
import org.javacord.api.entity.message.MessageDecoration;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CornControlCommand
        extends BotCommand {

    private static final String PREFIX = "corn";
    private static final String DESCRIPTION = "Community users control";

    private final Option ADD_OPTION = Option.builder("a")
            .longOpt("add")
            .hasArgs()
            .argName("user")
            .desc("Add user for community control")
            .build();

    private final Option DEL_OPTION = Option.builder("d")
            .longOpt("delete")
            .hasArgs()
            .argName("user")
            .desc("Remove user from community control")
            .build();

    private final Option ROLE_OPTION = Option.builder("r")
            .longOpt("role")
            .hasArg()
            .argName("role")
            .desc("Role that obtained by exceeding the number of reactions")
            .build();

    private final Option THRESHOLD_OPTION = Option.builder("t")
            .longOpt("threshold")
            .hasArg()
            .argName("number of reacts")
            .desc("The threshold at which the role is assigned")
            .build();

    private final Option EMOJI_OPTION = Option.builder("e")
            .longOpt("emoji")
            .hasArg()
            .argName("emoji of reacts")
            .desc("Emoji for which the role is assigned")
            .build();

    private final Option SHOW_OPTION = Option.builder("s")
            .desc("Show current settings")
            .build();

    public CornControlCommand() {
        super(PREFIX, DESCRIPTION);

        super.addCmdlineOption(ADD_OPTION, DEL_OPTION,
                ROLE_OPTION, THRESHOLD_OPTION, EMOJI_OPTION,
                SHOW_OPTION);
        super.enableOnlyServerCommandStrict();
    }

    /**
     * Обработка команды, поступившей из текстового канала сервера.
     *
     * @param server       Сервер текстового канала, откуда поступило сообщение
     * @param cmdline      обработанные аргументы командной строки
     * @param cmdlineArgs  оставшиеся значения командной строки
     * @param channel      текстовый канал, откуда поступило сообщение
     * @param event        событие сообщения
     * @param anotherLines другие строки в команде, не относящиеся к команде
     */
    @Override
    protected void executeCreateMessageEventServer(Server server,
                                                   CommandLine cmdline,
                                                   ArrayList<String> cmdlineArgs,
                                                   TextChannel channel,
                                                   MessageCreateEvent event,
                                                   ArrayList<String> anotherLines) {

        if (!canExecuteServerCommand(event, server)) {
            showAccessDeniedServerMessage(event);
            return;
        }

        boolean addAction = cmdline.hasOption(ADD_OPTION.getOpt());
        List<String> rawUsersToAdd = getOptionalArgsList(cmdline, ADD_OPTION.getOpt().charAt(0));
        boolean deleteAction = cmdline.hasOption(DEL_OPTION.getOpt());
        List<String> rawUsersToDelete = getOptionalArgsList(cmdline, DEL_OPTION.getOpt().charAt(0));

        boolean setRole = cmdline.hasOption(ROLE_OPTION.getOpt());
        String rawRole = cmdline.getOptionValue(ROLE_OPTION.getOpt());

        boolean thresholdChange = cmdline.hasOption(THRESHOLD_OPTION.getOpt());
        String rawThresholdValue = cmdline.getOptionValue(THRESHOLD_OPTION.getOpt());

        boolean emojiChange = cmdline.hasOption(EMOJI_OPTION.getOpt());
        String rawEmoji = cmdline.getOptionValue(EMOJI_OPTION.getOpt());

        boolean showMode = cmdline.hasOption(SHOW_OPTION.getOpt());
        if (showMode && (addAction || deleteAction || setRole || thresholdChange || emojiChange)) {
            showErrorMessage("Unable to use show and charge options at same time", event);
            return;
        }

        List<User> usersToAdd = Collections.emptyList();
        if (addAction && !rawUsersToAdd.isEmpty()) {
            ServerSideResolver.ParseResult<User> resolved =
                    ServerSideResolver.resolveUsersList(server, rawUsersToAdd);
            if (resolved.hasNotFound()) {
                showErrorMessage("Unable to find users "
                        + resolved.getNotFoundStringList()
                        + " for addition", event);
                return;
            }
            usersToAdd = resolved.getFound();
        }

        if (usersToAdd.stream().anyMatch(User::isBot)) {
            showErrorMessage("Unable add bots to community control users list", event);
            return;
        }

        List<User> usersToDelete = Collections.emptyList();
        if (deleteAction && !rawUsersToDelete.isEmpty()) {
            ServerSideResolver.ParseResult<User> resolved =
                    ServerSideResolver.resolveUsersList(server, rawUsersToDelete);
            if (resolved.hasNotFound()) {
                showErrorMessage("Unable to find users "
                        + resolved.getNotFoundStringList()
                        + " for deleting", event);
                return;
            }
            usersToDelete = resolved.getFound();
        }

        Optional<Role> role = !CommonUtils.isTrStringEmpty(rawRole)
                ? ServerSideResolver.resolveRole(server, rawRole)
                : Optional.empty();
        if (setRole && !CommonUtils.isTrStringEmpty(rawRole) && role.isEmpty()) {
            showErrorMessage("Role " + rawRole + " not found", event);
            return;
        }

        long threshold = 0L;
        if (thresholdChange && !CommonUtils.isTrStringEmpty(rawThresholdValue)) {
            if (!CommonUtils.isLong(rawThresholdValue)) {
                showErrorMessage("Threshold value must be a number", event);
                return;
            }

            threshold = CommonUtils.onlyNumbersToLong(rawThresholdValue);
            if (threshold <= 0) {
                showErrorMessage("Threshold value must be a positive number great that zero", event);
                return;
            }
        }

        Optional<KnownCustomEmoji> customEmoji = !CommonUtils.isTrStringEmpty(rawEmoji)
                ? ServerSideResolver.resolveCustomEmoji(server, rawEmoji)
                : Optional.empty();
        Optional<String> stringEmoji = !CommonUtils.isTrStringEmpty(rawEmoji)
                ? EmojiParser.extractEmojis(rawEmoji).stream().findFirst() : Optional.empty();
        if (emojiChange && !CommonUtils.isTrStringEmpty(rawEmoji)
                && customEmoji.isEmpty() && stringEmoji.isEmpty()) {
            showErrorMessage("Unable to find or use emoji " + rawEmoji, event);
            return;
        }

        Optional<String> dualUse = usersToAdd.stream()
                .filter(usersToDelete::contains)
                .map(User::getDiscriminatedName)
                .reduce((s1, s2) -> s1 + ", " + s2);
        if (dualUse.isPresent()) {
            showErrorMessage("Unable some time add and remove users: " + dualUse.get(), event);
            return;
        }

        final LongEmbedMessage message = LongEmbedMessage.withTitleInfoStyle("Community control");
        final CommunityControlDAO controlDAO = SettingsController.getInstance()
                .getMainDBController()
                .getCommunityControlDAO();

        if (!showMode) {

            usersToAdd.stream()
                    .filter(u -> controlDAO.addUser(server.getId(), u.getId()))
                    .map(User::getMentionTag)
                    .reduce((s1, s2) -> s1 + ", " + s2)
                    .ifPresent(added -> message.append("Added users to community control: ")
                            .append(added)
                            .appendNewLine());

            usersToDelete.stream()
                    .filter(u -> controlDAO.removeUser(server.getId(), u.getId()))
                    .map(User::getMentionTag)
                    .reduce((s1, s2) -> s1 + ", " + s2)
                    .ifPresent(added -> message.append("Removed users to community control: ")
                            .append(added)
                            .appendNewLine());

            CommunityControlSettings controlSettings = controlDAO.getSettings(server.getId())
                    .orElse(new CommunityControlSettings());
            controlSettings.setServerId(server.getId());

            if (thresholdChange && threshold > 0L) {
                message.append("Reactions threshold changed to ")
                        .append(threshold)
                        .append(" counts.")
                        .appendNewLine();
                controlSettings.setThreshold(threshold);
            }

            role.ifPresent(r -> {
                controlSettings.setRoleId(r.getId());
                message.append("Set role: ")
                        .append(r)
                        .append('\n');
            });

            customEmoji.ifPresent(ke -> {
                controlSettings.setCustomEmojiId(ke.getId());
                controlSettings.setUnicodeEmoji(null);
                message.append("Set emoji: ")
                        .append(ke)
                        .appendNewLine();
            });

            stringEmoji.ifPresent(e -> {
                controlSettings.setUnicodeEmoji(e);
                controlSettings.setCustomEmojiId(0L);
                message.append("Set emoji: ")
                        .append(e)
                        .appendNewLine();
            });

            controlDAO.setSettings(controlSettings);
        }

        addShowData(server, message);

        super.showMessage(message, event);
    }

    private void addShowData(final Server server, final LongEmbedMessage message) {

        final CommunityControlDAO controlDAO = SettingsController.getInstance()
                .getMainDBController()
                .getCommunityControlDAO();
        final CommunityControlSettings settings = controlDAO.getSettings(server.getId()).orElse(new CommunityControlSettings());
        List<String> communityControlUsers = controlDAO.getUsers(server.getId()).stream()
                .map(server::getMemberById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(User::getMentionTag)
                .collect(Collectors.toUnmodifiableList());
        Optional<String> controlUsersAsString = communityControlUsers.stream()
                .reduce((s1, s2) -> s1 + ", " + s2);
        Optional<Role> controlRole = server.getRoleById(settings.getRoleId());
        Optional<KnownCustomEmoji> customEmoji = server.getCustomEmojiById(settings.getCustomEmojiId());
        Optional<String> stringEmoji = Optional.ofNullable(settings.getUnicodeEmoji());
        boolean active = settings.getThreshold() > 0L
                && settings.getThreshold() < communityControlUsers.size()
                && (customEmoji.isPresent() || stringEmoji.isPresent())
                && controlRole.isPresent();
        message.append("Community control status:", MessageDecoration.BOLD)
                .append(" ")
                .append(active ? "enabled" : "disabled", MessageDecoration.CODE_SIMPLE)
                .appendNewLine();
        controlUsersAsString.ifPresent(users ->
                message.append("Users list: ")
                        .append(users)
                        .appendNewLine());
        if (settings.getThreshold() > 0L) {
            message.append("Reactions threshold: ")
                    .append(settings.getThreshold())
                    .append(" counts.")
                    .appendNewLine();
        }
        controlRole.ifPresent(r -> message.append("Role: ")
                .append(r)
                .appendNewLine());
        customEmoji.ifPresent(e -> message.append("Emoji: ")
                .append(e)
                .appendNewLine());
        stringEmoji.ifPresent(e -> message.append("Emoji: ")
                .append(e)
                .appendNewLine());
    }

    /**
     * Обработка команды, поступившей из привата.
     *
     * @param cmdline      обработанные аргументы командной строки
     * @param cmdlineArgs  оставшиеся значения командной строки
     * @param channel      текстовый канал, откуда поступило сообщение
     * @param event        событие сообщения
     * @param anotherLines другие строки в команде, не относящиеся к команде
     */
    @Override
    protected void executeCreateMessageEventDirect(CommandLine cmdline,
                                                   ArrayList<String> cmdlineArgs,
                                                   TextChannel channel,
                                                   MessageCreateEvent event,
                                                   ArrayList<String> anotherLines) {

    }
}
