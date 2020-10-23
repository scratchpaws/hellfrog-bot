package hellfrog.reacts;

import hellfrog.settings.SettingsController;
import hellfrog.settings.VotePoint;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.reaction.ReactionAddEvent;

import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

/**
 * Выполняет фильтрацию реакций в пунктах голосования
 */
public class VoteReactFilter {

    public void parseAction(ReactionAddEvent event) {
        Optional<User> mayBeUser = event.getUser();
        if (mayBeUser.isEmpty()) return;
        final User user = mayBeUser.get();
        if (user.isYourself()) return;
        Optional<Server> mayBeSrv = event.getServer();
        if (mayBeSrv.isEmpty()) return;

        Server srv = mayBeSrv.get();
        Emoji emoji = event.getEmoji();

        SettingsController.getInstance()
                .getServerPreferences(srv.getId())
                .getActiveVotes()
                .stream()
                .filter(v -> event.getMessageId() == v.getMessageId())
                .forEach(v -> {

                    // удаляем все попытки поставить "левые" реакции действующим голосованиям
                    boolean invalidReaction = v.getVotePoints()
                            .stream().noneMatch(vp ->
                                    vp.equalsEmoji(emoji)
                            );
                    boolean hasDefault = (v.isWithDefaultPoint() ||
                            v.getWinThreshold() > 0)
                            && v.getVotePoints().size() > 0;
                    if (hasDefault) {
                        VotePoint defPoint = v.getVotePoints().get(0);
                        invalidReaction = invalidReaction || defPoint.equalsEmoji(emoji);
                    }
                    if (invalidReaction) {
                        event.removeReaction();
                        return;
                    }

                    if (v.getRolesFilter() != null && !v.getRolesFilter().isEmpty()) {
                        boolean nonVoteRole = srv.getRoles(user)
                                .stream()
                                .noneMatch(r -> v.getRolesFilter()
                                        .contains(r.getId()));
                        if (nonVoteRole) {
                            event.removeReaction();
                            return;
                        }
                    }

                    // убираем попытку навесить иной пункт уже голосовавшего
                    if (v.isExceptionalVote()) {
                        Message msg = null;

                        Optional<ServerTextChannel> tch = srv.getTextChannelById(v.getTextChatId());
                        if (tch.isPresent()) {
                            try {
                                msg = srv.getApi()
                                        .getMessageById(v.getMessageId(), tch.get())
                                        .get(10L, TimeUnit.SECONDS);
                            } catch (Exception ignore) {
                                // сообщения больше не существует
                            }
                        }
                        if (msg == null) return; // голосовалки уже нет

                        boolean hasAnotherVP = msg.getReactions()
                                .stream()
                                .filter(r ->
                                        !r.getEmoji().getMentionTag().equals(emoji.getMentionTag()))
                                .anyMatch(r -> {
                                    try {
                                        return r.getUsers()
                                                .join()
                                                .contains(user);
                                    } catch (CompletionException ignore) {
                                        // что-то пошло не так, discord
                                        // не отдал юзеров реакции
                                        return false;
                                    }
                                });
                        if (hasAnotherVP) {
                            event.removeReaction();
                        }
                    }
                });
    }
}
