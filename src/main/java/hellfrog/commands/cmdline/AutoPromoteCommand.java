package hellfrog.commands.cmdline;

import hellfrog.common.CommonUtils;
import hellfrog.core.ServerSideResolver;
import hellfrog.settings.ServerPreferences;
import hellfrog.settings.SettingsController;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;
import java.util.Optional;

public class AutoPromoteCommand
    extends BotCommand {

    private static final String PREFIX = "auto";
    private static final String DESCRIPTION = "Auto promote role settings on all new users join";
    private static final String FOOTER = "Default timeout is 0 seconds after join.";

    private static final Option ROLE_OPTION = Option.builder("r")
            .hasArg()
            .argName("role")
            .desc("Role name, tag or id")
            .build();

    private static final Option TIMEOUT_OPTION = Option.builder("t")
            .hasArg()
            .argName("seconds")
            .desc("Timeout before role will be assign")
            .build();

    private static final Option ENABLE_OPTION = Option.builder("e")
            .desc("Enable auto promote on user join")
            .build();

    private static final Option DISABLE_OPTION = Option.builder("d")
            .desc("Disable auto promote on user join")
            .build();

    private static final Option SHOW_OPTION = Option.builder("s")
            .desc("Show current status info")
            .build();

    public AutoPromoteCommand() {
        super(PREFIX, DESCRIPTION);
        super.setFooter(FOOTER);
        super.addCmdlineOption(ROLE_OPTION, TIMEOUT_OPTION, ENABLE_OPTION, DISABLE_OPTION, SHOW_OPTION);
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

        if (!super.canExecuteServerCommand(event, server)) {
            super.showAccessDeniedServerMessage(event);
            return;
        }

        boolean enableFlag = cmdline.hasOption(ENABLE_OPTION.getOpt());
        boolean disableFlag = cmdline.hasOption(DISABLE_OPTION.getOpt());
        boolean showFlag = cmdline.hasOption(SHOW_OPTION.getOpt());
        String rawRole = cmdline.getOptionValue(ROLE_OPTION.getOpt());
        String rawTimeout = cmdline.getOptionValue(TIMEOUT_OPTION.getOpt());

        if (!enableFlag && !disableFlag && !showFlag
                && CommonUtils.isTrStringEmpty(rawRole)
                && CommonUtils.isTrStringEmpty(rawTimeout)) {

            super.showErrorMessage("One or more options required", event);
            return;
        }

        if ((enableFlag && disableFlag)
            || (enableFlag && showFlag)
            || (disableFlag && showFlag)) {
            super.showErrorMessage("Only one action allowed", event);
            return;
        }

        if (!CommonUtils.isTrStringEmpty(rawTimeout)) {
            if (!CommonUtils.isLong(rawTimeout)) {
                super.showErrorMessage("Timeout must be a number", event);
                return;
            }
            if (CommonUtils.onlyNumbersToLong(rawTimeout) < 0L) {
                super.showErrorMessage("Timeout must be a positive number", event);
                return;
            }
        }

        ServerPreferences preferences = SettingsController.getInstance()
                .getServerPreferences(server.getId());

        MessageBuilder message = new MessageBuilder();

        if (showFlag) {
            message.append("Auto promote role for all new users join ")
                    .append(preferences.getAutoPromoteEnabled() ? "enabled" : "disabled")
                    .appendNewLine();
            if (preferences.getAutoPromoteRoleId() != null &&
                server.getRoleById(preferences.getAutoPromoteRoleId()).isPresent()) {
                Role role = server.getRoleById(preferences.getAutoPromoteRoleId()).orElse(null);
                message.append("Role: ")
                        .append(role != null ? "@" + role.getName() : "@unknown")
                        .appendNewLine();
            }
            message.append("Timeout: ").append(preferences.getAutoPromoteTimeout())
                    .append(" seconds.").appendNewLine();
            super.showInfoMessage(message.getStringBuilder().toString(), event);
            return;
        } else if (enableFlag) {
            if (preferences.getAutoPromoteEnabled() && CommonUtils.isTrStringEmpty(rawRole)
                && CommonUtils.isTrStringEmpty(rawTimeout)) {
                super.showErrorMessage("Auto promote already enabled", event);
                return;
            }

            if (CommonUtils.isTrStringEmpty(rawRole)
                    && (preferences.getAutoPromoteRoleId() == null
                        || server.getRoleById(preferences.getAutoPromoteRoleId()).isEmpty())) {
                super.showErrorMessage("You must specify the server role", event);
                return;
            }

            message.append("Auto promote role for all new users join enabled.")
                    .appendNewLine();

        } else if (disableFlag) {
            if (!preferences.getAutoPromoteEnabled()) {
                super.showErrorMessage("Auto promote already disabled", event);
                return;
            }
            if (!CommonUtils.isTrStringEmpty(rawRole) || !CommonUtils.isTrStringEmpty(rawTimeout)) {
                super.showErrorMessage("Unable to change role or timeout with disabling", event);
                return;
            }

            preferences.setAutoPromoteEnabled(false);
            SettingsController.getInstance().saveServerSideParameters(server.getId());
            super.showInfoMessage("Auto promote role on all new users join disabled", event);
            return;
        } else {
            message.append("Auto promote role settings changed:")
                    .appendNewLine();
        }

        if (!CommonUtils.isTrStringEmpty(rawRole)) {
            Optional<Role> mayBeRole = ServerSideResolver.resolveRole(server, rawRole);
            if (mayBeRole.isEmpty()) {
                super.showErrorMessage("Unable to find role "
                    + rawRole, event);
                return;
            } else {
                Role role = mayBeRole.get();
                message.append("Auto role: ")
                        .append("@").append(role.getName()).appendNewLine();
                preferences.setAutoPromoteRoleId(role.getId());
            }
        }

        if (!CommonUtils.isTrStringEmpty(rawTimeout)) {
            long timeout = CommonUtils.onlyNumbersToLong(rawTimeout);
            message.append("Timeout: ")
                    .append(timeout).append(" seconds.").appendNewLine();
            preferences.setAutoPromoteTimeout(timeout);
        }

        if (enableFlag) {
            preferences.setAutoPromoteEnabled(true);
        }

        SettingsController.getInstance().saveServerSideParameters(server.getId());
        super.showInfoMessage(message.getStringBuilder().toString(), event);
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
        // сюда не доходит
    }
}
