package open.dolphin.delegater;

import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
//import java.util.concurrent.TimeUnit;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import open.dolphin.project.Project;
import open.dolphin.setting.MiscSettingPanel;
import open.dolphin.util.HashUtil;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.JerseyClientBuilder;
//import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

/**
 * RestClient
 * @author Kazushi Minagawa. Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class RestClient {

    private static final RestClient instance;
    
    private static final int TIMEOUT_IN_MILLISEC = 60 * 1000; // 60sec
    
    private String baseURI;
    
    private final boolean useJersey;
    private final boolean useComet;
    
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
        
        useJersey = Project.getBoolean(MiscSettingPanel.USE_JERSEY, MiscSettingPanel.DEFAULT_USE_JERSEY);
        useComet = !Project.getBoolean(MiscSettingPanel.USE_WEBSOCKET, MiscSettingPanel.DEFAULT_USE_WEBSOCKET);
        
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
/*
    public String getBaseURI() {
        return baseURI;
    }
*/
    public void setBaseURI(String strUri) {

        String oldURI = baseURI;
        baseURI = strUri;

        if (baseURI == null || baseURI.equals(oldURI)) {
            return;
        }
        
        URI uri = URI.create(baseURI);
        webTarget = client.target(uri);
        if (useComet) {
            asyncWebTarget = asyncClient.target(uri);
        }
    }
    
    public WebTarget getWebTarget() {
        return webTarget;
    }
    
    public WebTarget getAsyncWebTarget() {
        return asyncWebTarget;
    }

    
    // オレオレSSL復活ｗ
    private void setupSslClients() {
        
        // DolphnAuthFilterを設定する
        authFilter = new DolphinAuthFilter();
        
        try {
            SSLContext ssl = OreOreSSL.getSslContext();
            HostnameVerifier verifier = OreOreSSL.getVerifier();
            client = createClient(ssl, verifier, false);
            client.register(authFilter);
            if (useComet) {
                asyncClient = createClient(ssl, verifier, true);
                asyncClient.register(authFilter);
            }

        } catch (KeyManagementException | NoSuchAlgorithmException ex) {
            client = createClient(null, null, false);
            client.register(authFilter);
            if (useComet) {
                asyncClient = createClient(null, null, true);
                asyncClient.register(authFilter);
            }
        }
    }
    
    // Clientを作成する
    private Client createClient(SSLContext ssl, HostnameVerifier verifier, boolean async) {

        Client ret = useJersey
                ? createJerseyClient(ssl, verifier, async)
                : createResteasyClient(ssl, verifier, async);
*/
        Client ret = createJerseyClient(ssl, verifier, async);
        return ret;
    }
    
    // JerseyClientを作成する
    private Client createJerseyClient(SSLContext ssl, HostnameVerifier verifier, boolean async) {
        
        JerseyClientBuilder builder = new JerseyClientBuilder();
        
        if (ssl != null && verifier != null) {
            builder.sslContext(ssl).hostnameVerifier(verifier);
        }
        if (!async) {
            builder.getConfiguration().property(ClientProperties.READ_TIMEOUT, TIMEOUT_IN_MILLISEC);
        }
        
        return builder.build();
    }
    
/*
    // ResteasyClientを作成する
    private Client createResteasyClient(SSLContext ssl, HostnameVerifier verifier, boolean async) {
        
        ResteasyClientBuilder builder = new ResteasyClientBuilder();
        
        if (ssl != null && verifier != null) {
            //builder.disableTrustManager();
            builder.sslContext(ssl).hostnameVerifier(verifier);
        }
        if (!async) {
            builder.connectionPoolSize(20);
            builder.socketTimeout(TIMEOUT_IN_MILLISEC, TimeUnit.MILLISECONDS);
        }
        
        return builder.build();
    }
}
