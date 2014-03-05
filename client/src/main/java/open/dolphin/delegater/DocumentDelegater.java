package open.dolphin.delegater;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.ImageIcon;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import open.dolphin.client.ImageEntry;
import open.dolphin.dto.DocumentSearchSpec;
import open.dolphin.dto.ImageSearchSpec;
import open.dolphin.dto.ModuleSearchSpec;
import open.dolphin.common.util.BeanUtils;
import open.dolphin.infomodel.AppointmentModel;
import open.dolphin.infomodel.DocInfoModel;
import open.dolphin.infomodel.DocumentModel;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.infomodel.IModuleModel;
import open.dolphin.infomodel.KarteBean;
import open.dolphin.infomodel.LetterModule;
import open.dolphin.infomodel.ModuleModel;
import open.dolphin.infomodel.ObservationModel;
import open.dolphin.infomodel.PatientMemoModel;
import open.dolphin.infomodel.RegisteredDiagnosisModel;
import open.dolphin.infomodel.SchemaModel;

/**
 * Session と Document の送受信を行う Delegater クラス。
 *
 * @author Kazushi Minagawa
 * @author modified by masuda, Masuda Naika
 * @author modified by katoh, Hashimoto iin
 */
public class  DocumentDelegater extends BusinessDelegater {

    private static final String TITLE_LETTER = "紹介状:";
    private static final String TITLE_REPLY = "返書:";
    private static final String TITLE_CERTIFICATE = "診断書";
    
    private static final boolean debug = false;
    private static final DocumentDelegater instance;

    static {
        instance = new DocumentDelegater();
    }

    public static DocumentDelegater getInstance() {
        return instance;
    }

    private DocumentDelegater() {
    }
    
    /**
     * 患者のカルテを取得する。
     * @param patientPk 患者PK
     * @param fromDate 履歴の検索開始日
     * @return カルテ
     */
    public KarteBean getKarte(long patientPK, Date fromDate) throws Exception {
        
        String path = "karte/" + String.valueOf(patientPK);

        Response response = getWebTarget()
                .path(path)
                .queryParam(FROM_DATE, toRestFormat(fromDate))
                .request(MEDIATYPE_JSON_UTF8)
                .get();

        checkHttpStatus(response);
        InputStream is = response.readEntity(InputStream.class);
        KarteBean karte = (KarteBean)
                getConverter().fromJson(is, KarteBean.class);
        
        response.close();

        return karte;
    }
    
    /**
     * Documentを保存する。
     * @param karteModel KarteModel
     * @return Result Code
     */
    public long postDocument(DocumentModel karteModel) throws Exception {
        
        // 確定日、適合開始日、記録日、ステータスを
        // DocInfo から DocumentModel(KarteEntry) に移す
        karteModel.toPersist();
        Entity entity = toJsonEntity(karteModel);
        
        String path = "karte/document";
        
        Response response = getWebTarget()
                .path(path)
                .request(MEDIATYPE_TEXT_UTF8)
                .post(entity);

        int status = checkHttpStatus(response);
        String entityStr = response.readEntity(String.class);
        debug(status, entityStr);
        
        response.close();

        return Long.parseLong(entityStr);
    }
    
    /**
     * Documentを検索して返す。
     * @param id DocumentID
     * @return DocumentValue
     */
    public List<DocumentModel> getDocuments(List<Long> ids) throws Exception {
        
        String path = "karte/document";

        Response response = getWebTarget()
                .path(path)
                .queryParam(IDS, getConverter().fromList(ids))
                .request(MEDIATYPE_JSON_UTF8)
                .get();

        checkHttpStatus(response);
        InputStream is = response.readEntity(InputStream.class);
        TypeReference typeRef = new TypeReference<List<DocumentModel>>(){};
        List<DocumentModel> list = (List<DocumentModel>)
                getConverter().fromGzippedJson(is, typeRef);
        
        response.close();

        return list;
    }

    /**
     * 文書履歴を検索して返す。
     * @param spec DocumentSearchSpec 検索仕様
     * @return DocInfoModel の Collection
     */
    public List<DocInfoModel> getDocumentList(DocumentSearchSpec spec) throws Exception {

        if (spec.getDocType().startsWith("karte")) {
            return getKarteList(spec);
        } else if (spec.getDocType().equals(IInfoModel.DOCTYPE_LETTER)) {
            return getLetterList(spec);
        }

        return null;
    }
    
//katoh^
    private List<DocInfoModel> getKarteList(DocumentSearchSpec spec) throws Exception {
        
        String path = "karte/docinfo/" + String.valueOf(spec.getKarteId());

        Response response = getWebTarget()
                .path(path)
                .queryParam(FROM_DATE, toRestFormat(spec.getFromDate()))
                .queryParam(TO_DATE, toRestFormat(spec.getToDate()))
                .queryParam(INCLUDE_MODIFIED, String.valueOf(spec.isIncludeModifid()))
                .request(MEDIATYPE_JSON_UTF8)
                .get();

        checkHttpStatus(response);
        InputStream is = response.readEntity(InputStream.class);
        TypeReference typeRef = new TypeReference<List<DocInfoModel>>(){};
        List<DocInfoModel> list = (List<DocInfoModel>)
                getConverter().fromGzippedJson(is, typeRef);
        
        response.close();
        
        return list;
    }
//katoh$
    
    private List<DocInfoModel> getLetterList(DocumentSearchSpec spec) throws Exception {
        
        String path = "odletter/list/" + String.valueOf(spec.getKarteId());

        Response response = getWebTarget()
                .path(path)
                .request(MEDIATYPE_JSON_UTF8)
                .get();

        checkHttpStatus(response);
        InputStream is = response.readEntity(InputStream.class);
        
        TypeReference typeRef = new TypeReference<List<LetterModule>>(){};
        List<LetterModule> list = (List<LetterModule>)
                getConverter().fromJson(is, typeRef);
        
        response.close();

        List<DocInfoModel> ret = new ArrayList<>();

        if (list != null && !list.isEmpty()) {
            for (LetterModule module : list) {
                DocInfoModel docInfo = new DocInfoModel();
                docInfo.setDocPk(module.getId());
                docInfo.setDocType(IInfoModel.DOCTYPE_LETTER);
                docInfo.setDocId(String.valueOf(module.getId()));
                docInfo.setConfirmDate(module.getConfirmed());
                docInfo.setFirstConfirmDate(module.getConfirmed());
                StringBuilder sb = new StringBuilder();
                if (module.getTitle()!=null) {
                    sb.append(module.getTitle());
                } else if(module.getLetterType().equals(IInfoModel.CONSULTANT)) {
                    sb.append(TITLE_REPLY).append(module.getClientHospital());
                } else if (module.getLetterType().equals(IInfoModel.CLIENT)) {
                    sb.append(TITLE_LETTER).append(module.getConsultantHospital());
                } else if (module.getLetterType().equals(IInfoModel.MEDICAL_CERTIFICATE)) {
                    sb.append(TITLE_CERTIFICATE);
                }
                docInfo.setTitle(sb.toString());
                docInfo.setHandleClass(module.getHandleClass());

                ret.add(docInfo);
            }
        } else {
            System.err.println("parse no results");
        }

        return ret;
    }
    
  
    /**
     * ドキュメントを論理削除する。
     * @param pk 論理削除するドキュメントの prmary key
     * @return 削除件数
     */
    public int deleteDocument(long pk) throws Exception {
        
        String path = "karte/document/" + String.valueOf(pk);

        Response response = getWebTarget()
                .path(path)
                .request()
                .delete();

        checkHttpStatus(response);
        
        response.close();

        return 1;
    }
    
    /**
     * 文書履歴のタイトルを変更する。
     * @param pk Document の pk
     * @return 変更した件数
     */
    public int updateTitle(DocInfoModel docInfo) {
        
        try {
            String path = "karte/document/" + String.valueOf(docInfo.getDocPk());
            Entity entity = toTextEntity(docInfo.getTitle());

            Response response = getWebTarget()
                    .path(path)
                    .request(MEDIATYPE_TEXT_UTF8)
                    .put(entity);
            
            int status = checkHttpStatus(response);
            String entityStr = response.readEntity(String.class);
            debug(status, entityStr);
            
            response.close();

            return Integer.parseInt(entityStr);
            
        } catch (Exception ex) {
            return -1;
        }
    }
    
    
    /**
     * Moduleを検索して返す。
     * @param spec ModuleSearchSpec 検索仕様
     * @return Module の Collection
     */
    public List<List<ModuleModel>> getModuleList(ModuleSearchSpec spec) throws Exception {

        String path = "karte/modules/" + String.valueOf(spec.getKarteId());

        Date[] froms = spec.getFromDate();
        Date[] tos = spec.getToDate();

        int len = froms.length;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            if (i != 0) {
                sb.append(CAMMA);
            }
            sb.append(toRestFormat(froms[i]));
        }
        String fromsStr = sb.toString();

        len = tos.length;
        sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            if (i != 0) {
                sb.append(CAMMA);
            }
            sb.append(toRestFormat(tos[i]));
        }
        String tosStr = sb.toString();

        Response response = getWebTarget()
                .path(path)
                .queryParam(FROMS, fromsStr)
                .queryParam(TOS, tosStr)
                .queryParam(ENTITY, spec.getEntity())
                .request(MEDIATYPE_JSON_UTF8)
                .get();

        checkHttpStatus(response);
        InputStream is = response.readEntity(InputStream.class);
        TypeReference typeRef = new TypeReference<List<List<ModuleModel>>>(){};
        List<List<ModuleModel>> ret = (List<List<ModuleModel>>) 
                getConverter().fromJson(is, typeRef);

        response.close();
        
        for (List<ModuleModel> list : ret) {
            for (ModuleModel module : list) {
                IModuleModel model = (IModuleModel) BeanUtils.xmlDecode(module.getBeanBytes());
                module.setModel(model);
            }
        }
        return ret;
    }
    
    /**
     * イメージを取得する。
     * @param id 画像のId
     * @return SchemaModel
     */
    public SchemaModel getImage(long id) throws Exception {

        String path = "karte/image/" + String.valueOf(id);

        Response response = getWebTarget()
                .path(path)
                .request(MEDIATYPE_JSON_UTF8)
                .get();

        checkHttpStatus(response);
        InputStream is = response.readEntity(InputStream.class);
        SchemaModel model = (SchemaModel) 
                getConverter().fromJson(is, SchemaModel.class);
        
        response.close();

        byte[] bytes = model.getJpegByte();
        ImageIcon icon = new ImageIcon(bytes);
        if (icon != null) {
            model.setIcon(icon);
        }

        return model;
    }


    /**
     * Imageを検索して返す。
     * @param spec ImageSearchSpec 検索仕様
     * @return Imageリストのリスト
     */
    public List<List<ImageEntry>> getImageList(ImageSearchSpec spec) {
/*
        String path = "karte/images/" + String.valueOf(spec.getKarteId());
        MultivaluedMap<String, String> qmap = new MultivaluedMapImpl();

        Date[] froms = spec.getFromDate();
        Date[] tos = spec.getToDate();
        
        int len = froms.length;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            if (i != 0) {
                sb.append(CAMMA);
            }
            sb.append(REST_DATE_FRMT.format(froms[i]));
        }
        qmap.add("froms", sb.toString());
        
        len = tos.length;
        sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            if (i != 0) {
                sb.append(CAMMA);
            }
            sb.append(REST_DATE_FRMT.format(tos[i]));
        }
        qmap.add("tos", sb.toString());
        
        ClientResponse response = getQueryResource(path, qmap)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get(ClientResponse.class);

        int status = response.getStatus();
        String entityStr = response.getEntity(String.class);

        debug(status, entityStr);

        if (status != HTTP200) {
            return null;
        }

        TypeReference typeRef = new TypeReference<List<List<SchemaModel>>>(){};

        // 検索結果
        List<List<SchemaModel>> result = (List<List<SchemaModel>>) 
                getConverter().fromJsonTypeRef(entityStr, typeRef);

        List<List<ImageEntry>> ret = new ArrayList<List<ImageEntry>>();
        for (List<SchemaModel> periodList : result) {

            // ImageEntry 用のリスト
            List<ImageEntry> el = new ArrayList<ImageEntry>();
            // 抽出期間をイテレートする
            for (SchemaModel model : periodList) {
                // シェーマモデルをエントリに変換しリストに加える
                ImageEntry entry = ImageTool.getImageEntryFromSchema(model, spec.getIconSize());
                el.add(entry);
            }
            // リターンリストへ追加する
            ret.add(el);

        }

        return ret;
*/
        return null;
    }

    //---------------------------------------------------------------------------
    
    public List<Long> putDiagnosis(List<RegisteredDiagnosisModel> list) throws Exception {
        
        String path = "karte/diagnosis/";
        TypeReference typeRef = new TypeReference<List<RegisteredDiagnosisModel>>(){};
        Entity entity = toJsonEntity(list, typeRef);

        Response response = getWebTarget()
                .path(path)
                .request(MEDIATYPE_TEXT_UTF8)
                .post(entity);

        int status = checkHttpStatus(response);
        String entityStr = response.readEntity(String.class);
        debug(status, entityStr);
        
        response.close();

        List<Long> ret = getConverter().toLongList(entityStr);

        return ret;
    }
    
    public int updateDiagnosis(List<RegisteredDiagnosisModel> list) throws Exception {

        String path = "karte/diagnosis/";
        TypeReference typeRef = new TypeReference<List<RegisteredDiagnosisModel>>(){};
        Entity entity = toJsonEntity(list, typeRef);

        Response response = getWebTarget()
                .path(path)
                .request(MEDIATYPE_TEXT_UTF8)
                .put(entity);

        int status = checkHttpStatus(response);

        String entityStr = response.readEntity(String.class);
        debug(status, entityStr);
        
        response.close();

        return Integer.parseInt(entityStr);
    }
    
    public int removeDiagnosis(List<Long> ids) throws Exception {
        
        String path = "karte/diagnosis/";
        
        Response response = getWebTarget()
                .path(path)
                .queryParam(IDS, getConverter().fromList(ids))
                .request()
                .delete();

        int status = checkHttpStatus(response);
        debug(status, "delete response");
        
        response.close();

        return ids.size();
    }
    
    /**
     * Diagnosisを検索して返す。
     * @param spec DiagnosisSearchSpec 検索仕様
     * @return DiagnosisModel の Collection
     */
    public List<RegisteredDiagnosisModel> getDiagnosisList(
            long karteId, Date fromDate, boolean activeOnly) throws Exception {
        
        String path = "karte/diagnosis/" + String.valueOf(karteId);

        Response response = getWebTarget()
                .path(path)
                .queryParam(FROM_DATE, toRestFormat(fromDate))
                .queryParam(ACTIVE_ONLY, String.valueOf(activeOnly))
                .request(MEDIATYPE_JSON_UTF8)
                .get();

        checkHttpStatus(response);
        InputStream is = response.readEntity(InputStream.class);
        TypeReference typeRef = new TypeReference<List<RegisteredDiagnosisModel>>(){};
        List<RegisteredDiagnosisModel> list = (List<RegisteredDiagnosisModel>)
                getConverter().fromJson(is, typeRef);
        
        response.close();

        return list;
    }

    
    public List<Long> addObservations(List<ObservationModel> observations) throws Exception {
        
        String path = "/karte/observations";
        TypeReference typeRef = new TypeReference<List<ObservationModel>>(){};
        Entity entity = toJsonEntity(observations, typeRef);

        Response response = getWebTarget()
                .path(path)
                .request(MEDIATYPE_TEXT_UTF8)
                .post(entity);

        int status = checkHttpStatus(response);
        String entityStr = response.readEntity(String.class);
        debug(status, entityStr);
        
        response.close();

        List<Long> list = getConverter().toLongList(entityStr);

        return list;
    }
    
    public int removeObservations(List<Long> ids) throws Exception {
        
        String path = "/karte/observations";
        
        Response response = getWebTarget()
                .path(path)
                .queryParam(IDS, getConverter().fromList(ids))
                .request()
                .delete();

        int status = checkHttpStatus(response);
        debug(status, "delete response");
        
        response.close();

        return ids.size();
    }

    //-------------------------------------------------------------------------
    
    public int updatePatientMemo(PatientMemoModel pm) throws Exception {
        
        String path = "/karte/memo";

        Entity entity = toJsonEntity(pm);

        Response response = getWebTarget()
                .path(path)
                .request(MEDIATYPE_TEXT_UTF8)
                .put(entity);

        int status = checkHttpStatus(response);
        String entityStr = response.readEntity(String.class);
        debug(status, entityStr);
        
        response.close();

        return Integer.parseInt(entityStr);
    }

    //-------------------------------------------------------------------------
    
    public List<List<AppointmentModel>> getAppoinmentList(ModuleSearchSpec spec) throws Exception {
        
        String path = "karte/appo/" + String.valueOf(spec.getKarteId());

        Date[] froms = spec.getFromDate();
        Date[] tos = spec.getToDate();

        int len = froms.length;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            if (i != 0) {
                sb.append(CAMMA);
            }
            sb.append(toRestFormat(froms[i]));
        }
        String fromsStr = sb.toString();

        len = tos.length;
        sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            if (i != 0) {
                sb.append(CAMMA);
            }
            sb.append(toRestFormat(tos[i]));
        }
        String tosStr = sb.toString();

        Response response = getWebTarget()
                .path(path)
                .queryParam(FROMS, fromsStr)
                .queryParam(TOS, tosStr)
                .request(MEDIATYPE_JSON_UTF8)
                .get();

        checkHttpStatus(response);
        InputStream is = response.readEntity(InputStream.class);
        TypeReference typeRef = new TypeReference<List<List<AppointmentModel>>>(){};
        List<List<AppointmentModel>> ret = (List<List<AppointmentModel>>)
                getConverter().fromJson(is, typeRef);
        
        response.close();

        return ret;
    }


    public void updatePVTState(long pvtPK, int state) throws Exception {
        
        StringBuilder sb = new StringBuilder();
        sb.append("pvt/");
        sb.append(pvtPK);
        sb.append(CAMMA);
        sb.append(state);
        String path = sb.toString();

        Response response = getWebTarget()
                .path(path)
                .request(MEDIATYPE_TEXT_UTF8)
                .put(null);

        int status = checkHttpStatus(response);
        debug(status, "put response");
        
        response.close();
    }
    
    @Override
    protected void debug(int status, String entity) {
        if (debug || DEBUG) {
            super.debug(status, entity);
        }
    }
}