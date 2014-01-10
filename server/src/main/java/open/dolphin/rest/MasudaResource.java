package open.dolphin.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Date;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import open.dolphin.infomodel.*;
import open.dolphin.session.MasudaServiceBean;

/**
 * MasudaResource
 * @author masuda, Masuda Naika
 */
@Path("masuda")
public class MasudaResource extends AbstractResource {
    
    private static final boolean debug = false;
    
    @Inject
    private MasudaServiceBean masudaServiceBean;

    
    public MasudaResource() {
    }
    
    @GET
    @Path("routineMed/list/{karteId}")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getRoutineMedModels(@PathParam("karteId") Long karteId,
                @QueryParam(FIRST_RESULT) Integer firstResult,
                @QueryParam(MAX_RESULTS) Integer maxResults) {
        
        List<RoutineMedModel> list = masudaServiceBean.getRoutineMedModels(karteId, firstResult, maxResults);
        
        TypeReference typeRef = new TypeReference<List<RoutineMedModel>>(){};
        StreamingOutput so = getJsonOutStream(list, typeRef);
        
        return Response.ok(so).build();
    }
    
    @GET
    @Path("routineMed/{id}")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getRoutineMedModel(@PathParam("param") Long id) {
        
        RoutineMedModel model = masudaServiceBean.getRoutineMedModel(id);
        
        StreamingOutput so = getJsonOutStream(model);
        
        return Response.ok(so).build();
    }
    
    @DELETE
    @Path("routineMed/{id}/")
    public void removeRoutineMedModel(@PathParam("id") Long id) {

        long ret = masudaServiceBean.removeRoutineMedModel(id);

        debug(String.valueOf(ret));
    }
    
    @POST
    @Path("routineMed/")
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public Response addRoutineMedModel(String json) {
        
        RoutineMedModel model = (RoutineMedModel)
                getConverter().fromJson(json, RoutineMedModel.class);
        
        long id = masudaServiceBean.addRoutineMedModel(model);
        
        String ret = String.valueOf(id);
        debug(ret);
        
        return Response.ok(ret).build();
    }
    
    @PUT
    @Path("routineMed/")
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public Response updateRoutineMedModel(String json) {
        
        RoutineMedModel model = (RoutineMedModel)
                getConverter().fromJson(json, RoutineMedModel.class);
        
        long id = masudaServiceBean.updateRoutineMedModel(model);
        
        String ret = String.valueOf(id);
        debug(ret);
        
        return Response.ok(ret).build();
    }
    
    // 中止項目
    @GET
    @Path("discon")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getDisconItemModels() {

        // 施設
        String fid = getRemoteFacility();

        List<DisconItemModel> list = masudaServiceBean.getDisconItems(fid);

        TypeReference typeRef = new TypeReference<List<DisconItemModel>>(){};
        StreamingOutput so = getJsonOutStream(list, typeRef);
        
        return Response.ok(so).build();
    }

    @POST
    @Path("discon")
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public Response addDisconItemModel(String json) {

        String fid = getRemoteFacility();

        DisconItemModel model = (DisconItemModel)
                getConverter().fromJson(json, DisconItemModel.class);
        model.setFacilityId(fid);

        long id = masudaServiceBean.addDisconItem(model);
        String ret = String.valueOf(id);
        debug(ret);
        
        return Response.ok(ret).build();
    }

    @PUT
    @Path("discon")
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public Response updateDiconItemModel(String json) {

        String fid = getRemoteFacility();

        DisconItemModel model = (DisconItemModel)
                getConverter().fromJson(json, DisconItemModel.class);
        model.setFacilityId(fid);

        long id = masudaServiceBean.updateDisconItem(model);
        String ret = String.valueOf(id);
        debug(ret);
        
        return Response.ok(ret).build();
    }

    @DELETE
    @Path("discon/{param}")
    public void removeDisconItem(@PathParam("param") String param) {

        long id = Long.valueOf(param);

        long ret = masudaServiceBean.removeDisconItem(id);

        debug(String.valueOf(ret));
    }

    // 採用薬
    @GET
    @Path("usingDrug")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getUsingDrugModels() {

        // 施設
        String fid = getRemoteFacility();

        List<UsingDrugModel> list = masudaServiceBean.getUsingDrugModels(fid);

        TypeReference typeRef = new TypeReference<List<UsingDrugModel>>(){};
        StreamingOutput so = getJsonOutStream(list, typeRef);
        
        return Response.ok(so).build();
    }

    @POST
    @Path("usingDrug")
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public Response addUsingDrugModel(String json) {

        String fid = getRemoteFacility();
        
        UsingDrugModel model = (UsingDrugModel)
                getConverter().fromJson(json, UsingDrugModel.class);
        model.setFacilityId(fid);

        long id = masudaServiceBean.addUsingDrugModel(model);
        String ret = String.valueOf(id);
        debug(ret);
        
        return Response.ok(ret).build();
    }

    @PUT
    @Path("usingDrug")
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public Response updateUsingDrugModel(String json) {

        String fid = getRemoteFacility();

        UsingDrugModel model = (UsingDrugModel)
                getConverter().fromJson(json, UsingDrugModel.class);
        model.setFacilityId(fid);

        long id = masudaServiceBean.updateUsingDrugModel(model);
        String ret = String.valueOf(id);
        debug(ret);
        
        return Response.ok(ret).build();
    }

    @DELETE
    @Path("usingDrug/{param}")
    public void removeUsingDrugModel(@PathParam("param") String param) {

        long id = Long.parseLong(param);

        long ret = masudaServiceBean.removeUsingDrugModel(id);

        debug(String.valueOf(ret));
    }

    // 指定したEntityのModuleModelをまとめて取得
    @GET
    @Path("moduleSearch/{karteId}")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getModulesEntitySearch(@PathParam("karteId") Long karteId,
                @QueryParam(FROM_DATE) String fromDateStr,
                @QueryParam(TO_DATE) String toDateStr,
                @QueryParam(ENTITIES) String entitiesStr) {

        String fid = getRemoteFacility();

        Date fromDate = parseDate(fromDateStr);
        Date toDate = parseDate(toDateStr);
        
        List<String> entities = getConverter().toStrList(entitiesStr);

        List<ModuleModel> list = masudaServiceBean.getModulesEntitySearch(fid, karteId, fromDate, toDate, entities);
        
        TypeReference typeRef = new TypeReference<List<ModuleModel>>(){};
        StreamingOutput so = getJsonOutStream(list, typeRef);
        
        return Response.ok(so).build();
    }

    // 今月の最後の受診日を調べる
    @GET
    @Path("lastPvt/{ptId}/")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getLastPvtInThisMonth(@PathParam("ptId") String ptId) {

        String fid = getRemoteFacility();
        
        PatientVisitModel model = masudaServiceBean.getLastPvtInThisMonth(fid, ptId);
        
        StreamingOutput so = getJsonOutStream(model);
        
        return Response.ok(so).build();
    }

    // 指定したidのdocInfoをまとめて取得、検索結果で使用
    @GET
    @Path("docList")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getDocumentList(@QueryParam(IDS) String ids) {

        List<Long> docPkList = getConverter().toLongList(ids);

        List<DocInfoModel> list = masudaServiceBean.getDocumentList(docPkList);
        
        TypeReference typeRef = new TypeReference<List<DocInfoModel>>(){};
        StreamingOutput so = getJsonOutStream(list, typeRef);
        
        return Response.ok(so).build();
    }

    @GET
    @Path("search/makeIndex")
    @Produces(MEDIATYPE_TEXT_UTF8)
    public Response makeDocumentModelIndex(
            @QueryParam(FROM_DOC_PK) Long fromDocPk, 
            @QueryParam(MAX_RESULTS) Integer maxResults) {

        String fid = getRemoteFacility();

        String ret = masudaServiceBean.makeDocumentModelIndex(fid, fromDocPk, maxResults);

        debug(ret);
        return Response.ok(ret).build();
    }

    @GET
    @Path("search/hibernate")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getKarteFullTextSearch(
            @QueryParam(KARTE_ID) Long karteId,
            @QueryParam(TEXT) String text) {

        String fid = getRemoteFacility();

        List<PatientModel> list = masudaServiceBean.getKarteFullTextSearch(fid, karteId, text);

        TypeReference typeRef = new TypeReference<List<PatientModel>>(){};
        StreamingOutput so = getJsonOutStream(list, typeRef);
        
        return Response.ok(so).build();
    }

    @GET
    @Path("search/grep")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getSearchResult(
            @QueryParam(TEXT) String text,
            @QueryParam(FROM_ID) Long fromId,
            @QueryParam(MAX_RESULTS) Integer maxResult,
            @QueryParam(PC_ONLY) Boolean progressCourseOnly) {

        String fid = getRemoteFacility();

        SearchResultModel model = masudaServiceBean.getSearchResult(fid, text, fromId, maxResult, progressCourseOnly);
        
        StreamingOutput so = getJsonOutStream(model);
        
        return Response.ok(so).build();
    }
    
    @GET
    @Path("examHistory/{karteId}")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getExamHistory(@PathParam("karteId") Long karteId,
            @QueryParam(FROM_DATE) String fromDateStr,
            @QueryParam(TO_DATE) String toDateStr) {
        
        String fid = getRemoteFacility();
        Date fromDate = parseDate(fromDateStr);
        Date toDate = parseDate(toDateStr);
        
        List<ExamHistoryModel> list = masudaServiceBean.getExamHistory(fid, karteId, fromDate, toDate);
        
        TypeReference typeRef = new TypeReference<List<ExamHistoryModel>>(){};
        StreamingOutput so = getJsonOutStream(list, typeRef);
        
        return Response.ok(so).build();
    }
    
    @GET
    @Path("outOfMed")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getOutOfMedStockPatient(
            @QueryParam(FROM_DATE) String fromDateStr,
            @QueryParam(TO_DATE) String toDateStr,
            @QueryParam(YOYUU) Integer yoyuu) {

        String fid = getRemoteFacility();
        Date fromDate = parseDate(fromDateStr);
        Date toDate = parseDate(toDateStr);

        List<PatientModel> list = masudaServiceBean.getOutOfMedStockPatient(fid, fromDate, toDate, yoyuu);
        
        TypeReference typeRef = new TypeReference<List<PatientModel>>(){};
        StreamingOutput so = getJsonOutStream(list, typeRef);
        
        return Response.ok(so).build();
    }
    
    @GET
    @Path("inFacilityLabo/list")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getInFacilityLaboItemList() {
        
        String fid = getRemoteFacility();
        List<InFacilityLaboItem> list = masudaServiceBean.getInFacilityLaboItemList(fid);
        
        TypeReference typeRef = new TypeReference<List<InFacilityLaboItem>>(){};
        StreamingOutput so = getJsonOutStream(list, typeRef);
        
        return Response.ok(so).build();
    }
    
    @PUT
    @Path("inFacilityLabo/list")
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public Response updateInFacilityLaboItem(String json) {
        
        String fid = getRemoteFacility();
        
        TypeReference typeRef = new TypeReference<List<InFacilityLaboItem>>(){};
        List<InFacilityLaboItem> list = (List<InFacilityLaboItem>)
                getConverter().fromJson(json, typeRef);
        
        long ret = masudaServiceBean.updateInFacilityLaboItem(fid, list);
        
        return Response.ok(String.valueOf(ret)).build();
    }

    // 電子点数表　未使用
    @POST
    @Path("etensu/update")
    @Produces(MEDIATYPE_TEXT_UTF8)
    public Response updateETensu1Table(String json) {
        
        TypeReference typeRef = new TypeReference<List<ETensuModel1>>(){};
        List<ETensuModel1> list = (List<ETensuModel1>)
                getConverter().fromJson(json, typeRef);
        
        String ret = masudaServiceBean.updateETensu1Table(list);
        
        return Response.ok(ret).build();
    }
    
    @GET
    @Path("santeiHistory/init")
    @Produces(MEDIATYPE_TEXT_UTF8)
    public Response initSanteiHistory(
            @QueryParam(FROM_ID) Long fromIndex,
            @QueryParam(MAX_RESULTS) Integer maxResults) {
        
        String fid = getRemoteFacility();
        
        String ret = masudaServiceBean.initSanteiHistory(fid, fromIndex, maxResults);
        
        return Response.ok(ret).build();
    }
    
    @GET
    @Path("santeiHistory/{karteId}")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getSanteiHistory(@PathParam("karteId") Long karteId,
            @QueryParam(FROM_DATE) String fromDateStr,
            @QueryParam(TO_DATE) String toDateStr,
            @QueryParam(SRYCDS) String srycds) {
    
        Date fromDate = parseDate(fromDateStr);
        Date toDate = parseDate(toDateStr);
        List<String> srycdList = null;
        if (srycds != null && !srycds.isEmpty()) {
            srycdList = getConverter().toStrList(srycds);
        }
        
        List<SanteiHistoryModel> list = masudaServiceBean.getSanteiHistory(karteId, fromDate, toDate, srycdList);
        
        TypeReference typeRef = new TypeReference<List<SanteiHistoryModel>>(){};
        StreamingOutput so = getJsonOutStream(list, typeRef);
        
        return Response.ok(so).build();
    }
    
    @GET
    @Path("rpHistory/list/{karteId}")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getRpHistory(@PathParam("karteId") Long karteId,
            @QueryParam(FROM_DATE) String fromDateStr,
            @QueryParam(TO_DATE) String toDateStr,
            @QueryParam(LAST_ONLY) Boolean lastOnly) {

        Date fromDate = parseDate(fromDateStr);
        Date toDate = parseDate(toDateStr);
        
        List<List<RpModel>> list = masudaServiceBean.getRpModelList(karteId, fromDate, toDate, lastOnly);
         
        TypeReference typeRef = new TypeReference<List<List<RpModel>>>(){};
        StreamingOutput so = getJsonOutStream(list, typeRef);
        
        return Response.ok(so).build();
    }
    
    @POST
    @Path("userProperty/{uid}")
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public Response postUserProperties(@PathParam("uid") String userId, String json) {
        
        TypeReference typeRef = new TypeReference<List<UserPropertyModel>>(){};
        List<UserPropertyModel> list = (List<UserPropertyModel>) 
                getConverter().fromJson(json, typeRef);
        
        int cnt = masudaServiceBean.postUserProperties(list);
        
        return Response.ok(String.valueOf(cnt)).build();
    }
    
    @GET
    @Path("userProperty/{uid}")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getUserProperties(@PathParam("uid") String userId) {
        
        List<UserPropertyModel> list = masudaServiceBean.getUserProperties(userId);

        TypeReference typeRef = new TypeReference<List<UserPropertyModel>>(){};
        StreamingOutput so = getJsonOutStream(list, typeRef);
        
        return Response.ok(so).build();
    }

    
    @PUT
    @Path("admission")
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public Response putAdmissionModels(String json) {
        
        TypeReference typeRef = new TypeReference<List<AdmissionModel>>(){};
        List<AdmissionModel> list = (List<AdmissionModel>) 
                getConverter().fromJson(json, typeRef);
        
        int cnt = masudaServiceBean.updateAdmissionModels(list);
        
        return Response.ok(String.valueOf(cnt)).build();
    }
    
    @DELETE
    @Path("admission/{ids}")
    @Produces(MEDIATYPE_TEXT_UTF8)
    public Response deleteAdmissionModels(@PathParam("ids") String ids) {
        
        List<Long> idList = getConverter().toLongList(ids);
        
        int cnt = masudaServiceBean.deleteAdmissionModels(idList);
        
        return Response.ok(String.valueOf(cnt)).build();
    }
    
    @GET
    @Path("tempKarte/{userId}")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getTempKartePatients(@PathParam("userId") String userPkStr, 
            @QueryParam(FROM_DATE) String fromDateStr) {
        
        Date fromDate = parseDate(fromDateStr);
        long userPk = Long.valueOf(userPkStr);

        List<PatientModel> list = masudaServiceBean.getTempDocumentPatients(fromDate, userPk);
        
        TypeReference typeRef = new TypeReference<List<PatientModel>>(){};
        StreamingOutput so = getJsonOutStream(list, typeRef);
        
        return Response.ok(so).build();
    }
    
    @POST
    @Path("indication/postget")
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getIndicationList(String json) {
        
        String fid = getRemoteFacility();
        TypeReference typeRef1 = new TypeReference<List<String>>(){};
        List<String> srycds = (List<String>)
                getConverter().fromJson(json, typeRef1);
        
        List<IndicationModel> list = masudaServiceBean.getIndicationList(fid, srycds);
        
        TypeReference typeRef2 = new TypeReference<List<IndicationModel>>(){};
        StreamingOutput so = getJsonOutStream(list, typeRef2);
        
        return Response.ok(so).build();
    }
    
    @GET
    @Path("indication/{srycd}")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getIndicationModel(@PathParam(SRYCD) String srycd) {
        
        String fid = getRemoteFacility();
        IndicationModel model = masudaServiceBean.getIndicationModel(fid, srycd);
        StreamingOutput so = getJsonOutStream(model);
        
        return Response.ok(so).build();
    }
    
    @POST
    @Path("indication/list")
    @Produces(MEDIATYPE_TEXT_UTF8)
    public Response addIndicationModels(String json) {
        
        TypeReference typeRef = new TypeReference<List<IndicationModel>>(){};
        List<IndicationModel> list = (List<IndicationModel>) 
                getConverter().fromJson(json, typeRef);
        
        int cnt = masudaServiceBean.addIndicationModels(list);
        
        return Response.ok(String.valueOf(cnt)).build();
    }
    
    @PUT
    @Path("indication")
    @Produces(MEDIATYPE_TEXT_UTF8)
    public Response updateIndicationModel(String json) {

        IndicationModel model = (IndicationModel) 
                getConverter().fromJson(json, IndicationModel.class);
        
        long id = masudaServiceBean.updateIndicationModel(model);
        
        return Response.ok(String.valueOf(id)).build();
    }
    
    @DELETE
    @Path("indication/{param}")
    public void removeIndicationModel(@PathParam("param") String param) {

        long id = Long.parseLong(param);

        long ret = masudaServiceBean.removeIndicationModel(id);

        debug(String.valueOf(ret));
    }
    
    @Override
    protected void debug(String msg) {
        if (debug || DEBUG) {
            super.debug(msg);
        }
    }
    
    
}
