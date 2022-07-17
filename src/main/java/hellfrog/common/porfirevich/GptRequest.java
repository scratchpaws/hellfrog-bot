package hellfrog.common.porfirevich;

import java.io.Serializable;

public class GptRequest
        implements Serializable {

    private String prompt;
    private int length = 30;

    public String getPrompt() {
        return prompt;
    }

    public GptRequest setPrompt(String prompt) {
        this.prompt = prompt;
        return this;
    }

    public int getLength() {
        return length;
    }

    public GptRequest setLength(int length) {
        this.length = length;
        return this;
    }

    @Override
    public String toString() {
        return "GptRequest{" +
                "prompt='" + prompt + '\'' +
                ", length=" + length +
                '}';
    }
}
