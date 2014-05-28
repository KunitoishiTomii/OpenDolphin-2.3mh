package open.dolphin.impl.psearch;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import open.dolphin.infomodel.PatientModel;
import open.dolphin.table.ListTableSorter;

/**
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika, table sorter
 */
public class AddressTipsTable extends JTable {
    
    @Override
    public String getToolTipText(MouseEvent e) {
        
        //ListTableModel<PatientModel> model = (ListTableModel<PatientModel>) getModel();
        ListTableSorter sorter = (ListTableSorter) getModel();
        int row = rowAtPoint(e.getPoint());
        //PatientModel pvt = model.getObject(row);
        PatientModel pvt = (PatientModel) sorter.getObject(row);
        return pvt != null ? pvt.contactAddress() : null;
    }

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
