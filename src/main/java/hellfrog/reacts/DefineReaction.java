package hellfrog.reacts;

import com.vdurmont.emoji.EmojiParser;
import hellfrog.common.CommonConstants;
import hellfrog.common.CommonUtils;
import hellfrog.core.ServerSideResolver;
import hellfrog.settings.SettingsController;
import hellfrog.settings.WtfMap;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Enumeration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefineReaction
        extends MsgCreateReaction
        implements CommonConstants {

    private static final String PREFIX = "dfn";
    private static final String DESCRIPTION = "Set (dfn @user = msg) association for user";

    private final ReentrantLock createDefinitionLock = new ReentrantLock();

    private static final Pattern DEFINE_PATTERN = Pattern.compile("^(dfn|дфн|def|деф)\\s+.*=.*",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern CUT_DEFINE_PATTERN = Pattern.compile("^(dfn|дфн|def|деф)\\s+",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public DefineReaction() {
        super.setCommandPrefix(PREFIX);
        super.setCommandDescription(DESCRIPTION);
        super.enableAccessControl();
        super.enableStrictByChannel();
    }

    @Override
    public boolean canReact(MessageCreateEvent event) {
        String message = event.getMessageContent();
        return DEFINE_PATTERN.matcher(message).find();
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

        if (DEFINE_PATTERN.matcher(strMessage).find()) {
            Matcher searchCut = CUT_DEFINE_PATTERN.matcher(strMessage);
            if (searchCut.find()) {
                String defineSubstring = strMessage.substring(searchCut.end());
                String[] nameAndDefine = defineSubstring.split("=", 2);
                if (nameAndDefine.length >= 1 && nameAndDefine.length <= 2) {
                    Optional<User> mayBeUser = ServerSideResolver.resolveUser(server, nameAndDefine[0].trim());
                    mayBeUser.ifPresentOrElse(dfnUser -> {
                        String define = nameAndDefine.length == 2
                                ? ServerSideResolver.resolveMentions(server, nameAndDefine[1].strip())
                                : "";
                        if (!wtfAssign.containsKey(dfnUser.getId())
                                || wtfAssign.get(dfnUser.getId()) == null) {
                            createDefinitionLock.lock();
                            try {
                                if (!wtfAssign.containsKey(dfnUser.getId())
                                        || wtfAssign.get(dfnUser.getId()) == null) {
                                    WtfMap wtfMap = new WtfMap();
                                    wtfAssign.put(dfnUser.getId(), wtfMap);
                                }
                            } finally {
                                createDefinitionLock.unlock();
                            }
                        }
                        WtfMap wtfMap = wtfAssign.get(dfnUser.getId());
                        if (!CommonUtils.isTrStringEmpty(define)) {
                            wtfMap.getNameValues().put(user.getId(), define);
                            wtfMap.getLastName().set(user.getId());
                            SettingsController.getInstance().saveServerSideParameters(server.getId());
                            new MessageBuilder()
                                    .append("Ok, saved ")
                                    .append(EmojiParser.parseToUnicode(":ok_hand:"))
                                    .send(textChannel);
                        } else {
                            String removed = wtfMap.getNameValues().remove(user.getId());
                            if (removed != null) {
                                Enumeration<Long> enm = wtfMap.getNameValues().keys();
                                if (enm.hasMoreElements()) {
                                    wtfMap.getLastName().set(enm.nextElement());
                                } else {
                                    wtfMap.getLastName().set(0L);
                                }
                                SettingsController.getInstance().saveServerSideParameters(server.getId());
                                new MessageBuilder()
                                        .append("Ok, removed ")
                                        .append(EmojiParser.parseToUnicode(":ok_hand:"))
                                        .send(textChannel);
                            } else {
                                new MessageBuilder()
                                        .append("But already is empty ")
                                        .append(EmojiParser.parseToUnicode(":ok_hand:"))
                                        .send(textChannel);
                            }
                        }
                    }, () -> new MessageBuilder()
                            .append("I can't find this user ")
                            .append(EmojiParser.parseToUnicode(":shrug:"))
                            .send(textChannel));
                }
            }
        }
    }
}
