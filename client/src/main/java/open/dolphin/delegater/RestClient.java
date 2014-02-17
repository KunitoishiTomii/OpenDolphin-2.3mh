package open.dolphin.delegater;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import open.dolphin.project.Project;
import open.dolphin.setting.MiscSettingPanel;
import open.dolphin.util.HashUtil;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

/**
 * RestClient
 * @author Kazushi Minagawa. Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class RestClient {

    private static final RestClient instance;
    
    private static final int TIMEOUT_IN_MILLISEC = 30 * 1000; // 30sec
    
    private String baseURI;
    
    // 非同期通信を分けるほうがよいのかどうか不明だが
    private Client client;
    private Client asyncClient;
    private WebTarget webTarget;
    private WebTarget asyncWebTarget;
    
    // Authentication filter
    private DolphinAuthFilter authFilter;
    
    static {
        instance = new RestClient();
    }

    private RestClient() {
        setupSslClients();
    }

    public static RestClient getInstance() {
        return instance;
    }

    public void setUpAuthentication(String username, String passwd, boolean hashPass) {
        String password = hashPass ? passwd : HashUtil.MD5(passwd);
        authFilter.setUserName(username);
        authFilter.setPassword(password);
    }

    public String getBaseURI() {
        return baseURI;
    }

    public void setBaseURI(String uri) {

        String oldURI = baseURI;
        baseURI = uri;

        if (baseURI == null || baseURI.equals(oldURI)) {
            return;
        }
        
        webTarget = client.target(baseURI);
        asyncWebTarget = asyncClient.target(baseURI);
    }
    
    public WebTarget getWebTarget() {
        return webTarget;
    }
    
    public WebTarget getAsyncWebTarget() {
        return asyncWebTarget;
    }

    
    // オレオレSSL復活ｗ
    private void setupSslClients() {
        
        boolean useJersey = Project.getBoolean(MiscSettingPanel.USE_JERSEY, MiscSettingPanel.DEFAULT_USE_JERSEY);

        try {
            SSLContext ssl = SSLContext.getInstance("TLS");
            TrustManager[] certs = {new OreOreTrustManager()};
            ssl.init(null, certs, new SecureRandom());
            HostnameVerifier verifier = new OreOreHostnameVerifier();
            
            client = createClient(useJersey, ssl, verifier, true);
            asyncClient = createClient(useJersey, ssl, verifier, false);

        } catch (KeyManagementException | NoSuchAlgorithmException ex) {
            client = createClient(useJersey, null, null, true);
            asyncClient = createClient(useJersey, null, null, false);
        }

        // DolphnAuthFilterを設定する
        authFilter = new DolphinAuthFilter();
        client.register(authFilter);
        asyncClient.register(authFilter);
    }
    
    // Clientを作成する
    private Client createClient(boolean useJersey, SSLContext ssl, HostnameVerifier verifier, boolean async) {

        Client ret = useJersey
                ? createJerseyClient(ssl, verifier, async)
                : createResteasyClient(ssl, verifier, async);
        
        return ret;
    }
    
    // JerseyClientを作成する
    private Client createJerseyClient(SSLContext ssl, HostnameVerifier verifier, boolean async) {
        
        JerseyClientBuilder builder = new JerseyClientBuilder();
        
        if (ssl != null && verifier != null) {
            builder = builder.sslContext(ssl).hostnameVerifier(verifier);
        }
        if (async) {
            builder.getConfiguration().property(ClientProperties.READ_TIMEOUT, TIMEOUT_IN_MILLISEC);
        }
        
        return builder.build();
    }
    
    // ResteasyClientを作成する
    private Client createResteasyClient(SSLContext ssl, HostnameVerifier verifier, boolean async) {
        
        ResteasyClientBuilder builder = new ResteasyClientBuilder();
        
        if (ssl != null && verifier != null) {
            int poolSize = async ? 1 : 3;
            builder.connectionPoolSize(poolSize);
            builder.disableTrustManager();
        }
        if (async) {
            builder = builder.socketTimeout(TIMEOUT_IN_MILLISEC, TimeUnit.MILLISECONDS);
        }
        
        return builder.build();
    }

    private static class OreOreHostnameVerifier implements HostnameVerifier {

        @Override
        public boolean verify(String string, SSLSession ssls) {
            return true;
        }
        
    }
    
    private static class OreOreTrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }
}
