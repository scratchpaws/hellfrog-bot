package hellfrog.common;

import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.impl.cookie.DefaultCookieSpec;

public class HttpClientAnyCookieSpec
    extends DefaultCookieSpec {

    @Override
    public void validate(Cookie cookie, CookieOrigin origin) {
    }
}
