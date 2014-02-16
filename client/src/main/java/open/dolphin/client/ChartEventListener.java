package open.dolphin.client;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.Response;
import open.dolphin.delegater.ChartEventDelegater;
import open.dolphin.infomodel.*;
import open.dolphin.project.Project;
import open.dolphin.common.util.BeanUtils;
import open.dolphin.common.util.JsonConverter;
import open.dolphin.delegater.ClientChartEventEndpoint;
import open.dolphin.delegater.PatientDelegater;
import open.dolphin.util.NamedThreadFactory;

/**
 * カルテオープンなどの状態の変化をまとめて管理する
 * @author masuda, Masuda Naika
 */
public class ChartEventListener {

     // このクライアントのパラメーター類
    private String clientUUID;
    private String orcaId;
    private String deptCode;
    private String departmentDesc;
    private String doctorName;
    private String userId;
    private String jmariCode;
    private String facilityId;
    
    private boolean useWebSocket;
    private ClientChartEventEndpoint endpoint;

    private List<IChartEventListener> listeners;
    
    // ChartEvent監視タスク
    private EventListenThread listenThread;
    //private ChartEventCallback eventCallback;
    
    // 状態変化を各listenerに通知するタスク
    private ExecutorService onEventExec;
    
    private static final ChartEventListener instance;

    static {
        instance = new ChartEventListener();
    }

    private ChartEventListener() {
        init();
     }

    public static ChartEventListener getInstance() {
        return instance;
    }
    
    private void init() {
        clientUUID = Dolphin.getInstance().getClientUUID();
        orcaId = Project.getUserModel().getOrcaId();
        deptCode = Project.getUserModel().getDepartmentModel().getDepartment();
        departmentDesc = Project.getUserModel().getDepartmentModel().getDepartmentDesc();
        doctorName = Project.getUserModel().getCommonName();
        userId = Project.getUserModel().getUserId();
        jmariCode = Project.getString(Project.JMARI_CODE);
        facilityId = Project.getFacilityId();
        listeners = new ArrayList<>();
    }

    public String getClientUUID() {
        return clientUUID;
    }

    public void addListener(IChartEventListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(IChartEventListener listener) {
        listeners.remove(listener);
    }

    // 状態変更処理の共通入り口
    private void publish(ChartEventModel evt) {
        onEventExec.execute(new LocalOnEventTask(evt));
    }

    public void publishMsg(ChartEventModel evt) {
        evt.setIssuerUUID(clientUUID);
        //evt.setFacilityId = Project.getFacilityId();
        publish(evt);
    }
    
    public void publishPvtDelete(PatientVisitModel pvt) {
        
        ChartEventModel evt = new ChartEventModel(clientUUID);
        evt.setParamFromPvt(pvt);
        evt.setEventType(ChartEventModel.EVENT.PVT_DELETE);
        
        publish(evt);
    }
    
    public void publishPvtState(PatientVisitModel pvt) {
        
        ChartEventModel evt = new ChartEventModel(clientUUID);
        evt.setParamFromPvt(pvt);
        evt.setEventType(ChartEventModel.EVENT.PVT_STATE);
        
        publish(evt);
    }
    
    public void publishKarteOpened(PatientVisitModel pvt) {
        
        // 閲覧のみの処理、ええい！面倒だ！
        if (!clientUUID.equals(pvt.getPatientModel().getOwnerUUID())) {
            return;
        }

        // PatientVisitModel.BIT_OPENを立てる
        pvt.setStateBit(PatientVisitModel.BIT_OPEN, true);
        // ChartStateListenerに通知する
        ChartEventModel evt = new ChartEventModel(clientUUID);
        evt.setParamFromPvt(pvt);
        evt.setEventType(ChartEventModel.EVENT.PVT_STATE);
        
        publish(evt);
    }
    
    public void publishKarteClosed(PatientVisitModel pvt) {
        
        // 閲覧のみの処理、ええい！面倒だ！
        if (!clientUUID.equals(pvt.getPatientModel().getOwnerUUID())) {
            return;
        }
        
        // PatientVisitModel.BIT_OPENとownerUUIDをリセットする
        pvt.setStateBit(PatientVisitModel.BIT_OPEN, false);
        pvt.getPatientModel().setOwnerUUID(null);
        
        // ChartStateListenerに通知する
        ChartEventModel evt = new ChartEventModel(clientUUID);
        evt.setParamFromPvt(pvt);
        evt.setEventType(ChartEventModel.EVENT.PVT_STATE);
        
        publish(evt);
    }

    public void start() {
        NamedThreadFactory factory = new NamedThreadFactory("ChartEvent Handle Task");
        onEventExec = Executors.newSingleThreadExecutor(factory);
        try {
            endpoint = new ClientChartEventEndpoint();
            endpoint.init();
            useWebSocket = true;
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            listenThread = new EventListenThread();
            listenThread.start();
            useWebSocket = false;
        }
    }

    public void stop() {
        if (useWebSocket) {
            endpoint.close();
        } else {
            listenThread.halt();
        }
        shutdownExecutor();
    }

    private void shutdownExecutor() {

        try {
            onEventExec.shutdown();
            if (!onEventExec.awaitTermination(20, TimeUnit.MILLISECONDS)) {
                onEventExec.shutdownNow();
            }
        } catch (InterruptedException ex) {
            onEventExec.shutdownNow();
        } catch (NullPointerException ex) {
        }
        onEventExec = null;
    }

    // Commetでサーバーと同期するスレッド
    private class EventListenThread extends Thread {
        
        private Future<Response> future;
        private boolean isRunning;
        
        private EventListenThread() {
            super("ChartEvent Listen Thread");
            isRunning = true;
        }
        
        public void halt() {
            isRunning = false;
            interrupt();

            if (future != null) {
                future.cancel(true);
            }
        }
        
        @Override
        public void run() {
            
            while (isRunning) {
                try {
                    future = ChartEventDelegater.getInstance().subscribe();
                    Response response = future.get();
                    onEventExec.execute(new RemoteOnEventTask(response));
                } catch (Exception e) {
                    //e.printStackTrace(System.err);
                }
            }
        }
    }
    
    // 自クライアントの状態変更後、サーバーに通知するタスク
    private class LocalOnEventTask implements Runnable {
        
        private final ChartEventModel evt;
        
        private LocalOnEventTask(ChartEventModel evt) {
            this.evt = evt;
        }

        @Override
        public void run() {
            
            // まずは自クライアントを更新
            for (IChartEventListener listener : listeners) {
                try {
                    listener.onEvent(evt);
                } catch (Exception ex) {
                }
            }
            
            // サーバーに更新を通知
            try {
                if (useWebSocket) {
                    endpoint.putChartEvent(evt);
                } else {
                    ChartEventDelegater del = ChartEventDelegater.getInstance();
                    del.putChartEvent(evt);
                }
            } catch (Exception ex) {
            }
        }
    }
    
    // 状態変化通知メッセージをデシリアライズし各リスナに処理を分配する
    private class RemoteOnEventTask implements Runnable {
        
        private final Response response;
        
        private RemoteOnEventTask(Response response) {
            this.response = response;
        }

        @Override
        public void run() {
            
            if (response == null) {
                return;
            }
            if (response.getStatus() / 100 != 2) {
                response.close();
                return;
            }
            
            InputStream is = response.readEntity(InputStream.class);
            ChartEventModel evt = (ChartEventModel) 
                    JsonConverter.getInstance().fromJson(is, ChartEventModel.class);
            
            response.close();
            
            if (evt != null) {
                processRemoteChartEvent(evt);
            }
        }
    }
    
    private void processRemoteChartEvent(ChartEventModel evt) {
        
        // PatientModelが乗っかってきている場合は保険をデコード
        PatientModel pm = evt.getPatientModel();
        if (pm != null) {
            decodeHealthInsurance(pm);
        }
        PatientVisitModel pvt = evt.getPatientVisitModel();
        if (pvt != null) {
            decodeHealthInsurance(pvt.getPatientModel());
        }

        // 各リスナーで更新処理をする
        for (IChartEventListener listener : listeners) {
            try {
                listener.onEvent(evt);
            } catch (Exception ex) {
            }
        }
    }
    
    // web socket
    public void onWebSocketMessage(String json) {
        onEventExec.execute(new RemoteOnEventTaskWs(json));
    }
    
    private class RemoteOnEventTaskWs implements Runnable {
        
        private final String json;
        
        private RemoteOnEventTaskWs(String json) {
            this.json = json;
        }

        @Override
        public void run() {

            ChartEventModel evt = (ChartEventModel) 
                    JsonConverter.getInstance().fromJson(json, ChartEventModel.class);
            
            if (evt != null) {
                processRemoteChartEvent(evt);
            }
        }
    }

    /**
     * バイナリの健康保険データをオブジェクトにデコードする。
     *
     * @param patient 患者モデル
     */
    private void decodeHealthInsurance(PatientModel patient) {

        // Health Insurance を変換をする beanXML2PVT
        Collection<HealthInsuranceModel> c = patient.getHealthInsurances();

        if (c != null && !c.isEmpty()) {

            List<PVTHealthInsuranceModel> list = new ArrayList<>(c.size());

            for (HealthInsuranceModel model : c) {
                try {
                    // byte[] を XMLDecord
                    PVTHealthInsuranceModel hModel = (PVTHealthInsuranceModel) 
                            BeanUtils.xmlDecode(model.getBeanBytes());
                    list.add(hModel);
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
            }

            patient.setPvtHealthInsurances(list);
            patient.getHealthInsurances().clear();
            patient.setHealthInsurances(null);
        }
    }
    
    // FakePatientVisitModelを作る
    public PatientVisitModel createFakePvt(PatientModel pm) {

        // 来院情報を生成する
        PatientVisitModel pvt = new PatientVisitModel();
        pvt.setId(0L);
        pvt.setPatientModel(pm);
        pvt.setFacilityId(facilityId);

        //--------------------------------------------------------
        // 受け付けを通していないのでログイン情報及び設定ファイルを使用する
        // 診療科名、診療科コード、医師名、医師コード、JMARI
        // 2.0
        //---------------------------------------------------------
        pvt.setDeptName(departmentDesc);
        pvt.setDeptCode(deptCode);
        pvt.setDoctorName(doctorName);
        if (orcaId != null) {
            pvt.setDoctorId(orcaId);
        } else {
            pvt.setDoctorId(userId);
        }
        pvt.setJmariNumber(jmariCode);
        
        // 来院日
        pvt.setPvtDate(ModelUtils.getDateTimeAsString(new Date()));
        return pvt;
    }
    
    public PatientVisitModel createFakePvt(String patientId) {
        try {
            PatientModel pm = PatientDelegater.getInstance().getPatientById(patientId);
            if (pm != null) {
                return createFakePvt(pm);
            }
        } catch (Exception ex) {
        }
        return null;
    }
}
