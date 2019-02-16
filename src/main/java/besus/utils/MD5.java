package besus.utils;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by besus on 04.04.17.
 */
@SuppressWarnings("WeakerAccess")
public class MD5 {

    public static final Charset UTF_8 = StandardCharsets.UTF_8;
    private static MessageDigest md5;

    static {
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new UnsupportedOperationException("Cannot create md5 digest.", e);
        }
    }

    public static String of(String src) {
        return String.format("%x", of(src, UTF_8));
    }

    public static BigInteger of(String src, Charset charset) {
        return new BigInteger(1, digest(src.getBytes(charset)));
    }

    public static synchronized byte[] digest(byte[] bytes) {
        return md5.digest(bytes);
    }

    public static String of(String src, int padding, boolean uppercase, Charset charset) {
        return String.format("%0" + padding + (uppercase ? "X": "x"), of(src, charset));
    }

    public static String of(String src, int padding) {
        return of(src, padding, false, UTF_8);
    }
}
