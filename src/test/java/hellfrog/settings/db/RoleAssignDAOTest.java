package hellfrog.settings.db;

import org.junit.jupiter.api.Test;

public class RoleAssignDAOTest {

    @Test
    public void queueTest() throws Exception {

        MainDBController.destroyTestDatabase();
        try (MainDBController mainDBController = MainDBController.getInstance(InstanceType.TEST)) {
            RoleAssignDAO roleAssignDAO = mainDBController.getRoleAssignDAO();
        }
    }
}
