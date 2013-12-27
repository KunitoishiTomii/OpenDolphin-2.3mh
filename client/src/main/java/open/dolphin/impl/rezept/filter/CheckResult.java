package open.dolphin.impl.rezept.filter;

/**
 * CheckResult
 * 
 * @author masuda, Masuda Naika
 */
public class CheckResult {
    
    private int result;
    private String filterName;
    private String msg;
    
    public void setResult(int result) {
        this.result = result;
    }
    public int getResult() {
        return result;
    }
    public void setFilterName(String filterName) {
        this.filterName = filterName;
    }
    public String getFilterName() {
        return filterName;
    }
    public void setMsg(String msg) {
        this.msg = msg;
    }
    public String getMsg() {
        return msg;
    }
}
