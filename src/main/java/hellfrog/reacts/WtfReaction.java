package hellfrog.reacts;

import com.vdurmont.emoji.EmojiParser;
import hellfrog.common.CommonConstants;
import hellfrog.common.MessageUtils;
import hellfrog.core.ServerSideResolver;
import hellfrog.settings.SettingsController;
import hellfrog.settings.WtfMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Enumeration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WtfReaction
        extends MsgCreateReaction
        implements CommonConstants {

    private static final String PREFIX = "wtf";
    private static final String DESCRIPTION = "Get (wtf @user) association for user (or wtfall @user for all)";

    private static final Pattern SEARCH_PATTERN = Pattern.compile("^(wtf|втф)\\s+.*",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern CUT_SEARCH_PATTERN = Pattern.compile("^(wtf|втф)\\s+",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern SEARCH_ALL_PATTERN = Pattern.compile("^(wtfall|втфол)\\s+.*",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern CUT_SEARCH_ALL_PATTERN = Pattern.compile("^(wtfall|втфол)\\s+",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Logger log = LogManager.getLogger(WtfReaction.class.getSimpleName());

    public WtfReaction() {
        super.setCommandPrefix(PREFIX);
        super.setCommandDescription(DESCRIPTION);
        super.enableAccessControl();
        super.enableStrictByChannel();
    }

    @Override
    public boolean canReact(MessageCreateEvent event) {
        String message = event.getMessageContent();
        return SEARCH_PATTERN.matcher(message).find()
                || SEARCH_ALL_PATTERN.matcher(message).find();
    }

    @Override
    void parallelExecuteReact(String strMessage,
                              @Nullable Server server,
                              @Nullable User user,
                              TextChannel textChannel,
                              Instant messageCreateDate,
                              Message sourceMessage) {

        if (server == null || user == null) return;

        ConcurrentHashMap<Long, WtfMap> wtfAssign =
                SettingsController.getInstance().getServerPreferences(server.getId()).getWtfMapper();

        if (SEARCH_PATTERN.matcher(strMessage).find()) {
            displayUserValues(strMessage, CUT_SEARCH_PATTERN, server, wtfAssign, textChannel, false);
        } else if (SEARCH_ALL_PATTERN.matcher(strMessage).find()) {
            displayUserValues(strMessage, CUT_SEARCH_ALL_PATTERN, server, wtfAssign, textChannel, true);
        }
    }

    private void displayUserValues(String strMessage, @NotNull Pattern cutterPattern, Server server,
                                   ConcurrentHashMap<Long, WtfMap> wtfAssign,
                                   TextChannel textChannel, boolean all) {

        Matcher searchCut = cutterPattern.matcher(strMessage);
        if (searchCut.find()) {
            String rawNickName = strMessage.substring(searchCut.end());
            Optional<User> mayBeUser = ServerSideResolver.resolveUser(server, rawNickName.trim());
            mayBeUser.ifPresentOrElse(wtfUser -> {
                WtfMap wtfMap = wtfAssign.get(wtfUser.getId());
                if (wtfMap == null || wtfMap.getNameValues().isEmpty()) {
                    new MessageBuilder()
                            .append("I don't know ")
                            .append(EmojiParser.parseToUnicode(":shrug:"))
                            .send(textChannel);
                    return;
                }
                MessageBuilder result = new MessageBuilder();
                if (all) {
                    for (long reporter : wtfMap.getNameValues().keySet()) {
                        appendUserToPrint(server, result, wtfMap, wtfUser, reporter);
                    }
                } else {
                    long last = wtfMap.getLastName().get();
                    if (last == 0L
                            || !wtfMap.getNameValues().containsKey(last)
                            || wtfMap.getNameValues().get(last) == null) {
                        Enumeration<Long> enm = wtfMap.getNameValues().keys();
                        if (enm.hasMoreElements())
                            last = enm.nextElement();
                    }
                    appendUserToPrint(server, result, wtfMap, wtfUser, last);
                }
                MessageUtils.sendLongMessage(result, textChannel);
            }, () -> new MessageBuilder()
                    .append("I can't find this user ")
                    .append(EmojiParser.parseToUnicode(":shrug:"))
                    .send(textChannel));
        }

    }

    private void appendUserToPrint(Server server, MessageBuilder result,
                                   WtfMap wtfMap, User wtfUser, long reporter) {
        try {
            User reporterUser = server.getApi()
                    .getUserById(reporter)
                    .get(OP_WAITING_TIMEOUT, OP_TIME_UNIT);
            String reporterName = server.getMemberById(reporter).map(server::getDisplayName)
                    .orElse(reporterUser.getName());
            String value = ServerSideResolver.resolveMentions(server, wtfMap.getNameValues().get(reporter));
            result.append(MessageUtils.escapeSpecialSymbols(reporterName))
                    .append(" say that ")
                    .append(MessageUtils.escapeSpecialSymbols(server.getDisplayName(wtfUser)))
                    .append(" - ")
                    .append(value)
                    .appendNewLine();
        } catch (Exception err) {
            log.error("Unable to get user by id " + reporter + ": " + err.getMessage(), err);
        }
    }
}
