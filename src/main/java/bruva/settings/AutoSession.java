package bruva.settings;

import bruva.core.HibernateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.io.Closeable;
import java.util.List;

/**
 * Обёртка над Hibernate Session и её методами, автоматически
 * выполняющая фиксацию и откат транзакций, в зависимости от
 * успешности выполнения действия.
 */
public class AutoSession
        implements Closeable, AutoCloseable {

    private Session hibernateSession;
    private boolean commitRequired = false;
    private boolean rollbackRequired = true;
    private static final Logger log = LogManager.getLogger(AutoSession.class.getSimpleName());

    private AutoSession(Session hibernateSession) {
        this.hibernateSession = hibernateSession;
        hibernateSession.beginTransaction();
    }

    public static AutoSession openSession() throws Exception {
        return new AutoSession(HibernateUtils.getSessionFactory().openSession());
    }

    public<T> CriteriaQuery<T> createQuery(Class<T> type) {
        return getCriteriaBuilder().createQuery(type);
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

    public<T> void save(T object) {
        hibernateSession.saveOrUpdate(object);
        success();
    }

    public void success() {
        this.commitRequired = true;
        this.rollbackRequired = false;
    }

    public void close() {
        if (rollbackRequired) {
            try {
                hibernateSession.getTransaction().rollback();
            } catch (Exception err) {
                log.error("Unable to rollback transaction: " + err.getMessage(), err);
            }
        } else if (commitRequired) {
            try {
                hibernateSession.getTransaction().commit();
            } catch (Exception err) {
                log.error("Unable to commit transaction: " + err.getMessage(), err);
            }
        }
        try {
            hibernateSession.close();
        } catch (Exception err) {
            log.error("Unable to close session: " + err.getMessage(), err);
        }
    }
}
