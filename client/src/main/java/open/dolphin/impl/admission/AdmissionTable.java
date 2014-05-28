/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package open.dolphin.impl.admission;

import java.awt.event.KeyEvent;
import javax.swing.JTable;
import javax.swing.KeyStroke;

/**
 * 入院リストのテーブルクラス。
 * @author buntaro
 */
class AdmissionTable extends JTable{
    @Override
    protected boolean processKeyBinding(KeyStroke ks,
                                    KeyEvent e,
                                    int condition,
                                    boolean pressed){
        boolean bReturn;
        if(e.getExtendedKeyCode() == KeyEvent.VK_ENTER && pressed == true){
            // Enter押下時の動作は上位層で定義
            bReturn = false;
        }
        else{
            // Enter以外はJTableデフォルトの実装に任せる
            bReturn = super.processKeyBinding(ks, e, condition, pressed);
        }
        
        return bReturn;
    }
}
