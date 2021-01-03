package hellfrog.common;

import hellfrog.common.worldometers.CovidStatistic;
import hellfrog.common.worldometers.TwoLettersCountries;
import hellfrog.settings.SettingsController;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class WorldometersParser {

    private static final String SITE_PAGE = "https://www.worldometers.info";
    private static final String COVID_PAGE = SITE_PAGE + "/coronavirus/";
    private static final Logger log = LogManager.getLogger("Worldometers Parser");
    private static final ReentrantLock covidStatisticUpdateLock = new ReentrantLock();

    private static volatile Map<String, CovidStatistic> actualCovidStatistic = null;
    private static volatile Instant covidStatisticLastUpdate = null;

    public static Map<String, CovidStatistic> getActualCovidStatistic() {
        if (requiredUpdate()) {
            covidStatisticUpdateLock.lock();
            try {
                if (requiredUpdate()) {
                    actualCovidStatistic = parseCoronavirusPage();
                    covidStatisticLastUpdate = Instant.now();
                }
            } finally {
                covidStatisticUpdateLock.unlock();
            }
        }
        return actualCovidStatistic;
    }

    public static String getCovidPage() {
        return COVID_PAGE;
    }

    private static boolean requiredUpdate() {
        return actualCovidStatistic == null
                || actualCovidStatistic.isEmpty()
                || covidStatisticLastUpdate == null
                || Math.abs(Duration.between(covidStatisticLastUpdate, Instant.now()).toHours()) >= 4.0;
    }

    private static Map<String, CovidStatistic> parseCoronavirusPage() {

        SimpleHttpClient httpClient = SettingsController.getInstance().getHttpClientsPool().borrowClient();
        BroadCast.MessagesLogger messagesLogger = BroadCast.getLogger();
        try {
            HttpGet pageGet = new HttpGet(COVID_PAGE);
            String responseText;
            try (CloseableHttpResponse httpResponse = httpClient.execute(pageGet)) {
                HttpEntity entity = httpResponse.getEntity();
                if (entity == null) {
                    responseText = "";
                } else {
                    try {
                        responseText = EntityUtils.toString(entity);
                    } finally {
                        EntityUtils.consume(entity);
                    }
                }
                int statusCode = httpResponse.getStatusLine().getStatusCode();
                if (statusCode != HttpStatus.SC_OK) {
                    String errMsg = String.format("Unable load page %s: HTTP code: %d", COVID_PAGE, statusCode);
                    log.error(errMsg);
                    messagesLogger.addErrorMessage(errMsg);
                    return Collections.emptyMap();
                }
            } catch (Exception err) {
                String errMsg = String.format("Unable load page %s: %s", COVID_PAGE, err.getMessage());
                log.error(errMsg, err);
                messagesLogger.addErrorMessage(errMsg);
                return Collections.emptyMap();
            }

            Document htmlDocument = Jsoup.parse(responseText, COVID_PAGE);
            Elements allRows = htmlDocument.getElementsByTag("tr");
            if (allRows != null && !allRows.isEmpty()) {

                Map<String, CovidStatistic> result = new HashMap<>();
                StringBuilder unknownCountries = new StringBuilder();

                for (Element row : allRows) {
                    Elements cells = row.getElementsByTag("td");
                    if (cells != null && cells.size() == 19) {
                        try {
                            String countryName = cells.get(1).text();
                            if (TwoLettersCountries.IGNORE_CODES.contains(countryName)) {
                                continue;
                            }
                            if (CommonUtils.isTrStringEmpty(countryName)) {
                                continue;
                            }
                            if (TwoLettersCountries.COUNTRIES_MAP_REVERSE.containsKey(countryName)) {
                                long totalCases = CommonUtils.onlyNumbersToLong(cells.get(2).text());
                                long newCases = CommonUtils.onlyNumbersToLong(cells.get(3).text());
                                long totalDeaths = CommonUtils.onlyNumbersToLong(cells.get(4).text());
                                long newDeaths = CommonUtils.onlyNumbersToLong(cells.get(5).text());
                                long totalRecovered = CommonUtils.onlyNumbersToLong(cells.get(6).text());
                                long activeCases = CommonUtils.onlyNumbersToLong(cells.get(8).text());
                                long seriousCritical = CommonUtils.onlyNumbersToLong(cells.get(9).text());
                                long topCases1MPop = CommonUtils.onlyNumbersToLong(cells.get(10).text());
                                long deaths1MPop = CommonUtils.onlyNumbersToLong(cells.get(11).text());
                                long totalTests = CommonUtils.onlyNumbersToLong(cells.get(12).text());
                                long tests1MPop = CommonUtils.onlyNumbersToLong(cells.get(13).text());
                                long population = CommonUtils.onlyNumbersToLong(cells.get(14).text());
                                CovidStatistic covidStatistic = new CovidStatistic(countryName, totalCases,
                                        newCases, totalDeaths, newDeaths, totalRecovered, activeCases, seriousCritical,
                                        topCases1MPop, deaths1MPop, totalTests, tests1MPop, population);
                                result.put(TwoLettersCountries.COUNTRIES_MAP_REVERSE.get(countryName), covidStatistic);
                            } else {
                                unknownCountries.append(countryName).append(", ");
                            }
                        } catch (Exception err) {
                            String errMsg = String.format("Error while parse page from %s: %s", COVID_PAGE, err.getMessage());
                            messagesLogger.addErrorMessage(errMsg);
                            log.error(errMsg, err);
                        }
                    }
                }

                if (!unknownCountries.isEmpty()) {
                    log.warn("Unable to find countries: {}", unknownCountries.toString());
                }

                return Collections.unmodifiableMap(result);
            } else {
                String warnMsg = String.format("Warn: page %s return result that cannot be parsed", COVID_PAGE);
                messagesLogger.addWarnMessage(warnMsg);
            }
        } finally {
            SettingsController.getInstance().getHttpClientsPool().returnClient(httpClient);
            messagesLogger.send();
        }
        return Collections.emptyMap();
    }
}
