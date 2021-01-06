package hellfrog.settings.db;

import hellfrog.TestUtils;
import org.junit.jupiter.api.Test;

public class AutoPromoteRolesDAOTest {

    @Test
    public void promotesTest() throws Exception {

        MainDBController.destroyTestDatabase();
        try (MainDBController mainDBController = MainDBController.getInstance(InstanceType.TEST)) {
            AutoPromoteRolesDAO autoPromoteRolesDAO = mainDBController.getAutoPromoteRolesDAO();
        }
    }
}
