package hellfrog.settings.db;

import hellfrog.TestUtils;
import hellfrog.settings.entity.*;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class VotesDAOTest {

    private static final Path SETTINGS_PATH = Paths.get("./settings/");
    private static final String TEST_DB_NAME = "test.sqlite3";
    private static final Path tstBase = SETTINGS_PATH.resolve(TEST_DB_NAME);

    @Test
    public void testPlainVote() throws Exception {
        Files.deleteIfExists(tstBase);

        ThreadLocalRandom tlr = ThreadLocalRandom.current();
        List<ActiveVote> testVotesList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ActiveVote testVote = new ActiveVote();
            testVote.setServerId(TestUtils.randomDiscordEntityId());
            testVote.setTextChatId(TestUtils.randomDiscordEntityId());
            testVote.setWinThreshold(tlr.nextLong(0L, 10L));
            testVote.setHasDefault(tlr.nextBoolean());
            testVote.setExceptional(tlr.nextBoolean());
            testVote.setVoteText(TestUtils.randomStringName(1000));
            if (tlr.nextBoolean()) {
                testVote.setHasTimer(true);
                testVote.setFinishTime(Instant.now().plus(tlr.nextLong(1L, 10L),
                        ChronoUnit.MINUTES));
            }

            List<ActiveVotePoint> votePoints = new ArrayList<>();
            int pointsCount = tlr.nextInt(1, 11);
            for (int j = 0; j < pointsCount; j++) {
                ActiveVotePoint votePoint = new ActiveVotePoint();
                votePoint.setPointText(TestUtils.randomStringName(20));
                if (tlr.nextBoolean()) {
                    votePoint.setUnicodeEmoji(TestUtils.randomStringName(2));
                } else {
                    votePoint.setCustomEmojiId(TestUtils.randomDiscordEntityId());
                }
                votePoints.add(votePoint);
            }
            testVote.setVotePoints(votePoints);

            testVotesList.add(testVote);

            int filtersCount = tlr.nextInt(0, 4);
            if (filtersCount > 0) {
                List<VoteRole> rolesFilter = new ArrayList<>();
                for (int j = 0; j < filtersCount; j++) {
                    VoteRole voteRole = new VoteRole();
                    voteRole.setRoleId(TestUtils.randomDiscordEntityId());
                    rolesFilter.add(voteRole);
                }
                testVote.setRolesFilter(rolesFilter);
            }
        }

        try (MainDBController mainDBController = new MainDBController(TEST_DB_NAME, false)) {
            VotesDAO votesDAO = mainDBController.getVotesDAO();

            for (ActiveVote templateVote : testVotesList) {
                ActiveVote createdVote = votesDAO.addVote(templateVote);
                long messageId = TestUtils.randomDiscordEntityId();
                createdVote.setMessageId(messageId);
                templateVote.setMessageId(messageId); // for future comparing
                ActiveVote activated = votesDAO.activateVote(createdVote);
                Assertions.assertEquals(createdVote.getId(), activated.getId());
                templateVote.setId(activated.getId());  // for future comparing
            }
        }

        try (MainDBController mainDBController = new MainDBController(TEST_DB_NAME, false)) {
            VotesDAO votesDAO = mainDBController.getVotesDAO();

            for (ActiveVote testVote : testVotesList) {
                ActiveVote vote = votesDAO.getVoteById(testVote.getId());
                Assertions.assertNotNull(vote);
                List<ActiveVote> serverVotes = votesDAO.getAll(testVote.getServerId());
                Assertions.assertFalse(serverVotes.isEmpty());
                compareVotes(testVote, vote);
                boolean found = false; // duplicate check
                for (ActiveVote vote1 : serverVotes) {
                    if (vote1.getId() == testVote.getId()) {
                        Assertions.assertFalse(found);
                        found = true;
                        compareVotes(testVote, vote1);
                    }
                }
            }

            for (ActiveVote testVote : testVotesList) {
                ActiveVote vote = votesDAO.getVoteById(testVote.getId());
                Assertions.assertNotNull(vote);
                if (vote.isHasTimer() && vote.getFinishTime() != null) {
                    Instant past = vote.getFinishTime().minus(1L, ChronoUnit.SECONDS);
                    List<ActiveVote> expired = votesDAO.getAllExpired(testVote.getServerId(), past);
                    Assertions.assertFalse(expired.isEmpty());
                    // check what expired vote exist into getAllExpired() method result
                    boolean found = false;
                    for (ActiveVote vote1 : expired) {
                        if (vote1.getId() == vote.getId()) {
                            found = true;
                            compareVotes(vote1, vote);
                            break;
                        }
                    }
                    Assertions.assertTrue(found);
                }
            }

            for (ActiveVote testVote : testVotesList) {
                ActiveVote vote = votesDAO.getVoteById(testVote.getId());
                Assertions.assertNotNull(vote);
                Assertions.assertNotNull(vote.getRolesFilter());
                for (VoteRole voteRole : vote.getRolesFilter()) {
                    Assertions.assertEquals(vote.getMessageId(), voteRole.getMessageId());
                }
            }

            for (ActiveVote testVote : testVotesList) {
                Assertions.assertTrue(votesDAO.deleteVote(testVote));
                Assertions.assertNull(votesDAO.getVoteById(testVote.getId()));
                List<ActiveVote> allForServer = votesDAO.getAll(testVote.getServerId());
                for (ActiveVote vote : allForServer) {
                    Assertions.assertNotEquals(testVote.getId(), vote.getId());
                }
            }
        }
    }

    private void compareVotes(@Nullable ActiveVote first, @Nullable ActiveVote second) {
        Assertions.assertNotNull(first);
        Assertions.assertNotNull(second);
        if (first == second) {
            return;
        }
        long finishEpochSeconds = first.getFinishTime() != null ? first.getFinishTime().getEpochSecond() : -1L;
        long otherFinishEpochSeconds = second.getFinishTime() != null ? second.getFinishTime().getEpochSecond() : -1L;
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
        List<Long> copyOfRolesFilterFirst = first.getRolesFilter().stream()
                .map(VoteRole::getRoleId).collect(Collectors.toList());
        List<Long> copyOfRilesFilterSecond = second.getRolesFilter().stream()
                .map(VoteRole::getRoleId).collect(Collectors.toList());
        Collections.sort(copyOfRolesFilterFirst);
        Collections.sort(copyOfRilesFilterSecond);
        Assertions.assertEquals(copyOfRolesFilterFirst, copyOfRilesFilterSecond);
        for (ActiveVotePoint votePoint : first.getVotePoints()) {
            boolean found = false;
            for (ActiveVotePoint another : second.getVotePoints()) {
                if (compareVotePoint(votePoint, another)) {
                    found = true;
                    break;
                }
            }
            Assertions.assertTrue(found);
        }
    }

    private boolean compareVotePoint(@Nullable ActiveVotePoint first, @Nullable ActiveVotePoint second) {
        if (first == null || second == null) {
            return false;
        }
        if (first == second) {
            return true;
        }
        return Objects.equals(first.getPointText(), second.getPointText())
                && Objects.equals(first.getUnicodeEmoji(), second.getUnicodeEmoji())
                && first.getCustomEmojiId() == second.getCustomEmojiId();
    }
}
