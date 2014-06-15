package open.dolphin.delegater;

import java.io.IOException;
import java.net.URI;
import javax.net.ssl.SSLContext;
import javax.websocket.OnMessage;
import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.Session;
import open.dolphin.client.ChartEventListener;
import open.dolphin.client.Dolphin;
import open.dolphin.common.util.JsonConverter;
import open.dolphin.infomodel.ChartEventModel;
import open.dolphin.project.Project;
import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.container.jdk.client.JdkClientContainer;
import org.glassfish.tyrus.client.SslEngineConfigurator;
//import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
//import org.glassfish.tyrus.client.ClientManager;
//import javax.websocket.ContainerProvider;
//import javax.websocket.WebSocketContainer;
//import io.undertow.websockets.jsr.DefaultWebSocketClientSslProvider;

/**
 * ClientChartEventEndpoint
 *
 * @author masuda, Masuda Naika
 */
@ClientEndpoint
public class ClientChartEventEndpoint {

    private Session wsSession;

    public void connect() throws Exception {

        String clientUUID = Dolphin.getInstance().getClientUUID();
        String fidUid = Project.getUserModel().getUserId();
        String passwd = Project.getUserModel().getPassword();
        String baseURI = Project.getString(Project.SERVER_URI);
        boolean useSSL = baseURI.toLowerCase().startsWith("https");
        int pos = baseURI.indexOf(":");

        StringBuilder sb = new StringBuilder();
        sb.append(useSSL ? "wss" : "ws");
        sb.append(baseURI.substring(pos)).append("/dolphin/ws/");
        sb.append(fidUid).append("/").append(passwd).append("/").append(clientUUID);
        URI uri = URI.create(sb.toString());
        
        // tyrus JDK7 client
        ClientManager client = ClientManager.createClient(JdkClientContainer.class.getName());
        if (useSSL) {
            SSLContext ssl = OreOreSSL.getSslContext();
            SslEngineConfigurator sslConfig = new SslEngineConfigurator(ssl, true, false, false);
            client.getProperties().put(ClientManager.SSL_ENGINE_CONFIGURATOR, sslConfig);
        }
        wsSession = client.connectToServer(this, uri);
/*
        // tyrus grizzly
        ClientManager client = ClientManager.createClient();
        if (useSSL) {
            SSLContext ssl = OreOreSSL.getSslContext();
            SSLEngineConfigurator sslConfig = new SSLEngineConfigurator(ssl, true, false, false);
            client.getProperties().put(ClientManager.SSL_ENGINE_CONFIGURATOR, sslConfig);
        }
        wsSession = client.connectToServer(this, uri);
/*
/*
        // undertow.websocket-jsr
        if (useSSL) {
            SSLContext ssl = OreOreSSL.getSslContext();
            DefaultWebSocketClientSslProvider.setSslContext(ssl);
        }
        WebSocketContainer c = ContainerProvider.getWebSocketContainer();
        wsSession = c.connectToServer(this, uri);
        
*/
    }

    public void close() {
        try {
            CloseReason cr = new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "end client");
            wsSession.close(cr);
        } catch (IOException | IllegalStateException ex) {
        }
    }

    public void putChartEvent(ChartEventModel evt) throws IOException {
        String json = JsonConverter.getInstance().toJson(evt);
        wsSession.getBasicRemote().sendText(json);
    }

    @OnMessage
    public void onMessage(String json) {
        ChartEventListener.getInstance().onWebSocketMessage(json);
    }
}
