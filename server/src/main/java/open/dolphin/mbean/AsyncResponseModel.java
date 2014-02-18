package open.dolphin.mbean;

import javax.ws.rs.container.AsyncResponse;

/**
 * CometのAsyncResponseを記録するコンテナ
 * 
 * @author masuda, Masuda Naika
 */
public class AsyncResponseModel {
    
    private final AsyncResponse asyncResponse;
    
    private final String clientUUID;
    
    private final String fid;
    
    public AsyncResponseModel(AsyncResponse asyncResponse, String fid, String clientUUID) {
        this.asyncResponse = asyncResponse;
        this.fid = fid;
        this.clientUUID = clientUUID;
    }
    
    public AsyncResponse getAsyncResponse() {
        return asyncResponse;
    }
    public String getClientUUID() {
        return clientUUID;
    }
    public String getFid() {
        return fid;
    }
}
