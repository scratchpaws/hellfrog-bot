package hellfrog.common;

import org.apache.http.ssl.TrustStrategy;

import java.security.cert.X509Certificate;

public class HttpClientSSLIgnoreStrategy implements TrustStrategy {

    @Override
    public boolean isTrusted(X509Certificate[] x509Certificates, String s) {
        return true;
    }
}
