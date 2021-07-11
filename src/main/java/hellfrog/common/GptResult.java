package hellfrog.common;

import org.jetbrains.annotations.NotNull;

public class GptResult {

    private final String resultText;
    private final String footerText;

    GptResult(@NotNull final String resultText,
              @NotNull final String footerText) {
        this.resultText = resultText;
        this.footerText = footerText;
    }

    public String getResultText() {
        return resultText;
    }

    public String getFooterText() {
        return footerText;
    }

    @Override
    public String toString() {
        return "GptResult{" +
                "resultText='" + resultText + '\'' +
                ", footerText='" + footerText + '\'' +
                '}';
    }
}
