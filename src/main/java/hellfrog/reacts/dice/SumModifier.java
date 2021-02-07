package hellfrog.reacts.dice;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class SumModifier {

    private final long value;
    private final ModifierType modifierType;
    private final String printable;

    @Contract(pure = true)
    public SumModifier(long value, @NotNull ModifierType modifierType) {
        this.value = value;
        this.modifierType = modifierType;
        this.printable = switch (modifierType) {
            case ADD -> "+" + value;
            case SUB -> "-" + value;
            case MUL -> "x" + value;
        };
    }

    public long modify(final long currentSum) {
        if (value > 0) {
            return switch (modifierType) {
                case ADD -> currentSum + value;
                case SUB -> currentSum - value;
                case MUL -> currentSum * value;
            };
        }
        return currentSum;
    }

    public String getPrintable() {
        return printable;
    }
}
