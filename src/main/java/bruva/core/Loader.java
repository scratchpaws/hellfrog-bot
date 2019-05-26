package bruva.core;

import bruva.settings.CommonName;
import bruva.settings.CommonSetting;
import bruva.settings.DAO.CommonSettingsDAO;

public class Loader {

    public static void main(String ...args) throws Exception {
        HibernateUtils.getSessionFactory();

        CommonSettingsDAO preferencesDAO = new CommonSettingsDAO();
        String name = preferencesDAO.get(CommonName.BOT_NAME);
        System.out.println("Current value: " + name);
        preferencesDAO.set(CommonName.BOT_NAME, "Ktl");
        name = preferencesDAO.get(CommonName.BOT_NAME);
        System.out.println("New value: " + name);
        //HibernateUtils.generateDDL("./ddl.txt");
        for (CommonName commonName : CommonName.values()) {
            System.out.println(commonName + " - " + preferencesDAO.get(commonName));
        }
        for (CommonSetting commonSetting : preferencesDAO.getAll())
            System.out.println(commonSetting);
        HibernateUtils.close();
    }
}
