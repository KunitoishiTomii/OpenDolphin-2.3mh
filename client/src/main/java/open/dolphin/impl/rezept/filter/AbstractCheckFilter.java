package open.dolphin.impl.rezept.filter;

import java.util.List;
import open.dolphin.impl.rezept.RezeptViewer;
import open.dolphin.impl.rezept.model.RE_Model;

/**
 * AbstractCheckFilter
 * 
 * @author masuda, Masuda Naika
 */
public abstract class AbstractCheckFilter {

    protected RezeptViewer viewer;
    
    public void setRezeptViewer(RezeptViewer viewer) {
        this.viewer = viewer;
    }
    
    public abstract List<CheckResult> doCheck(RE_Model reModel);
}
