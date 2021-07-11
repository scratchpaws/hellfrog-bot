package hellfrog.common.sbergpt;

import java.io.Serializable;

public class GptRequest
        implements Serializable {

    private String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "GptRequest{" +
                "text='" + text + '\'' +
                '}';
    }
}
