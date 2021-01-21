package hellfrog.commands.cmdline;

import hellfrog.settings.SettingsController;
import hellfrog.settings.db.CommonPreferencesDAO;
import hellfrog.settings.db.ServerPreferencesDAO;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

public class PrefixCommand
        extends BotCommand {

    private static final String PREF = "pref";
    private static final String DESCRIPTION = "Show or change bot call prefix";
    private static final String FOOTER = "This is an expert command. To invoke an interactive user-friendly " +
            "command, use \"prefix\" command.";

    private final Option setOption = Option.builder("s")
            .desc("Set bot prefix")
            .longOpt("set")
            .build();

    private final Option getOption = Option.builder("g")
            .desc("Get bot prefix")
            .longOpt("get")
            .build();

    private final Option globalSwitcherOption = Option.builder("l")
            .desc("Global default bot setting (only for global bot owners)")
            .longOpt("global")
            .build();

    public PrefixCommand() {
        super(PREF, DESCRIPTION);
        addCmdlineOption(setOption, getOption, globalSwitcherOption);
        super.setAdminCommand();
        super.setCommandAsExpert();
        super.setFooter(FOOTER);
    }

    /**
     * Обработка команды, поступившей из текстового канала сервера.
     *
     * @param server      Сервер текстового канала, откуда поступило сообщение
     * @param cmdline     обработанные аргументы командной строки
     * @param cmdlineArgs оставшиеся значения командной строки
     * @param channel     текстовый канал, откуда поступило сообщение
     * @param event       событие сообщения
     */
    @Override
    protected void executeCreateMessageEventServer(Server server,
                                                   CommandLine cmdline, ArrayList<String> cmdlineArgs,
                                                   TextChannel channel, MessageCreateEvent event,
                                                   ArrayList<String> anotherLines) {

        SettingsController settingsController = SettingsController.getInstance();
        CommonPreferencesDAO commonPreferencesDAO = settingsController.getMainDBController().getCommonPreferencesDAO();
        ServerPreferencesDAO serverPreferencesDAO = settingsController.getMainDBController().getServerPreferencesDAO();

        if (cmdline.hasOption(getOption.getOpt()) && cmdline.hasOption(setOption.getOpt())) {
            showErrorMessage("Сannot specify changing and display parameters at the same time.", event);
            return;
        }

        if (cmdline.hasOption(getOption.getOpt())) {
            if (!cmdline.hasOption(globalSwitcherOption.getOpt())) {
                // индивидуально для сервреа
                long serverId = server.getId();
                String serverName = server.getName();
                String serverPrefix = serverPreferencesDAO.getPrefix(serverId);
                showInfoMessage("Current bot prefix for " + serverName + " server is: " +
                        serverPrefix, event);
            } else {
                // глобально
                String globalPrefix = commonPreferencesDAO.getBotPrefix();
                showInfoMessage("Current bot global prefix is: " +
                        globalPrefix, event);
            }
        } else if (cmdline.hasOption(setOption.getOpt())) {
            if (cmdlineArgs.size() < 1) {
                showErrorMessage("Prefix not set", event);
            } else {

                String newPrefix = cmdlineArgs.get(0).trim();

                if (!cmdline.hasOption(globalSwitcherOption.getOpt())) {
                    // индивидуально для сервера
                    long serverId = server.getId();
                    String serverName = server.getName();
                    if (canExecuteServerCommand(event, server)) {
                        serverPreferencesDAO.setPrefix(serverId, newPrefix);
                        showInfoMessage("Prefix changed to " + newPrefix.trim() +
                                " on server " + serverName, event);
                        if (server.canYouChangeOwnNickname()) {
                            User botUser = server.getApi().getYourself();
                            server.updateNickname(botUser, commonPreferencesDAO.getBotName() + " ("
                                    + newPrefix + " help)");
                        }
                    } else {
                        showAccessDeniedServerMessage(event);
                    }
                } else {
                    // глобально
                    if (canExecuteGlobalCommand(event)) {
                        commonPreferencesDAO.setBotPrefix(newPrefix);
                        showInfoMessage("Prefix changed to " + newPrefix.trim() +
                                " (globally, by default) ", event);
                    } else {
                        showAccessDeniedGlobalMessage(event);
                    }
                }
            }
        }
    }

    /**
     * Обработка команды, поступившей из привата.
     *
     * @param cmdline     обработанные аргументы командной строки
     * @param cmdlineArgs оставшиеся значения командной строки
     * @param channel     текстовый канал, откуда поступило сообщение
     * @param event       событие сообщения
     */
    @Override
    protected void executeCreateMessageEventDirect(CommandLine cmdline, ArrayList<String> cmdlineArgs,
                                                   TextChannel channel, MessageCreateEvent event,
                                                   ArrayList<String> anotherLines) {

        SettingsController settingsController = SettingsController.getInstance();
        CommonPreferencesDAO commonPreferencesDAO = settingsController.getMainDBController().getCommonPreferencesDAO();

        if (cmdline.hasOption(getOption.getOpt()) && cmdline.hasOption(setOption.getOpt())) {
            showErrorMessage("Сannot specify changing and display parameters at the same time.", event);
            return;
        }

        if (cmdline.hasOption(getOption.getOpt())) {
            String globalPrefix = commonPreferencesDAO.getBotPrefix();
            showInfoMessage("Current bot global prefix is: " +
                    globalPrefix, event);
        } else if (cmdline.hasOption(setOption.getOpt())) {
            if (cmdlineArgs.size() < 1) {
                showErrorMessage("Prefix not set", event);
            } else {
                String newPrefix = cmdlineArgs.get(0).trim();
                if (canExecuteGlobalCommand(event)) {
                    commonPreferencesDAO.setBotPrefix(newPrefix);
                    showInfoMessage("Prefix changed to " + newPrefix.trim() +
                            " (globally, by default) ", event);
                } else {
                    showAccessDeniedGlobalMessage(event);
                }
            }
        }
    }
}
