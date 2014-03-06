package open.dolphin.delegater;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.InputStream;
import java.util.List;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import open.dolphin.infomodel.LaboModuleValue;
import open.dolphin.infomodel.NLaboModule;
import open.dolphin.infomodel.PatientLiteModel;
import open.dolphin.infomodel.PatientModel;

/**
 * Labo 関連の Delegater クラス。
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class LaboDelegater extends BusinessDelegater {
    
    private static final boolean debug = false;
    private static final LaboDelegater instance;

    static {
        instance = new LaboDelegater();
    }

    public static LaboDelegater getInstance() {
        return instance;
    }

    private LaboDelegater() {
    }

    public List<PatientLiteModel> getConstrainedPatients(List<String> idList) throws Exception {

        String path = "lab/patient";

        Response response = getWebTarget()
                .path(path)
                .queryParam(IDS, getConverter().fromList(idList))
                .request(MEDIATYPE_JSON_UTF8)
                .get();

        checkHttpStatus(response);
        InputStream is = response.readEntity(InputStream.class);
        TypeReference typeRef = new TypeReference<List<PatientLiteModel>>(){};
        List<PatientLiteModel> list = (List<PatientLiteModel>)
                getConverter().fromJson(is, typeRef);

        response.close();
        
        return list;
    }
    
    /**
     * 検査結果を追加する。
     * @param value 追加する検査モジュール
     * @return      患者オブジェクト
     */
    public PatientModel postNLaboModule(NLaboModule value) throws Exception {
        
        String path = "lab/module/";

        Entity entity = toJsonEntity(value);

        Response response = getWebTarget()
                .path(path)
                .request(MEDIATYPE_JSON_UTF8)
                .post(entity);

        checkHttpStatus(response);
        InputStream is = response.readEntity(InputStream.class);
        PatientModel patient = (PatientModel)
                getConverter().fromJson(is, PatientModel.class);
        
        response.close();

        return patient;
    }

    /**
     * ラボモジュールを検索する。
     * @param patientId     対象患者のID
     * @param firstResult   取得結果リストの最初の番号
     * @param maxResult     取得する件数の最大値
     * @return              ラボモジュールを採取日で降順に格納したリスト
     */
    public List<NLaboModule> getLaboTest(String patientId, int firstResult, int maxResult) throws Exception {

        String path = "lab/module/" + patientId;

        Response response = getWebTarget()
                .path(path)
                .queryParam(FIRST_RESULT, firstResult)
                .queryParam(MAX_RESULTS, maxResult)
                .request(MEDIATYPE_JSON_UTF8)
                .get();

        checkHttpStatus(response);
        InputStream is = response.readEntity(InputStream.class);
        TypeReference typeRef = new TypeReference<List<NLaboModule>>(){};
        List<NLaboModule> list = (List<NLaboModule>)
                getConverter().fromJson(is, typeRef);
        
        response.close();

        return list;
    }

//masuda^   旧ラボ
    public PatientModel putMmlLaboModule(LaboModuleValue value) throws Exception {

        String path = "lab/mmlModule";

        Entity entity = toJsonEntity(value);

        Response response = getWebTarget()
                .path(path)
                .request(MEDIATYPE_JSON_UTF8)
                .post(entity);

        checkHttpStatus(response);
        InputStream is = response.readEntity(InputStream.class);
        PatientModel patient = (PatientModel) 
                getConverter().fromJson(is, PatientModel.class);
        
        response.close();

        decodeHealthInsurance(patient);

        return patient;
    }
    
    // 削除
    public int deleteNlaboModule(long id) throws Exception {

        String path = "lab/module/id/" + String.valueOf(id);

        Response response = getWebTarget()
                .path(path)
                .request()
                .delete();

        int status = checkHttpStatus(response);
        debug(status, "delete response");
        
        response.close();

        return 1;
    }
    
    public int deleteMmlLaboModule(long id) throws Exception {

        String path = "lab/mmlModule/id/" + String.valueOf(id);

        Response response = getWebTarget()
                .path(path)
                .request()
                .delete();

        int status = checkHttpStatus(response);
        debug(status, "delete response");
        
        response.close();

        return 1;
    }

    @Override
    protected void debug(int status, String entity) {
        if (debug || DEBUG) {
            super.debug(status, entity);
        }
    }
//masuda$
}
