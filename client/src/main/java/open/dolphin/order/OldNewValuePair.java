package open.dolphin.order;

/**
 *
 * @author masuda, Masuda Naika
 */
public class OldNewValuePair {
    
    private final Object oldValue;
    private final Object newValue;
    
    public OldNewValuePair(Object oldValue, Object newValue) {
        this.oldValue = oldValue;
        this.newValue = newValue;
    }
    
    public Object getOldValue() {
        return oldValue;
    }
    
    public Object getNewValue() {
        return newValue;
    }
}
