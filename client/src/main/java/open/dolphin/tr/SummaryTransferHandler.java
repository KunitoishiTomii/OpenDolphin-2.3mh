package open.dolphin.tr;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import javax.swing.ActionMap;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.text.JTextComponent;
import open.dolphin.client.GUIConst;
import open.dolphin.client.KartePane;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.infomodel.ModuleModel;

/**
 * サマリーのTransferHandler StampもSchemaもDrop可能
 * スタンプ箱からは不可
 *
 * @author masuda, Masuda Naika
 */
public class SummaryTransferHandler extends SOATransferHandler {

    private static final SummaryTransferHandler instance;

    static {
        instance = new SummaryTransferHandler();
    }

    private SummaryTransferHandler() {
    }

    public static SummaryTransferHandler getInstance() {
        return instance;
    }

    // KartePaneにTransferableをインポートする
    @Override
    public boolean importData(TransferSupport support) {

        if (!canImport(support)) {
            importDataFailed();
            return false;
        }

        Transferable tr = support.getTransferable();
        JTextComponent dest = (JTextComponent) support.getComponent();

        boolean imported = false;

        KartePane destPane = getKartePane(dest);
        if (tr.isDataFlavorSupported(OrderListTransferable.orderListFlavor)) {
            // KartePaneからのオーダスタンプをインポートする P
            imported = doStampDrop(tr, destPane);

        } else if (tr.isDataFlavorSupported(stringFlavor)) {
            // テキストをインポートする SOA/P
            imported = doTextDrop(tr, dest);

        } else if (tr.isDataFlavorSupported(ImageEntryTransferable.imageEntryFlavor)) {
            // シェーマボックスからのDnDをインポートする SOA
            imported = doImageEntryDrop(tr, destPane);

        } else if (tr.isDataFlavorSupported(SchemaListTransferable.schemaListFlavor)) {
            // Paneからのシェーマをインポートする SOA
            imported = doSchemaDrop(tr, destPane);

        } else if (tr.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            // 画像ファイルのドロップをインポートする SOA
            imported = doFileDrop(tr, destPane);

        } else if (tr.isDataFlavorSupported(DataFlavor.imageFlavor)) {
            // クリップボードの画像をインポートする SOA
            imported = doClippedImageDrop(tr, destPane);
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
        
        JTextComponent tc = (JTextComponent) support.getComponent();

        // 選択範囲内にDnDならtrue
        if (isDndOntoSelectedText(support)) {
            return false;
        }
        if (!tc.isEditable()) {
            return false;
        }
        
        if (hasFlavor(support.getTransferable())) {
            return true;
        }
        return false;
    }
    
    /**
     * Flavorリストのなかに受け入れられものがあるかどうかを返す。
     */
    private boolean hasFlavor(Transferable tr) {
        
        // OrderStamp List OK
        if (tr.isDataFlavorSupported(OrderListTransferable.orderListFlavor)) {
            return true;
        }
        // String ok
        if (tr.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            return true;
        }
        // Schema OK
        if (tr.isDataFlavorSupported(SchemaListTransferable.schemaListFlavor)) {
            return true;
        }
        // Image OK
        if (tr.isDataFlavorSupported(ImageEntryTransferable.imageEntryFlavor)) {
            return true;
        }
        // File OK
        if (tr.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            return true;
        }
        // クリップボードの画像 OK
        if (tr.isDataFlavorSupported(DataFlavor.imageFlavor)) {
            return true;
        }

        return false;
    }

    private boolean canPaste(KartePane soaPane) {
        
        if (!soaPane.getTextPane().isEditable()) {
            return false;
        }
        Transferable tr = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
        if (tr == null) {
            return false;
        }
        
        return hasFlavor(tr);
    }
    
    /**
     * DropされたStamp(ModuleModel)をインポートする。
     * @param tr Transferable
     * @return インポートに成功した時 true
     */
    private boolean doStampDrop(Transferable tr, KartePane kartePane) {

        try {
            // スタンプのリストを取得する
            OrderList list = (OrderList) tr.getTransferData(OrderListTransferable.orderListFlavor);
            ModuleModel[] stamps = list.getOrderList();

//masuda^   スタンプコピー時に別患者のカルテかどうかをチェックする
            boolean differentKarte = false;
            long destKarteId = kartePane.getParent().getContext().getKarte().getId();
            for (ModuleModel mm : stamps) {
                if (mm.getKarteBean() == null) {
                    continue;
                }
                long karteId = mm.getKarteBean().getId();
                if (karteId != destKarteId) {
                    differentKarte = true;
                    break;
                }
            }
            if (differentKarte) {
                String[] options = {"取消", "無視"};
                String msg = "異なる患者カルテにスタンプをコピーしようとしています。\n継続しますか？";
                int val = JOptionPane.showOptionDialog(kartePane.getParent().getContext().getFrame(), msg, "スタンプコピー",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
                if (val != 1) {
                    // 取り消し
                    return false;
                }
            }
//masuda$
            // pPaneにスタンプを挿入する
            for (ModuleModel stamp : stamps) {
                // roleをSOAに変更しておく
                stamp.getModuleInfoBean().setStampRole(IInfoModel.ROLE_SOA);
                kartePane.stamp(stamp);
            }

            return true;

        } catch (UnsupportedFlavorException | IOException e) {
            e.printStackTrace(System.err);
        }
        return false;
    }
    
    @Override
    public void enter(JComponent jc, ActionMap map) {

        KartePane pPane = getKartePane((JTextComponent) jc);
        if (pPane.getTextPane().isEditable()) {
            map.get(GUIConst.ACTION_PASTE).setEnabled(canPaste(pPane));
            map.get(GUIConst.ACTION_INSERT_TEXT).setEnabled(true);
            map.get(GUIConst.ACTION_INSERT_STAMP).setEnabled(true);
            //map.get(GUIConst.ACTION_INSERT_SCHEMA).setEnabled(true);
        }
    }
}
