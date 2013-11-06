package open.dolphin.delegater;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.InputStream;
import java.util.List;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import open.dolphin.infomodel.PatientVisitModel;

/**
 * PVT 関連の Business Delegater　クラス。
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class PVTDelegater extends BusinessDelegater {

    private static final String RES_PVT = "pvt2/";
    
    private static final boolean debug = false;
    private static final PVTDelegater instance;

    static {
        instance = new PVTDelegater();
    }

    public static PVTDelegater getInstance() {
        return instance;
    }

    private PVTDelegater() {
    }

    /**
     * 受付情報 PatientVisitModel をデータベースに登録する。
     *
     * @param pvtModel 受付情報 PatientVisitModel
     * @param principal UserId と FacilityId
     * @return 保存に成功した個数
     */
    public int addPvt(PatientVisitModel pvtModel) throws Exception {
        
        // convert
        Entity entity = toJsonEntity(pvtModel);

        // resource post
        String path = RES_PVT;
        Response response = getWebTarget()
                .path(path)
                .request(MEDIATYPE_TEXT_UTF8)
                .post(entity);

        int status = checkHttpStatus(response);
        String enityStr = response.readEntity(String.class);
        debug(status, enityStr);
        
        response.close();

        // result = count
        int cnt = Integer.parseInt(enityStr);
        return cnt;
    }

    public int removePvt(long id) throws Exception {
        
        String path = RES_PVT + String.valueOf(id);

        Response response = getWebTarget()
                .path(path)
                .request()
                .delete();

        int status = checkHttpStatus(response);
        String enityStr = "delete response";
        debug(status, enityStr);
        
        response.close();

        return 1;
     }

    public List<PatientVisitModel> getPvtList() throws Exception {
        
        StringBuilder sb = new StringBuilder();
        sb.append(RES_PVT);
        sb.append("pvtList");
        String path = sb.toString();

        Response response = getWebTarget()
                .path(path)
                .request(MEDIATYPE_JSON_UTF8)
                .get();

        checkHttpStatus(response);
        InputStream is = response.readEntity(InputStream.class);
        TypeReference typeRef = new TypeReference<List<PatientVisitModel>>(){};
        List<PatientVisitModel> pvtList = (List<PatientVisitModel>)
                getConverter().fromJson(is, typeRef);
        
        response.close();

        // 保険をデコード
        decodePvtHealthInsurance(pvtList);

        return pvtList;
    }

    @Override
    protected void debug(int status, String entity) {
        if (debug || DEBUG) {
            super.debug(status, entity);
        }
    }
}
