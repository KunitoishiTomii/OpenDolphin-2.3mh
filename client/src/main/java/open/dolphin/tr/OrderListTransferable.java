package open.dolphin.tr;

import java.awt.datatransfer.*;
import java.io.IOException;
import open.dolphin.infomodel.BundleDolphin;
import open.dolphin.infomodel.BundleMed;
import open.dolphin.infomodel.IModuleModel;
import open.dolphin.infomodel.ModuleModel;

/**
 * Transferable class of the PTrain.
 *
 * @author  Kazushi Minagawa, Digital Globe, Inc.
 */
public final class OrderListTransferable extends DolphinTransferable {

    /** Data Flavor of this class */
    public static DataFlavor orderListFlavor = new DataFlavor(OrderList.class, "Order List");

    public static final DataFlavor[] flavors = {OrderListTransferable.orderListFlavor, DataFlavor.stringFlavor};

    private final OrderList list;


    /** Creates new OrderListTransferable */
    public OrderListTransferable(OrderList list) {
        this.list = list;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
    	return flavors;
    }

    @Override
    public boolean isDataFlavorSupported( DataFlavor flavor ) {
        for (DataFlavor df : flavors) {
            if (df.equals(flavor)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object getTransferData(DataFlavor flavor)
	    throws UnsupportedFlavorException, IOException {

        if (orderListFlavor.equals(flavor)) {
            return list;
        } else if (DataFlavor.stringFlavor.equals(flavor)) {
            return getStampText();
        } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }

    @Override
    public String toString() {
        return "OrderList Transferable";
    }
    
    // スタンプ内容のテキストを作る
    private String getStampText() {
        
        ModuleModel[] stamps = list.getOrderList();

        StringBuilder sb = new StringBuilder();
        for (ModuleModel stamp : stamps) {
            IModuleModel model = stamp.getModel();
            if (model instanceof BundleMed) {
                BundleMed bm = (BundleMed) model;
                sb.append(bm.getAdminDisplayString2());
            } else if (model instanceof BundleDolphin) {
                BundleDolphin bd = (BundleDolphin) model;
                sb.append(bd.toString());
            }
        }
        return sb.toString();
    }
}