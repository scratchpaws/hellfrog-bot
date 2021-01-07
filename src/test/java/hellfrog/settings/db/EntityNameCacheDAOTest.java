package hellfrog.settings.db;

import hellfrog.TestUtils;
import hellfrog.core.LogsStorage;
import hellfrog.settings.db.entity.NameType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class EntityNameCacheDAOTest {

    @Test
    public void testGlobalNames() throws Exception {

        final List<NameEntity> creation = generateGlobal();
        final List<NameEntity> firstUpdate = generateGlobalUpdates(creation);
        final List<NameEntity> secondUpdate = generateGlobalUpdates(creation);

        MainDBController.destroyTestDatabase();
        try (MainDBController mainDBController = MainDBController.getInstance(InstanceType.TEST)) {
            final EntityNameCacheDAO entityNameCacheDAO = mainDBController.getEntityNameCacheDAO();

            creation.parallelStream().forEach(nameEntity -> {
                Assertions.assertTrue(entityNameCacheDAO.find(nameEntity.id).isEmpty(), "Entity name cache must be empty");
                entityNameCacheDAO.update(nameEntity.id, nameEntity.name, nameEntity.type);
                entityNameCacheDAO.find(nameEntity.id).ifPresentOrElse(stored -> {
                    Assertions.assertEquals(nameEntity.id, stored.getEntityId());
                    Assertions.assertEquals(nameEntity.name, stored.getName());
                    Assertions.assertEquals(nameEntity.type, stored.getEntityType());
                }, Assertions::fail);
            });

            testGlobalUpdate(firstUpdate, entityNameCacheDAO);
            testGlobalUpdate(secondUpdate, entityNameCacheDAO);
        }

        Assertions.assertTrue(LogsStorage.isErrorsEmpty(), "Errors log must be empty");
        Assertions.assertTrue(LogsStorage.isWarnsEmpty(), "Warning logs must be empty");
    }

    @Test
    public void testServerNames() throws Exception {

        final List<ServerNameEntity> creation = generateServers();
        final List<ServerNameEntity> firstUpdate = generateServerUpdates(creation);
        final List<ServerNameEntity> secondUpdate = generateServerUpdates(creation);

        MainDBController.destroyTestDatabase();
        try (MainDBController mainDBController = MainDBController.getInstance(InstanceType.TEST)) {
            final EntityNameCacheDAO entityNameCacheDAO = mainDBController.getEntityNameCacheDAO();

            creation.parallelStream().forEach(serverNameEntity -> {
                Assertions.assertTrue(entityNameCacheDAO.find(serverNameEntity.serverId, serverNameEntity.entityId).isEmpty(),
                        "Entity name cache must be empty");
                entityNameCacheDAO.update(serverNameEntity.serverId, serverNameEntity.entityId, serverNameEntity.entityName);
                entityNameCacheDAO.find(serverNameEntity.serverId, serverNameEntity.entityId).ifPresentOrElse(stored -> {
                    Assertions.assertEquals(serverNameEntity.serverId, stored.getServerId());
                    Assertions.assertEquals(serverNameEntity.entityId, stored.getEntityId());
                    Assertions.assertEquals(serverNameEntity.entityName, stored.getName());
                }, Assertions::fail);
            });

            testServerUpdate(firstUpdate, entityNameCacheDAO);
            testServerUpdate(secondUpdate, entityNameCacheDAO);
        }

        Assertions.assertTrue(LogsStorage.isErrorsEmpty(), "Errors log must be empty");
        Assertions.assertTrue(LogsStorage.isWarnsEmpty(), "Warning logs must be empty");
    }

    private void testGlobalUpdate(@NotNull final List<NameEntity> updates,
                                  @NotNull final EntityNameCacheDAO entityNameCacheDAO) {

        updates.parallelStream().forEach(nameEntity ->
                entityNameCacheDAO.find(nameEntity.id).ifPresentOrElse(stored -> {
                    entityNameCacheDAO.update(nameEntity.id, nameEntity.name, nameEntity.type);
                    entityNameCacheDAO.find(nameEntity.id).ifPresentOrElse(updated -> {
                        Assertions.assertEquals(nameEntity.id, updated.getEntityId());
                        Assertions.assertEquals(nameEntity.name, updated.getName());
                        Assertions.assertEquals(nameEntity.type, updated.getEntityType());
                    }, Assertions::fail);
                }, Assertions::fail));
    }

    private void testServerUpdate(@NotNull final List<ServerNameEntity> updates,
                                  @NotNull final EntityNameCacheDAO entityNameCacheDAO) {

        updates.parallelStream().forEach(serverNameEntity ->
                entityNameCacheDAO.find(serverNameEntity.serverId, serverNameEntity.entityId).ifPresentOrElse(stored -> {
                    entityNameCacheDAO.update(serverNameEntity.serverId, serverNameEntity.entityId, serverNameEntity.entityName);
                    entityNameCacheDAO.find(serverNameEntity.serverId, serverNameEntity.entityId).ifPresentOrElse(updated -> {
                        Assertions.assertEquals(serverNameEntity.serverId, updated.getServerId());
                        Assertions.assertEquals(serverNameEntity.entityId, updated.getEntityId());
                        Assertions.assertEquals(serverNameEntity.entityName, updated.getName());
                    }, Assertions::fail);
                }, Assertions::fail));
    }

    private List<NameEntity> generateGlobal() {
        List<NameEntity> entities = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            entities.add(new NameEntity());
        }
        return Collections.unmodifiableList(entities);
    }

    private List<ServerNameEntity> generateServers() {
        List<ServerNameEntity> entities = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            entities.add(new ServerNameEntity());
        }
        return Collections.unmodifiableList(entities);
    }

    private List<NameEntity> generateGlobalUpdates(@NotNull final List<NameEntity> source) {
        int minCount = Math.min(source.size(), 50);
        int maxCount = Math.min(source.size(), 100);
        List<NameEntity> entities = new ArrayList<>();
        for (NameEntity orig : TestUtils.randomSublist(source, minCount, maxCount)) {
            entities.add(new NameEntity(orig.id));
        }
        return Collections.unmodifiableList(entities);
    }

    private List<ServerNameEntity> generateServerUpdates(@NotNull final List<ServerNameEntity> source) {
        int minCount = Math.min(source.size(), 50);
        int maxCount = Math.max(source.size(), 100);
        List<ServerNameEntity> entities = new ArrayList<>();
        for (ServerNameEntity orig : TestUtils.randomSublist(source, minCount, maxCount)) {
            entities.add(new ServerNameEntity(orig.serverId, orig.entityId));
        }
        return Collections.unmodifiableList(entities);
    }

    private static class NameEntity {

        private final long id;
        private final String name;
        private final NameType type;

        public NameEntity(long entityId) {
            ThreadLocalRandom tlr = ThreadLocalRandom.current();
            this.id = entityId;
            name = TestUtils.randomStringName(tlr.nextInt(2, 121));
            NameType[] allTypes = NameType.values();
            type = allTypes[tlr.nextInt(0, allTypes.length)];
        }

        public NameEntity() {
            ThreadLocalRandom tlr = ThreadLocalRandom.current();
            this.id = TestUtils.randomDiscordEntityId();
            name = TestUtils.randomStringName(tlr.nextInt(2, 121));
            NameType[] allTypes = NameType.values();
            type = allTypes[tlr.nextInt(0, allTypes.length)];
        }
    }

    private static class ServerNameEntity {

        private final long serverId;
        private final long entityId;
        private final String entityName;

        ServerNameEntity(long serverId, long entityId) {
            ThreadLocalRandom tlr = ThreadLocalRandom.current();
            this.serverId = serverId;
            this.entityId = entityId;
            this.entityName = TestUtils.randomStringName(tlr.nextInt(2, 121));
        }

        ServerNameEntity() {
            ThreadLocalRandom tlr = ThreadLocalRandom.current();
            this.serverId = TestUtils.randomDiscordEntityId();
            this.entityId = TestUtils.randomDiscordEntityId();
            this.entityName = TestUtils.randomStringName(tlr.nextInt(2, 121));
        }
    }
}
