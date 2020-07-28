package hellfrog.settings.db;

import hellfrog.TestUtils;
import hellfrog.settings.db.entity.Vote;
import hellfrog.settings.db.entity.VotePoint;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class VotesDAOTest {

    @Test
    public void testPlainVote() throws Exception {

        ThreadLocalRandom tlr = ThreadLocalRandom.current();
        List<Vote> testVotesList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Vote testVote = new Vote()
                    .setServerId(TestUtils.randomDiscordEntityId())
                    .setTextChatId(TestUtils.randomDiscordEntityId())
                    .setWinThreshold(tlr.nextLong(0L, 10L))
                    .setHasDefault(tlr.nextBoolean())
                    .setExceptional(tlr.nextBoolean())
                    .setVoteText(TestUtils.randomStringName(1000));
            if (tlr.nextBoolean()) {
                testVote.setHasTimer(true)
                        .setFinishTime(Instant.now().plus(tlr.nextLong(1L, 10L),
                                ChronoUnit.MINUTES));
            }

            List<VotePoint> votePoints = testVote.getVotePoints();
            int pointsCount = tlr.nextInt(1, 11);
            for (int j = 0; j < pointsCount; j++) {
                VotePoint votePoint = new VotePoint()
                        .setPointText(TestUtils.randomStringName(20));
                if (tlr.nextBoolean()) {
                    votePoint.setUnicodeEmoji(TestUtils.randomStringName(2));
                } else {
                    votePoint.setCustomEmojiId(TestUtils.randomDiscordEntityId());
                }
                votePoints.add(votePoint);
            }

            testVotesList.add(testVote);

            List<Long> rolesFilter = testVote.getRolesFilter();
            int filtersCount = tlr.nextInt(0, 4);
            if (filtersCount > 0)
                rolesFilter.addAll(TestUtils.randomDiscordEntitiesIds(0, filtersCount));
        }

        MainDBController.destroyTestDatabase();
        try (MainDBController mainDBController = MainDBController.getInstance(InstanceType.TEST)) {
            VotesDAO votesDAO = mainDBController.getVotesDAO();

            for (Vote templateVote : testVotesList) {
                Vote createdVote = votesDAO.addVote(templateVote);
                long messageId = TestUtils.randomDiscordEntityId();
                createdVote.setMessageId(messageId);
                templateVote.setMessageId(messageId); // for future comparing
                Vote activated = votesDAO.activateVote(createdVote);
                Assertions.assertEquals(createdVote.getId(), activated.getId());
                templateVote.setId(activated.getId());  // for future comparing
            }
        }

        try (MainDBController mainDBController = MainDBController.getInstance(InstanceType.TEST)) {
            VotesDAO votesDAO = mainDBController.getVotesDAO();

            for (Vote testVote : testVotesList) {
                Vote vote = votesDAO.getVoteById(testVote.getId());
                Assertions.assertNotNull(vote);
                List<Vote> serverVotes = votesDAO.getAll(testVote.getServerId());
                Assertions.assertFalse(serverVotes.isEmpty());
                compareVotes(testVote, vote);
                boolean found = false; // duplicate check
                for (Vote vote1 : serverVotes) {
                    if (vote1.getId() == testVote.getId()) {
                        Assertions.assertFalse(found);
                        found = true;
                        compareVotes(testVote, vote1);
                    }
                }
            }

            for (Vote testVote : testVotesList) {
                Vote vote = votesDAO.getVoteById(testVote.getId());
                Assertions.assertNotNull(vote);
                if (vote.isHasTimer() && vote.getFinishTime().isPresent()) {
                    Instant past = vote.getFinishTime().get().minus(1L, ChronoUnit.SECONDS);
                    List<Vote> expired = votesDAO.getAllExpired(testVote.getServerId(), past);
                    Assertions.assertFalse(expired.isEmpty());
                    // check what expired vote exist into getAllExpired() method result
                    boolean found = false;
                    for (Vote vote1 : expired) {
                        if (vote1.getId() == vote.getId()) {
                            found = true;
                            compareVotes(vote1, vote);
                            break;
                        }
                    }
                    Assertions.assertTrue(found);
                }
            }

            for (Vote testVote : testVotesList) {
                if (!testVote.getRolesFilter().isEmpty()) {
                    List<Long> allowedRoles = votesDAO.getAllowedRoles(testVote.getMessageId());
                    Assertions.assertFalse(allowedRoles.isEmpty());
                    for (long roleId : testVote.getRolesFilter()) {
                        Assertions.assertTrue(allowedRoles.contains(roleId));
                    }
                }
            }

            for (Vote testVote : testVotesList) {
                Assertions.assertTrue(votesDAO.deleteVote(testVote));
                Assertions.assertNull(votesDAO.getVoteById(testVote.getId()));
                Assertions.assertTrue(votesDAO.getAllowedRoles(testVote.getMessageId()).isEmpty());
                List<Vote> allForServer = votesDAO.getAll(testVote.getServerId());
                for (Vote vote : allForServer) {
                    Assertions.assertNotEquals(testVote.getId(), vote.getId());
                }
            }
        }
    }

    private void compareVotes(@Nullable Vote first, @Nullable Vote second) {
        Assertions.assertNotNull(first);
        Assertions.assertNotNull(second);
        if (first == second) {
            return;
        }
        long finishEpochSeconds = first.getFinishTime().map(Instant::getEpochSecond).orElse(-1L);
        long otherFinishEpochSeconds = second.getFinishTime().map(Instant::getEpochSecond).orElse(-1L);
        Assertions.assertEquals(finishEpochSeconds, otherFinishEpochSeconds);
        Assertions.assertEquals(first.getId(), second.getId());
        Assertions.assertEquals(first.getServerId(), second.getServerId());
        Assertions.assertEquals(first.getTextChatId(), second.getTextChatId());
        Assertions.assertEquals(first.getMessageId(), second.getMessageId());
        Assertions.assertEquals(first.isHasTimer(), second.isHasTimer());
        Assertions.assertEquals(first.isExceptional(), second.isExceptional());
        Assertions.assertEquals(first.isHasDefault(), second.isHasDefault());
        Assertions.assertEquals(first.getWinThreshold(), second.getWinThreshold());
        Assertions.assertEquals(finishEpochSeconds, otherFinishEpochSeconds);
        Assertions.assertEquals(first.getVotePoints().size(), second.getVotePoints().size());
        Assertions.assertEquals(first.getVoteText(), second.getVoteText());
        List<Long> copyOfRolesFilterFirst = new ArrayList<>(first.getRolesFilter());
        List<Long> copyOfRilesFilterSecond = new ArrayList<>(second.getRolesFilter());
        Collections.sort(copyOfRolesFilterFirst);
        Collections.sort(copyOfRilesFilterSecond);
        Assertions.assertEquals(copyOfRolesFilterFirst, copyOfRilesFilterSecond);
        for (VotePoint votePoint : first.getVotePoints()) {
            boolean found = false;
            for (VotePoint another : second.getVotePoints()) {
                if (compareVotePoint(votePoint, another)) {
                    found = true;
                    break;
                }
            }
            Assertions.assertTrue(found);
        }
    }

    private boolean compareVotePoint(@Nullable VotePoint first, @Nullable VotePoint second) {
        if (first == null || second == null) {
            return false;
        }
        if (first == second) {
            return true;
        }
        return Objects.equals(first.getPointText().orElse(null),
                second.getPointText().orElse(null))
                && Objects.equals(first.getUnicodeEmoji().orElse(null),
                second.getUnicodeEmoji().orElse(null))
                && first.getCustomEmojiId() == second.getCustomEmojiId();
    }
}
