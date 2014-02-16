package open.dolphin.delegater;

import java.io.IOException;
import java.net.URI;
import javax.websocket.OnMessage;
import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import open.dolphin.client.ChartEventListener;
import open.dolphin.client.Dolphin;
import open.dolphin.common.util.JsonConverter;
import open.dolphin.infomodel.ChartEventModel;
import open.dolphin.project.Project;

/**
 *
 * @author masuda
 */
@ClientEndpoint
public class ClientChartEventEndpoint {
    
    private Session wsSession;
    
    public void init() throws Exception {

        String clientUUID  = Dolphin.getInstance().getClientUUID();
        String fidUid = Project.getUserModel().getUserId();
        String passwd = Project.getUserModel().getPassword();
        String baseURI = Project.getString(Project.SERVER_URI);
        int pos = baseURI.indexOf(":");
        StringBuilder sb = new StringBuilder();
        //sb.append("ws").append(baseURI.substring(pos)).append("/dolphin/ws/");
        sb.append("wss://192.168.24.53:8443").append("/dolphin/ws/");
        sb.append(fidUid).append("/").append(passwd).append("/").append(clientUUID);
        URI uri = new URI(sb.toString());
        WebSocketContainer c = ContainerProvider.getWebSocketContainer();
        wsSession = c.connectToServer(new ClientChartEventEndpoint(), uri);
    }
    
    public void close() {
        try {
            CloseReason cr = new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "end client");
            wsSession.close(cr);
        } catch (IOException ex) {
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
