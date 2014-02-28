package open.dolphin.mbean;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.inject.Singleton;
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
    
    // facilityIdとpvtListのマップ
    private final ConcurrentHashMap<String, List<PatientVisitModel>> pvtListMap;
    
    // サーバーのUUID
    private final String serverUUID;
    
    // ユーザーのキャッシュ
    private final ConcurrentHashMap<String, String> userMap;
    
    // データベースのタイプ
    private String database;

    
    public ServletContextHolder() {
        serverUUID = UUID.randomUUID().toString();
        arList = new CopyOnWriteArrayList<>();
        pvtListMap = new ConcurrentHashMap<>();
        userMap = new ConcurrentHashMap<>();
    }

    public List<AsyncResponseModel> getAsyncResponseList() {
        return arList;
    }
    
    public String getServerUUID() {
        return serverUUID;
    }
    
    //public void setServerUUID(String uuid) {
    //    serverUUID = uuid;
    //}

    public ConcurrentHashMap<String, List<PatientVisitModel>> getPvtListMap() {
        return pvtListMap;
    }
    
    public List<PatientVisitModel> getPvtList(String fid) {
        List<PatientVisitModel> pvtList = pvtListMap.get(fid);
        if (pvtList == null) {
            pvtList = new CopyOnWriteArrayList<>();
            pvtListMap.putIfAbsent(fid, pvtList);
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
    
    public ConcurrentHashMap<String, String> getUserMap() {
        return userMap;
    }
    
    public String getDatabase() {
        return database;
    }
    
    public void setDatabase(String database) {
        this.database = database;
    }
}
