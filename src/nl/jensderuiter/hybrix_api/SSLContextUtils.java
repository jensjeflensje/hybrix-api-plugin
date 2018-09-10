package nl.jensderuiter.hybrix_api;


import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.security.KeyStore;

public class SSLContextUtils {
	private static final String MANAGER_ALGORITHM = "SunX509";

    public static SSLContext createSSLContext(
            InputStream keyStore, String keyPassword,
            InputStream trustStore, String trustPassword) {
        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            char[] pw = keyPassword.toCharArray();
            ks.load(keyStore, pw);
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(MANAGER_ALGORITHM);
            kmf.init(ks, pw);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(MANAGER_ALGORITHM);
            KeyStore ts = KeyStore.getInstance("JKS");
            ts.load(trustStore, trustPassword.toCharArray());
            tmf.init(ks);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            return sslContext;

        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
