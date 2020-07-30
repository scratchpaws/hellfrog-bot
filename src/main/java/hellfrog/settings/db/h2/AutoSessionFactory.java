package hellfrog.settings.db.h2;

import org.hibernate.SessionFactory;
import org.jetbrains.annotations.NotNull;

class AutoSessionFactory {

    private final SessionFactory sessionFactory;

    AutoSessionFactory(@NotNull SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @NotNull AutoSession openSession() throws Exception {
        return new AutoSession(sessionFactory.openSession());
    }
}
