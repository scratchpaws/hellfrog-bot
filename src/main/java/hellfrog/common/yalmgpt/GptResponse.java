package hellfrog.common.yalmgpt;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class GptResponse
        implements Serializable {

    @JsonProperty("bad_query")
    int badQuery = 0;
    int error = 0;
    String query = "";
    String text = "";

    public int getBadQuery() {
        return badQuery;
    }

    public void setBadQuery(int badQuery) {
        this.badQuery = badQuery;
    }

    public int getError() {
        return error;
    }

    public void setError(int error) {
        this.error = error;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
