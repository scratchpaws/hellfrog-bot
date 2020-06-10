package hellfrog.commands.scenes.gptentity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class GptRequest
    implements Serializable {

    private String prompt;
    private int length = 30;
    private int numSamples = 1;

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

    @JsonProperty("num_samples")
    public int getNumSamples() {
        return numSamples;
    }

    public GptRequest setNumSamples(int numSamples) {
        this.numSamples = numSamples;
        return this;
    }

    @Override
    public String toString() {
        return "GptRequest{" +
                "prompt='" + prompt + '\'' +
                ", length=" + length +
                ", numSamples=" + numSamples +
                '}';
    }
}
