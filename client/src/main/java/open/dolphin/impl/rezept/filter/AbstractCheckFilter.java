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

    protected CheckResult createCheckResult(String msg, int resultId) {
        CheckResult result = new CheckResult();
        result.setFilterName(getFilterName());
        result.setResult(resultId);
        result.setMsg(msg);
        return result;
    }

    public void setRezeptViewer(RezeptViewer viewer) {
        this.viewer = viewer;
    }

    public abstract String getFilterName();

    public abstract List<CheckResult> doCheck(RE_Model reModel);
}
