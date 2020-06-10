package hellfrog.commands.scenes.gptentity;

import hellfrog.common.CommonUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class GptResponse
    implements Serializable {

    private List<String> replies = Collections.emptyList();
    private String detail = "";

    public List<String> getReplies() {
        return replies;
    }

    public GptResponse setReplies(List<String> replies) {
        this.replies = replies;
        return this;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = CommonUtils.isTrStringNotEmpty(detail) ? detail : "";
    }
}
