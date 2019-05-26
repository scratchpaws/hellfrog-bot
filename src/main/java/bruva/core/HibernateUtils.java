package bruva.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.locks.ReentrantLock;

public class HibernateUtils {

    private static StandardServiceRegistry registry = null;
    private static SessionFactory sessionFactory = null;
    private static ReentrantLock creationLock = new ReentrantLock();
    private static final Logger log = LogManager.getLogger(HibernateUtils.class.getSimpleName());

    @Nullable
    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            creationLock.lock();
            try {
                buildSessionFactory();
            } finally {
                creationLock.unlock();
            }
        }
        return sessionFactory;
    }

    private static void buildSessionFactory() {
        if (sessionFactory == null) {
            try {
                registry = new StandardServiceRegistryBuilder().configure().build();
                MetadataSources metadataSources = new MetadataSources(registry);
                Metadata metadata = metadataSources.getMetadataBuilder().build();
                sessionFactory = metadata.buildSessionFactory();
            } catch (Exception err) {
                log.error("Unable to create session factory", err);
                close();
            }
        }
    }

    public static void close() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
        if (registry != null) {
            registry.close();
        }
    }
}
