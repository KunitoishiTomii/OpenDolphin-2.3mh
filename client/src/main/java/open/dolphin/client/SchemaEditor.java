package open.dolphin.client;

import java.beans.PropertyChangeListener;
import open.dolphin.infomodel.SchemaModel;

/**
 *
 * @author Kazushi Minagawa.
 */
public interface SchemaEditor {
    
    public static final String VALUE_PROP = "imageProp";
    
    public void setEditable(boolean b);
    
    public void setSchema(SchemaModel model);
    
    public void start();
    
    public void addPropertyChangeListener(PropertyChangeListener l);
    
    public void removePropertyChangeListener(PropertyChangeListener l);

}
