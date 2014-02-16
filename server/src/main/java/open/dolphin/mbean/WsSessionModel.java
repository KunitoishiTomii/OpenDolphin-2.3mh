package open.dolphin.mbean;

import javax.websocket.Session;

/**
 * WsSessionModel
 * 
 * @author masuda, Masuda Naika
 */
public class WsSessionModel {
    
    private final Session session;
    
    private final String clientUUID;
    
    private final String fid;
    
    public WsSessionModel(Session session, String fid, String clientUUID) {
        this.session = session;
        this.fid = fid;
        this.clientUUID = clientUUID;
    }
    
    public Session getWsSession() {
        return session;
    }
    public String getClientUUID() {
        return clientUUID;
    }
    public String getFid() {
        return fid;
    }
}
