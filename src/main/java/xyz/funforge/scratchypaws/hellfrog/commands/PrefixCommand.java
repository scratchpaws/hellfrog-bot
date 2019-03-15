package xyz.funforge.scratchypaws.hellfrog.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;
import xyz.funforge.scratchypaws.hellfrog.settings.SettingsController;

import java.util.ArrayList;

public class PrefixCommand
        extends BotCommand {

    private static final String PREF = "pref";
    private static final String DESCRIPTION = "Show or change bot call prefix";

    public PrefixCommand() {
        super(PREF, DESCRIPTION);

        Option set = Option.builder("s")
                .desc("Set bot prefix")
                .longOpt("set")
                .build();

        Option get = Option.builder("g")
                .desc("Get bot prefix")
                .longOpt("get")
                .build();

        Option globalSwitcher = Option.builder("l")
                .desc("Global default bot setting (only for global bot owners)")
                .longOpt("global")
                .build();

        addCmdlineOption(set, get, globalSwitcher);
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

        if (cmdline.hasOption('g') && cmdline.hasOption('s')) {
            showErrorMessage("Сannot specify changing and display parameters at the same time.", channel);
            return;
        }

        if (cmdline.hasOption('g')) {
            if (!cmdline.hasOption('l')) {
                // индивидуально для сервреа
                long serverId = server.getId();
                String serverName = server.getName();
                String serverPrefix = settingsController.getBotPrefix(serverId);
                showInfoMessage("Current bot prefix for " + serverName + " server is: " +
                        serverPrefix, channel);
            } else {
                // глобально
                String globalPrefix = settingsController.getGlobalCommonPrefix();
                showInfoMessage("Current bot global prefix is: " +
                        globalPrefix, channel);
            }
        } else if (cmdline.hasOption('s')) {
            if (cmdlineArgs.size() < 1) {
                showErrorMessage("Prefix not set", channel);
            } else {

                String newPrefix = cmdlineArgs.get(0).trim();

                if (!cmdline.hasOption('l')) {
                    // индивидуально для сервера
                    long serverId = server.getId();
                    String serverName = server.getName();
                    if (canExecuteServerCommand(event, server)) {
                        settingsController.setBotPrefix(serverId, newPrefix);
                        showInfoMessage("Prefix changed to " + newPrefix.trim() +
                                " on server " + serverName, channel);
                    } else {
                        showAccessDeniedServerMessage(channel);
                    }
                } else {
                    // глобально
                    if (canExecuteGlobalCommand(event)) {
                        settingsController.setGlobalCommonPrefix(newPrefix);
                        showInfoMessage("Prefix changed to " + newPrefix.trim() +
                                " (globally, by default) ", channel);
                    } else {
                        showAccessDeniedGlobalMessage(channel);
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

        if (cmdline.hasOption('g') && cmdline.hasOption('s')) {
            showErrorMessage("Сannot specify changing and display parameters at the same time.", channel);
            return;
        }

        if (cmdline.hasOption('g')) {
            String globalPrefix = settingsController.getGlobalCommonPrefix();
            showInfoMessage("Current bot global prefix is: " +
                    globalPrefix, channel);
        } else if (cmdline.hasOption('s')) {
            if (cmdlineArgs.size() < 1) {
                showErrorMessage("Prefix not set", channel);
            } else {
                String newPrefix = cmdlineArgs.get(0).trim();
                if (canExecuteGlobalCommand(event)) {
                    settingsController.setGlobalCommonPrefix(newPrefix);
                    showInfoMessage("Prefix changed to " + newPrefix.trim() +
                            " (globally, by default) ", channel);
                } else {
                    showAccessDeniedGlobalMessage(channel);
                }
            }
        }
    }
}
