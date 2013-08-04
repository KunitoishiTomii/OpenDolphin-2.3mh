package open.dolphin.tr;

import java.awt.Image;
import java.awt.datatransfer.Transferable;
import javax.swing.JComponent;
import javax.swing.JList;
import open.dolphin.client.ImageEntry;

/**
 * ImageEntryTransferHandler
 *
 * @author masuda, Masuda Naika
 */
public class ImageEntryTransferHandler extends DolphinTransferHandler {
    
    private static ImageEntryTransferHandler instance;

    static {
        instance = new ImageEntryTransferHandler();
    }

    private ImageEntryTransferHandler() {
    }
    
    public static ImageEntryTransferHandler getInstance() {
        return instance;
    }

    @Override
    protected Transferable createTransferable(JComponent src) {
        
        JList imageList = (JList) src;
        ImageEntry entry = (ImageEntry) imageList.getSelectedValue();
        
        if (entry == null) {
            return null;
        }
        
        startTransfer(src);
        
        // ドラッグ中のイメージを設定する
        Image image = entry.getImageIcon().getImage();
        setDragImage(image);
        
        Transferable tr = new ImageEntryTransferable(entry);
        return tr;
    }

    @Override
    public boolean canImport(TransferSupport support) {
        return false;
    }

    @Override
    public boolean importData(TransferSupport support) {
        importDataFailed();
        return false;
    }
}
