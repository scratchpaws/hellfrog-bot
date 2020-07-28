package hellfrog.settings.db.h2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.jetbrains.annotations.NotNull;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.io.Closeable;
import java.util.List;

public class AutoSession
        implements Closeable, AutoCloseable {

    private static SessionFactory sessionFactory = null;
    private final Session hibernateSession;
    private boolean commitRequired = false;
    private boolean rollbackRequired = true;

    private static final Logger log = LogManager.getLogger("DB session");

    private AutoSession(Session hibernateSession) {
        this.hibernateSession = hibernateSession;
    }

    public static void setSessionFactory(@NotNull SessionFactory factory) {
        sessionFactory = factory;
    }

    public static @NotNull AutoSession openSession() throws Exception {
        if (sessionFactory == null) {
            throw new RuntimeException("Session factory is null");
        }
        return new AutoSession(sessionFactory.openSession());
    }

    public<T> CriteriaQuery<T> createQuery(Class<T> type) {
        return getCriteriaBuilder().createQuery(type);
    }

    public<T> Query<T> createQuery(String queryText, Class<T> type) throws Exception {
        return hibernateSession.createQuery(queryText, type);
    }

    public Query createQuery(String queryText) throws Exception {
        return hibernateSession.createQuery(queryText);
    }

    public CriteriaBuilder getCriteriaBuilder() {
        return hibernateSession.getCriteriaBuilder();
    }

    public<R> Query<R> createQuery(CriteriaQuery<R> criteriaQuery) {
        return hibernateSession.createQuery(criteriaQuery);
    }

    public<T> List<T> getAll(Class<T> type) {
        CriteriaQuery<T> criteriaQuery = createQuery(type);
        Root<T> root = criteriaQuery.from(type);
        criteriaQuery.select(root);
        Query<T> query = hibernateSession.createQuery(criteriaQuery);
        List<T> result = query.list();
        success();
        return result;
    }

    public<T> void save(T object) throws Exception {
        hibernateSession.saveOrUpdate(object);
        success();
    }

    public void success() {
        this.commitRequired = true;
        this.rollbackRequired = false;
    }

    @Override
    public void close() {
        if (rollbackRequired) {
            try {
                hibernateSession.getTransaction().rollback();
            } catch (Exception err) {
                String errMsg = String.format("Unable to rollback transaction: %s", err.getMessage());
                log.error(errMsg, err);
            }
        } else if (commitRequired) {
            try {
                hibernateSession.getTransaction().commit();
            } catch (Exception err) {
                String errMsg = String.format("Unable to commit transaction: %s", err.getMessage());
                log.error(errMsg, err);
            }
        }
        try {
            hibernateSession.close();
        } catch (Exception err) {
            String errMsg = String.format("Unable to close session: %s", err.getMessage());
            log.error(errMsg, err);
        }
    }
}
