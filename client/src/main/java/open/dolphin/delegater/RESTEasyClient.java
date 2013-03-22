package open.dolphin.delegater;

import javax.ws.rs.core.MultivaluedMap;
import open.dolphin.client.Dolphin;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.util.HashUtil;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;

/**
 * RESTEasyClient
 * @author masuda, Masuda Naika
 */
public class RESTEasyClient {

    private String clientUUID;
    private String baseURI;
    private String fidUid;
    private String passMD5;
    
    private static final RESTEasyClient instance;
    
    static {
        instance = new RESTEasyClient();
    }

    private RESTEasyClient() {
        clientUUID = Dolphin.getInstance().getClientUUID();
    }

    public static RESTEasyClient getInstance() {
        return instance;
    }

    public void setUpAuthentication(String fidUid, String password, boolean hashPass) {

        this.fidUid = fidUid;
        this.passMD5 = hashPass ? password : HashUtil.MD5(password);
    }
    
    public String getBaseURI() {
        return baseURI;
    }

    public void setBaseURI(String uri) {
        baseURI = uri;
    }
    
    public String getPath(String path) {
        StringBuilder sb = new StringBuilder();
        sb.append(baseURI);
        if (!path.startsWith("/")) {
            sb.append("/");
        }
        sb.append(path);
        return sb.toString();
    }
    
    public ClientRequest getClientRequest(String path, MultivaluedMap<String, String> qmap) {

        ClientRequest request = getClientRequest(path);
        if (qmap != null) {
            request.getQueryParameters().putAll(qmap);
        }
        return request;
    }
    
    public ClientRequest getClientRequest(String path) {
        
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpParams params = httpClient.getParams();
        HttpConnectionParams.setConnectionTimeout(params, 30 * 1000);
        HttpConnectionParams.setSoTimeout(params, 0);
        ClientExecutor executor = new ApacheHttpClient4Executor(httpClient);
        
        ClientRequest request = new ClientRequest(getPath(path), executor);
        request.header(IInfoModel.USER_NAME, fidUid);
        request.header(IInfoModel.PASSWORD, passMD5);
        request.header(IInfoModel.CLIENT_UUID, clientUUID);
        
        return request;
    }
 
    private String[] splitFidUid(String username) {
        int pos = username.indexOf(IInfoModel.COMPOSITE_KEY_MAKER);
        String fid = username.substring(0, pos);
        String uid = username.substring(pos + 1);
        return new String[]{fid, uid};
    }
}
