package open.dolphin.client;

import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import open.dolphin.infomodel.ClaimBundle;
import open.dolphin.infomodel.ClaimConst;
import open.dolphin.infomodel.ClaimItem;
import open.dolphin.infomodel.ModuleModel;

/**
 * 診療行為重複チェック
 * 
 * @author masuda, Masuda Naika
 */
public class CheckDuplication {
    
    // 除外する診療行為コード
    private static final String SRYCD_INJECTION = "130";
    private static final String SRYCD_ADMIN = "001000";
    
    
    public boolean checkStart(Chart context, List<ModuleModel> stamps) {
        
        // ClaimItemを列挙
        List<ClaimItem> claimItems = new ArrayList<>();
        for (ModuleModel stamp : stamps) {
            ClaimBundle cb = (ClaimBundle) stamp.getModel();
            for (ClaimItem ci : cb.getClaimItem()) {
                String srycd = ci.getCode();
                if (!srycd.startsWith(SRYCD_INJECTION) 
                        && !srycd.startsWith(SRYCD_ADMIN)
                        && !srycd.matches(ClaimConst.REGEXP_COMMENT_MED)) {
                    claimItems.add(ci);
                }
            }
        }
        
        // 重複チェック
        List<ClaimItem> dupItems = new ArrayList<>();
        int len = claimItems.size();
        for (int i = 0; i < len; ++i) {
            ClaimItem ci1 = claimItems.get(i);
            for (int j = i; j < len; ++j) {
                if (i != j) {
                    ClaimItem ci2 = claimItems.get(j);
                    if (ci1.getCode().equals(ci2.getCode())) {
                        dupItems.add(ci1);
                    }
                }
            }
        }
        
        if (dupItems.isEmpty()) {
            return false;
        } else {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (ClaimItem ci : dupItems) {
                if (!first) {
                    sb.append("\n");
                } else {
                    first = false;
                }
                sb.append(ci.getName()).append("は重複しています。");
            }
            
            String msg = sb.toString();
            Toolkit.getDefaultToolkit().beep();
            String[] options = {"取消", "無視"};
            int val = JOptionPane.showOptionDialog(context.getFrame(), msg, "重複確認",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
            if (val != 1) {
                // 取り消し
                return true;
            }
        }

        return false;
    }
}
