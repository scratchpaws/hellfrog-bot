package hellfrog.commands.cmdline;

import hellfrog.common.CommonUtils;
import hellfrog.core.ServerSideResolver;
import hellfrog.core.AutoPromoteService;
import hellfrog.settings.SettingsController;
import hellfrog.settings.db.entity.AutoPromoteConfig;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AutoPromoteCommand
        extends BotCommand {

    private static final String PREFIX = "auto";
    private static final String DESCRIPTION = "Auto promote role settings on all new users join";
    private static final String FOOTER = """
            This command configures the automatic role assignment when new members log on to the server.
            You can add or remove a role assignment configuration.
            You can either add or remove a configuration at one time. 
            There can be only one configuration for a role at a time.
            When adding or removing a configuration, be sure to specify the name or role identifier.
            The timeout parameter is optional, by default it is 0 seconds.
            i.e. the role is assigned as soon as the member logs on to the server.
            No timeout is required to remove the configuration.
            Keep in mind that the timeout should preferably be a multiple of 5 seconds
            (the queue of assigned roles for new members is checked every 5 seconds).
            Re-adding a configuration with an existing role will change the current role assignment timeout.
            Roles deleted on the server will be removed from the configurations automatically.""";

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

    private static final Option SHOW_OPTION = Option.builder("s")
            .longOpt("show")
            .desc("Show current status info")
            .build();

    private static final Option ADD_OPTION = Option.builder("a")
            .desc("Add (or update) auto assign role configuration")
            .longOpt("add")
            .build();

    private static final Option DEL_OPTION = Option.builder("d")
            .desc("Delete auto assign role configuration")
            .longOpt("del")
            .build();

    public AutoPromoteCommand() {
        super(PREFIX, DESCRIPTION);
        super.setFooter(FOOTER);
        super.addCmdlineOption(ROLE_OPTION, TIMEOUT_OPTION, SHOW_OPTION, ADD_OPTION, DEL_OPTION);
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

        boolean addFlag = cmdline.hasOption(ADD_OPTION.getOpt());
        boolean delFlag = cmdline.hasOption(DEL_OPTION.getOpt());
        boolean showFlag = cmdline.hasOption(SHOW_OPTION.getOpt());

        String rawRole = cmdline.getOptionValue(ROLE_OPTION.getOpt());
        String rawTimeout = cmdline.getOptionValue(TIMEOUT_OPTION.getOpt());

        boolean hasRole = cmdline.hasOption(ROLE_OPTION.getOpt()) && CommonUtils.isTrStringNotEmpty(rawRole);
        boolean hasTimeout = cmdline.hasOption(TIMEOUT_OPTION.getOpt()) && CommonUtils.isTrStringNotEmpty(rawTimeout);

        if (!addFlag && !delFlag && !showFlag) {
            super.showErrorMessage("You need to specify some action parameter (add/del/show). See help (-h) for details.", event);
            return;
        }

        if (addFlag ^ delFlag ^ showFlag) {
            AutoPromoteService autoPromoteService = SettingsController.getInstance().getAutoPromoteService();
            List<AutoPromoteConfig> configList = autoPromoteService.getWorkingConfigurations(server);

            if (showFlag) {
                showAction(configList, event, server);
                return;
            }

            if (!hasRole) {
                super.showErrorMessage("To add or remove a configuration, you need to specify a role. " +
                        "See help (-h) for details.", event);
                return;
            }

            long timeout = 0L;
            if (hasTimeout) {
                if (!CommonUtils.isLong(rawTimeout)) {
                    super.showErrorMessage("Timeout must be a number", event);
                    return;
                }
                timeout = CommonUtils.onlyNumbersToLong(rawTimeout);
                if (timeout < 0L) {
                    super.showErrorMessage("Timeout must be a positive number", event);
                    return;
                }
            }

            Role role = ServerSideResolver.resolveRole(server, rawRole).orElse(null);
            if (role == null) {
                String errorMessage = "Unable to find role: " + rawRole
                        + (delFlag ? " Roles deleted on the server will be removed from the configurations automatically." : "");
                super.showErrorMessage(errorMessage, event);
                return;
            }

            if (addFlag) {
                addUpdateAction(autoPromoteService, configList, event, server, role, timeout);
            }

            if (delFlag) {
                removeAction(autoPromoteService, configList, event, server, role);
            }
        } else {
            super.showErrorMessage("Only one action allowed", event);
        }
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

    private void showAction(@NotNull final List<AutoPromoteConfig> configList,
                            @NotNull final MessageCreateEvent event,
                            @NotNull final Server server) {

        StringBuilder result = new StringBuilder();
        result.append("Auto promote roles for all new users join is ")
                .append(configList.isEmpty() ? "**disabled**" : "**enabled**");

        if (!configList.isEmpty()) {
            result.append("\nConfigurations:\n");
            int count = 0;
            for (AutoPromoteConfig config : configList) {
                Optional<Role> mayBeRole = server.getRoleById(config.getRoleId());
                if (mayBeRole.isPresent()) {
                    count++;
                    result.append(count).append(". ").append(mayBeRole.get().getMentionTag())
                            .append(", ");
                    if (config.getTimeout() > 0L) {
                        result.append("assign timeout: ").append(config.getTimeout()).append(" sec.");
                    } else {
                        result.append("assign immediately.");
                    }
                    result.append('\n');
                }
            }
        }

        List<String> messages = CommonUtils.splitEqually(result.toString(), 2000);
        for (String text : messages) {
            showInfoMessage(text, event);
        }
    }

    private void addUpdateAction(@NotNull final AutoPromoteService autoPromoteService,
                                 @NotNull final List<AutoPromoteConfig> configList,
                                 @NotNull final MessageCreateEvent event,
                                 @NotNull final Server server,
                                 @NotNull final Role role,
                                 final long timeout) {

        StringBuilder message = new StringBuilder();
        boolean modify = configList.stream()
                .anyMatch(config -> config.getRoleId() == role.getId());
        if (modify) {
            message.append("Modified ");
        } else {
            message.append("Added ");
        }
        message.append("auto promote role configuration:\n")
                .append(role.getMentionTag()).append(", ");

        if (timeout > 0L) {
            message.append("assign timeout: ").append(timeout).append(" sec.");
        } else {
            message.append("assign immediately.");
        }

        autoPromoteService.addUpdateConfiguration(server, role, timeout);

        super.showInfoMessage(message.toString(), event);
    }

    private void removeAction(@NotNull final AutoPromoteService autoPromoteService,
                              @NotNull final List<AutoPromoteConfig> configList,
                              @NotNull final MessageCreateEvent event,
                              @NotNull final Server server,
                              @NotNull final Role role) {

        configList.stream()
                .filter(config -> config.getRoleId() == role.getId())
                .findFirst()
                .ifPresentOrElse(config -> {
                    StringBuilder message = new StringBuilder("Removed configuration:\n")
                            .append(role.getMentionTag()).append(", ");
                    if (config.getTimeout() > 0L) {
                        message.append("assign timeout: ").append(config.getTimeout()).append(" sec.");
                    } else {
                        message.append("assign immediately.");
                    }
                    autoPromoteService.removeConfiguration(server, role);

                    super.showInfoMessage(message.toString(), event);
                }, () -> super.showErrorMessage("No changes: no configuration found for role " + role.getMentionTag(), event));
    }
}
