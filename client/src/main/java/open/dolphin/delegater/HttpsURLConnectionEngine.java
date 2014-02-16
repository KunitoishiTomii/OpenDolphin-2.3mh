package open.dolphin.delegater;

import org.jboss.resteasy.client.jaxrs.internal.ClientInvocation;
import java.io.IOException;
import java.net.HttpURLConnection;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import org.jboss.resteasy.client.jaxrs.engines.URLConnectionEngine;

/**
 * HttpsURLConnectionEngine
 * 
 * @author masuda, Masuda Naika
 */
public class HttpsURLConnectionEngine extends URLConnectionEngine {
    
    public HttpsURLConnectionEngine(SSLContext ssl, HostnameVerifier verifier) {
        setSslContext(ssl);
        setHostnameVerifier(verifier);
    }

    @Override
    protected HttpURLConnection createConnection(final ClientInvocation request) throws IOException {
        
        HttpURLConnection connection = (HttpURLConnection) request.getUri().toURL().openConnection();
        
        // sslを設定する
        if (connection instanceof HttpsURLConnection
                && sslContext != null && hostnameVerifier != null) {
            
            HttpsURLConnection con = (HttpsURLConnection) connection;
            con.setSSLSocketFactory(sslContext.getSocketFactory());
            con.setHostnameVerifier(hostnameVerifier);
        }
        
        connection.setRequestMethod(request.getMethod());

        return connection;
    }
}
