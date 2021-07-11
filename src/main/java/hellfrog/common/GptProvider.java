package hellfrog.common;

import java.util.concurrent.CompletableFuture;

public interface GptProvider {

    CompletableFuture<GptResult> appendText(final String sourceText);
}
