
package open.dolphin.infomodel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.persistence.*;

/**
 * 定期処方薬の延滞茶
 * 
 * @author masuda, Masuda Naika
 */
@Entity
@Table(name = "msd_routineMed")
public class RoutineMedModel implements Serializable, Comparable, Cloneable {
    
    @Id 
    @GeneratedValue(strategy=GenerationType.AUTO)
    private long id;
    
    @Column(nullable=false)
    private long karteId;
    
    @Column(nullable=false)
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date registDate;
    
    private Boolean bookmark = false;
    
    private String memo;
    
    @JsonDeserialize(contentAs=ModuleModel.class)
    //@ElementCollection
    //@CollectionTable(name="msd_routineMed_moduleList")
    //@OneToMany(fetch=FetchType.LAZY)    // PostgresだとEAGERだめ。MySQLは大丈夫
    @Transient
    private List<ModuleModel> moduleList;
    
    // OneToManyだと重複を許してくれないので…　ダサいｗ
    private String moduleIds;
    
    @JsonIgnore
    @Transient
    private String status;
    
    public RoutineMedModel() {
    }
    
    public long getId() {
        return id;
    }
    
    public long getKarteId() {
        return karteId;
    }
    
    public Date getRegistDate() {
        return registDate;
    }

    public boolean getBookmark() {
        return bookmark;
    }

    public String getMemo() {
        return memo;
    }
    
    public List<ModuleModel> getModuleList() {
        return moduleList;
    }
    
    public String getModuleIds() {
        return moduleIds;
    }
    
    public void setId(long id) {
        this.id = id;
    }

    public void setKarteId(long karteId) {
        this.karteId = karteId;
    }

    public void setRegistDate(Date registDate) {
        this.registDate = registDate;
    }

    public void setBookmark(boolean b) {
        bookmark = b;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public void setModuleList(List<ModuleModel> list) {
        moduleList = list;
    }
    
    public void setModuleIds(String ids) {
        moduleIds = ids;
    }
    
    // for display
    public String getRegistDateStr() {
        final SimpleDateFormat sdf = new SimpleDateFormat(IInfoModel.DATE_WITHOUT_TIME);
        return sdf.format(registDate);
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public int compareTo(Object o) {
        RoutineMedModel target = (RoutineMedModel) o;
        return registDate.compareTo(target.getRegistDate());
    }

    @Override
    public RoutineMedModel clone() throws CloneNotSupportedException {
        
        RoutineMedModel ret = new RoutineMedModel();
        ret.setId(id);
        ret.setKarteId(karteId);
        ret.setRegistDate(new Date(registDate.getTime()));
        ret.setBookmark(bookmark);
        ret.setMemo(memo);
        ret.setModuleIds(moduleIds);
        ret.setStatus(status);
        ret.setModuleList(moduleList);
        return ret;
    }
}
