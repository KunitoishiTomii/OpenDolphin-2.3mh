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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import open.dolphin.client.Dolphin;
import open.dolphin.util.HashUtil;
import org.glassfish.jersey.client.ClientProperties;

/**
 * JerseyClient
 * @author Kazushi Minagawa. Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class JerseyClient {

    private static final JerseyClient instance;
    private static final String USER_NAME = "userName";
    private static final String PASSWORD = "password";
    private static final String CLIENT_UUID = "clientUUID";
    private static final int TIMEOUT1 = 30; // 30sec
    private static final String ENCODING = "UTF-8";
    
    private String clientUUID;
    private String baseURI;
    private String userName;
    private String password;
    
    // 非同期通信を分けるほうがよいのかどうか不明だが
    private Client client;
    private Client asyncClient;
    private WebTarget webTarget;
    private WebTarget asyncWebTarget;
    
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
        try {
            this.userName = username;
            this.password = hashPass ? passwd : HashUtil.MD5(passwd);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
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

    public Invocation.Builder buildRequest(String path, MultivaluedMap<String, String> qmap, MediaType mt) {
        
        WebTarget target = webTarget.path(path);
        
        // Jersey2.1、めんどくさいなぁ…
        if (qmap != null) {
            for (Map.Entry<String, List<String>> entry : qmap.entrySet()) {
                for (String value : entry.getValue()) {
                    target = target.queryParam(entry.getKey(), value);
                }
            }
        }
        
        Invocation.Builder builder = (mt != null)
                ? target.request(mt).acceptEncoding(ENCODING)
                : target.request();
        
        builder.header(USER_NAME, userName)
                .header(PASSWORD, password);
        
        return builder;
    }
    
    public Invocation.Builder buildAsyncRequest(String path, MediaType mt) {
        
        WebTarget target = asyncWebTarget.path(path);
        
        Invocation.Builder builder = (mt != null)
                ? target.request(mt).acceptEncoding(ENCODING)
                : target.request();
        
        builder.header(USER_NAME, userName)
                .header(PASSWORD, password)
                .header(CLIENT_UUID, clientUUID);
        
        return builder;
    }

    // オレオレSSL復活ｗ
    private void setupSslClients() {
        try {
            SSLContext ssl = SSLContext.getInstance("TLS");
            TrustManager[] certs = {new OreOreTrustManager()};
            ssl.init(null, certs, new SecureRandom());
            HostnameVerifier verifier = new OreOreHostnameVerifier();

            client =  ClientBuilder.newBuilder().sslContext(ssl).hostnameVerifier(verifier).build();
            asyncClient = ClientBuilder.newBuilder().sslContext(ssl).hostnameVerifier(verifier).build();
            
        } catch (Exception  ex) {
            client = ClientBuilder.newClient();
            asyncClient = ClientBuilder.newClient();
        }
        
        // 同期通信はタイムアウトを設定
        int readTimeout = TIMEOUT1 * 1000;
        client.property(ClientProperties.READ_TIMEOUT, readTimeout);
        
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
