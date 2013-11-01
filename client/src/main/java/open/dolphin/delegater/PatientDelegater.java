package open.dolphin.delegater;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import open.dolphin.dto.PatientSearchSpec;
import open.dolphin.infomodel.HealthInsuranceModel;
import open.dolphin.infomodel.PatientModel;

/**
 * 患者関連の Business Delegater　クラス。
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class  PatientDelegater extends BusinessDelegater {

    private static final String BASE_RESOURCE       = "patient/";
    private static final String NAME_RESOURCE       = "patient/name/";
    private static final String KANA_RESOURCE       = "patient/kana/";
    private static final String ID_RESOURCE         = "patient/id/";
    private static final String DIGIT_RESOURCE      = "patient/digit/";
    private static final String PVT_DATE_RESOURCE   = "patient/pvt/";

    private static final boolean debug = false;
    private static final PatientDelegater instance;

    static {
        instance = new PatientDelegater();
    }

    public static PatientDelegater getInstance() {
        return instance;
    }

    private PatientDelegater() {
    }
    
    /**
     * 患者を追加する。
     * @param patient 追加する患者
     * @return PK
     */
    public long addPatient(PatientModel patient) throws Exception {
        
        Entity entity = toJsonEntity(patient);

        String path = BASE_RESOURCE;

        Response response = buildRequest(path, null)
                .accept(MEDIATYPE_TEXT_UTF8)
                .post(entity);

        int status = checkHttpStatus(response);
        String entityStr = response.readEntity(String.class);
        debug(status, entityStr);
        
        response.close();

        return Long.valueOf(entityStr);
    }
    
    /**
     * 患者を検索する。
     * @param pid 患者ID
     * @return PatientModel
     */
    public PatientModel getPatientById(String pid) throws Exception {
        
        String path = ID_RESOURCE;

        Response response = buildRequest(path, null)
                .accept(MEDIATYPE_JSON_UTF8)
                .get();

        checkHttpStatus(response);
        InputStream is = response.readEntity(InputStream.class);
        PatientModel patient = (PatientModel)
                getConverter().fromJson(is, PatientModel.class);
        
        response.close();

        return patient;
    }
    
    /**
     * 患者を検索する。
     * @param spec PatientSearchSpec 検索仕様
     * @return PatientModel の Collection
     */
    public List<PatientModel> getPatients(PatientSearchSpec spec) throws Exception {
        
        StringBuilder sb = new StringBuilder();

        switch (spec.getCode()) {

            case PatientSearchSpec.NAME_SEARCH:
                sb.append(NAME_RESOURCE);
                sb.append(spec.getName());
                break;

            case PatientSearchSpec.KANA_SEARCH:
                sb.append(KANA_RESOURCE);
                sb.append(spec.getName());
                break;

            case PatientSearchSpec.DIGIT_SEARCH:
                sb.append(DIGIT_RESOURCE);
                sb.append(spec.getDigit());
                break;

           case PatientSearchSpec.DATE_SEARCH:
                sb.append(PVT_DATE_RESOURCE);
                sb.append(spec.getDigit());
                break;
        }

        String path = sb.toString();

        Response response = buildRequest(path, null)
                .accept(MEDIATYPE_JSON_UTF8)
                .get();

        checkHttpStatus(response);
        InputStream is = response.readEntity(InputStream.class);
        TypeReference typeRef = new TypeReference<List<PatientModel>>(){};
        List<PatientModel> list = (List<PatientModel>)
                getConverter().fromJson(is, typeRef);
        
        response.close();

        return list;
    }

    /**
     * 患者を更新する。
     * @param patient 更新する患者
     * @return 更新数
     */
    public int updatePatient(PatientModel patient) throws Exception {
        
        Entity entity = toJsonEntity(patient);

        String path = BASE_RESOURCE;

        Response response = buildRequest(path, null) 
                .accept(MEDIATYPE_TEXT_UTF8)
                .put(entity);

        int status = checkHttpStatus(response);
        String entityStr = response.readEntity(String.class);
        debug(status, entityStr);
        
        response.close();

        return Integer.parseInt(entityStr);
    }
    
    // patientIDリストからPatienteModelのリストを取得する
    public List<PatientModel> getPatientList(Collection patientIdList) throws Exception {
        
        String path = BASE_RESOURCE + "list";
        String ids = getConverter().fromList(patientIdList);

        MultivaluedMap<String, String> qmap = new MultivaluedHashMap();
        qmap.add("ids", ids);

        Response response = buildRequest(path, qmap)
                .accept(MEDIATYPE_JSON_UTF8)
                .get();

        checkHttpStatus(response);
        InputStream is = response.readEntity(InputStream.class);
        TypeReference typeRef = new TypeReference<List<PatientModel>>(){};
        List<PatientModel> list = (List<PatientModel>)
                getConverter().fromJson(is, typeRef);
        
        response.close();

        return list;
    }

    // カルテオープン時に保険情報を更新する
    public void updateHealthInsurances(PatientModel pm) throws Exception {
        
        long pk = pm.getId();
        String path = BASE_RESOURCE + "insurances/" + String.valueOf(pk);

        Response response = buildRequest(path, null)
                .accept(MEDIATYPE_JSON_UTF8)
                .get();

        checkHttpStatus(response);
        InputStream is = response.readEntity(InputStream.class);
        TypeReference typeRef = new TypeReference<List<HealthInsuranceModel>>(){};
        List<HealthInsuranceModel> list = (List<HealthInsuranceModel>)
                getConverter().fromJson(is, typeRef);
        
        response.close();

        pm.setHealthInsurances(list);
        // 忘れがちｗ
        decodeHealthInsurance(pm);
    }
    
    public List<PatientModel> getPast100DayPatients(int pastDay) throws Exception {
        
        String path = BASE_RESOURCE + "past100day/" + String.valueOf(pastDay);
        
        Response response = buildRequest(path, null)
                .accept(MEDIATYPE_JSON_UTF8)
                .get();

        checkHttpStatus(response);
        InputStream is = response.readEntity(InputStream.class);
        TypeReference typeRef = new TypeReference<List<PatientModel>>(){};
        List<PatientModel> list = (List<PatientModel>)
                getConverter().fromJson(is, typeRef);
        
        response.close();

        return list;
    }
    
    @Override
    protected void debug(int status, String entity) {
        if (debug || DEBUG) {
            super.debug(status, entity);
        }
    }
}
