package bruva.settings.DAO;

import bruva.settings.AutoSession;
import bruva.settings.CommonSetting;
import bruva.settings.CommonName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.*;

public class CommonSettingsDAO {

    private static final Logger log = LogManager.getLogger(CommonSettingsDAO.class.getSimpleName());

    public List<CommonSetting> getAll() throws Exception {
        try (AutoSession session = AutoSession.openSession()) {
            return session.getAll(CommonSetting.class);
        }
    }

    public void save(CommonSetting commonPreference) throws Exception {
        try (AutoSession session = AutoSession.openSession()) {
            session.save(commonPreference);
        }
    }

    public String get(CommonName parameterName) throws Exception {
        String parameterKey = CommonName.NAMES.get(parameterName);
        if (parameterKey == null)
            throw new IllegalArgumentException("Parameter unknown");

        try (AutoSession session = AutoSession.openSession()) {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<CommonSetting> cq = cb.createQuery(CommonSetting.class);
            Root<CommonSetting> prefR = cq.from(CommonSetting.class);
            cq.select(prefR);
            cq.where(cb.equal(prefR.get("name"), parameterKey));
            List<CommonSetting> result = session.createQuery(cq).list();

            if (result == null || result.isEmpty()) {
                CommonSetting common = new CommonSetting();
                common.setName(parameterKey);
                common.setValue(CommonName.DEFAULT_VALUES.get(parameterName));
                common.setComment(CommonName.COMMENTS.get(parameterName));
                common.setInsertDate(new Date());
                common.setUpdateDate(new Date());
                session.save(common);
            }

            session.success();

            return result != null && !result.isEmpty()
                    ? result.get(0).getValue()
                    : CommonName.DEFAULT_VALUES.get(parameterName);
        }
    }

    public void set(CommonName parameterName, String parameterValue)
            throws Exception {
        String parameterKey = CommonName.NAMES.get(parameterName);
        if (parameterKey == null)
            throw new IllegalArgumentException("Parameter unknown");
        if (parameterValue == null)
            parameterValue = CommonName.DEFAULT_VALUES.get(parameterName);

        try (AutoSession session = AutoSession.openSession()) {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<CommonSetting> cq = cb.createQuery(CommonSetting.class);
            Root<CommonSetting> prefR = cq.from(CommonSetting.class);
            cq.select(prefR);
            cq.where(cb.equal(prefR.get("name"), parameterKey));
            List<CommonSetting> output = session.createQuery(cq).list();
            CommonSetting modify = output != null && !output.isEmpty()
                    ? output.get(0) : null;
            if (modify == null) {
                modify = new CommonSetting();
                modify.setName(parameterKey);
                modify.setInsertDate(new Date());
                modify.setComment(CommonName.COMMENTS.get(parameterName));
            }
            modify.setValue(parameterValue);
            modify.setUpdateDate(new Date());
            session.save(modify);
        }
    }
}
