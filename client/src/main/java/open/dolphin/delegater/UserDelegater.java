package open.dolphin.delegater;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.InputStream;
import java.util.List;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.infomodel.UserModel;
import open.dolphin.project.Project;

/**
 * User 関連の Business Delegater　クラス。
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class UserDelegater extends BusinessDelegater {

    private static final String RES_USER = "user/";
    
    private static final boolean debug = false;
    private static final UserDelegater instance;

    static {
        instance = new UserDelegater();
    }

    public static UserDelegater getInstance() {
        return instance;
    }

    private UserDelegater() {
    }
    
    public UserModel login(String fid, String uid, String password) throws Exception {

        StringBuilder sb = new StringBuilder();
        sb.append(fid);
        sb.append(IInfoModel.COMPOSITE_KEY_MAKER);
        sb.append(uid);
        String fidUid = sb.toString();

        //RESTEasyClient restEasy = RESTEasyClient.getInstance();
        //String baseURI = Project.getBaseURI();
        //restEasy.setBaseURI(baseURI);
        //restEasy.setUpAuthentication(fidUid, password, false);
        JerseyClient jersey = JerseyClient.getInstance();
        String baseURI = Project.getBaseURI();
        jersey.setBaseURI(baseURI);
        jersey.setUpAuthentication(fidUid, password, false);
        
        if (DEBUG) {
            System.out.println(baseURI);
            System.out.println(fidUid);
            System.out.println(password);
        }

        return getUser(fidUid);
    }
    
    public UserModel getUser(String userPK) throws Exception {
        
        StringBuilder sb = new StringBuilder();
        sb.append(RES_USER);
        sb.append(userPK);
        String path = sb.toString();

        Response response = buildRequest(path, null, MediaType.APPLICATION_JSON_TYPE)
                   .get(Response.class);

        int status = response.getStatus();
        isHTTP200(status);
        InputStream is = response.readEntity(InputStream.class);

        UserModel userModel = (UserModel) 
                getConverter().fromJson(is, UserModel.class);

        return userModel;
    }
    
    public List<UserModel> getAllUser() throws Exception {
        
        String path = RES_USER;

        Response response = buildRequest(path, null, MediaType.APPLICATION_JSON_TYPE)
                   .get(Response.class);

        int status = response.getStatus();
        isHTTP200(status);
        InputStream is = response.readEntity(InputStream.class);

        TypeReference typeRef = new TypeReference<List<UserModel>>(){};
        List<UserModel> list  = (List<UserModel>) 
                getConverter().fromJson(is, typeRef);

        return list;
    }
    
    public int addUser(UserModel userModel) throws Exception {

        String path = RES_USER;
        Entity entity = toJsonEntity(userModel);

        Response response = buildRequest(path, null, MediaType.TEXT_PLAIN_TYPE)
                .post(entity, Response.class);

        int status = response.getStatus();
        String entityStr = (String) response.readEntity(String.class);
        debug(status, entityStr);
        isHTTP200(status);
        
        int cnt = Integer.parseInt(entityStr);

        return cnt;
    }
    
    public int updateUser(UserModel userModel) throws Exception {

        String path = RES_USER;
        Entity entity = toJsonEntity(userModel);

        Response response = buildRequest(path, null, MediaType.TEXT_PLAIN_TYPE)   
                .put(entity, Response.class);

        int status = response.getStatus();
        String entityStr = (String) response.readEntity(String.class);
        debug(status, entityStr);
        isHTTP200(status);

        int cnt = Integer.parseInt(entityStr);

        return cnt;
    }
    
    public int deleteUser(String uid) throws Exception {
        
        String path = RES_USER + uid;

        Response response = buildRequest(path, null, null)
                .delete(Response.class);

        int status = response.getStatus();
        debug(status, "delete response");
        isHTTP200(status);

        return 1;
    }
    
    public int updateFacility(UserModel userModel) throws Exception {

        String path = RES_USER + "facility";

        Entity entity = toJsonEntity(userModel);

        Response response = buildRequest(path, null, MediaType.TEXT_PLAIN_TYPE)
                .put(entity, Response.class);

        int status = response.getStatus();
        String entityStr = (String) response.readEntity(String.class);
        debug(status, entityStr);
        isHTTP200(status);

        int cnt = Integer.parseInt(entityStr);

        return cnt;
    }
    
    public String login(String fidUid, String clientUUID, boolean force) throws Exception {
        
        String path = RES_USER + "login";
        
        MultivaluedMap<String, String> qmap = new MultivaluedHashMap();
        qmap.add("fidUid", fidUid);
        qmap.add("clientUUID", clientUUID);
        qmap.add("force", String.valueOf(force));
        
        Response response = buildRequest(path, qmap, MediaType.TEXT_PLAIN_TYPE)
                .get(Response.class);
        int status = response.getStatus();
        String currentUUID = (String) response.readEntity(String.class);
        debug(status, currentUUID);
        isHTTP200(status);
        
        return currentUUID;
    }
    
    public String logout(String fidUid, String clientUUID) throws Exception {
        
        String path = RES_USER + "logout";
        
        MultivaluedMap<String, String> qmap = new MultivaluedHashMap();
        qmap.add("fidUid", fidUid);
        qmap.add("clientUUID", clientUUID);
        
        Response response = buildRequest(path, qmap, MediaType.TEXT_PLAIN_TYPE)
                .get(Response.class);
        int status = response.getStatus();
        String oldUUID = (String) response.readEntity(String.class);
        debug(status, oldUUID);
        isHTTP200(status);
        
        return oldUUID;
    }
    
    @Override
    protected void debug(int status, String entity) {
        if (debug || DEBUG) {
            super.debug(status, entity);
        }
    }
}
