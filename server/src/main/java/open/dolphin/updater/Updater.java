package open.dolphin.updater;

import java.util.Date;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import open.dolphin.infomodel.MsdUpdaterModel;

/**
 * Updator
 * 
 * @author masuda, Masuda Naika
 */
@Stateless
public class Updater {
    
    private static final String SQL = 
            "select count(m) from MsdUpdaterModel m where m.moduleName = :moduleName and m.versionDate = :verDate";
    
    private static final Class[] moduleClasses = new Class[] {
        AddInitialUser.class,   // 初期ユーザ―登録
        DbSchemaUpdater.class,  // Database Schemaを変更
        LetterConverter.class,  // Letterを新フォーマットに変換
        PvtStateUpdater.class,  // 今日の診察終了PvtStateを変換
        RoutineMedUpdater.class,// RoutineMed修正
    };

    @PersistenceContext
    private EntityManager em;
    
    public void start() {
        
        for (Class clazz : moduleClasses) {
            try {
                AbstractUpdaterModule module = (AbstractUpdaterModule) clazz.newInstance();
                String moduleName = module.getModuleName();
                Date versionDate = module.getVersionDate();
                long count = (Long) em.createQuery(SQL)
                        .setParameter("moduleName", moduleName)
                        .setParameter("verDate", versionDate)
                        .getSingleResult();
                if (count == 0) {
                    module.setEntityManager(em);
                    MsdUpdaterModel ret = module.start();
                    if (ret != null) {
                        em.persist(ret);
                    }
                }
            } catch (InstantiationException | IllegalAccessException ex) {
            }
        }
    }
    
}
