package hellfrog.common;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SimpleHttpClient
    implements Closeable {

    private final HttpClientContext httpClientContext;
    private final CloseableHttpClient closeableHttpClient;
    private final Logger log = LogManager.getLogger(SimpleHttpClient.class.getSimpleName());

    public SimpleHttpClient() {
        httpClientContext = HttpClientContext.create();
        Registry<CookieSpecProvider> registry = RegistryBuilder.<CookieSpecProvider>create()
                .register("easy", new HttpClientAnyCookieProvider())
                .build();
        RequestConfig requestConfig = RequestConfig.custom()
                .setCookieSpec("easy")
                .build();
        httpClientContext.setRequestConfig(requestConfig);
        httpClientContext.setCookieSpecRegistry(registry);
        CookieStore cookieStore = new BasicCookieStore();
        httpClientContext.setCookieStore(cookieStore);
        CloseableHttpClient httpClient;
        try {
            SSLContext sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial(new HttpClientSSLIgnoreStrategy())
                    .build();
            HostnameVerifier allowAllHosts = new NoopHostnameVerifier();
            SSLConnectionSocketFactory connectionFactory = new SSLConnectionSocketFactory(sslContext, allowAllHosts);
            httpClient = HttpClients.custom()
                    .setDefaultHeaders(buildDefaultHeaders())
                    .setSSLSocketFactory(connectionFactory)
                    .build();
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException err) {
            String errMsg = String.format("Unable to set SSL certificate ignoration context: %s", err.getMessage());
            log.fatal(errMsg, err);
            httpClient = HttpClients.custom()
                    .setDefaultHeaders(buildDefaultHeaders())
                    .build();
        }
        this.closeableHttpClient = httpClient;
    }

    public CloseableHttpResponse execute(final HttpUriRequest request)
            throws IOException {
        return closeableHttpClient.execute(request, httpClientContext);
    }

    public List<URI> getRedirectLocations() {
        return httpClientContext.getRedirectLocations();
    }

    public HttpHost getTargetHost() {
        return httpClientContext.getTargetHost();
    }

    public URI getLatestURI(@NotNull URI requestURI) throws URISyntaxException {
        HttpHost target = httpClientContext.getTargetHost();
        List<URI> redirectLocations = httpClientContext.getRedirectLocations();
        return URIUtils.resolve(requestURI, target, redirectLocations);
    }

    public URI getLatestURI(@NotNull HttpUriRequest request) throws URISyntaxException {
        return getLatestURI(request.getURI());
    }

    @Override
    public void close() {
        try {
            closeableHttpClient.close();
        } catch (Exception ignore) {}
    }

    @NotNull
    private Collection<Header> buildDefaultHeaders() {
        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/79.0.3945.117 Safari/537.36"));
        headers.add(new BasicHeader("DNT","1"));
        headers.add(new BasicHeader("Accept-Language","ru-RU,ru;q=0.9"));
        return headers;
    }
}
