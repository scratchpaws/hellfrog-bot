package bruva.settings.DAO;

import bruva.settings.AutoSession;
import bruva.settings.Entity.CommonName;
import bruva.settings.Entity.CommonSetting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.query.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.List;

public class CommonSettingsDAO {

    private static final Logger log = LogManager.getLogger(CommonSettingsDAO.class.getSimpleName());
    private static final String PARAM_VALUE_FROM_NAME_HQL = "from CommonSetting cs where cs.name = :name";

    public List<CommonSetting> getAll() throws Exception {
        try (AutoSession session = AutoSession.openSession()) {
            return session.getAll(CommonSetting.class);
        }
    }

    @Nullable
    private CommonSetting getCommonSettingByKey(@NotNull AutoSession session, @NotNull String parameterKey)
            throws Exception {

        Query<CommonSetting> query = session.createQuery(PARAM_VALUE_FROM_NAME_HQL, CommonSetting.class);
        query.setParameter("name", parameterKey);
        List<CommonSetting> result = query.list();
        return result != null && !result.isEmpty() ? result.get(0) : null;
    }

    private void writeCommonSetting(@NotNull AutoSession session, @NotNull CommonName parameterName,
                                    @Nullable CommonSetting commonSetting, @Nullable String value)
            throws Exception {

        String parameterKey = CommonName.NAMES.get(parameterName);

        if (commonSetting == null) {
            commonSetting = new CommonSetting();
            commonSetting.setName(parameterKey);
            commonSetting.setInsertDate(new Date());
            commonSetting.setComment(CommonName.COMMENTS.getOrDefault(parameterName, "empty"));
        }
        commonSetting.setUpdateDate(new Date());
        commonSetting.setValue(value != null ? value : CommonName.DEFAULT_VALUES.get(parameterName));
        session.save(commonSetting);
    }

    public String get(CommonName parameterName) throws Exception {
        String parameterKey = CommonName.NAMES.get(parameterName);
        if (parameterKey == null)
            throw new IllegalArgumentException("Parameter unknown");

        try (AutoSession session = AutoSession.openSession()) {
            CommonSetting result = getCommonSettingByKey(session, parameterKey);
            if (result == null) {
                writeCommonSetting(session, parameterName, null, null);
            }
            session.success();
            return result != null ? result.getValue()
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
            CommonSetting modify = getCommonSettingByKey(session, parameterKey);
            writeCommonSetting(session, parameterName, modify, parameterValue);
        }
    }
}
