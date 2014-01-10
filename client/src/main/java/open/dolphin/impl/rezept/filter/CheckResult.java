package open.dolphin.impl.rezept.filter;

/**
 * CheckResult
 * 
 * @author masuda, Masuda Naika
 */
public class CheckResult {
    
    public static final int CHECK_NO_ERROR = 0;
    public static final int CHECK_INFO = 1;
    public static final int CHECK_WARNING = 2;
    public static final int CHECK_ERROR = 3;
    
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
