package open.dolphin.stampbox;

import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComponent;
import javax.swing.TransferHandler;
import javax.swing.TransferHandler.TransferSupport;

/**
 *
 * @author Kazushi Minagawa.
 */
public class TransferAction implements ActionListener {
    
    private final JComponent comp;
    private final TransferHandler handler;
    private final Transferable tr;
    
    public TransferAction(JComponent comp, TransferHandler handler, Transferable tr) {
        this.comp = comp;
        this.handler = handler;
        this.tr = tr;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
//masuda^   use TransferSupport
        //handler.importData(comp, tr);
        handler.importData(new TransferSupport(comp, tr));
//masuda$
    }
}
