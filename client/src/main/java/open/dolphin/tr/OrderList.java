package open.dolphin.tr;

import java.io.Serializable;
import open.dolphin.infomodel.ModuleModel;

/**
 * OrderList
 *
 * @author  Kazushi Minagawa
 */
public final class OrderList implements Serializable {

    //private static final long serialVersionUID = -6049175115811888229L;

    private final ModuleModel[] orderList;

    public OrderList(ModuleModel[] stamp) {
    	orderList = stamp;
    }

    public ModuleModel[] getOrderList() {
    	return orderList;
    }

}