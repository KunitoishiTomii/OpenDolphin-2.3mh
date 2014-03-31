package open.dolphin.tr;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import javax.swing.ActionMap;
import javax.swing.JComponent;
import javax.swing.text.JTextComponent;
import open.dolphin.client.GUIConst;

/**
 * TextComponentTransferHandler (renamed from BundleTransferHandler)
 * @author Minagawa,Kazushi. Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class TextComponentTransferHandler extends AbstractKarteTransferHandler {

    private static final TextComponentTransferHandler instance;

    static {
        instance = new TextComponentTransferHandler();
    }

    private TextComponentTransferHandler() {
    }

    public static TextComponentTransferHandler getInstance() {
        return instance;
    }


    @Override
    protected Transferable createTransferable(JComponent src) {

        JTextComponent source = (JTextComponent) src;

        // テキストの選択範囲を記憶
        startTransfer(src);
        boolean b = setSelectedTextArea(source);
        if (!b) {
            endTransfer();
            return null;
        }

        String data = source.getSelectedText();
        return new StringSelection(data);
    }

    @Override
    public boolean importData(TransferSupport support) {

        if (!canImport(support)) {
            importDataFailed();
            return false;
        }

        Transferable tr = support.getTransferable();
        JTextComponent dest = (JTextComponent) support.getComponent();

        boolean imported = false;

        if (tr.isDataFlavorSupported(stringFlavor)) {
            // テキストをインポートする SOA/P
            imported = doTextDrop(tr, dest);
        }
        
        if (imported) {
            importDataSuccess(dest);
        } else {
            importDataFailed();
        }

        return imported;
    }

    /**
     * インポート可能かどうかを返す。
     */
    @Override
    public boolean canImport(TransferSupport support) {
        
        // 選択範囲内にDnDならtrue
        if (isDndOntoSelectedText(support)){
            return false;
        }

        JTextComponent tc = (JTextComponent) support.getComponent();
        boolean canImport = true;
        canImport = canImport && tc.isEditable();
        canImport = canImport && support.isDataFlavorSupported(DataFlavor.stringFlavor);

        return canImport;
    }
    
    @Override
    public void enter(JComponent jc, ActionMap map) {

        JTextComponent tc = (JTextComponent) jc;
        map.get(GUIConst.ACTION_CUT).setEnabled(tc.isEditable());
        map.get(GUIConst.ACTION_COPY).setEnabled(true);
        boolean pasteOk = (tc.isEditable() && canPaste(tc));
        map.get(GUIConst.ACTION_PASTE).setEnabled(pasteOk);
    }

    @Override
    public void exit(JComponent jc) {
    }

    private boolean canPaste(JTextComponent tc) {

        if (!tc.isEditable()) {
            return false;
        }
        Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
        if (t == null) {
            return false;
        }

        boolean canImport = true;
        canImport = canImport && t.isDataFlavorSupported(DataFlavor.stringFlavor);
        return canImport;
    }
}
