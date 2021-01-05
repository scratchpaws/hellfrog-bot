package hellfrog.settings.db;

import hellfrog.TestUtils;
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
    public void testValues() throws Exception {

        final List<TestEntity> creation = generate();
        final List<TestEntity> firstUpdate = update(creation);
        final List<TestEntity> secondUpdate = update(creation);

        MainDBController.destroyTestDatabase();
        try (MainDBController mainDBController = MainDBController.getInstance(InstanceType.TEST)) {
            final EntityNameCacheDAO entityNameCacheDAO = mainDBController.getEntityNameCacheDAO();

            creation.parallelStream().forEach(testEntity -> {
                Assertions.assertTrue(entityNameCacheDAO.find(testEntity.id).isEmpty(), "Entity name cache must be empty");
                entityNameCacheDAO.update(testEntity.id, testEntity.name, testEntity.type);
                entityNameCacheDAO.find(testEntity.id).ifPresentOrElse(stored -> {
                    Assertions.assertEquals(testEntity.id, stored.getEntityId());
                    Assertions.assertEquals(testEntity.name, stored.getName());
                    Assertions.assertEquals(testEntity.type, stored.getEntityType());
                }, Assertions::fail);
            });

            testUpdate(firstUpdate, entityNameCacheDAO);
            testUpdate(secondUpdate, entityNameCacheDAO);
        }
    }

    private void testUpdate(@NotNull final List<TestEntity> updates,
                            @NotNull final EntityNameCacheDAO entityNameCacheDAO) {

        updates.parallelStream().forEach(testEntity ->
                entityNameCacheDAO.find(testEntity.id).ifPresentOrElse(stored -> {
                    entityNameCacheDAO.update(testEntity.id, testEntity.name, testEntity.type);
                    entityNameCacheDAO.find(testEntity.id).ifPresentOrElse(updated -> {
                        Assertions.assertEquals(testEntity.id, updated.getEntityId());
                        Assertions.assertEquals(testEntity.name, updated.getName());
                        Assertions.assertEquals(testEntity.type, updated.getEntityType());
                    }, Assertions::fail);
                }, Assertions::fail));
    }

    private List<TestEntity> generate() {
        List<TestEntity> entities = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            entities.add(new TestEntity());
        }
        return Collections.unmodifiableList(entities);
    }

    private List<TestEntity> update(@NotNull final List<TestEntity> source) {
        int minCount = Math.min(source.size(), 50);
        int maxCount = Math.min(source.size(), 100);
        List<TestEntity> entities = new ArrayList<>();
        for (TestEntity orig : TestUtils.randomSublist(source, minCount, maxCount)) {
            entities.add(new TestEntity(orig.id));
        }
        return Collections.unmodifiableList(entities);
    }

    private static class TestEntity {

        private final long id;
        private final String name;
        private final NameType type;

        public TestEntity(long entityId) {
            ThreadLocalRandom tlr = ThreadLocalRandom.current();
            this.id = entityId;
            name = TestUtils.randomStringName(tlr.nextInt(2, 121));
            NameType[] allTypes = NameType.values();
            type = allTypes[tlr.nextInt(0, allTypes.length)];
        }

        public TestEntity() {
            ThreadLocalRandom tlr = ThreadLocalRandom.current();
            this.id = TestUtils.randomDiscordEntityId();
            name = TestUtils.randomStringName(tlr.nextInt(2, 121));
            NameType[] allTypes = NameType.values();
            type = allTypes[tlr.nextInt(0, allTypes.length)];
        }
    }
}
