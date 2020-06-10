package hellfrog.commands.scenes.ddgentity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * DuckDuckGo API Response related topic
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DDGRelatedTopic {

    @JsonProperty("Result")
    private String result;
    @JsonProperty("Icon")
    private DDGIcon icon;
    @JsonProperty("FirstURL")
    private String firstURL;
    @JsonProperty("Text")
    private String text;

    public DDGRelatedTopic() {
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public DDGIcon getIcon() {
        return icon;
    }

    public void setIcon(DDGIcon icon) {
        this.icon = icon;
    }

    public String getFirstURL() {
        return firstURL;
    }

    public void setFirstURL(String firstURL) {
        this.firstURL = firstURL;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DDGRelatedTopic that = (DDGRelatedTopic) o;
        return Objects.equals(result, that.result) &&
                Objects.equals(icon, that.icon) &&
                Objects.equals(firstURL, that.firstURL) &&
                Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(result, icon, firstURL, text);
    }

    @Override
    public String toString() {
        return "DDGRelatedTopic{" +
                "Result='" + result + '\'' +
                ", Icon=" + icon +
                ", FirstURL='" + firstURL + '\'' +
                ", Text='" + text + '\'' +
                '}';
    }
}
