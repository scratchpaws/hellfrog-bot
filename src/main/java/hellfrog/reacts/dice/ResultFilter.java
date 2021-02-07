package hellfrog.reacts.dice;

import org.jetbrains.annotations.Contract;

public class ResultFilter {

    private final FilterType filterType;
    private final long filterValue;

    @Contract(pure = true)
    public ResultFilter(FilterType filterType, long filterValue) {
        this.filterType = filterType != null ? filterType : FilterType.NOP;
        this.filterValue = filterValue;
    }

    public boolean isOk(final long value) {
        if (filterValue > 0) {
            return switch (filterType) {
                case EQ -> value == filterValue;
                case LT -> value < filterValue;
                case GT -> value > filterValue;
                case LE -> value <= filterValue;
                case GE -> value >= filterValue;
                case NE -> value != filterValue;
                case NOP -> true;
            };
        }
        return true;
    }

    public boolean notOk(final long value) {
        return !isOk(value);
    }
}
