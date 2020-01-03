package hellfrog.settings.db;

import hellfrog.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BotOwnersDAOTest {

    private static final Path SETTINGS_PATH = Paths.get("./settings/");
    private static final String TEST_DB_NAME = "test.sqlite3";
    private static final Path tstBase = SETTINGS_PATH.resolve(TEST_DB_NAME);

    @Test
    public void testValues() throws Exception {
        List<Long> targetIds = new ArrayList<>();
        for (int i = 0; i <= 100; i++) {
            targetIds.add(TestUtils.randomDiscordEntityId());
        }

        Files.deleteIfExists(tstBase);
        try (MainDBController mainDBController = new MainDBController(TEST_DB_NAME, false)) {
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

        try (MainDBController mainDBController = new MainDBController(TEST_DB_NAME, false)) {
            BotOwnersDAO botOwnersDAO = mainDBController.getBotOwnersDAO();

            Assertions.assertFalse(botOwnersDAO.getAll().isEmpty());
            Collections.shuffle(targetIds);
            for (long item : targetIds) {
                Assertions.assertTrue(botOwnersDAO.isPresent(item));
            }
        }
    }

    @Test
    public void testParallel() throws Exception {
        List<Long> targetIds = new ArrayList<>();
        for (int i = 0; i <= 100; i++) {
            targetIds.add(TestUtils.randomDiscordEntityId());
        }

        Files.deleteIfExists(tstBase);
        try (MainDBController mainDBController = new MainDBController(TEST_DB_NAME, false)) {
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
    }
}
