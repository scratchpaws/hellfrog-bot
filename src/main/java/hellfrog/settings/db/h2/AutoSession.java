package hellfrog.settings.db.h2;

import hellfrog.core.LogsStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

class AutoSession
        implements Closeable, AutoCloseable {

    private final Session hibernateSession;
    private boolean commitRequired = false;
    private boolean rollbackRequired = true;

    private static final Logger log = LogManager.getLogger("DB session");

    AutoSession(Session hibernateSession) {
        this.hibernateSession = hibernateSession;
        this.hibernateSession.beginTransaction();
    }

    public<T> CriteriaQuery<T> createQuery(Class<T> type) {
        return getCriteriaBuilder().createQuery(type);
    }

    public<T> Query<T> createQuery(String queryText, Class<T> type) {
        return hibernateSession.createQuery(queryText, type);
    }

    @Nullable
    public<T> T get(Class<T> entityType, Serializable id) {
        return hibernateSession.get(entityType, id);
    }

    public Query createQuery(String queryText) {
        return hibernateSession.createQuery(queryText);
    }

    public CriteriaBuilder getCriteriaBuilder() {
        return hibernateSession.getCriteriaBuilder();
    }

    public<R> Query<R> createQuery(CriteriaQuery<R> criteriaQuery) {
        return hibernateSession.createQuery(criteriaQuery);
    }

    public Query<?> createQuery(CriteriaDelete<?> criteriaDelete) {
        return hibernateSession.createQuery(criteriaDelete);
    }

    public<T> void remove(@Nullable T object) {
        if (object == null) {
            return;
        }
        resetSuccess();
        hibernateSession.remove(object);
        success();
    }

    public<T> void removeAll(@Nullable Collection<T> objects) {
        if (objects == null || objects.isEmpty()) {
            return;
        }
        resetSuccess();
        for (T item : objects) {
            hibernateSession.remove(item);
        }
        success();
    }

    @SuppressWarnings("unchecked")
    public NativeQuery createNativeQuery(String queryScript) {
        return hibernateSession.createNativeQuery(queryScript);
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

    public<T> void save(@Nullable T object) {
        if (object == null) {
            return;
        }
        resetSuccess();
        hibernateSession.saveOrUpdate(object);
        success();
    }

    public<T> void saveAll(@Nullable Collection<T> objects) {
        if (objects == null || objects.isEmpty()) {
            return;
        }
        resetSuccess();
        for (T item : objects) {
            hibernateSession.saveOrUpdate(item);
        }
        success();
    }

    public void success() {
        this.commitRequired = true;
        this.rollbackRequired = false;
    }

    public void resetSuccess() {
        this.commitRequired = false;
        this.rollbackRequired = true;
    }

    @Override
    public void close() throws IOException {
        if (rollbackRequired) {
            try {
                hibernateSession.getTransaction().rollback();
            } catch (Exception err) {
                String errMsg = String.format("Unable to rollback transaction: %s", err.getMessage());
                LogsStorage.addErrorMessage(errMsg);
                throw new IOException(errMsg, err);
            }
        } else if (commitRequired) {
            try {
                hibernateSession.getTransaction().commit();
            } catch (Exception err) {
                String errMsg = String.format("Unable to commit transaction: %s", err.getMessage());
                LogsStorage.addErrorMessage(errMsg);
                throw new IOException(errMsg, err);
            }
        }
        try {
            hibernateSession.close();
        } catch (Exception err) {
            String errMsg = String.format("Unable to close session: %s", err.getMessage());
            LogsStorage.addErrorMessage(errMsg);
            throw new IOException(errMsg, err);
        }
    }
}
