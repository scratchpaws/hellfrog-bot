package hellfrog.commands;

import com.vdurmont.emoji.EmojiParser;
import hellfrog.common.CommonUtils;
import hellfrog.common.MessageUtils;
import hellfrog.core.ServerSideResolver;
import hellfrog.settings.ServerPreferences;
import hellfrog.settings.SettingsController;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.emoji.KnownCustomEmoji;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageDecoration;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CornControlCommand
        extends BotCommand {

    private static final String PREFIX = "corn";
    private static final String DESCRIPTION = "Community users control";

    private static final Option ADD_OPTION = Option.builder("a")
            .longOpt("add")
            .hasArgs()
            .argName("user")
            .desc("Add user for community control")
            .build();

    private static final Option DEL_OPTION = Option.builder("d")
            .longOpt("delete")
            .hasArgs()
            .argName("user")
            .desc("Remove user from community control")
            .build();

    private static final Option ROLE_OPTION = Option.builder("r")
            .longOpt("role")
            .hasArg()
            .argName("role")
            .desc("Role that obtained by exceeding the number of reactions")
            .build();

    private static final Option THRESHOLD_OPTION = Option.builder("t")
            .longOpt("threshold")
            .hasArg()
            .argName("number of reacts")
            .desc("The threshold at which the role is assigned")
            .build();

    private static final Option EMOJI_OPTION = Option.builder("e")
            .longOpt("emoji")
            .hasArg()
            .argName("emoji of reacts")
            .desc("Emoji for which the role is assigned")
            .build();

    private static final Option SHOW_OPTION = Option.builder("s")
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

        ServerPreferences serverPreferences =
                SettingsController.getInstance().getServerPreferences(server.getId());

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

        MessageBuilder resultMessage = new MessageBuilder();

        if (!showMode) {
            usersToAdd.stream()
                    .filter(u -> serverPreferences.getCommunityControlUsers().add(u.getId()))
                    .map(server::getDisplayName)
                    .reduce((s1, s2) -> s1 + ", " + s2)
                    .ifPresent(added -> resultMessage.append("Added users to community control: ")
                            .append(added)
                            .appendNewLine());

            usersToDelete.stream()
                    .filter(u -> serverPreferences.getCommunityControlUsers().remove(u.getId()))
                    .map(server::getDisplayName)
                    .reduce((s1, s2) -> s1 + ", " + s2)
                    .ifPresent(added -> resultMessage.append("Removed users to community control: ")
                            .append(added)
                            .appendNewLine());

            if (thresholdChange && threshold > 0L) {
                resultMessage.append("Reactions threshold changed to ")
                        .append(threshold)
                        .append(" counts.")
                        .appendNewLine();
                serverPreferences.setCommunityControlThreshold(threshold);
            }

            role.ifPresent(r -> {
                serverPreferences.setCommunityControlRoleId(r.getId());
                resultMessage.append("Set role: ")
                        .append("@").append(r.getName())
                        .appendNewLine();
            });

            customEmoji.ifPresent(ke -> {
                serverPreferences.setCommunityControlCustomEmojiId(ke.getId());
                serverPreferences.setCommunityControlEmoji(null);
                resultMessage.append("Set emoji: ")
                        .append(ke)
                        .appendNewLine();
            });

            stringEmoji.ifPresent(e -> {
                serverPreferences.setCommunityControlCustomEmojiId(0L);
                serverPreferences.setCommunityControlEmoji(e);
                resultMessage.append("Set emoji: ")
                        .append(e)
                        .appendNewLine();
            });

            SettingsController.getInstance()
                    .saveServerSideParameters(server.getId());
        }

        addShowData(server, serverPreferences, resultMessage);
        MessageUtils.sendLongMessage(resultMessage, channel);
    }

    private void addShowData(Server server, ServerPreferences serverPreferences, MessageBuilder resultMessage) {
        List<String> communityControlUsers = serverPreferences.getCommunityControlUsers().stream()
                .map(server::getMemberById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(u -> !u.isBot())
                .map(server::getDisplayName)
                .collect(Collectors.toUnmodifiableList());
        Optional<String> controlUsersAsString = communityControlUsers.stream()
                .reduce((s1, s2) -> s1 + ", " + s2);
        Optional<Role> controlRole = server.getRoleById(serverPreferences.getCommunityControlRoleId());
        Optional<KnownCustomEmoji> customEmoji = server.getCustomEmojiById(
                serverPreferences.getCommunityControlCustomEmojiId());
        Optional<String> stringEmoji = Optional.ofNullable(serverPreferences.getCommunityControlEmoji());
        boolean active = serverPreferences.getCommunityControlThreshold() > 0L
                && serverPreferences.getCommunityControlThreshold() <= communityControlUsers.size()
                && (customEmoji.isPresent() || stringEmoji.isPresent())
                && controlRole.isPresent();
        resultMessage.append("Community control status:", MessageDecoration.BOLD)
                .append(" ")
                .append(active ? "enabled" : "disabled", MessageDecoration.CODE_SIMPLE)
                .appendNewLine();
        controlUsersAsString.ifPresent(users ->
                resultMessage.append("Users list: ")
                        .append(users)
                        .appendNewLine());
        if (serverPreferences.getCommunityControlThreshold() > 0L) {
            resultMessage.append("Reactions threshold: ")
                    .append(serverPreferences.getCommunityControlThreshold())
                    .append(" counts.")
                    .appendNewLine();
        }
        controlRole.ifPresent(r -> resultMessage.append("Role: ")
                .append("@").append(r.getName())
                .appendNewLine());
        customEmoji.ifPresent(e -> resultMessage.append("Emoji: ")
                .append(e).appendNewLine());
        stringEmoji.ifPresent(e -> resultMessage.append("Emoji: ")
                .append(e).appendNewLine());
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
