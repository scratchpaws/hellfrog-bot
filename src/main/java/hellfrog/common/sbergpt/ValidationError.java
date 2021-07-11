package hellfrog.common.sbergpt;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class ValidationError
        implements Serializable {

    List<String> loc = Collections.emptyList();
    String msg = "";
    String type = "";

    public List<String> getLoc() {
        return loc;
    }

    public void setLoc(List<String> loc) {
        this.loc = loc;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
