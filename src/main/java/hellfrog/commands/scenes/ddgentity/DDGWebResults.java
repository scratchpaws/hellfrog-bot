package hellfrog.commands.scenes.ddgentity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Arrays;
import java.util.Collections;

/**
 * DuckDuckGo webpage result (reverse engineering)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DDGWebResults {

    private DDGWebResult[] items;

    public DDGWebResult[] getItems() {
        return items != null ? items : new DDGWebResult[0];
    }

    public void setItems(DDGWebResult[] items) {
        this.items = items;
    }

    @JsonIgnore
    public int length() {
        return items != null ? items.length : 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DDGWebResults that = (DDGWebResults) o;
        return Arrays.equals(items, that.items);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(items);
    }

    @Override
    public String toString() {
        return "DDGWebResults{" +
                "items=" + Arrays.toString(items) +
                '}';
    }
}
