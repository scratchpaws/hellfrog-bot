package hellfrog.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import hellfrog.commands.scenes.DuckDuckGoScenario;
import hellfrog.common.ddgentity.DDGSearchResult;
import hellfrog.common.ddgentity.DDGWebResult;
import hellfrog.common.ddgentity.DDGWebResults;
import hellfrog.settings.SettingsController;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DuckDuckGoSearch {

    private static final Logger log = LogManager.getLogger(DuckDuckGoSearch.class.getSimpleName());

    public static List<DDGSearchResult> searchInEngine(@NotNull final String searchQuery) throws OperationException {

        SimpleHttpClient client = SettingsController.getInstance()
                .getHttpClientsPool()
                .borrowClient();
        BroadCast.MessagesLogger messagesLogger = BroadCast.getLogger();
        try {
            URI nonJsPageQuery;
            try {
                nonJsPageQuery = new URIBuilder()
                        .setHost("duckduckgo.com")
                        .setScheme("https")
                        .addParameter("q", searchQuery)
                        .addParameter("t", "ffsb")
                        .build();
            } catch (URISyntaxException err) {
                String errMsg = String.format("DuckDuckGo URI build error: %s", err.getMessage());
                messagesLogger.addErrorMessage(errMsg);
                throw new OperationException(errMsg, "Internal bot error", err);
            }

            HttpGet request = new HttpGet(nonJsPageQuery);
            String responseText = executeHttpRequest(client, request, messagesLogger);
            if (CommonUtils.isTrStringEmpty(responseText)) {
                throw new OperationException("Empty DuckDuckGo search response");
            }

            int firstScriptLocation = responseText.indexOf("/t.js?q=");
            if (firstScriptLocation < 0) {
                throw new OperationException("No DuckDuckGo search results found");
            }
            int secondScriptLocation = responseText.indexOf("/d.js?q=", firstScriptLocation + 1);
            if (secondScriptLocation < 0) {
                throw new OperationException("No DuckDuckGo search results found");
            }

            String firstScriptQuery;
            String secondScriptQuery;

            try {
                firstScriptQuery = responseText.substring(firstScriptLocation,
                        responseText.indexOf("');", firstScriptLocation));
                secondScriptQuery = responseText.substring(secondScriptLocation,
                        responseText.indexOf("');", secondScriptLocation));
            } catch (IndexOutOfBoundsException err) {
                String errMsg = DuckDuckGoScenario.class.getSimpleName() + " parse error: " +
                        err.getMessage();
                messagesLogger.addErrorMessage(errMsg);
                throw new OperationException(errMsg, "Unable to parse DuckDuckGo search response", err);
            }

            URI firstScriptURI;
            URI secondScriptURI;
            try {
                firstScriptURI = new URI("https://duckduckgo.com" + firstScriptQuery);
                secondScriptURI = new URI("https://duckduckgo.com" + secondScriptQuery);
            } catch (URISyntaxException err) {
                String errMsg = DuckDuckGoScenario.class.getSimpleName() + " parse error: " +
                        err.getMessage();
                messagesLogger.addErrorMessage(errMsg);
                throw new OperationException(errMsg, "Unable to parse DuckDuckGo search response", err);
            }

            HttpGet firstScriptGet = new HttpGet(firstScriptURI);
            HttpGet secondScriptGet = new HttpGet(secondScriptURI);

            String firstResponse = executeHttpRequest(client, firstScriptGet, messagesLogger);
            String secondResponse = executeHttpRequest(client, secondScriptGet, messagesLogger);
            if (CommonUtils.isTrStringEmpty(firstResponse) && CommonUtils.isTrStringEmpty(secondResponse)) {
                throw new OperationException("No DuckDuckGo search results found");
            }

            responseText = CommonUtils.isTrStringEmpty(firstResponse) ? secondResponse : firstResponse;

            int innerJsJsonPosStart;
            int innerJsJsonPosEnd;
            String innerJsonText;
            try {
                innerJsJsonPosStart = responseText.indexOf("DDG.pageLayout.load('d',[");
                if (innerJsJsonPosStart < 0) {
                    throw new OperationException("No DuckDuckGo search results found");
                }
                innerJsJsonPosEnd = responseText.indexOf("]);", innerJsJsonPosStart);
                if (innerJsJsonPosEnd < 0) {
                    throw new OperationException("No DuckDuckGo search results found");
                }
                innerJsonText = "{\"items\" : ["
                        + responseText.substring(innerJsJsonPosStart + "DDG.pageLayout.load('d',[".length(), innerJsJsonPosEnd)
                        + "]}";
            } catch (IndexOutOfBoundsException err) {
                String errMsg = DuckDuckGoScenario.class.getSimpleName() + " parse error: " +
                        err.getMessage();
                messagesLogger.addErrorMessage(errMsg);
                throw new OperationException(errMsg, "Unable to parse DuckDuckGo search response", err);
            }

            ObjectMapper objectMapper = new ObjectMapper();
            DDGWebResults ddgWebResults;
            try {
                ddgWebResults = objectMapper.readValue(innerJsonText, DDGWebResults.class);
            } catch (Exception err) {
                String serviceMsg = String.format("Unable decode json \"%s\": %s", responseText,
                        err.getMessage());
                String userMsg = "Unable to parse search result";
                messagesLogger.addErrorMessage(serviceMsg);
                log.error(serviceMsg, err);
                throw new OperationException(serviceMsg, userMsg, err);
            }

            if (ddgWebResults.length() == 0) {
                throw new OperationException("No DuckDuckGo search results found");
            }

            List<DDGSearchResult> result = new ArrayList<>();
            for (int i = 0; i < ddgWebResults.length() && i < 3; i++) {
                DDGWebResult ddgWebResult = ddgWebResults.getItems()[i];
                String title = Jsoup.parse(ddgWebResult.getT()).text();
                String description = Jsoup.parse(ddgWebResult.getA()).text();
                String rawURI = Jsoup.parse(ddgWebResult.getU()).text();
                try {
                    URI url = new URI(rawURI);
                    result.add(new DDGSearchResult(url, title, description));
                } catch (URISyntaxException err) {
                    String errMsg = String.format("Unable to parse URI from DuckDuckGo response. Source URI: \"%s\". " +
                            "Error: %s", rawURI, err.getMessage());
                    log.error(errMsg, err);
                    messagesLogger.addErrorMessage(errMsg);
                }
            }

            return Collections.unmodifiableList(result);

        } finally {
            SettingsController.getInstance()
                    .getHttpClientsPool()
                    .returnClient(client);
            messagesLogger.send();
        }
    }

    private static String executeHttpRequest(@NotNull SimpleHttpClient client,
                                             @NotNull HttpUriRequest request,
                                             @NotNull BroadCast.MessagesLogger messagesLogger) throws OperationException {
        try (CloseableHttpResponse httpResponse = client.execute(request)) {
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                String message = String.format("DuckDuckGo Service HTTP error: %d", statusCode);
                messagesLogger.addErrorMessage(message);
                throw new OperationException(message);
            }
            HttpEntity entity = httpResponse.getEntity();
            try {
                if (entity == null) {
                    return "";
                } else {
                    return EntityUtils.toString(entity);
                }

            } finally {
                EntityUtils.consume(entity);
            }
        } catch (IOException err) {
            String userMsg = "Unable to send request to DuckDuckGo server: I/O error";
            String serviceMsg = String.format("Unable send request to DDG-server: %s", err.getMessage());
            log.error(serviceMsg, err);
            messagesLogger.addErrorMessage(serviceMsg);
            throw new OperationException(serviceMsg, userMsg, err);
        }
    }
}
