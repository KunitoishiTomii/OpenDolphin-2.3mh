package open.dolphin.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.List;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import open.dolphin.infomodel.ChartEventModel;
import open.dolphin.infomodel.HealthInsuranceModel;
import open.dolphin.infomodel.ModelUtils;
import open.dolphin.infomodel.PatientModel;
import open.dolphin.infomodel.PatientVisitModel;

/**
 *
 * @author Kazushi Minagawa, Digital Globe, Inc
 */
@Stateless
public class PatientServiceBean {

    private static final String QUERY_INSURANCE_BY_PATIENT_PK 
            = "from HealthInsuranceModel h where h.patient.id=:pk";
    
    private static final String PK = "pk";
    private static final String FID = "fid";
    private static final String PID = "pid";
    private static final String ID = "id";
    private static final String DATE = "date";
    
    private static final String QUERY_PATIENT_BY_PVTDATE 
            = "from PatientVisitModel p where p.facilityId = :fid and p.pvtDate like :date";
    private static final String QUERY_PATIENT_BY_NAME 
            = "from PatientModel p where p.facilityId=:fid and p.fullName like :name";
    private static final String QUERY_PATIENT_BY_KANA 
            = "from PatientModel p where p.facilityId=:fid and p.kanaName like :name";
    private static final String QUERY_PATIENT_BY_FID_PID 
            = "from PatientModel p where p.facilityId=:fid and p.patientId like :pid";
    private static final String QUERY_PATIENT_BY_TELEPHONE 
            = "from PatientModel p where p.facilityId = :fid and (p.telephone like :number or p.mobilePhone like :number)";
    private static final String QUERY_PATIENT_BY_ZIPCODE 
            = "from PatientModel p where p.facilityId = :fid and p.address.zipCode like :zipCode";

    private static final String NAME = "name";
    private static final String NUMBER = "number";
    private static final String ZIPCODE = "zipCode";
    private static final String PERCENT = "%";
    
    @PersistenceContext
    private EntityManager em;
    
//masuda^
    @Inject
    private ChartEventServiceBean eventServiceBean;
//masuda$

    public List<PatientModel> getPatientsByName(String fid, String name) {

        List<PatientModel> ret = 
                em.createQuery(QUERY_PATIENT_BY_NAME)
                .setParameter(FID, fid)
                .setParameter(NAME, name + PERCENT)
                .getResultList();

        // 後方一致検索を行う
        if (ret.isEmpty()) {
            ret = em.createQuery(QUERY_PATIENT_BY_NAME)
                .setParameter(FID, fid)
                .setParameter(NAME, PERCENT + name)
                .getResultList();
        }
        
        //-----------------------------------
        // 患者の健康保険を取得する
        setHealthInsurances(ret);
        //-----------------------------------

//masuda^   最終受診日設定
        if (!ret.isEmpty()) {
            setPvtDate(fid, ret);
        }
//masuda$
        
        return ret;
    }

    public List<PatientModel> getPatientsByKana(String fid, String name) {

        List<PatientModel> ret = 
                em.createQuery(QUERY_PATIENT_BY_KANA)
                .setParameter(FID, fid)
                .setParameter(NAME, name + PERCENT)
                .getResultList();

        if (ret.isEmpty()) {
            ret = em.createQuery(QUERY_PATIENT_BY_KANA)
                .setParameter(FID, fid)
                .setParameter(NAME, PERCENT + name)
                .getResultList();
        }

        //-----------------------------------
        // 患者の健康保険を取得する
        setHealthInsurances(ret);
        //-----------------------------------
        
//masuda^   最終受診日設定
        if (!ret.isEmpty()) {
            setPvtDate(fid, ret);
        }
//masuda$
        
        return ret;
    }

    public List<PatientModel> getPatientsByDigit(String fid, String digit) {

        List<PatientModel> ret = 
                em.createQuery(QUERY_PATIENT_BY_FID_PID)
                .setParameter(FID, fid)
                .setParameter(PID, digit+PERCENT)
                .getResultList();

        if (ret.isEmpty()) {
            ret = em.createQuery(QUERY_PATIENT_BY_TELEPHONE)
                .setParameter(FID, fid)
                .setParameter(NUMBER, digit+PERCENT)
                .getResultList();
        }

        if (ret.isEmpty()) {
            ret = em.createQuery(QUERY_PATIENT_BY_ZIPCODE)
                .setParameter(FID, fid)
                .setParameter(ZIPCODE, digit+PERCENT)
                .getResultList();
        }

        //-----------------------------------
        // 患者の健康保険を取得する
        setHealthInsurances(ret);
        //-----------------------------------
        
//masuda^   最終受診日設定
        if (!ret.isEmpty()) {
            setPvtDate(fid, ret);
        }
//masuda$
        
        return ret;
    }

    public List<PatientModel> getPatientsByPvtDate(String fid, String pvtDate) {

        List<PatientVisitModel> list =
                em.createQuery(QUERY_PATIENT_BY_PVTDATE)
                .setParameter(FID, fid)
                .setParameter(DATE, pvtDate+PERCENT)
                .getResultList();

        List<PatientModel> ret = new ArrayList<PatientModel>();

        for (PatientVisitModel pvt : list) {
            PatientModel patient = pvt.getPatientModel();
            // 患者の健康保険を取得する
            setHealthInsurances(patient);
            ret.add(patient);
//masuda^   最終受診日設定
            //patient.setPvtDate(pvt.getPvtDate());
        }
        if (!ret.isEmpty()) {
            setPvtDate(fid, ret);
        }
 //masuda$       

        return ret;
    }
    
    
//masuda^   過去１００日の受診者を検索する
    public List<PatientModel> getPast100DayPatients(String fid, int pastDay) {
        
        final String sql = "from PatientVisitModel p where p.facilityId = :fid and p.pvtDate > :date";
        
        GregorianCalendar gc = new GregorianCalendar();
        gc.add(GregorianCalendar.DATE, -pastDay);
        String mmlDate = ModelUtils.getDateAsString(gc.getTime());
        List<PatientVisitModel> list =
                em.createQuery(sql)
                .setParameter(FID, fid)
                .setParameter(DATE, mmlDate)
                .getResultList();

        List<PatientModel> ret = new ArrayList<PatientModel>();

        for (PatientVisitModel pvt : list) {
            PatientModel patient = pvt.getPatientModel();
            // PatientModelに受診日を設定
            patient.setPvtDate(pvt.getPvtDate());
            int index = ret.indexOf(patient);
            if (index == -1) {
                // ダミーの保険を設定する
                patient.setHealthInsurances(null);
                // リストにないならPatientModelをリストに登録する
                ret.add(patient);
            } else {
                // pvtDateが新しい場合は更新する
                PatientModel exist = ret.get(index);
                if (patient.getPvtDate2().after(exist.getPvtDate2())) {
                    exist.setPvtDate(patient.getPvtDate());
                }
            }
        }
        
        // 受診が途絶えている順でソートする
        Collections.sort(ret, new PvtDateComparator());
     
        return ret;
    }
    
    private class PvtDateComparator implements Comparator {

        @Override
        public int compare(Object o1, Object o2) {
            PatientModel pm1 = (PatientModel) o1;
            PatientModel pm2 = (PatientModel) o2;
            if (pm1.getPvtDate2().after(pm2.getPvtDate2())) {
                return 1;
            } else if (pm1.getPvtDate2().before(pm2.getPvtDate2())) {
                return -1;
            }
            return 0;
        }
        
    }
//masuda$
    
    /**
     * 患者ID(BUSINESS KEY)を指定して患者オブジェクトを返す。
     *
     * @param patientId 施設内患者ID
     * @return 該当するPatientModel
     */
    public PatientModel getPatientById(String fid,String pid) {

        // 患者レコードは FacilityId と patientId で複合キーになっている
        PatientModel bean = (PatientModel)
                em.createQuery(QUERY_PATIENT_BY_FID_PID)
                .setParameter(FID, fid)
                .setParameter(PID, pid)
                .getSingleResult();

        // Lazy Fetch の 基本属性を検索する
        // 患者の健康保険を取得する
        setHealthInsurances(bean);

        return bean;
    }

    /**
     * 患者を登録する。
     * @param patient PatientModel
     * @return データベース Primary Key
     */
    public long addPatient(PatientModel patient) {
        em.persist(patient);
        long pk = patient.getId();
        return pk;
    }

    /**
     * 患者情報を更新する。
     * @param patient 更新する患者
     * @return 更新数
     */
    public int update(PatientModel patient) {
        em.merge(patient);
//masuda^   患者情報が更新されたらPvtListも更新する必要あり
        updatePvtList(patient);
//masuda$
        return 1;
    }
    
//masuda^
    // pvtListのPatientModelを更新し、クライアントにも通知する
    private void updatePvtList(PatientModel pm) {
        String fid = pm.getFacilityId();
        List<PatientVisitModel> pvtList = eventServiceBean.getPvtList(fid);
        for (PatientVisitModel pvt : pvtList) {
            if (pvt.getPatientModel().getId() == pm.getId()) {
                pvt.setPatientModel(pm);
                 // クライアントに通知
                String uuid = eventServiceBean.getServerUUID();
                ChartEventModel msg = new ChartEventModel(uuid);
                msg.setPatientModel(pm);
                msg.setFacilityId(fid);
                msg.setEventType(ChartEventModel.EVENT.PM_MERGE);
                eventServiceBean.notifyEvent(msg);
            }
        }
    }
    
    private void setPvtDate(String fid, List<PatientModel> list) {
        
        final String sql =
                "from PatientVisitModel p where p.facilityId = :fid and p.patient.id = :patientPk "
                + "and p.status != :status order by p.pvtDate desc";
        
        for (PatientModel patient : list) {
            try {
                PatientVisitModel pvt = (PatientVisitModel) 
                        em.createQuery(sql)
                        .setParameter("fid", fid)
                        .setParameter("patientPk", patient.getId())
                        .setParameter("status", -1)
                        .setMaxResults(1)
                        .getSingleResult();
                patient.setPvtDate(pvt.getPvtDate());
            } catch (NoResultException e) {
            }
        }
    }
    
    public List<PatientModel> getPatientList(String fid, List<String> idList) {
        
        final String sql 
                = "from PatientModel p where p.facilityId = :fid and p.patientId in (:ids)";
        
        List<PatientModel> list = (List<PatientModel>)
                em.createQuery(sql)
                .setParameter("fid", fid)
                .setParameter("ids", idList)
                .getResultList();
        
        // 患者の健康保険を取得する。忘れがちｗ
        setHealthInsurances(list);
        
        return list;
    }
//masuda$
    
    private void setHealthInsurances(Collection<PatientModel> list) {
        if (list != null && !list.isEmpty()) {
            for (PatientModel pm : list) {
                setHealthInsurances(pm);
            }
        }
    }
    
    // 保険情報は後でクライアントから取りに行く
    // http://mdc.blog.ocn.ne.jp/blog/2013/02/post_f69f.html
    // ダミーの保険情報を設定する。LAZY_FETCHを回避する
    // com.fasterxml.jackson.databind.JsonMappingException: could not initialize proxy - no Session
    private void setHealthInsurances(PatientModel pm) {
        if (pm != null) {
            pm.setHealthInsurances(null);
        }
    }

    public List<HealthInsuranceModel> getHealthInsurances(long pk) {
        
        List<HealthInsuranceModel> ins =
                em.createQuery(QUERY_INSURANCE_BY_PATIENT_PK)
                .setParameter(PK, pk)
                .getResultList();
        
        return ins;
    }
}
