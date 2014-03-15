package open.dolphin.infomodel;

import java.io.Serializable;
import java.util.List;

/**
 * サーバー経由ORCA SQLモデル
 * @author masuda, Masuda Naika
 */
public class OrcaSqlModel implements Serializable {
    
    private String url;
    private String sql;
    private Object[] params;
    private List<String[]> valuesList;
    private String errorMsg;
    
    public OrcaSqlModel() {
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    public void setSql(String sql) {
        this.sql = sql;
    }
    
    public void setParams(Object[] params) {
        this.params = params;
    }
    public Object[] getParams() {
        return params;
    }
    
    public void setValuesList(List<String[]> list) {
        valuesList = list;
    }
    public void setErrorMessage(String msg) {
        errorMsg = msg;
    }
    
    public String getUrl() {
        return url;
    }
    public String getSql() {
        return sql;
    }
    public List<String[]> getValuesList() {
        return valuesList;
    }
    public String getErrorMessage() {
        return errorMsg;
    }
}
