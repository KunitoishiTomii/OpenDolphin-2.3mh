package open.dolphin.rest;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Named;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import open.dolphin.common.util.JsonConverter;
import open.dolphin.infomodel.ChartEventModel;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.mbean.ServletContextHolder;
import open.dolphin.session.ChartEventServiceBean;

/**
 * ChartEventEndpoint
 * 
 * @author masuda, Masuda Naika
 */
@Named
@ServerEndpoint("/ws/{fidUid}/{passwd}/{clientUUID}")
public class ServerChartEventEndpoint {
    
    private static final Logger logger = Logger.getLogger(ServerChartEventEndpoint.class.getSimpleName());

    @Inject
    private ChartEventServiceBean eventServiceBean;
    
    @Inject
    private ServletContextHolder contextHolder;
    
    
    @OnOpen
    public void onOpen(Session session, 
            @PathParam("fidUid") String fidUid,  
            @PathParam("passwd") String passwd, 
            @PathParam("clientUUID") String clientUUID) {
        
        Map<String, String> userMap = contextHolder.getUserMap();
        boolean auth = passwd.equals(userMap.get(fidUid));
        
        if (auth) {
            int pos = fidUid.indexOf(IInfoModel.COMPOSITE_KEY_MAKER);
            String fid = fidUid.substring(0, pos);
            session.getUserProperties().put("fid", fid);
            session.getUserProperties().put("clientUUID", clientUUID);
            contextHolder.getSessionList().add(session);
            logger.log(Level.INFO, "WebSocket authenticated: {0}", fidUid);
        } else {
            try {
                CloseReason cr = new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "Not authenticated.");
                session.close(cr);
                logger.log(Level.INFO, "WebSocket auth failed: {0}", fidUid);
            } catch (IOException ex) {
            }
        }
    }
    
    @OnClose
    public void onClose(Session session) {
        //logger.info("WebSocket onClose");
        contextHolder.getSessionList().remove(session);
    }
    
    @OnError
    public void onError(Session session, Throwable t) {
        //t.printStackTrace(System.err);
        contextHolder.getSessionList().remove(session);
    }
    
    @OnMessage
    public void onMessage(String json) {
        //logger.info("onMessage");
        ChartEventModel msg = (ChartEventModel)
                JsonConverter.getInstance().fromJson(json, ChartEventModel.class);

        eventServiceBean.processChartEvent(msg);
    }
}
