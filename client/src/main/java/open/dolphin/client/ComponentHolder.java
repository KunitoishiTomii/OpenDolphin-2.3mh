package open.dolphin.client;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.text.Position;

/**
 * IComponentHolder
 *
 * @author Kauzshi Minagawa
 * @author modified by masuda, Masuda Naika
 */
public interface ComponentHolder extends PropertyChangeListener {
    
    public KartePane getKartePane();
    
    public boolean isSelected();
    
    public void setSelected(boolean b);
    
    public void edit();
    
    public int getStartOffset();
    
    public void setStartPosition(Position start);
    
    public String getAttributeName();
    
    @Override
    public void propertyChange(PropertyChangeEvent e);
}