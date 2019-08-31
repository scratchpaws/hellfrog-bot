package hellfrog.settings.db;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class BotOwnersDAOTest {

    private static final Path SETTINGS_PATH = Paths.get("./settings/");
    private static final String TEST_DB_NAME = "test.sqlite3";

    @BeforeEach
    public void dropPreviousDb() throws Exception {
        Path tstBase = SETTINGS_PATH.resolve(TEST_DB_NAME);
        Files.deleteIfExists(tstBase);
    }

    @Test
    public void testValues() throws Exception {
        List<Long> targetIds = List.of(435413273500844033L, 497533970389401600L,
                181069894324846593L, 215548836419076106L,
                515909947427520522L, 220981944391958530L, 354306705829658624L);

        try (MainDBController mainDBController = new MainDBController(TEST_DB_NAME, false)) {
            BotOwnersDAO botOwnersDAO = mainDBController.getBotOwnersDAO();

            Assertions.assertTrue(botOwnersDAO.getAll().isEmpty());

            for (long item : targetIds) {
                Assertions.assertTrue(botOwnersDAO.addToOwners(item));
                Assertions.assertTrue(botOwnersDAO.isPresent(item));
                Assertions.assertFalse(botOwnersDAO.addToOwners(item));
            }
            for (long item : targetIds) {
                Assertions.assertTrue(botOwnersDAO.deleteFromOwners(item));
                Assertions.assertFalse(botOwnersDAO.isPresent(item));
                Assertions.assertFalse(botOwnersDAO.deleteFromOwners(item));
            }
        }
    }
}
