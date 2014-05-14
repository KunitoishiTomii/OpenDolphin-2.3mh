package open.dolphin.session;

import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import open.dolphin.infomodel.*;
import open.dolphin.mbean.AsyncResponseModel;
import open.dolphin.mbean.ServletContextHolder;

/**
 *
 * @author kazushi Minagawa, Digital Globe, Inc.
 */
@Stateless
public class UserServiceBean {
    
    private static final Logger logger = Logger.getLogger(UserServiceBean.class.getName());

    private static final String UID = "uid";
    private static final String FID = "fid";
    
    private static final String QUERY_USER_BY_UID 
            = "from UserModel u where u.userId=:uid";
    private static final String QUERY_USER_BY_FID_MEMBERTYPE 
            = "from UserModel u where u.userId like :fid and u.memberType!=:memberType";
    private static final String QUERY_FACILITY
            = "from FacilityModel f where f.facilityId like :fid";
    
    private static final String MEMBER_TYPE = "memberType";
    private static final String MEMBER_TYPE_EXPIRED = "EXPIRED";
    
    @Inject
    private ServletContextHolder contextHolder;
    
    @PersistenceContext
    private EntityManager em;
    
    
    public boolean authenticate(String userName, String password) {

        boolean ret = false;

        try {
            UserModel user = (UserModel)
                    em.createQuery(QUERY_USER_BY_UID)
                    .setParameter(UID, userName)
                    .getSingleResult();
            if (user.getPassword().equals(password) && user.getFailCount() < 5) {
                ret = true;
                user.setFailCount(0);
            } else {
                int failCount = user.getFailCount() + 1;
                user.setFailCount(failCount);
                String msg = String.format("Authentication Failed: user=%s, failCount=%d", userName, failCount);
                logger.warning(msg);
            }
            
        } catch (Exception e) {
        }

        return ret;
    }
/*
    public String[] getFidAndPassword(String userName) {
        
        try {
            String fid = IInfoModel.DEFAULT_FACILITY_OID;
            if (userName.contains(":")) {
                int pos = userName.indexOf(":");
                fid = userName.substring(0, pos);
                userName = userName.substring(pos + 1);
                FacilityModel facility = (FacilityModel)
                        em.createQuery(QUERY_FACILITY)
                        .setParameter(FID, "%" + fid)
                        .getSingleResult();
                fid = facility.getFacilityId();
            }
            userName = fid + IInfoModel.COMPOSITE_KEY_MAKER + userName;

            UserModel user = (UserModel) 
                    em.createQuery(QUERY_USER_BY_UID)
                    .setParameter(UID, userName)
                    .getSingleResult();
            return new String[]{fid, user.getPasswd()};
        } catch (Exception e) {
        }
        return null;
    }
*/
    /**
     * 施設管理者が院内Userを登録する。
     * @param add 登録するUser
     */
    public int addUser(UserModel add) {

        try {
            // 既存ユーザの場合は例外をスローする
            getUser(add.getUserId());
            throw new EntityExistsException();
        } catch (NoResultException e) {
        }
        em.persist(add);
        return 1;
    }

    /**
     * Userを検索する。
     * @param userId 検索するユーザの複合キー
     * @return 該当するUser
     */
    public UserModel getUser(String uid) {
        UserModel user = (UserModel)
                em.createQuery(QUERY_USER_BY_UID)
                .setParameter(UID, uid)
                .getSingleResult();

        if (user.getMemberType() != null && user.getMemberType().equals(MEMBER_TYPE_EXPIRED)) {
            throw new SecurityException("Expired User");
        }
        return user;
    }

    /**
     * 施設内の全Userを取得する。
     *
     * @return 施設内ユーザリスト
     */
    public List<UserModel> getAllUser(String fid) {

        List<UserModel> results =
                em.createQuery(QUERY_USER_BY_FID_MEMBERTYPE)
                .setParameter(FID, fid+":%")
                .setParameter(MEMBER_TYPE, MEMBER_TYPE_EXPIRED)
                .getResultList();
        return results;

//        Collection<UserModel> ret = new ArrayList<UserModel>();
//        for (Iterator iter = results.iterator(); iter.hasNext(); ) {
//            UserModel user = (UserModel) iter.next();
//            if (user.getMemberType() != null && (!user.getMemberType().equals("EXPIRED"))) {
//                ret.add(user);
//            }
//        }
//        return ret;
    }

    /**
     * User情報(パスワード等)を更新する。
     * @param update 更新するUser detuched
     */
    public int updateUser(UserModel update) {
        UserModel current = em.find(UserModel.class, update.getId());
        update.setMemberType(current.getMemberType());
        update.setRegisteredDate(current.getRegisteredDate());
        em.merge(update);
        return 1;
    }

    /**
     * Userを削除する。
     * @param removeId 削除するユーザのId
     */
    public int removeUser(String removeId) {

        //
        // 削除するユーザを得る
        //
        UserModel remove = getUser(removeId);

        // Stamp を削除する
        List<StampModel> stamps = 
                em.createQuery("from StampModel s where s.userId = :pk")
                .setParameter("pk", remove.getId())
                .getResultList();
        for (StampModel stamp : stamps) {
            em.remove(stamp);
        }

        // Subscribed Tree を削除する
        List<SubscribedTreeModel> subscribedTrees =
                em.createQuery("from SubscribedTreeModel s where s.user.id = :pk")
                .setParameter("pk", remove.getId())
                .getResultList();
        for (SubscribedTreeModel tree : subscribedTrees) {
            em.remove(tree);
        }

        // PublishedTree を削除する
        List<PublishedTreeModel> publishedTrees = 
                em.createQuery("from PublishedTreeModel p where p.user.id = :pk")
                .setParameter("pk", remove.getId())
                .getResultList();
        for (PublishedTreeModel tree : publishedTrees) {
            em.remove(tree);
        }

        // PersonalTreeを削除する
        List<StampTreeModel> stampTree =
                em.createQuery("from StampTreeModel s where s.user.id = :pk")
                .setParameter("pk", remove.getId())
                .getResultList();
        for (StampTreeModel tree : stampTree) {
            em.remove(tree);
        }

        //
        // ユーザを削除する
        //
        if (remove.getLicenseModel().getLicense().equals("doctor")) {
            StringBuilder sb = new StringBuilder();
            remove.setMemo(sb.toString());
            remove.setMemberType(MEMBER_TYPE_EXPIRED);
            remove.setPassword("c9dbeb1de83e60eb1eb3675fa7d69a02");
        } else {
            em.remove(remove);
        }

        return 1;
    }

    /**
     * 施設情報を更新する。
     * @param update 更新するUser detuched
     */
    public int updateFacility(UserModel update) {
        FacilityModel updateFacility = update.getFacilityModel();
        FacilityModel current = em.find(FacilityModel.class, updateFacility.getId());
        updateFacility.setMemberType(current.getMemberType());
        updateFacility.setRegisteredDate(current.getRegisteredDate());
        em.merge(updateFacility );
        return 1;
    }

    public String login(String fidUid, String clientUUID, boolean force) {
        UserModel user = getUser(fidUid);
        String currentUUID = user.getClientUUID();
        if (currentUUID == null || force) {
            currentUUID = clientUUID;
            user.setClientUUID(currentUUID);
            em.merge(user);
        }
        return currentUUID;
    }
    
    public String logout(String fidUid, String clientUUID) {
        UserModel user = getUser(fidUid);
        String oldUUID = user.getClientUUID();
        if (clientUUID.equals(oldUUID)) {
            user.setClientUUID(null);
            em.merge(user);
        }
        
        // remove AsyncResponse
        AsyncResponseModel toRemove = null;
        for (AsyncResponseModel arModel : contextHolder.getAsyncResponseList()) {
            if (clientUUID.equals(arModel.getClientUUID())) {
                toRemove = arModel;
                break;
            }
        }
        if (toRemove != null) {
            contextHolder.getAsyncResponseList().remove(toRemove);
            toRemove.getAsyncResponse().cancel();
        }
        
        return oldUUID;
    }
}
