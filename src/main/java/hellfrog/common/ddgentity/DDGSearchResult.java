package hellfrog.common.ddgentity;

import java.net.URI;

public class DDGSearchResult {

    private final URI uri;
    private final String title;
    private final String description;

    public DDGSearchResult(URI uri, String title, String description) {
        this.uri = uri;
        this.title = title;
        this.description = description;
    }

    public URI getUri() {
        return uri;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "DDGSearchResult{" +
                "uri=" + uri +
                ", text='" + title + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
