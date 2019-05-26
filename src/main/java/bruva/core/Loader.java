package bruva.core;

public class Loader {

    public static void main(String ...args) {
        HibernateUtils.getSessionFactory();
        HibernateUtils.close();
    }
}
