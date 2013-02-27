package open.dolphin.delegater;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Date;
import java.util.List;
import javax.ws.rs.core.MultivaluedMap;
import open.dolphin.infomodel.*;
import open.dolphin.util.BeanUtils;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;

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
    public List<RoutineMedModel> getRoutineMedModels(long karteId, int firstResult, int maxResults) {
        
        try {
            String path = RES_BASE + "routineMed/list/" + String.valueOf(karteId);
            
            MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
            qmap.add("firstResult", String.valueOf(firstResult));
            qmap.add("maxResults", String.valueOf(maxResults));
            
            ClientResponse response = getClientRequest(path, qmap)
                    .accept(MEDIATYPE_JSON_UTF8)
                    .get(ClientResponse.class);

            int status = response.getStatus();
            String entityStr = (String) response.getEntity(String.class);
            debug(status, entityStr);

            if (status != HTTP200) {
                return null;
            }
            
            TypeReference typeRef = new TypeReference<List<RoutineMedModel>>(){};
            List<RoutineMedModel> list = (List<RoutineMedModel>) 
                    getConverter().fromJson(entityStr, typeRef);

            // いつもデコード忘れるｗ
            for (RoutineMedModel model : list) {
                for (ModuleModel mm : model.getModuleList()) {
                    mm.setModel((InfoModel) ModelUtils.xmlDecode(mm.getBeanBytes()));
                }
            }
            
            return list;
            
        } catch (Exception ex) {
            return null;
        }
    }
    
    public RoutineMedModel getRoutineMedModel(long id) {
        
        try {
            String path = RES_BASE + "routineMed/" + String.valueOf(id);
            
            ClientResponse response = getClientRequest(path, null)
                    .accept(MEDIATYPE_JSON_UTF8)
                    .get(ClientResponse.class);

            int status = response.getStatus();
            String entityStr = (String) response.getEntity(String.class);
            debug(status, entityStr);

            if (status != HTTP200) {
                return null;
            }

            RoutineMedModel model = (RoutineMedModel)
                    getConverter().fromJson(entityStr, RoutineMedModel.class);
            if (model == null) {
                return null;
            }
            // いつもデコード忘れるｗ
            for (ModuleModel mm : model.getModuleList()) {
                mm.setModel((InfoModel) ModelUtils.xmlDecode(mm.getBeanBytes()));
            }

            return model;
            
        } catch (Exception ex) {
            return null;
        }
    }
    
    public void removeRoutineMedModel(RoutineMedModel model) {
        
        try {
            String path = RES_BASE + "routineMed/" + String.valueOf(model.getId());
            
            ClientResponse response = getClientRequest(path, null)
                        .accept(MEDIATYPE_TEXT_UTF8)
                        .delete(ClientResponse.class);

            int status = response.getStatus();
            debug(status, "delete response");
            
        } catch (Exception ex) {
        }
    }
    
    public void addRoutineMedModel(RoutineMedModel model) {
        
        try {
            String path = RES_BASE + "routineMed";

            String json = getConverter().toJson(model);

            ClientResponse response = getClientRequest(path, null)
                    .body(MEDIATYPE_JSON_UTF8, json)
                    .post(ClientResponse.class);

            int status = response.getStatus();
            String enityStr = (String) response.getEntity(String.class);
            debug(status, enityStr);
            
        } catch (Exception ex) {
        }
    }
    
    public void updateRoutineMedModel(RoutineMedModel model) {
        
        try {
            String path = RES_BASE + "routineMed";

            String json = getConverter().toJson(model);

            ClientResponse response = getClientRequest(path, null)
                    .body(MEDIATYPE_JSON_UTF8, json)
                    .put(ClientResponse.class);

            int status = response.getStatus();
            String enityStr = (String) response.getEntity(String.class);
            debug(status, enityStr);
            
        } catch (Exception ex) {
        }
    }
    
    // 中止項目
    public List<DisconItemModel> getDisconItemModels() {
        
        try {
            String path = RES_BASE + "discon";

            ClientResponse response = getClientRequest(path, null)
                    .accept(MEDIATYPE_JSON_UTF8)
                    .get(ClientResponse.class);

            int status = response.getStatus();
            String entityStr = (String) response.getEntity(String.class);
            debug(status, entityStr);

            if (status != HTTP200) {
                return null;
            }
            
            TypeReference typeRef = new TypeReference<List<DisconItemModel>>(){};
            List<DisconItemModel> list = (List<DisconItemModel>)
                    getConverter().fromJson(entityStr, typeRef);

            return list;
            
        } catch (Exception ex) {
            return null;
        }
    }

    public void addDisconItemModel(DisconItemModel model) {
        
        try {
            String path = RES_BASE + "discon";
            
            String json = getConverter().toJson(model);
            
            ClientResponse response = getClientRequest(path, null)
                    .body(MEDIATYPE_JSON_UTF8, json)
                    .post(ClientResponse.class);

            int status = response.getStatus();
            String enityStr = (String) response.getEntity(String.class);
            debug(status, enityStr);
            
        } catch (Exception ex) {
        }
    }

    public void removeDisconItemModel(DisconItemModel model) {
        
        try {
            String path = RES_BASE + "discon/" + String.valueOf(model.getId());

            ClientResponse response = getClientRequest(path, null)
                    .accept(MEDIATYPE_TEXT_UTF8)
                    .delete(ClientResponse.class);

            int status = response.getStatus();
            debug(status, "delete response");
            
        } catch (Exception ex) {
        }
    }

    public void updateDisconItemModel(DisconItemModel model) {
        
        try {
            String path = RES_BASE + "discon";
            
            String json = getConverter().toJson(model);

            ClientResponse response = getClientRequest(path, null)
                    .body(MEDIATYPE_JSON_UTF8, json)
                    .put(ClientResponse.class);

            int status = response.getStatus();
            String enityStr = (String) response.getEntity(String.class);
            debug(status, enityStr);
            
        } catch (Exception ex) {
        }
    }

    // 採用薬
    public List<UsingDrugModel> getUsingDrugModels() {
        
        try {
            String path = RES_BASE + "usingDrug";

            ClientResponse response = getClientRequest(path, null)
                    .accept(MEDIATYPE_JSON_UTF8)
                    .get(ClientResponse.class);

            int status = response.getStatus();
            String entityStr = (String) response.getEntity(String.class);
            debug(status, entityStr);

            if (status != HTTP200) {
                return null;
            }

            TypeReference typeRef = new TypeReference<List<UsingDrugModel>>(){};
            List<UsingDrugModel> list = (List<UsingDrugModel>)
                    getConverter().fromJson(entityStr, typeRef);

            return list;
            
        } catch (Exception ex) {
            return null;
        }
    }

    public void addUsingDrugModel(UsingDrugModel model) {
        
        try {
            String path = RES_BASE + "usingDrug";

            String json = getConverter().toJson(model);

            ClientResponse response = getClientRequest(path, null)
                        .body(MEDIATYPE_JSON_UTF8, json)
                        .post(ClientResponse.class);

            int status = response.getStatus();
            String enityStr = (String) response.getEntity(String.class);
            debug(status, enityStr);
            
        } catch (Exception ex) {
        }
    }

    public void removeUsingDrugModel(UsingDrugModel model) {
        
        try {
            String path = RES_BASE + "usingDrug/" + String.valueOf(model.getId());

            ClientResponse response = getClientRequest(path, null)
                        .accept(MEDIATYPE_TEXT_UTF8)
                        .delete(ClientResponse.class);

            int status = response.getStatus();
            debug(status, "delete response");
            
        } catch (Exception ex) {
        }
    }

    public void updateUsingDrugModel(UsingDrugModel model) {
        
        try {
            String path = RES_BASE + "usingDrug";

            String json = getConverter().toJson(model);

            ClientResponse response = getClientRequest(path, null)
                        .body(MEDIATYPE_JSON_UTF8, json)
                        .put(ClientResponse.class);

            int status = response.getStatus();
            String enityStr = (String) response.getEntity(String.class);
            debug(status, enityStr);
            
        } catch (Exception ex) {
        }
    }

    // 指定したEntityのModuleModelをがさっと取ってくる
    public List<ModuleModel> getModulesEntitySearch(long karteId, Date fromDate, Date toDate, List<String> entities) {
        
        try {
            if (entities == null || entities.isEmpty()) {
                return null;
            }
            
            String path = RES_BASE + "moduleSearch/" + String.valueOf(karteId);
            MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
            qmap.add("fromDate", toRestFormat(fromDate));
            qmap.add("toDate", toRestFormat(toDate));
            qmap.add("entities", getConverter().fromList(entities));

            ClientResponse response = getClientRequest(path, qmap)
                    .accept(MEDIATYPE_JSON_UTF8)
                    .get(ClientResponse.class);

            int status = response.getStatus();
            String entityStr = (String) response.getEntity(String.class);
            debug(status, entityStr);
            
            if (status != HTTP200) {
                return null;
            }
            
            TypeReference typeRef = new TypeReference<List<ModuleModel>>(){};
            List<ModuleModel> list = (List<ModuleModel>)
                    getConverter().fromJson(entityStr, typeRef);
            
            for (ModuleModel module : list) {
                module.setModel((InfoModel) BeanUtils.xmlDecode(module.getBeanBytes()));
            }

            return list;
            
        } catch (Exception ex) {
            return null;
        }
    }

    // FEV-70に患者情報を登録するときに使用する。PatientVisitを扱うが、ここに居候することにした
    public PatientVisitModel getLastPvtInThisMonth(PatientVisitModel pvt) {
        
        try {
            // long ptid は設定されていないのでだめ!
            //long ptId = pvt.getPatientModel().getId();
            String ptId = pvt.getPatientModel().getPatientId();

            String path = RES_BASE + "lastPvt/" + ptId;

            ClientResponse response = getClientRequest(path, null)
                    .accept(MEDIATYPE_JSON_UTF8)
                    .get(ClientResponse.class);

            int status = response.getStatus();
            String entityStr = (String) response.getEntity(String.class);
            debug(status, entityStr);

            if (status != HTTP200) {
                return null;
            }

            PatientVisitModel model = (PatientVisitModel)
                    getConverter().fromJson(entityStr, PatientVisitModel.class);

            return model;
            
        } catch (Exception ex) {
            return null;
        }
    }

    // 指定したdocIdのDocinfoModelを取得する
    public List<DocInfoModel> getDocumentList(List<Long> docPkList) {
        
        try {
            if (docPkList == null || docPkList.isEmpty()) {
                return null;
            }

            String path = RES_BASE + "docList";
            MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
            qmap.add("ids", getConverter().fromList(docPkList));

            ClientResponse response = getClientRequest(path, qmap)
                    .accept(MEDIATYPE_JSON_UTF8)
                    .get(ClientResponse.class);

            int status = response.getStatus();
            String entityStr = (String) response.getEntity(String.class);
            debug(status, entityStr);

            if (status != HTTP200) {
                return null;
            }
            
            TypeReference typeRef = new TypeReference<List<DocInfoModel>>(){};
            List<DocInfoModel> list = (List<DocInfoModel>)
                    getConverter().fromJson(entityStr, typeRef);

            return list;
            
        } catch (Exception ex) {
            return null;
        }
    }

    // Hibernate Searchの初期インデックスを作成する
    public String makeDocumentModelIndex(long fromDocPk, int maxResults) {
        
        try {
            String path = RES_BASE +"search/makeIndex";
            MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
            qmap.add("fromDocPk", String.valueOf(fromDocPk));
            qmap.add("maxResults", String.valueOf(maxResults));

            ClientResponse response = getClientRequest(path, qmap)
                    .accept(MEDIATYPE_TEXT_UTF8)
                    .get(ClientResponse.class);

            int status = response.getStatus();
            String entityStr = (String) response.getEntity(String.class);
            debug(status, entityStr);

            return entityStr;
        } catch (Exception ex) {
            return null;
        }
    }

    // HibernteSearchによる全文検索
    public List<PatientModel> getKarteFullTextSearch(long karteId, String text) {
        
        try {
            String path = RES_BASE + "search/hibernate";
            MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
            qmap.add("karteId", String.valueOf(karteId));
            qmap.add("text", text);

            ClientResponse response = getClientRequest(path, qmap)
                    .accept(MEDIATYPE_JSON_UTF8)
                    .get(ClientResponse.class);

            int status = response.getStatus();
            String entityStr = (String) response.getEntity(String.class);
            debug(status, entityStr);

            if (status != HTTP200) {
                return null;
            }
            
            TypeReference typeRef = new TypeReference<List<PatientModel>>(){};
            List<PatientModel> list = (List<PatientModel>)
                    getConverter().fromJson(entityStr, typeRef);
            
            return list;
            
        } catch (Exception ex) {
            return null;
        }
    }

    // grep方式の全文検索
    public SearchResultModel getSearchResult(String text, long fromId, int maxResult, boolean progressCourseOnly) {
        
        try {
            String path = RES_BASE + "search/grep";
            MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
            qmap.add("text", text);
            qmap.add("fromId", String.valueOf(fromId));
            qmap.add("maxResult", String.valueOf(maxResult));
            qmap.add("pcOnly", String.valueOf(progressCourseOnly));

            ClientResponse response = getClientRequest(path, qmap)
                    .accept(MEDIATYPE_JSON_UTF8)
                    .get(ClientResponse.class);

            int status = response.getStatus();
            String entityStr = (String) response.getEntity(String.class);
            debug(status, entityStr);

            if (status != HTTP200) {
                return null;
            }
            
            SearchResultModel model = (SearchResultModel)
                    getConverter().fromJson(entityStr, SearchResultModel.class);
            
            return model;
            
        } catch (Exception ex) {
            return null;
        }
    }

    // 検査履歴を取得する
    public List<ExamHistoryModel> getExamHistory(long karteId, Date fromDate, Date toDate) {
        
        try {
            String path = RES_BASE + "examHistory/" + String.valueOf(karteId);
            MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
            qmap.add("fromDate", toRestFormat(fromDate));
            qmap.add("toDate", toRestFormat(toDate));
            
            ClientResponse response = getClientRequest(path, qmap)
                    .accept(MEDIATYPE_JSON_UTF8)
                    .get(ClientResponse.class);
            
            int status = response.getStatus();
            String entityStr = (String) response.getEntity(String.class);
            debug(status, entityStr);

            if (status != HTTP200) {
                return null;
            }
            
            TypeReference typeRef = new TypeReference<List<ExamHistoryModel>>(){};
            List<ExamHistoryModel> list = (List<ExamHistoryModel>)
                    getConverter().fromJson(entityStr, typeRef);
            
            return list;
            
        } catch (Exception ex) {
            return null;
        }
    }
    
    // 処方切れ患者を検索する
    public List<PatientModel> getOutOfMedPatient(Date fromDate, Date toDate, int yoyuu) {
        
        try {
            String path = RES_BASE + "outOfMed";
            MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
            qmap.add("fromDate", toRestFormat(fromDate));
            qmap.add("toDate", toRestFormat(toDate));
            qmap.add("yoyuu", String.valueOf(yoyuu));

            ClientResponse response = getClientRequest(path, qmap)
                    .accept(MEDIATYPE_JSON_UTF8)
                    .get(ClientResponse.class);
            
            int status = response.getStatus();
            String entityStr = (String) response.getEntity(String.class);
            debug(status, entityStr);

            if (status != HTTP200) {
                return null;
            }
            
            TypeReference typeRef = new TypeReference<List<PatientModel>>(){};
            List<PatientModel> list = (List<PatientModel>)
                    getConverter().fromJson(entityStr, typeRef);
            
            return list;
        } catch (Exception ex) {
            return null;
        }
    }

    // 施設内検査
    public List<InFacilityLaboItem> getInFacilityLaboItemList() {
        
        try {
            String path = RES_BASE + "inFacilityLabo/list";
            ClientResponse response = getClientRequest(path, null)
                    .accept(MEDIATYPE_JSON_UTF8)
                    .get(ClientResponse.class);
            
            int status = response.getStatus();
            String entityStr = (String) response.getEntity(String.class);
            debug(status, entityStr);

            if (status != HTTP200) {
                return null;
            }
            
            TypeReference typeRef = new TypeReference<List<InFacilityLaboItem>>(){};
            List<InFacilityLaboItem> list = (List<InFacilityLaboItem>)
                    getConverter().fromJson(entityStr, typeRef);
            
            return list;
            
        } catch (Exception ex) {
            return null;
        }
    }
    
    public void updateInFacilityLaboItemList(List<InFacilityLaboItem> list) {
        
        try {
            String path = RES_BASE + "inFacilityLabo/list";
            
            String json = getConverter().toJson(list);

            ClientResponse response = getClientRequest(path, null)
                    .body(MEDIATYPE_JSON_UTF8, json)
                    .put(ClientResponse.class);

            int status = response.getStatus();
            String enityStr = (String) response.getEntity(String.class);
            debug(status, enityStr);
            
        } catch (Exception ex) {
        }
    }
    
    // 電子点数表　未使用
    public String updateETensu1Table(List<ETensuModel1> list) {
        
        try {
            String path = RES_BASE + "etensu/update/";

            String json = getConverter().toJson(list);

            ClientResponse response = getClientRequest(path, null)
                    .body(MEDIATYPE_JSON_UTF8, json)
                    .post(ClientResponse.class);
            
            int status = response.getStatus();
            String ret = (String) response.getEntity(String.class);
            debug(status, ret);

            if (status != HTTP200) {
                return null;
            }
            
            return ret;
            
        } catch (Exception ex) {
            return null;
        }
    }

    public String initSanteiHistory(long fromId, int maxResults) {
        
        try {
            String path = RES_BASE + "santeiHistory/init";
            
            MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
            qmap.add("fromId", String.valueOf(fromId));
            qmap.add("maxResults", String.valueOf(maxResults));
            
            ClientResponse response = getClientRequest(path, qmap)
                    .accept(MEDIATYPE_TEXT_UTF8)
                    .get(ClientResponse.class);
            
            int status = response.getStatus();
            String ret = (String) response.getEntity(String.class);
            debug(status, ret);

            if (status != HTTP200) {
                return null;
            }
            
            return ret;
            
        } catch (Exception ex) {
            return null;
        }
    }
    
    public List<SanteiHistoryModel> getSanteiHistory(long karteId, Date fromDate, Date toDate, List<String> srycds) {
        
        try {
            String path = RES_BASE + "santeiHistory/" + String.valueOf(karteId);
            MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
            qmap.add("fromDate", toRestFormat(fromDate));
            qmap.add("toDate", toRestFormat(toDate));
            if (srycds != null) {
                qmap.add("srycds", getConverter().fromList(srycds));
            }
            
            ClientResponse response = getClientRequest(path, qmap)
                    .accept(MEDIATYPE_JSON_UTF8)
                    .get(ClientResponse.class);
            
            int status = response.getStatus();
            String entityStr = (String) response.getEntity(String.class);
            debug(status, entityStr);

            if (status != HTTP200) {
                return null;
            }

            TypeReference typeRef = new TypeReference<List<SanteiHistoryModel>>(){};
            List<SanteiHistoryModel> list = (List<SanteiHistoryModel>)
                    getConverter().fromJson(entityStr, typeRef);
            
            return list;
            
        } catch (Exception ex) {
            return null;
        }
    }
    
    
    public List<List<RpModel>> getRpHistory(long karteId, Date fromDate, Date toDate, boolean lastOnly) {
        
        try {
            String path = RES_BASE + "rpHistory/list/" + String.valueOf(karteId);
            MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
            qmap.add("fromDate", toRestFormat(fromDate));
            qmap.add("toDate", toRestFormat(toDate));
            qmap.add("lastOnly", String.valueOf(lastOnly));
            
            ClientResponse response = getClientRequest(path, qmap)
                    .accept(MEDIATYPE_JSON_UTF8)
                    .get(ClientResponse.class);
            
            int status = response.getStatus();
            String entityStr = (String) response.getEntity(String.class);
            debug(status, entityStr);

            if (status != HTTP200) {
                return null;
            }
            
            TypeReference typeRef = new TypeReference<List<List<RpModel>>>(){};
            List<List<RpModel>> list = (List<List<RpModel>>)
                    getConverter().fromJson(entityStr, typeRef);
            
            return list;
            
        } catch (Exception ex) {
            return null;
        }
    }
    
    public void postUserProperties(String userId, List<UserPropertyModel> list) {
        
        try {
            String path = RES_BASE + "userProperty/" + userId;
            
            String json = getConverter().toJson(list);
            
            ClientResponse response = getClientRequest(path, null)
                    .body(MEDIATYPE_JSON_UTF8, json)
                    .post(ClientResponse.class);

            int status = response.getStatus();
            String enityStr = (String) response.getEntity(String.class);
            debug(status, enityStr);
            
        } catch (Exception ex) {
        }
    }
    
    public List<UserPropertyModel> getUserProperties(String userId) {
        
        try {
            String path = RES_BASE + "userProperty/" + userId;
            
            ClientResponse response = getClientRequest(path, null)
                    .accept(MEDIATYPE_JSON_UTF8)
                    .get(ClientResponse.class);
            
            int status = response.getStatus();
            String entityStr = (String) response.getEntity(String.class);
            debug(status, entityStr);

            if (status != HTTP200) {
                return null;
            }
            
            TypeReference typeRef = new TypeReference<List<UserPropertyModel>>(){};
            List<UserPropertyModel> list = (List<UserPropertyModel>) 
                    getConverter().fromJson(entityStr, typeRef);
            
            return list;
            
        } catch (Exception ex) {
            return null;
        }
    }
    
    // ユーザーの、現時点で過去日になった仮保存カルテを取得する
    public List<PatientModel> getTempDocumentPatients(Date fromDate, long userPk) {
        
        try {
            String path = RES_BASE + "tempKarte/" + String.valueOf(userPk);
            
            MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();
            qmap.add("fromDate", toRestFormat(fromDate));
            
            ClientResponse response = getClientRequest(path, qmap)
                    .accept(MEDIATYPE_JSON_UTF8)
                    .get(ClientResponse.class);
            
            int status = response.getStatus();
            String entityStr = (String) response.getEntity(String.class);
            debug(status, entityStr);

            if (status != HTTP200) {
                return null;
            }
            
            TypeReference typeRef = new TypeReference<List<PatientModel>>(){};
            List<PatientModel> list = (List<PatientModel>) 
                    getConverter().fromJson(entityStr, typeRef);
            
            return list;
            
        } catch (Exception ex) {
            return null;
        }
    }
    
    @Override
    protected void debug(int status, String entity) {
        if (debug || DEBUG) {
            super.debug(status, entity);
        }
    }
}
