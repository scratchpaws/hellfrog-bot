package hellfrog.settings.db.entity;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;

/**
 * Descriptions of one participant that another participant gave him
 */
public class WtfEntry {

    private final long authorId;
    private final Instant date;
    private final String description;
    private final URI uri;

    private WtfEntry(@NotNull Builder builder) {
        authorId = builder.authorId;
        date = builder.date;
        description = builder.description;
        uri = builder.uri;
    }

    @Contract(value = " -> new", pure = true)
    public static @NotNull Builder newBuilder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
                .authorId(this.authorId)
                .date(this.date)
                .description(this.description)
                .uri(this.uri);
    }

    public long getAuthorId() {
        return authorId;
    }

    public Instant getDate() {
        return date;
    }

    public String getDescription() {
        return description;
    }

    public URI getUri() {
        return uri;
    }

    @Override
    public String toString() {
        return "WtfEntry{" +
                "authorId=" + authorId +
                ", date=" + date +
                ", description='" + description + '\'' +
                ", uri=" + uri +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WtfEntry wtfEntry = (WtfEntry) o;
        return authorId == wtfEntry.authorId &&
                Objects.equals(date, wtfEntry.date) &&
                Objects.equals(description, wtfEntry.description) &&
                Objects.equals(uri, wtfEntry.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(authorId, date, description, uri);
    }


    public static final class Builder {
        private long authorId = 0L;
        private Instant date;
        private String description;
        private URI uri;

        private Builder() {
        }

        public Builder authorId(long val) {
            authorId = val;
            return this;
        }

        public Builder date(Instant val) {
            date = val;
            return this;
        }

        public Builder description(String val) {
            description = val;
            return this;
        }

        public Builder uri(URI val) {
            uri = val;
            return this;
        }

        @Contract(value = " -> new", pure = true)
        public @NotNull WtfEntry build() {
            return new WtfEntry(this);
        }
    }
}
