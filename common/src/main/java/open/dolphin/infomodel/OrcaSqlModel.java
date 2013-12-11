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
    private List<List<String>> valuesList;
    private String errorMsg;
    
    public OrcaSqlModel() {
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    public void setSql(String sql) {
        this.sql = sql;
    }
    public void setValuesList(List<List<String>> list) {
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
    public List<List<String>> getValuesList() {
        return valuesList;
    }
    public String getErrorMessage() {
        return errorMsg;
    }
}
