package open.dolphin.delegater;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MultivaluedMap;
import open.dolphin.client.Dolphin;
import open.dolphin.project.Project;
import open.dolphin.setting.MiscSettingPanel;
import open.dolphin.util.HashUtil;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;

/**
 * RestClient
 * @author Kazushi Minagawa. Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class RestClient {

    private static final RestClient instance;
    
    private static final String CLIENT_UUID = "clientUUID";
    private static final int TIMEOUT_IN_MILLISEC = 30 * 1000; // 30sec
    
    private final String clientUUID;
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
        clientUUID = Dolphin.getInstance().getClientUUID();
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

    public Invocation.Builder buildRequest(String path, MultivaluedMap<String, String> qmap) {
        
        WebTarget target = webTarget.path(path);
        
        // Jersey2.1、めんどくさいなぁ…
        if (qmap != null) {
            for (Map.Entry<String, List<String>> entry : qmap.entrySet()) {
                for (String value : entry.getValue()) {
                    target = target.queryParam(entry.getKey(), value);
                }
            }
        }
        
        Invocation.Builder builder = target.request();
        
        return builder;
    }
    
    public Invocation.Builder buildAsyncRequest(String path) {
    
        WebTarget target = asyncWebTarget.path(path);
        Invocation.Builder builder = target.request();
        // cometはclientUUIDもセットする
        builder.header(CLIENT_UUID, clientUUID);
        
        return builder;
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
    private Client createClient(boolean useJersey, SSLContext ssl, HostnameVerifier verifier, boolean enableTimeout) {

        Client ret = useJersey
                ? createJerseyClient(ssl, verifier, enableTimeout)
                : createResteasyClient(ssl, verifier, enableTimeout);
        
        return ret;
    }
    
    // JerseyClientを作成する
    private Client createJerseyClient(SSLContext ssl, HostnameVerifier verifier, boolean enableTimeout) {
        
        JerseyClient jerseyClient = (ssl != null && verifier != null)
                ? new JerseyClientBuilder().sslContext(ssl).hostnameVerifier(verifier).build()
                : new JerseyClientBuilder().build();
        
        // read timeoutを設定する
        if (enableTimeout) {
            jerseyClient.getConfiguration().property(ClientProperties.READ_TIMEOUT, TIMEOUT_IN_MILLISEC);
        }
        
        return jerseyClient;
    }
    
    // ResteasyClientを作成する
    private Client createResteasyClient(SSLContext ssl, HostnameVerifier verifier, boolean enableTimeout) {
        
        ResteasyClient resteasyClient = (ssl != null && verifier != null)
                ? new ResteasyClientBuilder().sslContext(ssl).hostnameVerifier(verifier).build()
                : new ResteasyClientBuilder().build();
        
        // read timeoutを設定する、めんどくちゃ
        if (enableTimeout && resteasyClient.httpEngine() instanceof ApacheHttpClient4Engine) {
            ApacheHttpClient4Engine engine = (ApacheHttpClient4Engine) resteasyClient.httpEngine();
            HttpParams params = engine.getHttpClient().getParams();
            HttpConnectionParams.setConnectionTimeout(params, TIMEOUT_IN_MILLISEC);
        }

        return resteasyClient;
    }

    private class OreOreHostnameVerifier implements HostnameVerifier {

        @Override
        public boolean verify(String string, SSLSession ssls) {
            return true;
        }
        
    }
    
    private class OreOreTrustManager implements X509TrustManager {

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
