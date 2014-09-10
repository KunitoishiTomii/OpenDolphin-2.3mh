package open.dolphin.tr;

import java.awt.Image;
import java.awt.datatransfer.*;
import java.io.IOException;
import open.dolphin.infomodel.SchemaModel;

/**
 * Transferable class of the Icon list.
 *
 * @author  Kazushi Minagawa, Digital Globe, Inc.
 */
public final class SchemaListTransferable extends DolphinTransferable {

    /** Data Flavor of this class */
    public static DataFlavor schemaListFlavor = new DataFlavor(SchemaList.class, "Schema List");

    public static final DataFlavor[] flavors = {SchemaListTransferable.schemaListFlavor, DataFlavor.imageFlavor};

    private final SchemaList list;

    /** Creates new SchemaListTransferable */
    public SchemaListTransferable(SchemaList list) {
        this.list = list;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
	return flavors;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
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

        if (schemaListFlavor.equals(flavor)) {
            return list;
        } else if (DataFlavor.imageFlavor.equals(flavor)) {
            return getSchemaImage();
        } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }

    @Override
    public String toString() {
        return "SchemaList Transferable";
    }
    
    private Image getSchemaImage() {
        
        // 一個目だけ
        if (list == null || list.getSchemaList() == null || list.getSchemaList().length == 0) {
            return null;
        }
        SchemaModel sm = list.getSchemaList()[0];
        
        return sm.getIcon().getImage();
    }
}