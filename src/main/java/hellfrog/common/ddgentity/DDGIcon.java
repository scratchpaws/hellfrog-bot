package hellfrog.common.ddgentity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * DuckDuckGo API Response icon URL and dimensions
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DDGIcon {

    @JsonProperty("URL")
    private String url;
    @JsonProperty("Height")
    private String height;
    @JsonProperty("Width")
    private String width;

    public DDGIcon() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public String getWidth() {
        return width;
    }

    public void setWidth(String width) {
        this.width = width;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DDGIcon ddgImage = (DDGIcon) o;
        return Objects.equals(url, ddgImage.url) &&
                Objects.equals(height, ddgImage.height) &&
                Objects.equals(width, ddgImage.width);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, height, width);
    }

    @Override
    public String toString() {
        return "DDGIcon{" +
                "URL='" + url + '\'' +
                ", Height='" + height + '\'' +
                ", Width='" + width + '\'' +
                '}';
    }
}
