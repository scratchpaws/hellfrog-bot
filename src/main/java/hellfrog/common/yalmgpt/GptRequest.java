package hellfrog.common.yalmgpt;

import java.io.Serializable;

public class GptRequest
        implements Serializable {

    int filter = 1;
    int intro = 0;
    String query = "";

    public int getFilter() {
        return filter;
    }

    public void setFilter(int filter) {
        this.filter = filter;
    }

    public int getIntro() {
        return intro;
    }

    public void setIntro(int intro) {
        this.intro = intro;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    @Override
    public String toString() {
        return "GptRequest{" +
                "filter=" + filter +
                ", intro=" + intro +
                ", query='" + query + '\'' +
                '}';
    }
}
