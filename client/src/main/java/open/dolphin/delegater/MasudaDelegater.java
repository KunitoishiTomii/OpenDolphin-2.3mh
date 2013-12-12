package open.dolphin.delegater;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import open.dolphin.infomodel.*;
import open.dolphin.common.util.BeanUtils;

/**
 * MasudaDelegater
 *
 * @author masuda, Masuda Naika
 */
public class MasudaDelegater extends BusinessDelegater {
    

    private static final String RES_BASE = "masuda/";

    private static final boolean debug = false;
    private static final MasudaDelegater instance;

    static {
        instance = new MasudaDelegater();
    }

    public static MasudaDelegater getInstance() {
        return instance;
    }

    private MasudaDelegater() {
    }

    // 定期処方
    public List<RoutineMedModel> getRoutineMedModels(
            long karteId, int firstResult, int maxResults) throws Exception {

        String path = RES_BASE + "routineMed/list/" + String.valueOf(karteId);

        Response response = getWebTarget()
                .path(path)
                .queryParam(FIRST_RESULT, String.valueOf(firstResult))
                .queryParam(MAX_RESULTS, String.valueOf(maxResults))
                .request(MEDIATYPE_JSON_UTF8)
                .get();

        checkHttpStatus(response);
        InputStream is = response.readEntity(InputStream.class);
        TypeReference typeRef = new TypeReference<List<RoutineMedModel>>(){};
        List<RoutineMedModel> list = (List<RoutineMedModel>) 
                getConverter().fromJson(is, typeRef);
        
        response.close();

        // いつもデコード忘れるｗ
        for (RoutineMedModel model : list) {
            for (ModuleModel mm : model.getModuleList()) {
                mm.setModel((IModuleModel) BeanUtils.xmlDecode(mm.getBeanBytes()));
            }
        }

        return list;
    }
    
    public RoutineMedModel getRoutineMedModel(long id) throws Exception {

        String path = RES_BASE + "routineMed/" + String.valueOf(id);

        Response response = getWebTarget()
                .path(path)
                .request(MEDIATYPE_JSON_UTF8)
                .get();

        checkHttpStatus(response);
        InputStream is = response.readEntity(InputStream.class);
        RoutineMedModel model = (RoutineMedModel)
                getConverter().fromJson(is, RoutineMedModel.class);
        
        response.close();
        
        if (model == null) {
            return null;
        }
        // いつもデコード忘れるｗ
        for (ModuleModel mm : model.getModuleList()) {
            mm.setModel((IModuleModel) BeanUtils.xmlDecode(mm.getBeanBytes()));
        }

        return model;
    }
    
    public void removeRoutineMedModel(RoutineMedModel model) throws Exception {

        String path = RES_BASE + "routineMed/" + String.valueOf(model.getId());

        Response response = getWebTarget()
                .path(path)
                .request()
                .delete();

        int status = checkHttpStatus(response);
        debug(status, "delete response");
        
        response.close();
    }
    
    public void addRoutineMedModel(RoutineMedModel model) throws Exception {
        
        String path = RES_BASE + "routineMed";

        Entity entity = toJsonEntity(model);

        Response response = getWebTarget()
                .path(path)
                .request(MEDIATYPE_TEXT_UTF8)
                .post(entity);

        int status = checkHttpStatus(response);
        String enityStr = response.readEntity(String.class);
        debug(status, enityStr);
        
        response.close();
    }
    
    public void updateRoutineMedModel(RoutineMedModel model) throws Exception {
        
        String path = RES_BASE + "routineMed";

        Entity entity = toJsonEntity(model);

        Response response = getWebTarget()
                .path(path)
                .request(MEDIATYPE_TEXT_UTF8)
                .put(entity);

        int status = checkHttpStatus(response);
        String enityStr = response.readEntity(String.class);
        debug(status, enityStr);
        
        response.close();
    }
    
    // 中止項目
    public List<DisconItemModel> getDisconItemModels() throws Exception {
        
        String path = RES_BASE + "discon";

        Response response = getWebTarget()
                .path(path)
                .request(MEDIATYPE_JSON_UTF8)
                .get();

        checkHttpStatus(response);
        InputStream is = response.readEntity(InputStream.class);
        TypeReference typeRef = new TypeReference<List<DisconItemModel>>(){};
        List<DisconItemModel> list = (List<DisconItemModel>)
                getConverter().fromJson(is, typeRef);
        
        response.close();

        return list;
    }

    public void addDisconItemModel(DisconItemModel model) throws Exception {
        
        String path = RES_BASE + "discon";

        Entity entity = toJsonEntity(model);

        Response response = getWebTarget()
                .path(path)
                .request(MEDIATYPE_TEXT_UTF8)
                .post(entity);

        int status = checkHttpStatus(response);
        String enityStr = response.readEntity(String.class);
        debug(status, enityStr);
        
        response.close();
    }

    public void removeDisconItemModel(DisconItemModel model) throws Exception {
        
        String path = RES_BASE + "discon/" + String.valueOf(model.getId());

        Response response = getWebTarget()
                .path(path)
                .request()
                .delete();

        int status = checkHttpStatus(response);
        debug(status, "delete response");
        
        response.close();
    }

    public void updateDisconItemModel(DisconItemModel model) throws Exception {
        
        String path = RES_BASE + "discon";

        Entity entity = toJsonEntity(model);

        Response response = getWebTarget()
                .path(path)
                .request(MEDIATYPE_TEXT_UTF8)
                .put(entity);

        int status = checkHttpStatus(response);
        String enityStr = response.readEntity(String.class);
        debug(status, enityStr);
        
        response.close();
    }

    // 採用薬
    public List<UsingDrugModel> getUsingDrugModels() throws Exception {
        
        String path = RES_BASE + "usingDrug";

        Response response = getWebTarget()
                .path(path)
                .request(MEDIATYPE_JSON_UTF8)
                .get();

        checkHttpStatus(response);
        InputStream is = response.readEntity(InputStream.class);
        TypeReference typeRef = new TypeReference<List<UsingDrugModel>>(){};
        List<UsingDrugModel> list = (List<UsingDrugModel>)
                getConverter().fromJson(is, typeRef);
        
        response.close();

        return list;
    }

    public void addUsingDrugModel(UsingDrugModel model) throws Exception {
        
        String path = RES_BASE + "usingDrug";

        Entity entity = toJsonEntity(model);

        Response response = getWebTarget()
                .path(path)
                .request(MEDIATYPE_TEXT_UTF8)
                .post(entity);

        int status = checkHttpStatus(response);
        String enityStr = response.readEntity(String.class);
        debug(status, enityStr);
        
        response.close();
    }

    public void removeUsingDrugModel(UsingDrugModel model) throws Exception {
        
        String path = RES_BASE + "usingDrug/" + String.valueOf(model.getId());

        Response response = getWebTarget()
                .path(path)
                .request()
                .delete();

        int status = checkHttpStatus(response);
        debug(status, "delete response");
        
        response.close();
    }

    public void updateUsingDrugModel(UsingDrugModel model) throws Exception {
        
        String path = RES_BASE + "usingDrug";

        Entity entity = toJsonEntity(model);
        
        Response response = getWebTarget()
                .path(path)
                .request(MEDIATYPE_TEXT_UTF8)
                .put(entity);

        int status = checkHttpStatus(response);
        String enityStr = response.readEntity(String.class);
        debug(status, enityStr);
        
        response.close();
    }

    // 指定したEntityのModuleModelをがさっと取ってくる
    public List<ModuleModel> getModulesEntitySearch(
            long karteId, Date fromDate, Date toDate, List<String> entities) throws Exception {
        
        if (entities == null || entities.isEmpty()) {
            return null;
        }

        String path = RES_BASE + "moduleSearch/" + String.valueOf(karteId);

        Response response = getWebTarget()
                .path(path)
                .queryParam(FROM_DATE, toRestFormat(fromDate))
                .queryParam(TO_DATE, toRestFormat(toDate))
                .queryParam(ENTITIES, getConverter().fromList(entities))
                .request(MEDIATYPE_JSON_UTF8)
                .get();

        checkHttpStatus(response);
        InputStream is = response.readEntity(InputStream.class);
        TypeReference typeRef = new TypeReference<List<ModuleModel>>(){};
        List<ModuleModel> list = (List<ModuleModel>)
                getConverter().fromJson(is, typeRef);
        
        response.close();

        for (ModuleModel module : list) {
            module.setModel((IModuleModel) BeanUtils.xmlDecode(module.getBeanBytes()));
        }

        return list;
    }

    // FEV-70に患者情報を登録するときに使用する。PatientVisitを扱うが、ここに居候することにした
    public PatientVisitModel getLastPvtInThisMonth(PatientVisitModel pvt) throws Exception {
        
        // long ptid は設定されていないのでだめ!
        //long ptId = pvt.getPatientModel().getId();
        String ptId = pvt.getPatientModel().getPatientId();

        String path = RES_BASE + "lastPvt/" + ptId;

        Response response = getWebTarget()
                .path(path)
                .request(MEDIATYPE_JSON_UTF8)
                .get();

        checkHttpStatus(response);
        InputStream is = response.readEntity(InputStream.class);
        PatientVisitModel model = (PatientVisitModel)
                getConverter().fromJson(is, PatientVisitModel.class);
        
        response.close();
        
        return model;
    }

    // 指定したdocIdのDocinfoModelを取得する
    public List<DocInfoModel> getDocumentList(List<Long> docPkList) throws Exception {
        
        if (docPkList == null || docPkList.isEmpty()) {
            return null;
        }

        String path = RES_BASE + "docList";
        
        Response response = getWebTarget()
                .path(path)
                .queryParam(IDS, getConverter().fromList(docPkList))
                .request(MEDIATYPE_JSON_UTF8)
                .get();

        checkHttpStatus(response);
        InputStream is = response.readEntity(InputStream.class);
        TypeReference typeRef = new TypeReference<List<DocInfoModel>>(){};
        List<DocInfoModel> list = (List<DocInfoModel>)
                getConverter().fromJson(is, typeRef);
        
        response.close();

        return list;
    }

    // Hibernate Searchの初期インデックスを作成する
    public String makeDocumentModelIndex(long fromDocPk, int maxResults) throws Exception {
        
        String path = RES_BASE +"search/makeIndex";

        Response response = getWebTarget()
                .path(path)
                .queryParam(FROM_DOC_PK, String.valueOf(fromDocPk))
                .queryParam(MAX_RESULTS, String.valueOf(maxResults))
                .request(MEDIATYPE_TEXT_UTF8)
                .get();

        int status = checkHttpStatus(response);
        String entityStr = response.readEntity(String.class);
        debug(status, entityStr);
        
        response.close();

        return entityStr;
    }

    // HibernteSearchによる全文検索
    public List<PatientModel> getKarteFullTextSearch(long karteId, String text) throws Exception {
        
        String path = RES_BASE + "search/hibernate";
        
        Response response = getWebTarget()
                .path(path)
                .queryParam(KARTE_ID, String.valueOf(karteId))
                .queryParam(TEXT, text)
                .request(MEDIATYPE_JSON_UTF8)
                .get();

        checkHttpStatus(response);
        InputStream is = response.readEntity(InputStream.class);
        TypeReference typeRef = new TypeReference<List<PatientModel>>(){};
        List<PatientModel> list = (List<PatientModel>)
                getConverter().fromJson(is, typeRef);
        
        response.close();

        return list;
    }

    // grep方式の全文検索
    public SearchResultModel getSearchResult(
            String text, long fromId, int maxResult, boolean progressCourseOnly) throws Exception {
        
        String path = RES_BASE + "search/grep";

        Response response = getWebTarget()
                .path(path)
                .queryParam(TEXT, text)
                .queryParam(FROM_ID, String.valueOf(fromId))
                .queryParam(MAX_RESULTS, String.valueOf(maxResult))
                .queryParam(PC_ONLY, String.valueOf(progressCourseOnly))
                .request(MEDIATYPE_JSON_UTF8)
                .get();

        checkHttpStatus(response);
        InputStream is = response.readEntity(InputStream.class);
        SearchResultModel model = (SearchResultModel)
                getConverter().fromJson(is, SearchResultModel.class);

        response.close();
        
        return model;
    }

    // 検査履歴を取得する
    public List<ExamHistoryModel> getExamHistory(long karteId, Date fromDate, Date toDate) throws Exception {
        
        String path = RES_BASE + "examHistory/" + String.valueOf(karteId);

        Response response = getWebTarget()
                .path(path)
                .queryParam(FROM_DATE, toRestFormat(fromDate))
                .queryParam(TO_DATE, toRestFormat(toDate))
                .request(MEDIATYPE_JSON_UTF8)
                .get();

        checkHttpStatus(response);
        InputStream is = response.readEntity(InputStream.class);
        TypeReference typeRef = new TypeReference<List<ExamHistoryModel>>(){};
        List<ExamHistoryModel> list = (List<ExamHistoryModel>)
                getConverter().fromJson(is, typeRef);
        
        response.close();

        return list;
    }
    
    // 処方切れ患者を検索する
    public List<PatientModel> getOutOfMedPatient(Date fromDate, Date toDate, int yoyuu) throws Exception {
        
        String path = RES_BASE + "outOfMed";

        Response response = getWebTarget()
                .path(path)
                .queryParam(FROM_DATE, toRestFormat(fromDate))
                .queryParam(TO_DATE, toRestFormat(toDate))
                .queryParam(YOYUU, String.valueOf(yoyuu))
                .request(MEDIATYPE_JSON_UTF8)
                .get();

        checkHttpStatus(response);
        InputStream is = response.readEntity(InputStream.class);
        TypeReference typeRef = new TypeReference<List<PatientModel>>(){};
        List<PatientModel> list = (List<PatientModel>)
                getConverter().fromJson(is, typeRef);

        response.close();
        
        return list;
    }

    // 施設内検査
    public List<InFacilityLaboItem> getInFacilityLaboItemList() throws Exception {
        
        String path = RES_BASE + "inFacilityLabo/list";
        
        Response response = getWebTarget()
                .path(path)
                .request(MEDIATYPE_JSON_UTF8)
                .get();

        checkHttpStatus(response);
        InputStream is = response.readEntity(InputStream.class);
        TypeReference typeRef = new TypeReference<List<InFacilityLaboItem>>(){};
        List<InFacilityLaboItem> list = (List<InFacilityLaboItem>)
                getConverter().fromJson(is, typeRef);

        response.close();
        
        return list;
    }
    
    public void updateInFacilityLaboItemList(List<InFacilityLaboItem> list) throws Exception {
        
        String path = RES_BASE + "inFacilityLabo/list";

        Entity entity = toJsonEntity(list);

        Response response = getWebTarget()
                .path(path)
                .request(MEDIATYPE_TEXT_UTF8)
                .put(entity);

        int status = checkHttpStatus(response);
        String enityStr = response.readEntity(String.class);
        debug(status, enityStr);
        
        response.close();
    }
    
    // 電子点数表　未使用
    public String updateETensu1Table(List<ETensuModel1> list) throws Exception {
        
        String path = RES_BASE + "etensu/update/";

        Entity entity = toJsonEntity(list);

        Response response = getWebTarget()
                .path(path)
                .request(MEDIATYPE_TEXT_UTF8)
                .post(entity);

        int status = checkHttpStatus(response);
        String ret = response.readEntity(String.class);
        debug(status, ret);
        
        response.close();

        return ret;
    }

    public String initSanteiHistory(long fromId, int maxResults) throws Exception {
        
        String path = RES_BASE + "santeiHistory/init";

        Response response = getWebTarget()
                .path(path)
                .queryParam(FROM_ID, String.valueOf(fromId))
                .queryParam(MAX_RESULTS, String.valueOf(maxResults))
                .request(MEDIATYPE_TEXT_UTF8)
                .get();

        int status = checkHttpStatus(response);
        String ret = response.readEntity(String.class);
        debug(status, ret);
        
        response.close();

        return ret;
    }
    
    public List<SanteiHistoryModel> getSanteiHistory(
            long karteId, Date fromDate, Date toDate, List<String> srycds) throws Exception {
        
        String path = RES_BASE + "santeiHistory/" + String.valueOf(karteId);
        String srycdsStr = (srycds == null || srycds.isEmpty())
                ? null : getConverter().fromList(srycds);
        
        Response response = getWebTarget()
                .path(path)
                .queryParam(FROM_DATE, toRestFormat(fromDate))
                .queryParam(TO_DATE, toRestFormat(toDate))
                .queryParam(SRYCDS, srycdsStr)
                .request(MEDIATYPE_JSON_UTF8)
                .get();

        checkHttpStatus(response);
        InputStream is = response.readEntity(InputStream.class);
        TypeReference typeRef = new TypeReference<List<SanteiHistoryModel>>(){};
        List<SanteiHistoryModel> list = (List<SanteiHistoryModel>)
                getConverter().fromJson(is, typeRef);

        response.close();
        
        return list;
    }
    
    
    public List<List<RpModel>> getRpHistory(
            long karteId, Date fromDate, Date toDate, boolean lastOnly) throws Exception {
        
        String path = RES_BASE + "rpHistory/list/" + String.valueOf(karteId);

        Response response = getWebTarget()
                .path(path)
                .queryParam(FROM_DATE, toRestFormat(fromDate))
                .queryParam(TO_DATE, toRestFormat(toDate))
                .queryParam(LAST_ONLY, String.valueOf(lastOnly))
                .request(MEDIATYPE_JSON_UTF8)
                .get();

        checkHttpStatus(response);
        InputStream is = response.readEntity(InputStream.class);
        TypeReference typeRef = new TypeReference<List<List<RpModel>>>(){};
        List<List<RpModel>> list = (List<List<RpModel>>)
                getConverter().fromJson(is, typeRef);
        
        response.close();

        return list;
    }
    
    public void postUserProperties(String userId, List<UserPropertyModel> list) throws Exception {
        
        String path = RES_BASE + "userProperty/" + userId;

        Entity entity = toJsonEntity(list);

        Response response = getWebTarget()
                .path(path)
                .request(MEDIATYPE_TEXT_UTF8)
                .post(entity);

        int status = checkHttpStatus(response);
        String enityStr = response.readEntity(String.class);
        debug(status, enityStr);
        
        response.close();
    }
    
    public List<UserPropertyModel> getUserProperties(String userId) throws Exception {
        
        String path = RES_BASE + "userProperty/" + userId;

        Response response = getWebTarget()
                .path(path)
                .request(MEDIATYPE_JSON_UTF8)
                .get();

        checkHttpStatus(response);
        InputStream is = response.readEntity(InputStream.class);
        TypeReference typeRef = new TypeReference<List<UserPropertyModel>>(){};
        List<UserPropertyModel> list = (List<UserPropertyModel>) 
                getConverter().fromJson(is, typeRef);
        
        response.close();

        return list;
    }
    
    // ユーザーの、現時点で過去日になった仮保存カルテを取得する
    public List<PatientModel> getTempDocumentPatients(Date fromDate, long userPk) throws Exception {
        
        String path = RES_BASE + "tempKarte/" + String.valueOf(userPk);

        Response response = getWebTarget()
                .path(path)
                .queryParam(FROM_DATE, toRestFormat(fromDate))
                .request(MEDIATYPE_JSON_UTF8)
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
