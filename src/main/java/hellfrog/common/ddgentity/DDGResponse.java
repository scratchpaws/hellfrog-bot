package hellfrog.common.ddgentity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.Objects;

/**
 * DuckDuckGo API Response.
 * Based by https://duckduckgo.com/api and https://github.com/ajanicij/goduckgo/
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DDGResponse {

    @JsonProperty("Definition")
    private String definition;
    @JsonProperty("DefinitionSource")
    private String definitionSource;
    @JsonProperty("Heading")
    private String heading;
    @JsonProperty("AbstractText")
    private String abstractText;
    @JsonProperty("Abstract")
    private String ddgAbstract;
    @JsonProperty("AbstractSource")
    private String abstractSource;
    @JsonProperty("Image")
    private String image;
    @JsonProperty("Type")
    private String type;
    @JsonProperty("AnswerType")
    private String answerType;
    @JsonProperty("Redirect")
    private String redirect;
    @JsonProperty("DefinitionURL")
    private String definitionURL;
    @JsonProperty("Answer")
    private String answer;
    @JsonProperty("AbstractURL")
    private String abstractURL;
    @JsonProperty("Results")
    private DDGRelatedTopic[] results;
    @JsonProperty("RelatedTopics")
    private DDGRelatedTopic[] relatedTopics;

    public DDGResponse() {}

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public String getDefinitionSource() {
        return definitionSource;
    }

    public void setDefinitionSource(String definitionSource) {
        this.definitionSource = definitionSource;
    }

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

    public String getAbstractText() {
        return abstractText;
    }

    public void setAbstractText(String abstractText) {
        this.abstractText = abstractText;
    }

    public String getDdgAbstract() {
        return ddgAbstract;
    }

    public void setDdgAbstract(String ddgAbstract) {
        this.ddgAbstract = ddgAbstract;
    }

    public String getAbstractSource() {
        return abstractSource;
    }

    public void setAbstractSource(String abstractSource) {
        this.abstractSource = abstractSource;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAnswerType() {
        return answerType;
    }

    public void setAnswerType(String answerType) {
        this.answerType = answerType;
    }

    public String getRedirect() {
        return redirect;
    }

    public void setRedirect(String redirect) {
        this.redirect = redirect;
    }

    public String getDefinitionURL() {
        return definitionURL;
    }

    public void setDefinitionURL(String definitionURL) {
        this.definitionURL = definitionURL;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getAbstractURL() {
        return abstractURL;
    }

    public void setAbstractURL(String abstractURL) {
        this.abstractURL = abstractURL;
    }

    public DDGRelatedTopic[] getResults() {
        return results;
    }

    public void setResults(DDGRelatedTopic[] results) {
        this.results = results;
    }

    public DDGRelatedTopic[] getRelatedTopics() {
        return relatedTopics;
    }

    public void setRelatedTopics(DDGRelatedTopic[] relatedTopics) {
        this.relatedTopics = relatedTopics;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DDGResponse that = (DDGResponse) o;
        return Objects.equals(definition, that.definition) &&
                Objects.equals(definitionSource, that.definitionSource) &&
                Objects.equals(heading, that.heading) &&
                Objects.equals(abstractText, that.abstractText) &&
                Objects.equals(ddgAbstract, that.ddgAbstract) &&
                Objects.equals(abstractSource, that.abstractSource) &&
                Objects.equals(image, that.image) &&
                Objects.equals(type, that.type) &&
                Objects.equals(answerType, that.answerType) &&
                Objects.equals(redirect, that.redirect) &&
                Objects.equals(definitionURL, that.definitionURL) &&
                Objects.equals(answer, that.answer) &&
                Objects.equals(abstractURL, that.abstractURL) &&
                Arrays.equals(results, that.results) &&
                Arrays.equals(relatedTopics, that.relatedTopics);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(definition, definitionSource, heading, abstractText, ddgAbstract, abstractSource, image, type, answerType, redirect, definitionURL, answer, abstractURL);
        result = 31 * result + Arrays.hashCode(results);
        result = 31 * result + Arrays.hashCode(relatedTopics);
        return result;
    }

    @Override
    public String toString() {
        return "DDGResponse{" +
                "Definition='" + definition + '\'' +
                ", DefinitionSource='" + definitionSource + '\'' +
                ", Heading='" + heading + '\'' +
                ", AbstractText='" + abstractText + '\'' +
                ", Abstract='" + ddgAbstract + '\'' +
                ", AbstractSource='" + abstractSource + '\'' +
                ", Image='" + image + '\'' +
                ", Type='" + type + '\'' +
                ", AnswerType='" + answerType + '\'' +
                ", Redirect='" + redirect + '\'' +
                ", DefinitionURL='" + definitionURL + '\'' +
                ", Answer='" + answer + '\'' +
                ", AbstractURL='" + abstractURL + '\'' +
                ", Results=" + Arrays.toString(results) +
                ", RelatedTopics=" + Arrays.toString(relatedTopics) +
                '}';
    }
}
