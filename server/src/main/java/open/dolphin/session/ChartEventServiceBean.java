package open.dolphin.session;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.websocket.Session;
import javax.ws.rs.core.MediaType;
//import javax.ws.rs.core.Response;
import open.dolphin.common.util.JsonConverter;
import open.dolphin.infomodel.*;
//import open.dolphin.mbean.AsyncResponseModel;
import open.dolphin.mbean.ServletContextHolder;

/**
 * ChartEventServiceBean
 *
 * @author masuda, Masuda Naika
 */
@Stateless
public class ChartEventServiceBean {
    
    private static final String CHARSET_UTF8 = "; charset=UTF-8";
    private static final String MEDIATYPE_JSON_UTF8 = MediaType.APPLICATION_JSON + CHARSET_UTF8;
    
    private static final String QUERY_INSURANCE_BY_PATIENT_PK 
            = "from HealthInsuranceModel h where h.patient.id=:pk";
    
    private static final String PK = "pk";
    
    private static final Logger logger = Logger.getLogger(ChartEventServiceBean.class.getSimpleName());
    
    @Inject
    private ServletContextHolder contextHolder;
    
    @PersistenceContext
    private EntityManager em;
    
    
    public void notifyEvent(ChartEventModel evt) {

        String fid = evt.getFacilityId();

//        // AsyncResponse
//        List<AsyncResponseModel> arList = contextHolder.getAsyncResponseList();
//        List<AsyncResponseModel> arSendList = new ArrayList<>();
//        
//        for (AsyncResponseModel arModel : arList) {
//
//            String acFid = arModel.getFid();
//            String acUUID = arModel.getClientUUID();
//            String issuerUUID = evt.getIssuerUUID();
//
//            // 同一施設かつChartEventModelの発行者でないクライアントに通知する
//            // fid == nullなら全部にブロードキャストする
//            if (fid == null || (fid.equals(acFid) && !acUUID.equals(issuerUUID))) {
//                arSendList.add(arModel);
//            }
//
//        }
//        
//        deliverChartEvent(arSendList, evt);
        
        // websocket
        List<Session> sessionList = contextHolder.getSessionList();
        List<Session> wsSendList = new ArrayList<>();

        for (Session session : sessionList) {

            String acFid = (String) session.getUserProperties().get("fid");
            String acUUID = (String) session.getUserProperties().get("clientUUID");
            String issuerUUID = evt.getIssuerUUID();

            // 同一施設かつChartEventModelの発行者でないクライアントに通知する
            // fid == nullなら全部にブロードキャストする
            if (fid == null || (fid.equals(acFid) && !acUUID.equals(issuerUUID))) {
                wsSendList.add(session);
            }

        }
        
        deliverChartEventWs(wsSendList, evt);
    }

//    @Asynchronous
//    private void deliverChartEvent(List<AsyncResponseModel> sendList, ChartEventModel evt) {
//        String json = JsonConverter.getInstance().toJson(evt);
//        for (AsyncResponseModel arModel : sendList) {
//            Response response = Response.ok(json).type(MEDIATYPE_JSON_UTF8).build();
//            arModel.getAsyncResponse().resume(response);
//        }
//    }
    
    @Asynchronous
    private void deliverChartEventWs(List<Session> sessionList, ChartEventModel evt) {
        String json = JsonConverter.getInstance().toJson(evt);
        for (Session session : sessionList) {
            try {
                if (session.isOpen()) {
                    session.getBasicRemote().sendText(json);
                }
            } catch (IOException ex) {
            }
        }
    }
 
    public String getServerUUID() {
        return contextHolder.getServerUUID();
    }

    public List<PatientVisitModel> getPvtList(String fid) {
        return contextHolder.getPvtList(fid);
    }
    
    /**
     * ChartEventModelを処理する
     */
    public int processChartEvent(ChartEventModel evt) {

        ChartEventModel.EVENT eventType = evt.getEventType();
        
        switch(eventType) {
            case PVT_DELETE:
                processPvtDeleteEvent(evt);
                break;
            case PVT_STATE:
                processPvtStateEvent(evt);
                break;
            case MSG_BROADCAST:
            case MSG_REPLY:
                break;
            default:
                return 0;
        }
        // クライアントに通知
        notifyEvent(evt);

        return 1;
    }
    
    private void processPvtDeleteEvent(ChartEventModel evt) {
        
        long pvtPk = evt.getPvtPk();
        String fid = evt.getFacilityId();

        // データベースから削除
        PatientVisitModel exist = em.find(PatientVisitModel.class, pvtPk);
        // WatingListから開いていないとexist = nullなので。
        if (exist != null) {
            em.remove(exist);
        }
        // pvtListから削除
        List<PatientVisitModel> pvtList = getPvtList(fid);
        PatientVisitModel toRemove = null;
        for (PatientVisitModel model : pvtList) {
            if (model.getId() == pvtPk) {
                toRemove = model;
                break;
            }
        }
        if (toRemove != null) {
            pvtList.remove(toRemove);
        }
    }
    
    private void processPvtStateEvent(ChartEventModel evt) {
        
        // msgからパラメーターを取得
        String fid = evt.getFacilityId();
        long pvtId = evt.getPvtPk();
        int state = evt.getState();
        int byomeiCount = evt.getByomeiCount();
        int byomeiCountToday = evt.getByomeiCountToday();
        String memo = evt.getMemo();
        String ownerUUID = evt.getOwnerUUID();
        long ptPk = evt.getPtPk();

        List<PatientVisitModel> pvtList = getPvtList(fid);

        // データベースのPatientVisitModelを更新
        PatientVisitModel pvt = em.find(PatientVisitModel.class, pvtId);
        if (pvt != null) {
            pvt.setState(state);
            pvt.setByomeiCount(byomeiCount);
            pvt.setByomeiCountToday(byomeiCountToday);
            pvt.setMemo(memo);
        }
        // データベースのPatientModelを更新
        PatientModel pm = em.find(PatientModel.class, ptPk);
        if (pm != null) {
            pm.setOwnerUUID(ownerUUID);
        }

        // pvtListを更新
        for (PatientVisitModel model : pvtList) {
            if (model.getId() == pvtId) {
                model.setState(state);
                model.setByomeiCount(byomeiCount);
                model.setByomeiCountToday(byomeiCountToday);
                model.setMemo(memo);
                model.getPatientModel().setOwnerUUID(ownerUUID);
                break;
            }
        }

    }

    public void start() {
        initializePvtList();
    }
    
    // 起動後最初のPvtListを作る
    private void initializePvtList() {

        contextHolder.setToday();
        
        // サーバーの「今日」で管理する
        final SimpleDateFormat frmt = new SimpleDateFormat(IInfoModel.DATE_WITHOUT_TIME);
        String fromDate = frmt.format(contextHolder.getToday().getTime());
        String toDate = frmt.format(contextHolder.getTomorrow().getTime());

        // PatientVisitModelを施設IDで検索する
        final String sql =
                "from PatientVisitModel p " +
                "where p.pvtDate >= :fromDate and p.pvtDate < :toDate " +
                "order by p.id";
        
        List<PatientVisitModel> result =
                em.createQuery(sql)
                .setParameter("fromDate", fromDate)
                .setParameter("toDate", toDate)
                .getResultList();

        // 患者の基本データを取得する
        // 来院情報と患者は ManyToOne の関係である
        //int counter = 0;

        for (PatientVisitModel pvt : result) {
            
            String fid = pvt.getFacilityId();
            contextHolder.getPvtList(fid).add(pvt);

            PatientModel patient = pvt.getPatientModel();

            // 患者の健康保険を取得する
            setHealthInsurances(patient);

            KarteBean karte = (KarteBean)
                    em.createQuery("from KarteBean k where k.patient.id = :pk")
                    .setParameter("pk", patient.getId())
                    .getSingleResult();

            // カルテの PK を得る
            long karteId = karte.getId();

            // 予約を検索する
            List<AppointmentModel> list =
                    em.createQuery("from AppointmentModel a where a.karte.id = :karteId and a.date = :date")
                    .setParameter("karteId", karteId)
                    .setParameter("date", contextHolder.getToday().getTime())
                    .getResultList();
            if (list != null && !list.isEmpty()) {
                AppointmentModel appo = list.get(0);
                pvt.setAppointment(appo.getName());
            }

            // 病名数をチェックする
            setByomeiCount(karteId, pvt);
            // 受付番号セット
            //pvt.setNumber(++counter);
        }
        
        logger.info("ChartEventService: pvtList initialized");
    }
    
    // データベースを調べてpvtに病名数を設定する
    public void setByomeiCount(long karteId, PatientVisitModel pvt) {

        // byomeiCountがすでに0でないならば、byomeiCountは設定済みであろう
        //if (pvt.getByomeiCount() != 0) {
        //    return;
        //}

        int byomeiCount = 0;
        int byomeiCountToday = 0;
        Date pvtDate = ModelUtils.getCalendar(pvt.getPvtDate()).getTime();

        // データベースから検索
        final String sql = "from RegisteredDiagnosisModel r where r.karte.id = :karteId";
        List<RegisteredDiagnosisModel> rdList =
                em.createQuery(sql)
                .setParameter("karteId", karteId)
                .getResultList();
        for (RegisteredDiagnosisModel rd : rdList) {
            Date start = ModelUtils.getStartDate(rd.getStarted()).getTime();
            Date ended = ModelUtils.getEndedDate(rd.getEnded()).getTime();
            if (start.getTime() == pvtDate.getTime()) {
                byomeiCountToday++;
            }
            if (ModelUtils.isDateBetween(start, ended, pvtDate)) {
                byomeiCount++;
            }
        }
        pvt.setByomeiCount(byomeiCount);
        pvt.setByomeiCountToday(byomeiCountToday);
    }
    
    // ０時にpvtListをリニューアルする
    public void renewPvtList() {
        
        contextHolder.setToday();
        
        Map<String, List<PatientVisitModel>> map = contextHolder.getPvtListMap();
        
        for (Map.Entry<String, List<PatientVisitModel>> entry : map.entrySet()) {
            List<PatientVisitModel> pvtList = entry.getValue();
            
            List<PatientVisitModel> toRemove = new ArrayList<>();
            for (PatientVisitModel pvt : pvtList) {
                // BIT_SAVE_CLAIMとBIT_MODIFY_CLAIMは削除する
                if (pvt.getStateBit(PatientVisitModel.BIT_SAVE_CLAIM) 
                        || pvt.getStateBit(PatientVisitModel.BIT_MODIFY_CLAIM)
                        || pvt.getStateBit(PatientVisitModel.BIT_CANCEL)) {     // 本家からの指摘
                    toRemove.add(pvt);
                }
            }
            pvtList.removeAll(toRemove);
            
            // クライアントに伝える。
            String fid = entry.getKey();
            String uuid = contextHolder.getServerUUID();
            ChartEventModel msg = new ChartEventModel(uuid);
            msg.setFacilityId(fid);
            msg.setEventType(ChartEventModel.EVENT.PVT_RENEW);
            notifyEvent(msg);
        }
        logger.info("ChartEventService: renew pvtList");
    }

    
    private void setHealthInsurances(PatientModel pm) {
        if (pm != null) {
            List<HealthInsuranceModel> ins = getHealthInsurances(pm.getId());
            pm.setHealthInsurances(ins);
        }
    }

    private List<HealthInsuranceModel> getHealthInsurances(long pk) {

        List<HealthInsuranceModel> ins =
                em.createQuery(QUERY_INSURANCE_BY_PATIENT_PK)
                .setParameter(PK, pk)
                .getResultList();
        return ins;
    }
}
