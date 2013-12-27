package open.dolphin.impl.rezept.filter;

import java.util.List;
import open.dolphin.impl.rezept.model.RE_Model;

/**
 * ICheckFilter
 * 
 * @author masuda, Masuda Naika
 */
public interface ICheckFilter {
    
    public static final int CHECK_NO_ERROR = 0;
    public static final int CHECK_INFO = 1;
    public static final int CHECK_WARNING = 2;
    public static final int CHECK_ERROR = 3;
    
    public List<CheckResult> doCheck(List<RE_Model> reModelList);
}
