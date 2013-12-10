package open.dolphin.tr;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JTable;
import open.dolphin.client.DiagnosisDocument;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.infomodel.ModuleInfoBean;
import open.dolphin.infomodel.RegisteredDiagnosisModel;
import open.dolphin.stampbox.StampTreeNode;
import open.dolphin.table.ListTableSorter;

/**
 * DiagnosisTransferHandler
 *
 * @author Minagawa,Kazushi
 *
 */
public class DiagnosisTransferHandler extends DolphinTransferHandler {
    
    private static final DataFlavor FLAVOR = LocalStampTreeNodeTransferable.localStampTreeNodeFlavor;
    private final DiagnosisDocument parent;
    private int action;

    public DiagnosisTransferHandler(DiagnosisDocument parent) {
        super();
        this.parent = parent;
    }

    public int getTransferAction() {
        return action;
    }
    
    @Override
    protected Transferable createTransferable(JComponent src) {
        
        action = 0;
        JTable sourceTable = (JTable) src;
        
        int[] selectedRows = sourceTable.getSelectedRows();
        int size = selectedRows.length;
        if (size == 0) {
            return null;
        }
        
        startTransfer(src);
        
        ListTableSorter sorter = (ListTableSorter) sourceTable.getModel();
        RegisteredDiagnosisModel[] rds = new RegisteredDiagnosisModel[size];
        List<String> strList = new ArrayList<>();
        for (int i = 0; i < size; ++i) {
            RegisteredDiagnosisModel rd = (RegisteredDiagnosisModel) sorter.getObject(selectedRows[i]);
            rds[i] = rd;
            strList.add(rd.getDiagnosisName());
        }
        
        // ドラッグ中のイメージを設定する
        Image image = createDragImage(strList, sourceTable.getFont());
        setDragImage(image);

        return new InfoModelTransferable(rds);
    }

    @Override
    public boolean importData(TransferSupport support) {

        if (!canImport(support)) {
            importDataFailed();
            return false;
        }
        
        try {
//masuda^   sorterもあるしてっぺん固定
/*
             // 病名の挿入位置を決めておく
             JTable dropTable = (JTable) c;
             int index = dropTable.getSelectedRow();
             if (index < 0) {
                index = 0;
             }
*/
//masuda$
            if (support.isDrop()) {
                action = support.getDropAction();
            }
            int index = 0;

            // Dropされたノードを取得する
            Transferable t = support.getTransferable();
            StampTreeNode droppedNode = (StampTreeNode) t.getTransferData(FLAVOR);

            // Import するイストを生成する
            List<ModuleInfoBean> importList = new ArrayList<>();

            // 葉の場合
            if (droppedNode.isLeaf()) {
                ModuleInfoBean stampInfo = droppedNode.getStampInfo();
                if (stampInfo.getEntity().equals(IInfoModel.ENTITY_DIAGNOSIS)) {
                    if (stampInfo.isSerialized()) {
                        importList.add(stampInfo);
                    } else {
                        parent.openEditor2();
                        importDataSuccess(support.getComponent());
                        return true;
                    }

                } else {
                    Toolkit.getDefaultToolkit().beep();
                    importDataFailed();
                    return false;
                }

            } else {
                // Dropされたノードの葉を列挙する
                Enumeration e = droppedNode.preorderEnumeration();
                while (e.hasMoreElements()) {
                    StampTreeNode node = (StampTreeNode) e.nextElement();
                    if (node.isLeaf()) {
                        ModuleInfoBean stampInfo = node.getStampInfo();
                        if (stampInfo.isSerialized() && (stampInfo.getEntity().equals(IInfoModel.ENTITY_DIAGNOSIS))) {
                            importList.add(stampInfo);
                        }
                    }
                }
            }
            // まとめてデータベースからフェッチしインポートする
            if (!importList.isEmpty()) {
                parent.importStampList(importList, index);
                importDataSuccess(support.getComponent());
                return true;

            } else {
                importDataFailed();
                return false;
            }

        } catch (Exception ioe) {
            ioe.printStackTrace(System.err);
        }

        importDataFailed();
        return false;
    }

    @Override
    public boolean canImport(TransferSupport support) {
        return support.isDataFlavorSupported(FLAVOR);
    }
}
