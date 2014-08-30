package open.dolphin.mbean;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.inject.Singleton;
import javax.websocket.Session;
import open.dolphin.infomodel.PatientVisitModel;

/**
 * サーブレットの諸情報を保持するクラス
 *
 * @author masuda, Masuda Naika
 */
@Singleton
public class ServletContextHolder {

    // 今日と明日
    private GregorianCalendar today;
    private GregorianCalendar tomorrow;

    // AsyncResponseのリスト
    private final List<AsyncResponseModel> arList;
    
    // javax.websocket.Sessionのリスト
    private final List<Session> sessionList;
    
    // facilityIdとpvtListのマップ
    private final Map<String, List<PatientVisitModel>> pvtListMap;
    
    // サーバーのUUID
    private final String serverUUID;
    
    // ユーザーのキャッシュ
    private final Map<String, String> userMap;
    
    // データベースのタイプ
    private String database;

    
    public ServletContextHolder() {
        serverUUID = UUID.randomUUID().toString();
        arList = new CopyOnWriteArrayList<>();
        sessionList = new CopyOnWriteArrayList<>();
        pvtListMap = new ConcurrentHashMap<>();
        userMap = new ConcurrentHashMap<>();
    }

    public List<AsyncResponseModel> getAsyncResponseList() {
        return arList;
    }
    
    public List<Session> getSessionList() {
        return sessionList;
    }
    
    public String getServerUUID() {
        return serverUUID;
    }
    
//    public void setServerUUID(String uuid) {
//        serverUUID = uuid;
//    }

    public Map<String, List<PatientVisitModel>> getPvtListMap() {
        return pvtListMap;
    }
    
    public List<PatientVisitModel> getPvtList(String fid) {
        List<PatientVisitModel> pvtList = pvtListMap.get(fid);
        if (pvtList == null) {
            pvtList = new CopyOnWriteArrayList<>();
            pvtListMap.put(fid, pvtList);
        }
        return pvtList;
    }

    // 今日と明日を設定する
    public void setToday() {
        today= new GregorianCalendar();
        int year = today.get(GregorianCalendar.YEAR);
        int month = today.get(GregorianCalendar.MONTH);
        int date = today.get(GregorianCalendar.DAY_OF_MONTH);
        today.clear();
        today.set(year, month, date);

        tomorrow = new GregorianCalendar();
        tomorrow.setTime(today.getTime());
        tomorrow.add(GregorianCalendar.DAY_OF_MONTH, 1);
    }
    
    public GregorianCalendar getToday() {
        return today;
    }
    public GregorianCalendar getTomorrow() {
        return tomorrow;
    }
    
    public Map<String, String> getUserMap() {
        return userMap;
    }
    
    public String getDatabase() {
        return database;
    }
    
    public void setDatabase(String database) {
        this.database = database;
    }
}
