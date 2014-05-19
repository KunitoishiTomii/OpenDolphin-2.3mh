package open.dolphin.client;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import open.dolphin.infomodel.SchemaModel;

/**
 * スタンプのデータを保持するコンポーネントで TextPane に挿入される。
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public final class SchemaHolder extends AbstractComponentHolder {
    
    public static final String ATTRIBUTE_NAME = "schemaHolder";
    private static final int ICON_SIZE = 192;

    private final SchemaHolderFunction function;
    
    private SchemaModel schema;

    public SchemaHolder(KartePane kartePane, SchemaModel schema) {
        super(kartePane);
        function = SchemaHolderFunction.getInstance();
        function.setDeleteAction(SchemaHolder.this);
        this.schema = schema;
        setImageIcon();
    }
    
    public SchemaModel getSchema() {
        return schema;
    }
    
    @Override
    public void mabeShowPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
            SchemaHolder sh = (SchemaHolder) e.getComponent();
            function.setSelectedSchema(sh);
            function.showPopupMenu(e.getPoint());
        }
    }
    
    @Override
    public void edit() {
        function.setSelectedSchema(this);
        function.edit();
    }
    
    @Override
    public void propertyChange(PropertyChangeEvent e) {

        function.getLogger().debug("SchemaHolder propertyChange");

        if (SchemaEditor.VALUE_PROP.equals(e.getPropertyName())) {
            SchemaModel newSchema = (SchemaModel) e.getNewValue();
            if (newSchema == null) {
                return;
            }
            schema = newSchema;
            setImageIconLater();
            kartePane.setDirty(true);
        }
    }

    @Override
    public String getAttributeName() {
        return ATTRIBUTE_NAME;
    }
    
    private void setImageIcon() {
        Dimension d = new Dimension(ICON_SIZE, ICON_SIZE);
        ImageIcon adjusted = function.getAdjustedImage(schema.getIcon(), d);
        setIcon(adjusted);
    }

    public void setImageIconLater() {

        // こっちもinvokeLaterにしてみる
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                setImageIcon();
            }
        });
    }
}