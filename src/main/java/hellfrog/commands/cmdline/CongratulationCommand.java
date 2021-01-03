package hellfrog.commands.cmdline;

import hellfrog.common.CommonUtils;
import hellfrog.core.ServerSideResolver;
import hellfrog.settings.ServerPreferences;
import hellfrog.settings.SettingsController;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;

public class CongratulationCommand
        extends BotCommand {

    private static final String PREFIX = "cong";
    private static final String DESCRIPTION = "Set channel for congratulations";
    private static final String FOOTER = "Specifies the channel where users can leave greetings. " +
            "The left message is automatically deleted from the channel and saved in the bot's memory. " +
            "At the end of the day, the bot will randomly extract these congratulations and send them " +
            "to the channel where users previously left congratulations. It is possible to " +
            "specify the time zone, the default is CET.";

    private final Option channelOption = Option.builder("c")
            .hasArg()
            .optionalArg(true)
            .argName("Channel")
            .longOpt("channel")
            .desc("Set text channel for enable congratulations. Empty to disable the accumulation of messages and disable congratulations.")
            .build();

    private final Option timezone = Option.builder("t")
            .hasArg()
            .argName("Integer")
            .longOpt("timezone")
            .desc("The number corresponding to the time zone is indicated. Positive for time zone plus GMT. " +
                    "Negative for the time zone minus Greenwich Mean Time. For example, \"3\" will set GMT + 3. " +
                    "Empty reset timezone to CET (default timezone).")
            .build();

    private final Option status = Option.builder("s")
            .longOpt("status")
            .desc("Displays the current settings.")
            .build();

    public CongratulationCommand() {
        super(PREFIX, DESCRIPTION);
        super.setFooter(FOOTER);

        super.enableOnlyServerCommandStrict();
        super.addCmdlineOption(channelOption, timezone, status);
    }

    @Override
    protected void executeCreateMessageEventServer(Server server,
                                                   CommandLine cmdline,
                                                   ArrayList<String> cmdlineArgs,
                                                   TextChannel channel,
                                                   MessageCreateEvent event,
                                                   ArrayList<String> anotherLines) {

        final SettingsController settingsController = SettingsController.getInstance();
        final ServerPreferences serverPreferences = settingsController.getServerPreferences(server.getId());
        final DiscordApi api = settingsController.getDiscordApi();

        if (cmdline.hasOption(status.getOpt())) {
            displayStatus(serverPreferences, server, api, channel);
        } else {

            boolean setupChannel = false;
            ServerTextChannel congratulationChannel = null;

            boolean setupTimezone = false;
            TimeZone timeZone = null;

            if (cmdline.hasOption(channelOption.getOpt())) {
                String rawChannel = cmdline.getOptionValue(channelOption.getOpt());
                if (CommonUtils.isTrStringNotEmpty(rawChannel)) {
                    Optional<ServerTextChannel> mayBeChannel = ServerSideResolver.resolveChannel(server, rawChannel);
                    if (mayBeChannel.isEmpty()) {
                        showErrorMessage("Unable to find text channel "
                                + ServerSideResolver.getReadableContent(rawChannel, Optional.of(server)), event);
                        return;
                    }
                    congratulationChannel = mayBeChannel.get();
                    if (rawChannel.equalsIgnoreCase("null")) {
                        congratulationChannel = null;
                    }
                }
                setupChannel = true;
            }

            if (cmdline.hasOption(timezone.getOpt())) {
                String rawTimeZone = cmdline.getOptionValue(timezone.getOpt());
                if (CommonUtils.isTrStringNotEmpty(rawTimeZone)) {
                    long timeZoneId = CommonUtils.onlyNumbersToLong(rawTimeZone);
                    String timezoneNumber = "GMT" + (timeZoneId >= 0 ? "+" + timeZoneId : String.valueOf(timeZoneId));
                    try {
                        timeZone = TimeZone.getTimeZone(timezoneNumber);
                    } catch (Exception err) {
                        showErrorMessage("Unable to found timezone " + timezoneNumber, event);
                        return;
                    }
                    if (rawTimeZone.equalsIgnoreCase("null")) {
                        timeZone = null;
                    }
                }
                setupTimezone = true;
            }

            if (!setupChannel && !setupTimezone) {
                showErrorMessage("Command parameters were not specified. See help.", event);
                return;
            }

            if (setupChannel) {
                long channelId = congratulationChannel != null ? congratulationChannel.getId() : 0L;
                serverPreferences.setCongratulationChannel(channelId);
            }

            if (setupTimezone) {
                String zoneId = timeZone != null ? timeZone.getID() : "";
                serverPreferences.setTimezone(zoneId);
            }

            settingsController.saveServerSideParameters(server.getId());

            displayStatus(serverPreferences, server, api, channel);
        }
    }

    private void displayStatus(@NotNull final ServerPreferences serverPreferences,
                               @NotNull final Server server,
                               @NotNull final DiscordApi api,
                               @NotNull final TextChannel channel) {

        String congratulationStatus = "Congratulations disabled.";
        Long congratulationChannelId = serverPreferences.getCongratulationChannel();
        if (congratulationChannelId != null && congratulationChannelId > 0L) {
            congratulationStatus = server.getTextChannelById(congratulationChannelId)
                    .map(ch -> "Congratulations enabled on channel " + ch.getMentionTag() + ".")
                    .orElse("Congratulations disabled.");
        }
        String timezoneStatus = "Timezone: CET";
        String timezoneId = serverPreferences.getTimezone();
        if (CommonUtils.isTrStringNotEmpty(timezoneId)) {
            try {
                TimeZone timeZone = TimeZone.getTimeZone(timezoneId);
                timezoneStatus = "Timezone: " + timeZone.getDisplayName(Locale.ENGLISH);
            } catch (Exception ignore) {
            }
        }
        String resultMessage = congratulationStatus + '\n' + timezoneStatus;
        new MessageBuilder()
                .setEmbed(new EmbedBuilder()
                        .setColor(Color.CYAN)
                        .setAuthor(api.getYourself())
                        .setTimestampToNow()
                        .setTitle("Congratulation settings")
                        .setDescription(resultMessage))
                .send(channel);
    }

    @Override
    protected void executeCreateMessageEventDirect(CommandLine cmdline,
                                                   ArrayList<String> cmdlineArgs,
                                                   TextChannel channel,
                                                   MessageCreateEvent event,
                                                   ArrayList<String> anotherLines) {

        showErrorMessage("The command can only be executed on the server", event);
    }
}
