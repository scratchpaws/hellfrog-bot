package hellfrog.common.sbergpt;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class GptResponse
        implements Serializable {

    private String predictions = "";
    private List<ValidationError> detail = Collections.emptyList();

    public String getPredictions() {
        return predictions;
    }

    public void setPredictions(String predictions) {
        this.predictions = predictions;
    }

    public List<ValidationError> getDetail() {
        return detail;
    }

    public void setDetail(List<ValidationError> detail) {
        this.detail = detail;
    }
}
