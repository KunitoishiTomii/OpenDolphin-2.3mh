package open.dolphin.delegater;

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
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MultivaluedMap;
import open.dolphin.client.Dolphin;
import open.dolphin.project.Project;
import open.dolphin.setting.MiscSettingPanel;
import open.dolphin.util.HashUtil;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

/**
 * JerseyClient
 * @author Kazushi Minagawa. Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class JerseyClient {

    private static final JerseyClient instance;
    
    private static final String CLIENT_UUID = "clientUUID";
    private static final int TIMEOUT1 = 30; // 30sec
    
    private String clientUUID;
    private String baseURI;
    
    // 非同期通信を分けるほうがよいのかどうか不明だが
    private Client client;
    private Client asyncClient;
    private WebTarget webTarget;
    private WebTarget asyncWebTarget;
    
    // Authentication filter
    private DolphinAuthFilter authFilter;
    
    static {
        instance = new JerseyClient();
    }

    private JerseyClient() {
        clientUUID = Dolphin.getInstance().getClientUUID();
        setupSslClients();
    }

    public static JerseyClient getInstance() {
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
            
            client = createNewBuilder(useJersey).sslContext(ssl).hostnameVerifier(verifier).build();
            asyncClient = createNewBuilder(useJersey).sslContext(ssl).hostnameVerifier(verifier).build();

        } catch (Exception ex) {
            client = createNewBuilder(useJersey).build();
            asyncClient = createNewBuilder(useJersey).build();
        }

        // 同期通信はタイムアウトを設定(Jerseyのみ。RESTEasyはやり方わからず…)
        int readTimeout = TIMEOUT1 * 1000;
        client.property(ClientProperties.READ_TIMEOUT, readTimeout);
        
        // DolphnAuthFilterを設定する
        authFilter = new DolphinAuthFilter();
        client.register(authFilter);
        asyncClient.register(authFilter);
    }
    
    private ClientBuilder createNewBuilder(boolean useJersey) {

        ClientBuilder clientBuilder = useJersey
                ? new JerseyClientBuilder()
                : new ResteasyClientBuilder();
        return clientBuilder;
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
