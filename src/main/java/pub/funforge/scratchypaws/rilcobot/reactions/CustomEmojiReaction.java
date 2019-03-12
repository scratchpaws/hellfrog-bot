package pub.funforge.scratchypaws.rilcobot.reactions;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.jetbrains.annotations.Nullable;
import pub.funforge.scratchypaws.rilcobot.common.CommonUtils;
import pub.funforge.scratchypaws.rilcobot.settings.SettingsController;
import pub.funforge.scratchypaws.rilcobot.settings.old.ServerStatistic;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomEmojiReaction
        extends MsgCreateReaction {

    private static final Pattern CUSTOM_EMOJI_SEARCH = Pattern.compile("<a?:.+?:\\d+>", Pattern.MULTILINE);

    @Override
    public boolean canReact(MessageCreateEvent event) {
        String messageString = event.getMessageContent();
        return CUSTOM_EMOJI_SEARCH.matcher(messageString).find();
    }

    @Override
    void parallelExecuteReact(String strMessage, @Nullable Server server, @Nullable User user, TextChannel textChannel, DiscordApi api) {

        if (server == null) return;
        long serverId = server.getId();
        if (user != null && (user.isYourself() || user.isBot())) return;

        ServerStatistic serverStatistic = SettingsController.getInstance()
                .getServerStatistic(serverId);
        if (serverStatistic.isCollectNonDefaultSmileStats()) {
            Matcher smileMatcher = CUSTOM_EMOJI_SEARCH.matcher(strMessage);
            while (smileMatcher.find()) {
                String matched = smileMatcher.group();
                String[] sub = matched.split(":");
                if (sub.length == 3) {
                    long customSmileId = CommonUtils.onlyNumbersToLong(sub[2]);
                    server.getCustomEmojiById(customSmileId)
                            .ifPresent(e -> serverStatistic.getSmileStatistic(customSmileId)
                                    .increment()
                            );
                }
            }
        }
    }
}
