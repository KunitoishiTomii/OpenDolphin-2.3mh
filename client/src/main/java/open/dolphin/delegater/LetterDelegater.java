package open.dolphin.delegater;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.InputStream;
import java.util.List;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import open.dolphin.infomodel.LetterModule;

/**
 * 紹介状用のデリゲータークラス。
 * @author Kazushi Minagawa.
 * @author modified by masuda, Masuda Naika
 */
public class LetterDelegater extends BusinessDelegater {

    private static final String PATH_FOR_LETTER = "odletter/letter/";
    private static final String PATH_FOR_LETTER_LIST = "odletter/list/";
    
    private static final boolean debug = false;
    private static final LetterDelegater instance;

    static {
        instance = new LetterDelegater();
    }

    public static LetterDelegater getInstance() {
        return instance;
    }

    private LetterDelegater() {
    }
    
    public long saveOrUpdateLetter(LetterModule model) throws Exception {

       String path = PATH_FOR_LETTER;
       
       Entity entity = toJsonEntity(model);

        Response response = buildRequest(path, null, MediaType.TEXT_PLAIN_TYPE)    
                .put(entity);

        int status = checkHttpStatus(response);
        String entityStr = response.readEntity(String.class);
        debug(status, entityStr);
        
        response.close();

        long pk = Long.parseLong(entityStr);
        return pk;
    }

    public LetterModule getLetter(long letterPk) throws Exception {
        
        String path = PATH_FOR_LETTER + String.valueOf(letterPk);

        Response response = buildRequest(path, null, MediaType.APPLICATION_JSON_TYPE)
                .get();

        checkHttpStatus(response);
        InputStream is = response.readEntity(InputStream.class);
        LetterModule ret = (LetterModule)
                getConverter().fromJson(is, LetterModule.class);
        
        response.close();

        return ret;
    }


    public List<LetterModule> getLetterList(long kartePk) throws Exception {
        
        String path = PATH_FOR_LETTER_LIST + String.valueOf(kartePk);

        Response response = buildRequest(path, null, MediaType.APPLICATION_JSON_TYPE)
                .get();

        checkHttpStatus(response);
        InputStream is = response.readEntity(InputStream.class);
        TypeReference typeRef = new TypeReference<List<LetterModule>>(){};
        List<LetterModule> ret = (List<LetterModule>)
                getConverter().fromJson(is, typeRef);
        
        response.close();

        return ret;
    }


    public void delete(long pk) throws Exception {

        String path = PATH_FOR_LETTER + String.valueOf(pk);

        Response response = buildRequest(path, null, null)
                .delete();

        int status = checkHttpStatus(response);
        debug(status, "delete response");
        
        response.close();
    }
    
    @Override
    protected void debug(int status, String entity) {
        if (debug || DEBUG) {
            super.debug(status, entity);
        }
    }
}
