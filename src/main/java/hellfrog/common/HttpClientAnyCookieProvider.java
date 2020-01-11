package hellfrog.common;

import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.protocol.HttpContext;

public class HttpClientAnyCookieProvider
    implements CookieSpecProvider {

    @Override
    public CookieSpec create(HttpContext httpContext) {
        return new HttpClientAnyCookieSpec();
    }
}
