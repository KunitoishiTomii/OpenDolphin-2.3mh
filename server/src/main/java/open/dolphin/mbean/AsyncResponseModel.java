package open.dolphin.mbean;

import javax.ws.rs.container.AsyncResponse;

/**
 * CometのAsyncResponseを記録するコンテナ
 * 
 * @author masuda, Masuda Naika
 */
public class AsyncResponseModel {
    
    private AsyncResponse asyncResponse;
    
    private String clientUUID;
    
    private String fid;
    
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
    
    public void setAsyncResponse(AsyncResponse asyncResponse) {
        this.asyncResponse = asyncResponse;
    }
    public void setClientUUID(String clientUUID) {
        this.clientUUID = clientUUID;
    }
    public void setFid(String fid) {
        this.fid = fid;
    }
}
