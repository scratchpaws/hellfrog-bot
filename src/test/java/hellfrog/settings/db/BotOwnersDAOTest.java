package hellfrog.settings.db;

import hellfrog.TestUtils;
import hellfrog.core.LogsStorage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BotOwnersDAOTest {


    @Test
    public void testValues() throws Exception {
        List<Long> targetIds = new ArrayList<>();
        for (int i = 0; i <= 100; i++) {
            targetIds.add(TestUtils.randomDiscordEntityId());
        }

        MainDBController.destroyTestDatabase();
        try (MainDBController mainDBController = MainDBController.getInstance(InstanceType.TEST)) {
            BotOwnersDAO botOwnersDAO = mainDBController.getBotOwnersDAO();

            Assertions.assertTrue(botOwnersDAO.getAll().isEmpty());

            for (long item : targetIds) {
                Assertions.assertTrue(botOwnersDAO.addToOwners(item));
                Assertions.assertTrue(botOwnersDAO.isPresent(item));
                Assertions.assertFalse(botOwnersDAO.addToOwners(item));
            }
            Collections.shuffle(targetIds);
            for (long item : targetIds) {
                Assertions.assertTrue(botOwnersDAO.deleteFromOwners(item));
                Assertions.assertFalse(botOwnersDAO.isPresent(item));
                Assertions.assertFalse(botOwnersDAO.deleteFromOwners(item));
            }
            Collections.shuffle(targetIds);
            for (long item : targetIds) {
                Assertions.assertTrue(botOwnersDAO.addToOwners(item));
            }
        }

        try (MainDBController mainDBController = MainDBController.getInstance(InstanceType.TEST)) {
            BotOwnersDAO botOwnersDAO = mainDBController.getBotOwnersDAO();

            Assertions.assertFalse(botOwnersDAO.getAll().isEmpty());
            Collections.shuffle(targetIds);
            for (long item : targetIds) {
                Assertions.assertTrue(botOwnersDAO.isPresent(item));
            }
        }

        Assertions.assertTrue(LogsStorage.isErrorsEmpty(), "Errors log must be empty");
        Assertions.assertTrue(LogsStorage.isWarnsEmpty(), "Warning logs must be empty");
    }

    @Test
    public void testParallel() throws Exception {
        List<Long> targetIds = new ArrayList<>();
        for (int i = 0; i <= 100; i++) {
            targetIds.add(TestUtils.randomDiscordEntityId());
        }

        MainDBController.destroyTestDatabase();
        try (MainDBController mainDBController = MainDBController.getInstance(InstanceType.TEST)) {
            BotOwnersDAO botOwnersDAO = mainDBController.getBotOwnersDAO();

            Assertions.assertTrue(botOwnersDAO.getAll().isEmpty());

            boolean allAdded = targetIds.parallelStream()
                    .allMatch(botOwnersDAO::addToOwners);
            Assertions.assertTrue(allAdded);
            Collections.shuffle(targetIds);
            boolean allPresent = targetIds.parallelStream()
                    .allMatch(botOwnersDAO::isPresent);
            Assertions.assertTrue(allPresent);
        }

        Assertions.assertTrue(LogsStorage.isErrorsEmpty(), "Errors log must be empty");
        Assertions.assertTrue(LogsStorage.isWarnsEmpty(), "Warning logs must be empty");
    }
}
