package xyz.funforge.scratchypaws.hellfrog.reactions;

import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import xyz.funforge.scratchypaws.hellfrog.settings.SettingsController;
import xyz.funforge.scratchypaws.hellfrog.settings.old.VotePoint;

import java.util.Optional;

/**
 * Выполняет фильтрацию реакций в пунктах голосования
 */
public class VoteReactFilter {

    public void parseAction(ReactionAddEvent event) {
        if (event.getUser().isYourself()) return;
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
                        boolean nonVoteRole = srv.getRoles(event.getUser())
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
                        Message msg = SettingsController.getInstance()
                                .getVoteController()
                                .getMessage(srv.getId(), v.getTextChatId(), v.getMessageId());
                        if (msg == null) return;
                        msg.getReactions().stream()
                                .filter(r -> !r.getEmoji().getMentionTag().equals(emoji.getMentionTag()))
                                .forEach(r -> r.removeUser(event.getUser()));
                    }
                });
    }
}
