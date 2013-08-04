package open.dolphin.tr;

import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import javax.swing.JComponent;
import javax.swing.JTable;
import open.dolphin.infomodel.RegisteredDiagnosisModel;
import open.dolphin.table.ListTableModel;


/**
 * RegisteredDiagnosisTransferHandler　病名エディタ
 *
 * @author Minagawa,Kazushi
 * @author modified by masuda, Masuda Naika, from 1.4
 */
public class RegisteredDiagnosisTransferHandler extends DolphinTransferHandler {
    
    private static final DataFlavor FLAVOR = RegisteredDiagnosisTransferable.registeredDiagnosisFlavor;
    private int fromIndex;

    @Override
    protected Transferable createTransferable(JComponent src) {
        
        JTable sourceTable = (JTable) src;
        fromIndex = sourceTable.getSelectedRow();
        if (fromIndex < 0) {
            return null;
        }
        
        startTransfer(src);
        
        ListTableModel<RegisteredDiagnosisModel> tableModel = (ListTableModel<RegisteredDiagnosisModel>) sourceTable.getModel();
        RegisteredDiagnosisModel rd = tableModel.getObject(fromIndex);
        
        // ドラッグ中のイメージを設定する
        Image image = createDragImage(rd.getDiagnosisName(), sourceTable.getFont());
        setDragImage(image);
        
        return new RegisteredDiagnosisTransferable(rd);
    }

    @Override
    public boolean importData(TransferSupport support) {
        
        JTable dropTable = (JTable) support.getComponent();
        if (!canImport(support) || dropTable != srcComponent) {
            fromIndex = -1;
            importDataFailed();
            return false;
        }
        
        try {
            ListTableModel<RegisteredDiagnosisModel> tableModel = (ListTableModel<RegisteredDiagnosisModel>) dropTable.getModel();
            JTable.DropLocation dropLocation = (JTable.DropLocation) support.getDropLocation();
            int toIndex = dropLocation.getRow();
            tableModel.moveRow(fromIndex, (toIndex > fromIndex) ? --toIndex : toIndex);
            dropTable.getSelectionModel().setSelectionInterval(toIndex, toIndex);
            importDataSuccess(dropTable);
            return true;
            
        } catch (Exception ioe) {
            fromIndex = -1;
            importDataFailed();
            return false;
        }
    }

    @Override
    protected void exportDone(JComponent c, Transferable data, int action) {
        fromIndex = -1;
        endTransfer();
    }
    
    @Override
    public int getSourceActions(JComponent c) {
        return MOVE;
    }
    
    @Override
    public boolean canImport(TransferSupport support) {
        return support.isDrop() 
                && support.isDataFlavorSupported(FLAVOR);
    }
}
