package open.dolphin.tr;

import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import javax.swing.JComponent;
import javax.swing.TransferHandler;
import open.dolphin.infomodel.ModelUtils;
import open.dolphin.infomodel.PatientModel;
import open.dolphin.infomodel.SimpleAddressModel;

/**
 * 基本情報をドラッグでコピー
 * 
 * @author masuda, Masuda Naika
 */
public class BasicInfoTransferHandler extends TransferHandler {
    
    private final PatientModel pm;
    
    public BasicInfoTransferHandler(PatientModel pm) {
        this.pm = pm;
    }
    
    @Override
    protected Transferable createTransferable(JComponent c) {
        
        StringBuilder sb = new StringBuilder();
        sb.append("患者ID：").append(pm.getPatientId()).append("\t");
        sb.append("患者名：").append(pm.getFullName()).append("  ").append(pm.getGenderDesc()).append("\n");
        sb.append("年齢：").append(ModelUtils.getAgeBirthday2(pm.getBirthday())).append("\n");
        SimpleAddressModel addr = pm.getAddress();
        if (addr != null) {
            sb.append("住所：").append(addr.getZipCode()).append(" ").append(addr.getAddress()).append("\n");
        }
        
        return new StringSelection(sb.toString());
    }

    @Override
    public int getSourceActions(JComponent c) {
        return COPY;
    }
    
    @Override
    public boolean canImport(TransferSupport support) {
        return false;
    }
}
