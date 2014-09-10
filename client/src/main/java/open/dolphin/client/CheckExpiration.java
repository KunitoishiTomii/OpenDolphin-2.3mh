package open.dolphin.client;

import java.awt.Toolkit;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JOptionPane;
import open.dolphin.dao.DaoException;
import open.dolphin.dao.SqlMiscDao;
import open.dolphin.infomodel.ClaimBundle;
import open.dolphin.infomodel.ClaimItem;
import open.dolphin.infomodel.ModuleModel;
import open.dolphin.infomodel.TensuMaster;

/**
 * 有効期限チェック
 * 
 * @author masuda, Masuda Naika
 */
public class CheckExpiration {
    
    public boolean checkStart(Chart context, List<ModuleModel> stamps) {
        
        // ClaimItemを列挙
        Set<String> srycds = new HashSet<>();
        for (ModuleModel stamp : stamps) {
            ClaimBundle cb = (ClaimBundle) stamp.getModel();
            for (ClaimItem ci : cb.getClaimItem()) {
                srycds.add(ci.getCode());
            }
        }

        // 有効期限チェック
        String now = new SimpleDateFormat("yyyyMMdd").format(new Date());
        List<TensuMaster> tmList;
        try {
            tmList = SqlMiscDao.getInstance().getTensuMasterList(srycds);
        } catch (DaoException ex) {
            return true;
        }
        
        List<TensuMaster> expired = new ArrayList<>();
        for (TensuMaster tm : tmList) {
            if (now.compareTo(tm.getYukoedymd()) == 1) {
                expired.add(tm);
            }
        }
        
        if (expired.isEmpty()) {
            return false;
        } else {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (TensuMaster tm : expired) {
                if (!first) {
                    sb.append("\n");
                } else {
                    first = false;
                }
                sb.append(tm.getName()).append("は有効期限切れです。");
            }
            
            String msg = sb.toString();
            Toolkit.getDefaultToolkit().beep();
            String[] options = {"取消", "無視"};
            int val = JOptionPane.showOptionDialog(context.getFrame(), msg, "有効期限確認",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
            if (val != 1) {
                // 取り消し
                return true;
            }
        }

        return false;
    }
}
