package open.dolphin.delegater;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.InputStream;
import java.util.List;
import javax.ws.rs.client.Entity;
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

        RestClient restClient = RestClient.getInstance();
        String baseURI = Project.getBaseURI();
        restClient.setBaseURI(baseURI);
        restClient.setUpAuthentication(fidUid, password, false);
        
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

        Response response = getWebTarget()
                .path(path)
                .request(MEDIATYPE_JSON_UTF8)
                .get();

        checkHttpStatus(response);
        InputStream is = response.readEntity(InputStream.class);
        UserModel userModel = (UserModel) 
                getConverter().fromJson(is, UserModel.class);
        
        response.close();

        return userModel;
    }
    
    public List<UserModel> getAllUser() throws Exception {
        
        String path = RES_USER;

        Response response = getWebTarget()
                .path(path)
                .request(MEDIATYPE_JSON_UTF8)
                .get();

        checkHttpStatus(response);
        InputStream is = response.readEntity(InputStream.class);
        TypeReference typeRef = new TypeReference<List<UserModel>>(){};
        List<UserModel> list  = (List<UserModel>) 
                getConverter().fromJson(is, typeRef);
        
        response.close();

        return list;
    }
    
    public int addUser(UserModel userModel) throws Exception {

        String path = RES_USER;
        Entity entity = toJsonEntity(userModel);

        Response response = getWebTarget()
                .path(path)
                .request(MEDIATYPE_TEXT_UTF8)
                .post(entity);

        int status = checkHttpStatus(response);
        String entityStr = response.readEntity(String.class);
        debug(status, entityStr);
        
        response.close();
        
        int cnt = Integer.parseInt(entityStr);

        return cnt;
    }
    
    public int updateUser(UserModel userModel) throws Exception {

        String path = RES_USER;
        Entity entity = toJsonEntity(userModel);

        Response response = getWebTarget()
                .path(path) 
                .request(MEDIATYPE_TEXT_UTF8)
                .put(entity);

        int status = checkHttpStatus(response);
        String entityStr = response.readEntity(String.class);
        debug(status, entityStr);
        
        response.close();

        int cnt = Integer.parseInt(entityStr);

        return cnt;
    }
    
    public int deleteUser(String uid) throws Exception {
        
        String path = RES_USER + uid;

        Response response = getWebTarget()
                .path(path)
                .request()
                .delete();

        int status = checkHttpStatus(response);
        debug(status, "delete response");
        
        response.close();

        return 1;
    }
    
    public int updateFacility(UserModel userModel) throws Exception {

        String path = RES_USER + "facility";

        Entity entity = toJsonEntity(userModel);

        Response response = getWebTarget()
                .path(path)
                .request(MEDIATYPE_TEXT_UTF8)
                .put(entity);

        int status = checkHttpStatus(response);
        String entityStr = response.readEntity(String.class);
        debug(status, entityStr);
        
        response.close();

        int cnt = Integer.parseInt(entityStr);

        return cnt;
    }
    
    public String login(String fidUid, String clientUUID, boolean force) throws Exception {
        
        String path = RES_USER + "login";

        Response response = getWebTarget()
                .path(path)
                .queryParam("fidUid", fidUid)
                .queryParam("clientUUID", clientUUID)
                .queryParam("force", String.valueOf(force))
                .request(MEDIATYPE_TEXT_UTF8)
                .get();
        
        int status = checkHttpStatus(response);
        String currentUUID = response.readEntity(String.class);
        debug(status, currentUUID);
        
        response.close();
        
        return currentUUID;
    }
    
    public String logout(String fidUid, String clientUUID) throws Exception {
        
        String path = RES_USER + "logout";
        
        Response response = getWebTarget()
                .path(path)
                .queryParam("fidUid", fidUid)
                .queryParam("clientUUID", clientUUID)
                .request(MEDIATYPE_TEXT_UTF8)
                .get();
        int status = checkHttpStatus(response);
        String oldUUID = response.readEntity(String.class);
        debug(status, oldUUID);
        
        response.close();
        
        return oldUUID;
    }
    
    @Override
    protected void debug(int status, String entity) {
        if (debug || DEBUG) {
            super.debug(status, entity);
        }
    }
}
